# VPN core refactor — review triage

Reviewer findings on `vpn-core-refactor`, with the verdict from reading the code
and where each was fixed. Delete this file once the branch has merged and the
items are closed.

All fifteen were checked against the code. All fifteen were real, and all
fifteen are fixed.

One thing is still owed: the saturation test for #2 needs real descriptors, so it
has to be instrumented, and there is no androidTest harness for the mux yet.

---

## 1. Concurrent TUN replacement can move the mux onto a closed descriptor

- **File:** `base/src/main/java/com/windscribe/vpn/backend/tunnel/WindscribeVpnService.kt`
- **Severity:** Blocker
- **Status:** fixed

**Verified:** `onTunReplaced` (`TunnelManager.kt:420`) is not `@Synchronized`, and `requestTun` releases `tunLock` before the callback (`WindscribeVpnService.kt:605`). `OpenVPNService.java:883` and `CharonVpnService.java:1134/1193/1230` call `host.requestTun()` directly, bypassing TunnelManager's monitor, so two callbacks really can be in flight. Two windows, not one: B also closes T0 while the mux is still pumping it.

`tunLock` serializes `establish()`, but the `onTunReplaced` callback runs after
the lock is released and retirement uses one global `previousTun` slot. If A
establishes, B establishes before A notifies, and B's callback wins the race, B
closes A's descriptor; A's later callback then moves the mux back onto it.
OpenVPN and charon invoke `requestTun()` from native-owned management threads
while TunnelManager can also replace during a handover, so this is a real
multi-origin path.

Prescription: treat establish → mux switch → retirement of that exact
predecessor as one serialized, generation-tagged transaction; pass the
predecessor/generation through the callback, and test out-of-order A/B
notification.

**Fixed:** every establish is stamped with a generation under `tunLock`, and the
descriptor it superseded is filed under that generation rather than in one global
slot. `requestTun` no longer closes anything — the old code closed the *older*
predecessor while a pump could still be on it, which was the second window in
this finding. `onTunReplaced` and `releasePreviousTun` carry the generation, and
`onTunReplaced` is `@Synchronized` so check, switch and retire are one
transaction; a generation older than one already applied is dropped, and its
predecessor still released, since nothing else would.

`TunnelManager` turned out to be the only implementor, so the interface change
never reached vendored StrongSwan or ics-openvpn. `TunReplacementOrderingTest`
asserts the packet path itself rather than the release — with no mux both branches
look identical — and two of its three fail without the ordering guard.

---

## 2. Pump writes are blocking, so bounded quiesce does not hold

- **File:** `common/src/main/java/com/windscribe/common/tunnel/PacketMux.java`
- **Severity:** Blocker
- **Status:** fixed

**Verified:** `Os.socketpair(AF_UNIX, SOCK_SEQPACKET, 0, ...)` (`PacketMux.java:232`) sets no `SOCK_NONBLOCK`, and nothing calls `fcntl` to add `O_NONBLOCK`. The TUN uses `builder.setBlocking(config.blocking)`. So `Os.write` parks, the EAGAIN branch is dead, and `quiesce()` proceeds after its timeout.

`Os.socketpair()` creates blocking endpoints and the TUN is established with
`setBlocking(true)`; nothing switches either destination to non-blocking. A full
engine/TUN queue parks a pump inside `Os.write()`, making the EAGAIN branch
unreachable and preventing the wakeup pipe from releasing it. `quiesce()` then
times out and proceeds to close or reuse a descriptor the pump may still hold —
the recycled-fd race this design exists to remove.

Prescription: make every mux-owned write path cancellable or non-blocking, and
add a saturation test that requires both acknowledgements before close/swap.

**Fixed:** `writeTo` already had an EAGAIN branch — it was written for non-blocking
descriptors that were never made non-blocking. `attachEngine` now sets
`O_NONBLOCK` on the mux's own side of the socketpair; `socketpair()` returns two
independent open file descriptions, so the engine keeps the blocking end it
expects. The TUN is established with `setBlocking(false)`, which made
`TunConfig.blocking` a dead knob that was always `true` — removed, rather than
left saying the opposite of what is true.

**Not covered:** the saturation test needs real descriptors, so it has to be
instrumented, and there is no androidTest harness for the mux. Still owed.

---

## 3. Disconnect does not cancel the engine-start coroutine

- **File:** `base/src/main/java/com/windscribe/vpn/backend/wireguard/WireguardBackend.kt`
  (and the OpenVPN and IKEv2 backends)
- **Severity:** Blocker
- **Status:** fixed

**Verified:** `WireguardBackend.kt:200` is `scope.launch { runConnect(...) }` with the Job discarded; `connectionJob` (line 223) is the timeout, not this.

`runConnect()` is launched in the singleton application scope. Disconnect only
cancels `connectionJob` (the timeout), while `runConnect()` can still be
suspended starting ctrld, awaiting the service, or building the plan, and later
call `TunnelManager.connect()`. A user disconnect or a newer protocol attempt
can finish teardown and then have the stale coroutine attach the engine and
resurrect the tunnel.

Prescription: retain and `cancelAndJoin()` an engine-start job, or serialize with
an attempt generation/mutex, and reject stale `connectionId`s immediately before
attach. Cover disconnect and newer-connect races at each suspension point.

**Fixed:** both, because cancellation alone is not enough — it cannot reach code
already past its last suspension point. `VpnBackend` retains the coroutine
(`launchEngineStart`) and `runDisconnect` cancels and joins it first; all three
backends go through that instead of launching into the scope. Immediately before
attaching, `runConnect` re-checks that the attempt's `connectionId` is still
current and that no disconnect has begun.

`cancelEngineStart` never joins itself: a disconnect issued from inside the
connect path would otherwise wait on its own coroutine forever.
`EngineStartCancellationTest` covers a newer connect and a disconnect arriving
mid-plan; two of its three fail without the guard.

---

## 4. Disconnect during profile/API preparation cannot cancel the connect

- **File:** `base/src/main/java/com/windscribe/vpn/backend/utils/WindVpnController.kt`
- **Severity:** Blocker
- **Status:** fixed

**Verified:** `WindVpnController.kt:355` is `scope.launch { connect(source) }` with no Job retained.

`connectAsync()` launches the whole preparation in application scope without
retaining its job. Before a backend exists, the notification's Disconnect path
can settle the service but does not cancel or invalidate this coroutine. The
slow profile build then completes and launches a backend after the user
explicitly disconnected.

Prescription: track the active preparation by connection ID and cancel/join or
generation-reject it on every disconnect, including the no-TUN/no-backend path.
Preserve the user's terminal reason instead of hard-coding `ConnectFailed`.

**Fixed:** `connectAsync` retains its job and `disconnect` cancels and joins it —
skipping it when the job is the caller's own, since the reconnect path calls
`disconnect` from inside the very connect it would be waiting on.

The hard-coded `ConnectFailed` on the no-backend path is now derived from the
error, so a deliberate stop ends the session as `UserDisconnect`, `UserPause` or
`UserTurnOff`. Reporting one as a failure left the policy watching, and blocking
outside the VPN, for something the user asked to end.

---

## 5. Failed engine startup is published as disconnected without settling

- **File:** `base/src/main/java/com/windscribe/vpn/backend/VpnBackend.kt`
- **Severity:** Blocker
- **Status:** fixed

**Verified:** `VpnBackend.kt:607-611` and `:616-619` both only `updateState(Disconnected)`. The DNS-proxy failure at `:579-585` shows the correct typed `disconnect()` these two skip.

The null-plan branch and the catch below it only call
`updateState(Disconnected)`. They leave the backend active, `connectionJob`
running, and the blocking/held TUN unresolved. In foreground failover that state
advances to another backend; 20–30s later the stale first backend's timeout
calls `disconnect()` against the shared TunnelManager and can tear down the
replacement engine.

Prescription: route both failures through the normal typed disconnect/session-end
path — cancel the timer, detach/deactivate, convert or release the hold — and
cover `plan == null` plus `engine.start()` throwing while the next protocol
connects.

**Fixed:** both exits now go through `disconnect(...)` with a typed error, the
same path the DNS-proxy and service-start failures above them already took — so
the fix was to stop these two being the odd ones out rather than to invent
anything.

Restructured so there is one `disconnect` call site and it sits *outside* the
`try`: putting it in the null-plan branch would have let a teardown that throws
be caught by the catch below and disconnected a second time.
`VpnBackendConnectFailureTest` drives `runConnect` through a test subclass for
both failures and the success case; two of its three fail on the old
`updateState(Disconnected)`.

---

## 6. `ensureRunning()` can return a service that already called `stopSelf()`

- **File:** `base/src/main/java/com/windscribe/vpn/backend/tunnel/VpnServiceGate.kt`
- **Severity:** Blocker
- **Status:** fixed

**Verified:** `VpnServiceGate.kt:69` is `current?.let { return it }` with no `isStopping` check, and `markStopping()` (line 47) sets the flag without clearing `service`. Line 87 has the same gap.

`markStopping()` leaves `current` pointing at the outgoing instance and the fast
path ignores `isStopping`. A tap or auto-connect after terminal `Disconnected`
but before `onDestroy()` attaches the new engine to the dying service; its
pending teardown closes that new TUN, and no replacement service was started.

Prescription: do not return or await the stopping instance — clear it atomically
when stopping, or await a distinct attachment — and test
`markStopping` → `ensureRunning` → old `onDestroy`.

**Fixed:** the service and its stopping flag are now one `Attachment` value, so no
reader can pair a service with a flag that never went with it. `ensureRunning`
and `await` skip a stopping attachment; `current` still returns it, because
teardown legitimately needs it to end the session and keep protecting sockets.

Clearing it on `markStopping` — the review's first suggestion — would have broken
those teardown callers, and awaiting a *distinct* attachment on its own would
hang: `attach` only ran from `onCreate`, and a start command arriving before a
pending `stopSelf()` revives the same instance without `onCreate` running again.
`onStartCommand` now re-attaches, so the wait ends whether Android revives the
instance or replaces it. `VpnServiceGateTest` covers both outcomes plus the late
`onDestroy`; one of its six fails on the old `await`.

---

## 7. DNS interception is frozen for the mux lifetime

- **File:** `common/src/main/java/com/windscribe/common/tunnel/TunnelManager.kt`
- **Severity:** Blocker
- **Status:** fixed

**Verified:** `buildDnsStage` is called once, at `TunnelManager.kt:361`, only when the first `PacketMux` is constructed. No replacement path rebuilds it.

`buildDnsStage()` runs only when the first mux is created; TUN replacements and
later engine attaches keep that stage. The UI permits DNS mode and address
changes while connected, and `ProxyDNSManager` may stop ctrld or choose a new
random port on the next attempt. Proxy→non-proxy can leave UDP/53 going to a
stopped listener (DNS outage); non-proxy→proxy keeps no stage (selected resolver
bypassed); a restarted proxy can leave the stage on the old port.

Prescription: reconcile or reconfigure the stage when type or port changes,
without dropping the TUN, and cover those transitions on a held-session
reconnect.

**Fixed:** narrower than it first read. The inputs *are* refreshed per connect —
`VPNProfileCreator` sets the details and `runConnect` calls
`startControlDIfRequired` before every attempt — so the defect was only that
`buildDnsStage` ran when the first mux was created and never again. A handover
reusing the TUN kept the previous connect's stage.

`ensureTunnel` now reconciles on both mux-reusing paths, and `PacketMux.setDnsStage`
swaps the stage without touching the interface. The field became volatile, since
the egress pump reads it per packet, and it is cleared before the outgoing stage
is stopped: a moment of pass-through is recoverable, a query handed to a dead
listener is not.

Keyed on the decision (proxy plus port) rather than rebuilt every time, because
restarting a stage that is still correct costs the bypass re-arm.
`DnsStageReconcileTest` covers unchanged, port moved, proxy off and proxy on;
three of its four fail if the reconcile never acts.

---

## 8. Boot work reports success before it owns connect dispatch

- **File:** `base/src/main/java/com/windscribe/vpn/workers/worker/BootWorker.kt`
- **Severity:** Blocker
- **Status:** fixed

**Verified:** `ShortcutStateManager.connect()` is not `suspend` — it calls `startConnect` or `load { startConnect }` and returns. `BootWorker.kt:46` returns `Result.success()` on the next line.

`ShortcutStateManager.connect()` launches `load()`/`connectInForeground()` in
application scope, so `doWork()` returns success while cold-start session and
network initialization have not reached the foreground VPN service. WorkManager
may then release the process and permanently lose the boot connection despite
recording success.

Prescription: expose an awaited shortcut/bootstrap operation, keep it in the
worker coroutine until a defined durable handoff (service attached, or
Connecting), and return retry/failure when that handoff fails.

**Fixed:** `ShortcutStateManager.connectNow` suspends through the bootstrap and the
dispatch, and `load(callback)` now shares its implementation instead of carrying a
second copy. The worker then waits on `VpnServiceGate.await` for the service to
attach — the handoff chosen deliberately: past it the service owns the attempt and
the process may go.

Bounded at 30s, longer than the gate's own 5s, since a cold start after boot is
doing considerably more than starting a service. Not reaching it is
`Result.retry()`, not success. Nothing to connect with — no cached session, or an
account that cannot — is `Result.failure()` immediately, since waiting out the
timeout would achieve nothing.

`BootWorkerHandoffTest` covers all three outcomes; all three fail if the worker
goes back to dispatching and returning.

---

## 9. The ordered session refresh/reconnect escapes WorkManager lifetime

- **File:** `base/src/main/java/com/windscribe/vpn/workers/worker/SessionWorker.kt`
- **Severity:** Blocker
- **Status:** fixed

**Verified:** `UserRepository.reload()` is not `suspend`; its body is `scope.launch { ... }` and the callback runs inside it, so `doWork()` cannot be behind it.

`UserRepository.reload()` launches its callback in the singleton application
scope and returns immediately, so `doWork()` reports success before account
handling, server-list refresh, credential refresh and `ReconnectAfterRefresh.run()`
execute. Process death can leave an AwaitingSession/blocking tunnel unresolved.
The focused test masks this by invoking the mocked callback synchronously.

Prescription: make reload/update awaited and keep the whole ordered sequence in
the worker coroutine; add a delayed-callback test proving `doWork()` cannot
finish first.

**Fixed:** `reload` keeps its fire-and-forget contract — seven callers, most on
the main thread with nothing to wait for — and `reloadNow` suspends until it and
its callback have finished. Both go through the same private launch, so the
dispatcher and ordering are unchanged; the only difference is the wait.
`SessionWorker.updateIfRequired` is now `suspend` and uses it.

`SessionWorkerAwaitsRefreshTest` uses a deliberately slow callback, which is the
point the review made: the two existing worker tests invoked it synchronously and
passed either way. Both were updated to the awaited call — and one of them
started failing until it was, which is the defect showing itself.

---

## 10. Self-exclusion during a hold destroys inclusive split-tunnel scope

- **File:** `common/src/main/java/com/windscribe/common/tunnel/TunnelManager.kt`
- **Severity:** Major
- **Status:** fixed

**Verified:** `excludeSelfWhileHolding` copies `disallowedApps + self` unconditionally, while `TunConfig.blackhole()` branches on `val inclusive = allowedApps.isNotEmpty()`. `requestTun` applies both lists and swallows the rejected `addAllowedApplication` calls with `runCatching`.

With inclusive mode, `allowedApps` is non-empty. `excludeSelfWhileHolding()`
adds self to `disallowedApps`, so `requestTun()` receives both lists; it applies
disallowed first and silently catches each rejected
`addAllowedApplication()`. The hold becomes "all apps except Windscribe" instead
of the user's allow-list, capturing apps that should bypass.
`VpnBackend.provisionalTunConfig()` repeats the same construction for IKEv2 and
Stealth/WSTunnel.

Prescription: preserve mode as `TunConfig.blackhole()` does — remove self from
`allowedApps` in inclusive mode, append to `disallowedApps` only in exclusive
mode. Add handover coverage for both.

**Fixed:** `TunConfig.withSelfExcluded()` states the rule once, and both callers
use it — `excludeSelfWhileHolding()` and `VpnBackend.provisionalTunConfig()`,
which had the same unconditional construction. The already-excluded guard is now
`excluded == live` rather than a `disallowedApps` lookup, so it is mode-aware
too. `TunConfigSelfExclusionTest` covers inclusive, exclusive, neither, and
idempotence; the inclusive case fails if the helper is made unconditional.

---

## 11. The MTU coverage direction is reversed

- **File:** `common/src/main/java/com/windscribe/common/tunnel/TunConfig.kt`
- **Severity:** Major
- **Status:** fixed

**Verified:** `tunCovers(config)` is `currentTunConfig()?.coversSameInterfaceAs(config)`, so the receiver is the live config: `mtu >= other.mtu` permits reuse when the live interface is **larger**, which is the unsafe direction. The KDoc's reasoning is wrong — the interface MTU governs what apps emit, not what the engine can carry.

Reusing a live MTU-1500 interface for an incoming MTU-1280 engine does not make
packets under-sized: Android still advertises 1500 and can deliver packets too
large for the incoming path, causing fragmentation or blackholing. The safe
no-rebuild direction is a live interface no larger than the incoming engine can
carry.

Prescription: require equality or `live.mtu <= incoming.mtu`, and add explicit
1500→1280 rejection plus 1280→1500 reuse coverage.

**Fixed:** The comparison is now `mtu <= other.mtu`, and the KDoc says why: the interface
MTU is what apps are told to emit, not what the engine can carry. `TunConfigMtuTest`
covers 1280→1500 reuse, 1500→1280 rejection and equality; two of its three tests
fail on the old `>=`.

---

## 12. The fused AAR writes logs into Windscribe's user-0 sandbox

- **File:** `base/src/main/assets/logback.xml`
- **Severity:** Major
- **Status:** fixed

**Verified:** `logback.xml` hard-codes `/data/user/0/com.windscribe.vpn/files/` in both `<file>` and `<fileNamePattern>`, while `LogRepository.getDebugFilePath()` derives from `appContext.filesDir`.

The active and rolled paths hard-code `/data/user/0/com.windscribe.vpn`, but
`windscribe-core` is for partner apps and Android secondary users have a
different sandbox path. The appender cannot create the file, while
`LogRepository` reads the real host `appContext.filesDir`, so display and upload
are empty or fail.

Prescription: derive both paths from the host application's files directory via
an initialized Logback property or a programmatic appender, and test a
non-Windscribe application ID plus a secondary-user-style data path.

**Fixed:** both paths are now `${DATA_DIR}`, which logback-android 3.0.0 binds to
`getFilesDirectoryPath()` on the application it resolves through
`AppGlobals.getInitialApplication()` — verified by decompiling
`AndroidContextUtil.setupProperties`. That is the host application, so it follows
a partner's package and a secondary user's data path without us knowing either,
and it is the same directory `LogRepository` already reads.

No programmatic appender was needed. `LogPathsAgreeTest` asserts neither path is
pinned to a package, both use the property, and the basenames still match
`DEBUG_LOG_FILE_NAME` and `DEBUG_LOG_ROLLED_FILE_NAME` — the drift that would
otherwise leave the upload reading a file nobody writes.

---

## 13. The fused deliverable does not export its shrinker contract

- **File:** `windscribe-core/build.gradle.kts`
- **Severity:** Blocker
- **Status:** fixed

**Verified:** `strongswan/consumer-rules.pro` and `common/consumer-rules.pro` are both **empty (0 lines)**. `LowerCaseLevelConverter` is kept at `mobile/proguard-rules.pro:86` and `tv/proguard-rules.pro:82` — proving the pattern is required — but `JsonMessageConverter` and `ConnectionIdConverter`, both instantiated by name from `logback.xml`, are kept nowhere.

Keep rules for StrongSwan's JNI/reflection names, wsnet/scapix, CdLib and the
Logback converters live only in the mobile/TV app rules, and those app modules
are not fused. The included StrongSwan/common consumer-rule files are empty, and
the two new converters are not kept by first-party release rules either. A
minified partner build — and current first-party release logging — can rename or
remove classes resolved by native code or by `logback.xml`.

Prescription: move ownership-specific rules into library consumer rules so the
fused AAR exports them. Add a required `:windscribe-core:bundle` plus a minified
consumer build/runtime smoke; current MR CI never builds this deliverable.

**Fixed (converters only):** `JsonMessageConverter`, `ConnectionIdConverter` and
`LowerCaseLevelConverter` now carry `@Keep`, matching `SizeBasedTriggerForLog`,
which is the other class `logback.xml` names by string. `@Keep` travels with the
class into the fused AAR, so a partner's R8 honours it through AGP's default
rules without depending on our consumer rules.

Proven with R8 rather than assumed: `:mobile:minifyFdroidReleaseWithR8` puts all
four in `seeds.txt` and none in `usage.txt`. Removing the annotation from
`JsonMessageConverter` and re-running moves it to `usage.txt` — R8 deletes the
class outright, so release logging would have failed on a pattern referencing a
class that no longer exists. No broad `com.windscribe.**` keep exists; the app
rules name classes individually, and only `LowerCaseLevelConverter` had a line.

**Fixed (the rest):** the ownership-specific rules moved out of the app files into
the module that owns each — StrongSwan's JNI names to `strongswan`, pcap4j and
minidns to `common`, and wsnet/scapix, CdLib and the logback/slf4j rules to a new
`base/consumer-rules.pro` with `consumerProguardFiles` wired.

Verified rather than assumed, in both directions. Re-running R8 on the app after
the move gives identical `seeds.txt` counts for every group — 739 StrongSwan, 304
wsnet, 33 scapix, 15810 pcap4j, 1474 minidns — so nothing regressed by leaving the
app files. And `:windscribe-core:bundle` now produces an AAR whose `proguard.txt`
carries those 38 lines, where at HEAD all three consumer files were empty and
`base` had no `consumerProguardFiles` at all, so the fused deliverable exported
nothing.

A `Windscribe core AAR` CI job builds the bundle, runs the app's R8, and fails if
any of the names logback or native code resolve by string is missing from
`seeds.txt` — the failure mode being that the app starts and the feature is simply
gone.

---

## 14. Account-driven TV disconnects are labelled as user intent

- **File:** `tv/src/main/java/com/windscribe/tv/windscribe/WindscribePresenterImpl.kt`
- **Severity:** Major
- **Status:** fixed

**Verified:** `WindscribePresenterImpl.kt:798` (Banned) and `:813` (Expired) in `setAccountStatus` both pass `USER_DISCONNECT`.

Banned and expired callbacks use `USER_DISCONNECT`. The controller therefore
clears the global connection preference and stands down/whitelists the network
even though the account or server caused the stop; it also tears down a
user-supplied custom config despite the account-exemption rule.

Prescription: use `SYSTEM_DISCONNECT` and the same `!isConnectingToConfigured`
gate as `SessionWorker`, with TV coverage for banned/expired and custom configs.

**Fixed:** Both banned and expired now pass `SYSTEM_DISCONNECT` and carry the same
`!isConnectingToConfigured` gate. The two remaining `USER_DISCONNECT` sites in
that file are real user actions — `onDisconnectIntentReceived` and
`disconnectFromVPN` — and are left as they are.

---

## 15. Account refresh still clears connection intent for exempt custom configs

- **File:** `base/src/main/java/com/windscribe/vpn/workers/worker/SessionWorker.kt`
- **Severity:** Major
- **Status:** fixed

**Verified:** `SessionWorker.kt:213` sets `globalUserConnectionPreference = false` inside `if (banned || expired)` with no `isConnectingToConfigured` gate, unlike `shouldDisconnect` at `:203`.

`shouldDisconnect` correctly excludes `isConnectingToConfigured`, but the
assignment that clears the global preference is unconditional for banned/expired
Windscribe accounts. It contradicts `ConnectionPolicyTest`'s "no account state
blocks a custom config": subsequent app-update/reconcile paths see the cleared
preference and do not restore or watch that third-party tunnel.

Prescription: keep the global intent for custom configs, and add an integrated
`SessionWorker` account-change test rather than testing only the pure policy.

**Fixed:** The clear is gated on `!isConnectingToConfigured`, matching `shouldDisconnect`
directly above it. `SessionWorkerAccountStatusTest` drives the real worker for
banned and expired, with a custom config and without; two of its three tests fail
on the old unconditional assignment.

---

# Second review round

Re-review of f0b3b1c3. Two new blockers, both fixed, and a list of follow-on
items.

## New blockers

**TUN generations were not scoped to the service** — fixed in `9b3e465a`.
`TunnelManager` is a singleton keeping `appliedGeneration` across detach and
attach, while `tunGeneration` restarted at zero in each new service, so the new
session's replacement was rejected as older and its release closed the descriptor
the mux had just moved onto. A regression from finding 1. Counter is process-wide
now, and the notification carries the host that established it.

**The fused AAR owned `application@label`** — fixed in `9b3e465a`. A partner's
manifest merge failed unless the host added `tools:replace`. The library no longer
sets it; mobile declares its own, tv already did.

## Follow-on items

| Item | State |
|---|---|
| Cancellation becoming a second failure | fixed, `7722f6ea` — live path was the controller, not the backend |
| Blocking wakeup pipes | fixed, `7722f6ea` |
| Sole-app inclusive split tunnel | fixed, `0e9bdac3` — the helper refuses rather than inverting |
| Worker cancellation escaping the application scope | fixed, `0e9bdac3`, **untested** — see below |
| Untracked direct `connect()` / holder paths | fixed |
| DNS retry and wiring coverage | fixed |
| Downstream custom-config credential handling | fixed |

**Untracked paths.** `VPNPermissionActivity` called `vpnBackendHolder.connect`
directly with a `connectionId` from an Intent extra, so a grant arriving after the
attempt was abandoned started a backend for it — and the dialog can sit open
indefinitely. It goes through `WindVpnController.connectWithGrantedPermission`,
which drops an id that is no longer the active attempt.

**Credential handling.** `ConnectionDataRepository` cleared the standing intent and
disconnected on `ERROR_UNABLE_TO_GENERATE_CREDENTIALS` at three sites — server
config, OpenVPN credentials, IKEv2 credentials. Those are our credentials; a
custom config never used them. Gated on `!isConnectingToConfigured`, matching
findings 14 and 15.

**DNS coverage.** `DnsStageSwapTest` covers the swap itself: both sinks wired, the
outgoing stage stopped before the incoming one starts, a stage that will not start
leaving pass-through rather than a dead one, dropping to null, and the same stage
twice. Two of its five fail against a naive swap.

The reviewer also reported 5 threads partial and 2 still open without per-thread
detail. Those have not been worked; the summary alone is not enough to act on
without guessing at the intended behaviour.

## Owed

- **The saturation test for finding 2.** Needs real descriptors and a device.
- **Cancellation propagation in `reloadNow` has no test.** `backgroundScope` work
  that has been abandoned is not driven to completion by `advanceUntilIdle`, so
  the harness cannot tell the two implementations apart. Checked, rather than
  assumed; the test that looked like proof was removed.
- **No device run.** Findings 1, 2 and 7 change the live packet path — non-blocking
  writes, the generation-tagged swap, the DNS stage reconcile — and none of it has
  run on hardware.
- **No green CI at the exact head.** Pipeline 348291 failed on runner
  infrastructure before checkout, not on code.
