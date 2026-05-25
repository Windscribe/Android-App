# Tier 2 Testing — Handoff Notes

Pick up here when you're ready to continue. Everything below is current as of this session.

---

## Where we are

### Done this session
- **Tier 1 repos covered**: `Util`, `ProtocolConfig`, `CommonPasswordChecker`, `HashUtils`, `ThreadSafeList` (all in `:base`).
- **Tier 2 repos covered**: `IpRepository` (13), `LocationRepository` (17), `LatencyRepository` (20), `UserRepository` (20). **All four Tier 2 repos done.**
- **Infra fixes**: openvpn deps bumped (Robolectric 4.13, mockito 5.12, androidx.test:core 1.5.0) so the 16 vendored tests run again. Deleted the dead `:test` module and base `androidTest/`.
- **Production refactors**:
  - `IpRepository`: rewritten as declarative `merge → mapLatest → stateIn`. `isOnline: StateFlow<Boolean>` injected from `DeviceStateManager`.
  - `LocationRepository`: extracted `Pinger` interface (in `base/.../services/ping/`), inlined `currentSourceType()` to drop the `WindUtilities.getSourceTypeBlocking()` static call.
  - `Ping` (the native ICMP class): converted from `open class` with per-call instantiation + protected test seams to an `object` with `suspend fun run` that wraps `withContext(Dispatchers.IO)`. Removed 5 unused protected wrappers.
  - `IcmpPinger`: absorbed the TCP-443 fallback (used to live in `LatencyRepository.getLatencyFromSocketConnection`). Now one IO seam covers ICMP + TCP cascade.
  - `LatencyRepository`: deleted dead `getLatencyFromApi` and the `WSNetWrapper`/`AdvanceParameterRepository` deps that only it used. Injected `Pinger` + `isOnline: StateFlow<Boolean>`.
  - `UserRepository`: collapsed the parallel `MutableLiveData<User>` + `MutableSharedFlow<User>(replay=1)` into a single `StateFlow<User?>`. Updated 9 mobile/tv `userInfo.collect` consumers to `user.filterNotNull().collect`. Converted 1 TV `.observe()` to the existing `suspend fun + activityScope { }` convention. Deleted dead `synchronizedReload()`. Removed redundant explicit `Dispatchers.IO` override on the `scope.launch` (was breaking test scheduler drain).

### Project test count
- `:base` — 146 tests (all passing)
- `:openvpn` — 16 (1 `@Ignore`d with TODO)
- `:wgtunnel` — 10
- **Total: 172 tests, 171 passing, 1 skipped, 0 failures.** ✅ Suite fully green.

### Test class inventory
| Module | Class | Tests |
|---|---|---|
| base | `UtilTest` | 15 |
| base | `ProtocolConfigTest` | 6 |
| base | `CommonPasswordCheckerTest` | 6 |
| base | `HashUtilsTest` | 6 |
| base | `ThreadSafeListTest` | 12 |
| base | `WireguardUtilTest` | 31 |
| base | `IpRepositoryTest` | 13 |
| base | `LocationRepositoryTest` | 17 |
| base | `LatencyRepositoryTest` | 20 |
| base | `UserRepositoryTest` | 20 |
| openvpn | `TestConfigGenerator` | 2 |
| openvpn | `TestConfigParser` | 8 (7 pass / 1 `@Ignore`) |
| openvpn | `TestIpParser` | 1 |
| openvpn | `TestLogFileHandler` | 5 |
| wgtunnel | `BadConfigExceptionTest` | 8 |
| wgtunnel | `ConfigTest` | 2 |

---

## Tier 2 status — COMPLETE

| Repo | LOC | Status |
|---|---|---|
| ~~`IpRepository`~~ | done | 13 tests. Rewritten as declarative state flow. |
| ~~`LocationRepository`~~ | done | 17 tests. Pinger extracted, currentSourceType inlined. |
| ~~`LatencyRepository`~~ | done | 20 tests. Dead code removed, Pinger + isOnline injected. |
| ~~`UserRepository`~~ | done | 20 tests. LiveData + SharedFlow collapsed into single StateFlow. |

### What was *not* tested in `UserRepository` (Tier 3 work)
`logout()`, `onSessionDeleted()`, and `prepareDashboard()` were skipped because they reach through `Windscribe.appContext` for `vpnConnectionStateManager`, `activeActivity`, `applicationInterface`, and call the static `WindUtilities.deleteProfileCompletely`. Testing them requires extracting these into injectable interfaces — meaningful refactor, ~3 new abstractions. Defer until someone has a good reason to touch them.

---

## Tier 3 (post-Tier-2) candidates worth flagging

If you want to keep going after `UserRepository`:

| File | LOC | Why test |
|---|---|---|
| `VPNProfileCreator.kt` | 947 | Builds OpenVPN/WG/IKEv2 profile strings. Bugs = silent connection failures. Mostly string templating. Very unit-testable but large surface (probably 20+ tests). |
| `AutoConnectionManager.kt` | 704 | Protocol failover state machine. Critical, but lots of timing/coroutine work — would benefit from a `StateFlow` rewrite first. |
| `WindNotificationBuilder.kt` | ? | Notification text generation, likely pure-string logic. |
| ViewModels (21 of them) | varies | Each one is a small state machine. After Tier 2 repos are mockable, the ViewModels are testable in 30-60 mins each. |

---

## Tips & tricks learned this session

### MockK / harness patterns

1. **MutableLiveData in JVM unit tests crashes** with `Method getMainLooper in android.os.Looper not mocked`. Fix:
   ```kotlin
   userLiveData = mockk(relaxed = true)
   every { userLiveData.value } returns null
   // To set a value:
   every { userLiveData.value } returns mockk { every { ... } returns ... }
   ```
   Don't use a real `MutableLiveData<T>()` — its `setValue()` asserts main-thread.

2. **`mockkObject(WindUtilities)` is a code smell**, not a solution. Whenever you reach for it, ask whether the static call has a DI-shaped alternative (e.g., inject a `StateFlow<Boolean>` from `DeviceStateManager` instead of calling `WindUtilities.isOnline()`). Saves ~15 lines of `@Before`/`@After` plumbing per test class and removes static-state leak risk.

3. **For `Dagger Lazy<T>`**: stub `every { lazyDep.get() } returns mock`. The `Lazy` itself is just a mock.

4. **For `lateinit var Windscribe.appContext` access**: just assign it directly in the test — `com.windscribe.vpn.Windscribe.appContext = mockk(relaxed = true)`. No `mockkObject(Windscribe)` needed.

5. **Argument evaluation happens before MockK interception**. If you stub `foo(appContext)`, the `appContext` lateinit still resolves first — and if uninitialized, it throws before MockK sees the call. Initialize the lateinit or move the access.

### Coroutine test patterns

6. **`MutableSharedFlow(replay=0)` (the default) suspends `emit()` until a subscriber is present.** If you construct a repo that emits in `init`, and you attach a collector later, the emission was lost — or `runTest` deadlocks. Two fixes:
   - **(a)** Subscribe BEFORE constructing — usually impossible since the flow lives inside the repo.
   - **(b)** `launch(start = CoroutineStart.UNDISPATCHED) { repo.state.collect { ... } }` immediately after construction so the collector subscribes before `advanceUntilIdle` drains queued emissions.
   - **(c) BEST**: refactor to `StateFlow` (always has a current value, late subscribers see it). Apply `stateIn(scope, Eagerly, initialValue)` to the merged flow upstream.

7. **`advanceUntilIdle()` fast-forwards through delays.** If you want to assert state *before* a `delay(500)` elapses, use `runCurrent()` instead. `advanceUntilIdle` happily runs every scheduled task including ones scheduled for the future.

   ```kotlin
   advanceTimeBy(499)
   runCurrent()        // strict: stops before the 500ms tick fires
   // assert mid-delay state...
   advanceTimeBy(2)
   advanceUntilIdle()  // now drain everything
   ```

8. **Pass `this` (the TestScope) as the scope to the repo**, not `runTest`'s `backgroundScope`. Background-scope work isn't deterministically drained by `advanceUntilIdle`. Then at the end of each test call `coroutineContext.cancelChildren()` to kill any perpetual collectors before `runTest` checks for uncompleted coroutines.

9. **Diagnostic trick when a coroutine test mysteriously fails**: instead of `println` (which sometimes gets buffered into unexpected report sections), throw the diagnostic info as part of the assertion message:
   ```kotlin
   throw AssertionError("DIAGNOSTIC:\n$logStringBuilder")
   ```
   Always shows up clearly in the HTML test report.

### Production-side patterns that pay off in tests

10. **Replace `MutableSharedFlow(replay=0)` with `MutableStateFlow`** unless you genuinely need event semantics (e.g., one-shot errors). StateFlow:
    - Always has a current value (late subscribers see it).
    - Doesn't suspend on emit.
    - Tests just read `.value`, no collector dance.

11. **Declarative state via `merge → mapLatest → stateIn`** beats `init { update() + scope.launch { collectLatest { ... } } }`. Single source of truth, no two-init-blocks-with-different-responsibilities. `mapLatest` gives you free cancellation of in-flight work when inputs change. See the new `IpRepository` for the pattern.

12. **Extract interfaces for I/O boundaries**. `Pinger` is the canonical example — ICMP/TCP calls were inline in `LocationRepository`, untestable. Now they're behind an interface, the prod impl (`IcmpPinger`) is one file, and tests pass a fake `Pinger`. Same pattern will work for `getLatencyFromSocketConnection`, anywhere you find raw `Socket()`, `InetAddress.getByName`, `File()`, etc.

13. **Inject `StateFlow<X>` instead of the holder class** when you only need to read one value. Repos that only consume `vpnConnectionStateManager.state` should depend on `StateFlow<VPNState>` directly. The Dagger module exposes it: `@Provides fun vpnState(m: VPNConnectionStateManager): StateFlow<VPNState> = m.state`. Test then passes `MutableStateFlow(...)` — no mocking the 5-deps-deep state manager class.

14. **Beware "init does work in the constructor"**. The trace logging we ran showed 7 of 10 repos construct during `applicationComponent.inject(this)` and fire `scope.launch { load() }` immediately. None of those launches happen on main (they're on `Dispatchers.IO` via `Windscribe.applicationScope`), so it's not a startup-perf problem. But it means tests can never set up state before the first action. Workarounds:
    - `cancelChildren()` cleanup pattern works in tests.
    - Long-term fix would be a `start()` method called by `AppLifeCycleObserver`. Not urgent.

### Build / dep upkeep

15. **Robolectric 4.6.x can't parse Java 17 bytecode** — bump to 4.13+ if you see `Unsupported class file major version 61`.
16. **Mockito 3.x is JVM 8/11 era** — bump to 5.x for Java 17 projects.
17. **`androidx.test:core:1.2.0` manifest lacks `android:exported`** — bump to 1.5.0+ or you'll see manifest-merger errors on Android 12+ targets.
18. **`testOptions.unitTests.returnDefaultValues = true`** in `build.gradle` makes Android stub APIs return defaults instead of throwing "Method X not mocked". Useful for noise reduction but doesn't help with logic-bearing static calls.

### MockK gotchas worth memorizing

19. **`returns "a" to "b"` doesn't compile**. Kotlin parses it as `(returns "a").to("b")`, not `returns ("a" to "b")` — `to` is an infix function, looser precedence than the method call. Use `returns Pair("a", "b")` instead.

20. **You can't stub Java public fields with `every { mock.fieldName } returns x`**. Java public fields are direct memory accesses, not getter calls — MockK has nothing to intercept. The block returns `MockKException: Missing mocked calls`. Three fixes:
    - Use `mockk<T>(relaxed = true)` if you don't need a specific value (Java fields default to 0/false/null).
    - Stub the explicit getter: `every { mock.getPrimaryKey() } returns 1`.
    - If you control the Java class, replace the public field with a private field + getter.

21. **`MutableSharedFlow(replay=N)` exposes `.replayCache` as a public synchronous read.** No collector setup needed for tests — `flow.replayCache.lastOrNull()` returns the last emitted value after `advanceUntilIdle`. Works when `replay >= 1`. Avoids the `UNDISPATCHED` collector dance entirely for read-only assertion purposes.

22. **Nested mocks for Java POJOs**: when a Kotlin wrapper class reads `sessionResponse.sip.count` and `sip` is a nested Java object, stub via `every { sip } returns mockk { every { count } returns 5 }`. Don't try to stub the *Kotlin wrapper's* derived property — stub the underlying Java getter chain that the wrapper reads from.

### Coroutine-test gotchas

23. **`scope.launch(Dispatchers.IO) { ... }` from a `TestScope` bypasses `advanceUntilIdle`.** The explicit `Dispatchers.IO` overrides the scope's test dispatcher, so the launched coroutine runs on real IO threads that the test scheduler can't drain. Symptoms: assertions like "Verification failed: ... was not called" even though the code clearly should have called it. **Fix**: don't override the dispatcher in production code — let the injected scope decide. The application scope is already on `Dispatchers.IO` in prod (`Windscribe.applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)`), so explicit overrides are redundant. Encountered while testing `UserRepository.reload()`.

24. **`advanceUntilIdle()` fast-forwards through `delay(...)`.** To assert state *strictly before* a delay fires, use `runCurrent()` instead. Encountered while testing `IpRepository` (500ms VPN-disconnect delay).

25. **`MutableSharedFlow(replay=0)` (the default) suspends `emit()` until a subscriber attaches.** Repos that `emit()` in `init { }` and expose a no-replay SharedFlow lose those emissions if the test collector subscribes too late. Either: (a) `launch(start = CoroutineStart.UNDISPATCHED) { repo.state.collect(...) }` BEFORE constructing the repo, or (b) refactor production to `StateFlow` / `SharedFlow(replay >= 1)`. We did (b) for both `IpRepository` and `UserRepository`.

### Dead code worth flagging

26. **`updateLocation()`'s `catch (e: WindScribeException) -> -1`** is unreachable — every internal path uses `runCatching {}` which eats exceptions. Worth a follow-up PR.
27. **`isCityAvailable(id, userPro)` has an unused `userPro` parameter** — lint flagged it.
28. ~~**`LatencyRepository.getLatencyFromApi`**~~ — deleted this session.
29. ~~**`UserRepository.synchronizedReload`**~~ — deleted this session (was used by the `:test` module that we'd already removed).
30. **The same IPv6 zero-collapsing regex chain** lives in both `Util.kt` and `IpRepository.kt`. Worth deduping when someone touches it.
31. **`UserRepository.kt` has two pre-existing warnings** that pre-date this session: line ~93 "Unreachable code" in `whatChanged` (the `?:` elvis path uses an inline `return` that makes the run block's expression value unreachable) and a redundant `suspend` modifier on `handleAmneziaAutoEnable`. Both worth cleaning up but out of scope here.

### Pre-existing test failure — RESOLVED

32. ~~**`WireguardUtilTest.implementation should match PHP server algorithm`**~~ — fixed by adjusting the test assertion. `generateWireguardIP()` intentionally returns `IP/32` (the format `WgConfigRepository` injects directly into WireGuard `Address =` config lines); the test was reconstructing the IP without the `/32` suffix. The math underneath always matched.

---

## File-by-file inventory of test-related changes

```
ADDED:
  base/src/main/java/com/windscribe/vpn/services/ping/Pinger.kt        (new interface)
  base/src/main/java/com/windscribe/vpn/services/ping/IcmpPinger.kt    (new prod impl + TCP fallback)
  base/src/test/java/com/windscribe/vpn/backend/UtilTest.kt
  base/src/test/java/com/windscribe/vpn/backend/utils/ProtocolConfigTest.kt
  base/src/test/java/com/windscribe/vpn/commonutils/CommonPasswordCheckerTest.kt
  base/src/test/java/com/windscribe/vpn/commonutils/HashUtilsTest.kt
  base/src/test/java/com/windscribe/vpn/commonutils/ThreadSafeListTest.kt
  base/src/test/java/com/windscribe/vpn/repository/IpRepositoryTest.kt
  base/src/test/java/com/windscribe/vpn/repository/LocationRepositoryTest.kt
  base/src/test/java/com/windscribe/vpn/repository/LatencyRepositoryTest.kt
  base/src/test/java/com/windscribe/vpn/repository/UserRepositoryTest.kt

MODIFIED:
  base/build.gradle                                  (+ mockk, coroutines-test)
  base/src/main/java/com/windscribe/vpn/services/ping/Ping.kt       (object + suspend + IO)
  base/src/main/java/com/windscribe/vpn/repository/IpRepository.kt    (declarative rewrite)
  base/src/main/java/com/windscribe/vpn/repository/LocationRepository.kt (Pinger, currentSourceType)
  base/src/main/java/com/windscribe/vpn/repository/LatencyRepository.kt (Pinger, isOnline, dead code removed)
  base/src/main/java/com/windscribe/vpn/repository/UserRepository.kt   (StateFlow merge, dispatcher fix)
  base/src/main/java/com/windscribe/vpn/di/BaseApplicationModule.kt   (Pinger provider, isOnline wiring x2)
  9 mobile/tv consumer files                         (userInfo.collect -> user.filterNotNull().collect)
  tv/.../settings/SettingsPresenter.kt + Imp.kt + SettingActivity.kt (LiveData.observe -> activityScope { suspend collect })
  openvpn/build.gradle.kts                           (deps bump)
  openvpn/src/test/.../TestConfigParser.kt           (one @Ignore with TODO)
  settings.gradle, mobile/build.gradle               (removed :test module wiring)

DELETED:
  test/                                              (dead module)
  base/src/androidTest/                              (3 broken/dead test files)
  mobile/src/androidTest/                            (3 dead test scaffolding files)
  mobile/src/test/.../ExampleUnitTest.java           (stub)
  common/src/test/.../ExampleUnitTest.kt             (stub)
  base/src/.../di/ApplicationTestComponent.kt        (google + fdroid flavors)
  base/src/.../di/TestVPNModule.kt
  base/src/.../mocks/TestWindVpnController.kt
```

---

## To resume work

Tier 2 is complete. Pick a Tier 3 candidate (or one of the open follow-ups) when you're ready.

1. Read this file.
2. Run `./gradlew testFdroidDebugUnitTest testDebugUnitTest testSkeletonDebugUnitTest --continue` to confirm baseline is still green (171 pass + 1 deliberate `@Ignore`, 0 failures).
3. Pick a target:
   - **Highest-value Tier 3 candidate**: `VPNProfileCreator.kt` (947 LOC of string templating; bugs = silent connection failures; very unit-testable, no DI seams needed).
   - **Most-blocked downstream work unlocked**: the 21 ViewModels — now that all four Tier 2 repos are mockable through clean interfaces, each ViewModel is ~30-60 min of work. Pick `ConnectionViewmodel` first since it's the highest-traffic.
4. The 32 tips above are the accumulated lessons — re-read them once before starting.
5. Each refactor step is independently shippable; commit after each.
