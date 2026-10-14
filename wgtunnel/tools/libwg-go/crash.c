/* SPDX-License-Identifier: Apache-2.0
 *
 * Copyright © 2024 Windscribe Limited. All Rights Reserved.
 */

#include <android/set_abort_message.h>
#include <errno.h>
#include <fcntl.h>
#include <poll.h>
#include <pthread.h>
#include <signal.h>
#include <stdlib.h>
#include <stddef.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

#define ABORT_MSG_CAPACITY 4096
#define READ_CHUNK 4000

/* How long the signal handler yields to the drain thread before giving up. */
#define ABORT_HANDOFF_MAX_MS 500

/* Bounded extra draining so the message carries the goroutine stack, not just the
 * first line. Worst case DRAIN_ROUNDS * DRAIN_QUIET_MS, inside the handoff. */
#define DRAIN_ROUNDS 16
#define DRAIN_QUIET_MS 25

static struct sigaction previous_sigabrt;
/* Set once the runtime has raised, which it only does after writing the whole
 * report - so it doubles as "no more output is coming". */
static volatile sig_atomic_t runtime_raised;
static char abort_msg[ABORT_MSG_CAPACITY];
static size_t abort_msg_len;
static int crash_pipe_fd;

static void append_abort_msg(const char *data, size_t len)
{
	size_t space = ABORT_MSG_CAPACITY - 1 - abort_msg_len;

	if (space == 0)
		return;
	if (len > space)
		len = space;
	memcpy(abort_msg + abort_msg_len, data, len);
	abort_msg_len += len;
	abort_msg[abort_msg_len] = '\0';
}

/* Has to be a plain pthread: runtime.fatalpanic calls freezetheworld() before it
 * prints, so no goroutine will be scheduled again. */
static void *crash_reader(void *unused)
{
	char chunk[READ_CHUNK];
	int rounds;

	(void)unused;
	pthread_setname_np(pthread_self(), "go-crash-drain");

	/* Blocks until the runtime writes a report. Nothing else is ever written to
	 * this pipe, so anything arriving here means the process is dying. */
	for (;;) {
		ssize_t n = read(crash_pipe_fd, chunk, sizeof(chunk));

		if (n < 0) {
			if (errno == EINTR)
				continue;
			return NULL;
		}
		if (n == 0)
			return NULL;
		append_abort_msg(chunk, (size_t)n);
		break;
	}

	/* Until the runtime has raised and nothing more is queued. A quiet pipe alone is
	 * not enough: formatting the goroutine stack leaves a gap after the first line. */
	for (rounds = 0; rounds < DRAIN_ROUNDS && abort_msg_len < ABORT_MSG_CAPACITY - 1; rounds++) {
		struct pollfd pfd;
		ssize_t n;

		pfd.fd = crash_pipe_fd;
		pfd.events = POLLIN;
		pfd.revents = 0;
		if (poll(&pfd, 1, DRAIN_QUIET_MS) > 0 && (pfd.revents & POLLIN) != 0) {
			n = read(crash_pipe_fd, chunk, sizeof(chunk));
			if (n <= 0)
				break;
			append_abort_msg(chunk, (size_t)n);
			continue;
		}
		if (runtime_raised)
			break;
	}

	android_set_abort_message(abort_msg);

	/* Normal thread context, handler removed first: an abort() from inside a signal
	 * handler loses the message. The runtime logs the full report under tag "Go". */
	sigaction(SIGABRT, &previous_sigabrt, NULL);
	abort();
	return NULL;
}

/* Go writes its whole report before raising, so the drain thread has data waiting and
 * only needs scheduling. Sleeping yields to it so it aborts from normal thread context,
 * the only way the message survives.
 *
 * Safe to take this handler: sigInstallGoHandler skips every non-_SigPanic signal in
 * c-archive and c-shared builds. */
static void await_drain_then_die(int sig, siginfo_t *info, void *ctx)
{
	int i;

	(void)sig;
	(void)ctx;

	/* A real abort() from elsewhere in the process already carries its own
	 * message. Don't delay it. */
	if (info == NULL || info->si_code == SI_QUEUE) {
		sigaction(SIGABRT, &previous_sigabrt, NULL);
		raise(SIGABRT);
		return;
	}

	runtime_raised = 1;

	for (i = 0; i < ABORT_HANDOFF_MAX_MS; i++) {
		struct timespec one_ms = { 0, 1000000 };

		nanosleep(&one_ms, NULL);
	}

	/* The drain never got there; die without a message rather than hang. */
	sigaction(SIGABRT, &previous_sigabrt, NULL);
	raise(SIGABRT);
}

/* Takes ownership of pipe_fd, which is deliberately never closed: a broken pipe
 * would stall the runtime mid-report. */
int wgStartCrashReader(int pipe_fd)
{
	pthread_t thread;
	struct sigaction sa;

	crash_pipe_fd = pipe_fd;
	fcntl(pipe_fd, F_SETPIPE_SZ, 1 << 20);
	if (pthread_create(&thread, NULL, crash_reader, NULL) != 0)
		return -1;
	pthread_detach(thread);

	memset(&sa, 0, sizeof(sa));
	sa.sa_sigaction = await_drain_then_die;
	/* SA_ONSTACK because this arrives on a Go thread, which runs on an altstack. */
	sa.sa_flags = SA_SIGINFO | SA_ONSTACK;
	sigemptyset(&sa.sa_mask);
	sigaction(SIGABRT, &sa, &previous_sigabrt);

	return 0;
}
