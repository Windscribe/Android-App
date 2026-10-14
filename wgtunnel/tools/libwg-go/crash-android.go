/* SPDX-License-Identifier: Apache-2.0
 *
 * Copyright © 2024 Windscribe Limited. All Rights Reserved.
 */

package main

// #cgo LDFLAGS: -llog
// #include <android/log.h>
// int wgStartCrashReader(int pipe_fd);
import "C"

import (
	"os"
	"runtime/debug"
	"sync"

	"golang.org/x/sys/unix"
)

var (
	crashOutputMu sync.Mutex
	// Keeps the write end alive; its finalizer would close the descriptor the
	// runtime writes to.
	crashPipe *os.File
)

func logCrash(format string, args ...interface{}) {
	AndroidLogger{
		level: C.ANDROID_LOG_ERROR,
		tag:   cstring("WireGuard/GoBackend/Crash"),
	}.Printf(format, args...)
}

// wgSetCrashOutput routes the Go runtime's fatal error line into the process
// abort message, where debuggerd puts it in the tombstone and the Play Console
// displays it. By default it goes only to logcat, which a crash report cannot
// reach, so every fatal error in this library arrives as the same single frame in
// runtime.raise with no way to tell them apart.
//
// Only the "panic:" / "fatal error:" line gets here. gwrite reaches logcat via
// write_err_android.go's writeErr, which never calls writeErrData and so never
// reaches this pipe; the header arrives through writeErrStr. That line is what
// identifies the crash - the goroutine stack stays in logcat.
//
// Returns 0 on success or if already installed, -1 otherwise.
//
//export wgSetCrashOutput
func wgSetCrashOutput() int32 {
	crashOutputMu.Lock()
	defer crashOutputMu.Unlock()

	if crashPipe != nil {
		return 0
	}

	var fds [2]int
	if err := unix.Pipe2(fds[:], unix.O_CLOEXEC); err != nil {
		logCrash("Could not create crash pipe: %v", err)
		return -1
	}
	read, write := fds[0], fds[1]

	if C.wgStartCrashReader(C.int(read)) != 0 {
		unix.Close(read)
		unix.Close(write)
		logCrash("Could not start crash reader thread")
		return -1
	}

	f := os.NewFile(uintptr(write), "go-crash-pipe")

	if err := debug.SetCrashOutput(f, debug.CrashOptions{}); err != nil {
		logCrash("SetCrashOutput failed: %v", err)
		return -1
	}
	// Only once the runtime holds it: a non-nil crashPipe means crash output is wired.
	crashPipe = f

	// After SetCrashOutput, which calls Fd() and restores blocking mode. A
	// blocking write into a full pipe would stall the dying thread and turn the
	// crash into a hang. O_NONBLOCK lives on the file description, so this covers
	// the runtime's dup of it.
	if err := unix.SetNonblock(write, true); err != nil {
		logCrash("Could not set crash pipe non-blocking: %v", err)
	}

	return 0
}
