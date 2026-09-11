# AGENTS.md — Windscribe Android App

## Project Context

**Repository**: ws/client/androidapp (GitLab)
**Purpose**: Windscribe VPN application for Android devices (phones, tablets, Android TV)
**Tech Stack**: Kotlin (100% of base/mobile/tv), Jetpack Compose (mobile UI), XML (TV UI), Native C/C++/Go (vendored protocol modules)
**Owner**: Windscribe Engineering Team
**Platform**: Android API 21+ (Lollipop and above)

## Quick Links

- [README.md](README.md) — Human-friendly overview and build instructions
- [SKILL.md](SKILL.md) — Operational workflows for AI agents
- [docs/architecture/VPN_BACKEND.md](docs/architecture/VPN_BACKEND.md) — **how the VPN backend works; read this before changing anything under `base/backend/`**
- [docs/architecture/MODULE_STRUCTURE.md](docs/architecture/MODULE_STRUCTURE.md) — modules and the dependency graph
- [docs/architecture/DATA_FLOW.md](docs/architecture/DATA_FLOW.md) — API, database, preferences, workers
- [docs/architecture/VPN_PROTOCOLS.md](docs/architecture/VPN_PROTOCOLS.md) — the six protocols
- [docs/architecture/DNS_INTERCEPTION.md](docs/architecture/DNS_INTERCEPTION.md) — DNS through Control D
- [docs/features/ANTI_CENSORSHIP.md](docs/features/ANTI_CENSORSHIP.md) — protocol tweaks, server routing, TLS padding
- [docs/workflows/DEBUGGING.md](docs/workflows/DEBUGGING.md) — reading the app log, build commands
- [docs/guides/OPENVPN_UPDATE.md](docs/guides/OPENVPN_UPDATE.md) — updating the OpenVPN native build

---

## Comments

Hard rule, and the one most often broken here: **state the non-obvious fact and stop.**

A comment earns its place only when the code cannot say it: a constraint from outside this file, an
ordering that looks arbitrary, a value the platform dictates. Everything else is noise that goes
stale.

Never write:

- the history of a bug, or what the code used to do — that is what `git log` is for
- what was tried and rejected, or why an alternative is worse
- a restatement of the line below it
- a symptom narrative ("without this, X breaks, then Y, and the user sees Z")

Size is the tell. If a comment is longer than the code it explains, it is wrong. One or two lines is
normal; a KDoc paragraph on a private helper is not. Prefer a clear name over a comment explaining
an unclear one.

```kotlin
// Wrong — four lines narrating a bug that git already records.
// logback resolves ${DATA_DIR} through AppGlobals.getInitialApplication(), which is null while
// the Application is constructed, so the very first getLogger call configures the appender before
// there is an application to ask. DATA_DIR comes back empty, the file lands at the filesystem
// root, and nothing is written for the whole process.
private fun bindLogDirectory(base: Context) {

// Right — the constraint, once.
/** logback's own `${DATA_DIR}` is empty this early; the appender needs a real files dir. */
private fun bindLogDirectory(base: Context) {
```

The same applies to docs under `docs/`: describe how it works now, minimally. No changelogs, no
rejected designs.

---

## Architecture

### System Overview

Windscribe Android is a multi-protocol VPN application with comprehensive privacy features. The app supports 6 VPN protocols, per-network configuration, split tunneling, DNS filtering (R.O.B.E.R.T), and custom configurations.

**Key Capabilities**:
- **6 VPN Protocols**: OpenVPN (UDP/TCP), IKEv2, Stealth, WSTunnel, WireGuard
- **Per-Network Auto-Config**: Different protocols for different networks (requires location permission)
- **Auto-Secure Network Whitelist**: Session-based mechanism preventing auto-reconnect after manual disconnect
- **Split Tunneling**: Per-app VPN routing
- **DNS Filtering**: R.O.B.E.R.T with customizable toggles
- **Custom Configs**: Import WireGuard/OpenVPN configurations
- **Static IPs**: Available with pro plans

### Module Dependency Graph

```
┌──────────────────────────────────────────────────────────────┐
│                         App Layer                             │
├──────────────┬───────────────────────────────────────────────┤
│   mobile/    │   tv/                                          │
│  (Compose)   │  (XML)                                         │
│              │                                                │
│  Phone/      │  Android TV                                    │
│  Tablet UI   │  UI                                            │
└──────┬───────┴──────┬─────────────────────────────────────────┘
       │              │
       └──────┬───────┘
              │
       ┌──────▼──────────────────────────────────────────┐
       │              base/                              │
       │  ┌─────────────────────────────────────────┐   │
       │  │ api/        - wsnet integration         │   │
       │  │ backend/    - VPN protocol handlers     │   │
       │  │ localdatabase/ - Room database          │   │
       │  │ repository/ - Data layer                │   │
       │  │ services/   - Android services          │   │
       │  │ state/      - State management          │   │
       │  └─────────────────────────────────────────┘   │
       └─────┬──────┬──────┬──────┬──────┬─────────────┘
             │      │      │      │      │
    ┌────────┘      │      │      │      └────────┐
    │               │      │      │               │
┌───▼────┐   ┌─────▼──┐ ┌─▼──┐ ┌─▼────────┐  ┌──▼───┐
│ wsnet/ │   │openvpn/│ │wg  │ │strongswan│  │common│
│        │   │        │ │tunnel│ │          │  │      │
│  API   │   │ OpenVPN│ │ WG │ │  IKEv2   │  │Tunnel│
│Library │   │ UDP/TCP│ │WS  │ │  IPSec   │  │ DNS  │
│        │   │ Stealth│ │ctrld│ │          │  │Split │
└────────┘   └────────┘ └────┘ └──────────┘  └──────┘
```

**Module Relationships**:
- **mobile** & **tv** → **base** (UI depends on core)
- **base** → **wsnet** (all API calls)
- **base** → **openvpn**, **wgtunnel**, **strongswan** (protocol implementations)
- **base** → **common** (tunnel wrapper, DNS routing)
- NO circular dependencies

### File Tree

```
androidapp/
├── common/                       # Tunnel contracts + PacketMux. Depends on nothing.
│   └── tunnel/                   #   VpnHost, VpnEngine, TunConfig, TunnelManager,
│                                 #   PacketMux, ControlDDnsStage
├── base/                         # ALL app logic
│   └── src/main/java/com/windscribe/vpn/
│       ├── backend/              # 🔑 VPN orchestration
│       │   ├── tunnel/           #   WindscribeVpnService — the only VpnService
│       │   │                     #   VpnServiceGate, AutoConnectWatcher
│       │   ├── utils/            #   WindVpnController, WindNotificationBuilder,
│       │   │                     #   ExcludedIpHolder
│       │   ├── wireguard/        #   WireGuard backend + TunnelTrafficMonitor
│       │   ├── openvpn/          #   OpenVPN backend
│       │   ├── ikev2/            #   IKEv2 backend
│       │   ├── VpnBackend.kt     # 🔑 Shared connect/disconnect skeleton
│       │   └── ProxyDNSManager.kt
│       ├── state/                # 🔑 ConnectionPolicy, ConnectionPolicyInputs,
│       │                         #   VPNConnectionStateManager, NetworkInfoManager,
│       │                         #   DeviceStateManager
│       ├── autoconnection/       #   AutoConnectionManager — protocol failover
│       ├── repository/           #   UserRepository, ServerListRepository, …
│       ├── localdatabase/        #   Room (WindscribeDatabase, v43)
│       ├── apppreference/        #   DataStore preferences
│       ├── workers/              #   WorkManager jobs (SessionWorker, …)
│       ├── api/, wsnet/          #   API surface over the wsnet AAR
│       └── di/                   #   Dagger modules
├── mobile/                       # Phone/tablet UI (Compose)
├── tv/                           # Android TV UI (XML)
├── openvpn/                      # OpenVPN engine + C++ native build
├── strongswan/                   # IKEv2 engine, prebuilt charon
├── wgtunnel/                     # WireGuard (wg-go) + ctrld, Go → .so
├── windscribe-core/              # Fused AAR of base + everything under it
├── docs/                         # Architecture and workflow documentation
├── AGENTS.md                     # This file
└── build.gradle.kts
```

There is **no** `wsnet` or `test` module — `wsnet` is a prebuilt AAR dependency.

**Key entry points**:
- `WindscribeVpnService` — the only `android.net.VpnService`; owns the TUN for a session
- `WindVpnController` — orchestration: what to connect to, in what order, when to give up
- `ConnectionPolicy` — every connect/disconnect/block decision, as pure functions
- `VpnBackend` — the shared connect/disconnect skeleton all three protocols run through
- `TunnelManager` / `PacketMux` — attach/detach engines, move packets
- `VPNConnectionStateManager` — publishes connection status and service mode


## Key Implementation Decisions

### 1. Multi-Protocol VPN Architecture

**Six Supported Protocols**:
1. **OpenVPN UDP** — Fast, best for most networks
2. **OpenVPN TCP** — Reliable, firewall-friendly
3. **IKEv2** — Fast mobile protocol (StrongSwan)
4. **Stealth** — OpenVPN TCP with obfuscation (stunnel)
5. **WSTunnel** — OpenVPN over WebSocket (max stealth)
6. **WireGuard** — Modern, fast, efficient

**Protocol Switching**: Automatic fallback on connection failure, manual override in settings, per-network configuration.

### 2. wsnet Library for ALL API Calls

**Design Decision**: NO direct Retrofit/OkHttp usage. All API calls go through `wsnet` library.

**Why**: Centralized auth, retry logic, error handling, analytics, circuit breaking.

**Pattern**:
```kotlin
// ❌ NEVER do this
val retrofit = Retrofit.Builder()...

// ✅ ALWAYS do this
interface IApiCallManager {
    suspend fun getServerList(userName: String): GenericResponseClass<String?, ApiErrorResponse?>
}

class ApiCallManager(private val wsNetServerAPI: WSNetServerAPI) : IApiCallManager {
    override suspend fun getServerList(userName: String) = suspendCancellableCoroutine { continuation ->
        val callback = wsNetServerAPI.serverLocations(userName) { code, json ->
            buildResponse(continuation, code, json, String::class.java)
        }
        continuation.invokeOnCancellation { callback.cancel() }
    }
}
```

### 3. Room Database for Local Storage

**All persistent data** stored in Room database:
- Server regions & cities
- Static IP configurations
- User preferences (duplicated from Tray for structured queries)
- Notification history
- Network profiles (per-network configs)

**Migration Strategy**: Schema changes require migration scripts in `WindscribeDatabase.kt`. Current version tracked in `schemas/` folder.

### 4. Hilt Dependency Injection

**Entire app** uses Hilt for DI. No manual `new` instantiation for core classes.

**Structure**:
- `@HiltAndroidApp` on the `Application` (`Windscribe` / `PhoneApplication`)
- `@Module @InstallIn(SingletonComponent::class)` modules under `*/di/` provide app-level singletons (e.g. `BaseApplicationModule`, `VPNModule`, flavor `ApplicationModule`/`BillingModule`)
- `@AndroidEntryPoint` on activities and services for field injection
- `@HiltViewModel class FooViewmodelImpl @Inject constructor(...)` for Compose ViewModels, retrieved per screen via `hiltViewModel()` in `NavigationStack.kt`

**Injection Pattern**:
```kotlin
@Inject lateinit var preferencesHelper: PreferencesHelper
@Inject lateinit var serverListRepository: ServerListRepository
```

### 5. Coroutines + Flows

**Async Operations**: 100% Kotlin coroutines + Flows.

**Patterns**:
- `suspend fun` for one-shot async operations
- `Flow<T>` for streams
- `SharedFlow<T>` for hot streams; one-shot events use `MutableSharedFlow(replay = 0)` (e.g. the
  billing managers' purchase events)
- `StateFlow<T>` for state

**Example**:
```kotlin
class ServerListRepository @Inject constructor(
    private val scope: CoroutineScope,
    private val localDbInterface: LocalDbInterface
) {
    private var _events = MutableSharedFlow<List<RegionAndCities>>(replay = 1)
    val regions: SharedFlow<List<RegionAndCities>> = _events

    init {
        load()
    }

    fun load() {
        scope.launch {
            _events.emit(localDbInterface.getAllRegionAsync())
        }
    }
}
```

### 6. One Decision Object: `ConnectionPolicy`

Every connect, disconnect, block and stand-down decision is a pure function on
`ConnectionPolicy` (`base/state/`), with no Android or coroutine dependencies. Callers read
their inputs through `ConnectionPolicyInputs` and ask the policy; **none of them re-implement a
condition locally**. That rule exists because the same decision used to be spelled out in five
places with four different predicates.

Includes what used to be the "auto-secure whitelist": a user disconnect stands the current network
down (`standsDown`), so auto-connect does not immediately undo it.

**Do not add a sixth place that decides these things.** Add the rule to `ConnectionPolicy`, cover it
in `ConnectionPolicyTest`, and call it.

See [docs/architecture/VPN_BACKEND.md](docs/architecture/VPN_BACKEND.md) §8.

### 7. Build Variants: Google vs F-Droid

**google** (Google Play):
- ✅ Google Play Billing
- ✅ Firebase Cloud Messaging (push notifications)
- ✅ In-App Review API
- ✅ Full feature set

**fdroid** (F-Droid):
- ❌ No proprietary Google dependencies
- ❌ No payment processing (free tier only)
- ❌ No push notifications
- ✅ 100% open source friendly

**Code Pattern**:
```kotlin
// base/src/google/... — GoogleBillingManager, AmazonBillingManager
// The fdroid flavor provides no-op equivalents, so :mobile compiles either way.
```

### 8. Language: Kotlin

`base`, `mobile` and `tv` are 100% Kotlin. Our own Java is confined to `:common` — `PacketMux` and
`ControlDDnsStage`, which sit on the packet path. The rest is the vendored native backend modules
(`openvpn`, `strongswan`, `wgtunnel`), which are upstream-sourced.

**Rule**: ALL code is Kotlin. No new Java files.

### 9. UI Architecture: Compose (Mobile) vs XML (TV)

**Mobile**: 100% Jetpack Compose
- Screen-based navigation (NavHost)
- ViewModels provided by Hilt (`@HiltViewModel` + `hiltViewModel()`)
- State management via `StateFlow` → `collectAsState()`

**TV**: XML layouts + view binding
- Activity-based navigation
- Traditional MVP pattern
- State exposed via `StateFlow`/`SharedFlow` (no LiveData)

### 10. One Service Owns the Interface

`WindscribeVpnService` is the **only** `android.net.VpnService` in the app and owns the TUN for an
entire session. WireGuard, OpenVPN and IKEv2 are plain `VpnEngine` objects behind `PacketMux`;
switching protocol detaches one and attaches another without closing the interface.

The service is started **before** the profile is built, not after, so the blocking interface is up
while the API round trips happen. Getting this order wrong reopens a traffic-in-the-clear window.

Because the interface outlives any single attempt it can be *held* with no engine attached — which is
how failover avoids a leak, and how blocking mode works without a separate killswitch.

Full flow, service modes, holds, failover budgets and the decision rules:
[docs/architecture/VPN_BACKEND.md](docs/architecture/VPN_BACKEND.md).

---

## Data Flow Patterns

### Pattern 1: User Action → API → Database → UI

**Example**: Fetching server list

```
User taps "Refresh" (UI)
    ↓
ViewModel.refreshServers() called
    ↓
ServerListRepository.updateServerList()
    ↓
ApiCallManager.getServerList(userName)
    ↓
wsnet.serverLocations() → API request
    ↓
API response (JSON)
    ↓
Parse to ServerListResponse
    ↓
LocalDbInterface.addToRegions(regions)
    ↓
Room database updated
    ↓
ServerListRepository._events.emit(regions)
    ↓
ViewModel observes via Flow
    ↓
UI observes ViewModel.state via StateFlow
    ↓
UI updates (new server list displayed)
```

**Code Example**:
```kotlin
// ViewModel
class SomeViewmodel(
    private val serverListRepository: ServerListRepository
) : ViewModel() {
    private val _state = MutableStateFlow<LocationsState>(LocationsState.Loading)
    val state: StateFlow<LocationsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            serverListRepository.regions.collect { regions →
                _state.value = LocationsState.Success(regions)
            }
        }
    }

    fun refreshServers() {
        viewModelScope.launch {
            _state.value = LocationsState.Loading
            when (val result = serverListRepository.updateServerList()) {
                is CallResult.Success → { /* already emitted via Flow */ }
                is CallResult.Error → _state.value = LocationsState.Error(result.errorMessage)
            }
        }
    }
}

// Repository
class ServerListRepository(
    private val apiCallManager: IApiCallManager,
    private val localDbInterface: LocalDbInterface
) {
    private var _events = MutableSharedFlow<List<RegionAndCities>>(replay = 1)
    val regions: SharedFlow<List<RegionAndCities>> = _events

    suspend fun updateServerList(): CallResult<Unit> {
        val apiResult = result<ServerListResponse> {
            apiCallManager.getServerList(userName)
        }
        return when (apiResult) {
            is CallResult.Success → {
                localDbInterface.addToRegions(apiResult.data.regions)
                _events.emit(localDbInterface.getAllRegionAsync())
                CallResult.Success(Unit)
            }
            is CallResult.Error → apiResult
        }
    }
}
```

### Pattern 2: VPN State Changes → UI Updates

**Example**: Connection status updates

```
VPN connects (backend)
    ↓
VpnBackend.setState(CONNECTED)
    ↓
VPNConnectionStateManager.updateState(CONNECTED)
    ↓
StateFlow<VPNState> emits
    ↓
ViewModel observes
    ↓
UI updates (shows "Connected", green indicator, IP address)
```

### Pattern 3: Preference Changes → Auto-Connect Behavior

**Example**: User enables auto-secure

```
User toggles "Auto-Secure" ON (UI)
    ↓
ViewModel.setAutoSecure(true)
    ↓
PreferencesHelper.isAutoSecureOn = true
    ↓
DataStore preference written
    ↓
NetworkInfoManager re-reads the network
    ↓
AutoConnectWatcher asks ConnectionPolicy.shouldAutoConnect()
    ↓
If it says yes → WindVpnController.connectAsync()
```

---

## Module Interaction Patterns

### base/backend ↔ Protocol Modules

Protocol modules implement `:common` contracts. They never depend on `:base`, and `:base` never
talks to a native library directly — it goes through `TunnelManager`.

```kotlin
// common/.../tunnel/VpnEngine.kt — implemented in each protocol module
interface VpnEngine {
    fun start(profile: EngineProfile, tunFd: Int)
    fun stop()
    fun onRevoked()
}

// base asks TunnelManager to attach one; the service owns the interface
tunnelManager.connect(engineId, profile, tunConfig, hold)
```

The engine is handed a file descriptor from `PacketMux`, not the TUN itself — which is what lets one
engine be swapped for another without closing the interface.

### mobile/tv → base (Repository Pattern)

**Mobile (Compose)**:
```kotlin
// mobile/src/main/java/com/windscribe/mobile/ui/home/HomeScreen.kt
@Composable
fun HomeScreen(viewModel: HomeViewmodel) {
    val state by viewModel.state.collectAsState()
    // UI rendering
}

// mobile/src/main/java/com/windscribe/mobile/ui/home/HomeViewmodel.kt
// Note the spelling: Viewmodel, not ViewModel, throughout mobile/ui.
```

**TV (XML)**:
```kotlin
// tv/src/main/java/com/windscribe/tv/home/HomeActivity.kt
class HomeActivity : AppCompatActivity() {
    @Inject lateinit var serverListRepository: ServerListRepository // from base/

    override fun onCreate(savedInstanceState: Bundle?) {
        (application as Windscribe).appComponent.inject(this)
        // Use repository
    }
}
```

### wsnet → External API

**All API calls** route through wsnet:
```kotlin
// base/api/ApiCallManager.kt
override suspend fun getServerList(userName: String) = suspendCancellableCoroutine { continuation →
    val callback = wsNetServerAPI.serverLocations(userName) { code, json →
        // wsnet handles: auth headers, retry logic, SSL pinning, analytics
        buildResponse(continuation, code, json, String::class.java)
    }
    continuation.invokeOnCancellation { callback.cancel() }
}
```

---

## Running the App

### Build Commands

```bash
# Debug builds
./gradlew assembleDebug                       # All variants
./gradlew :mobile:assembleGoogleDebug         # Mobile (Google Play)
./gradlew :mobile:assembleFdroidDebug         # Mobile (F-Droid)
./gradlew :tv:assembleGoogleDebug             # Android TV

# Release builds
./gradlew assembleRelease
./gradlew bundleGoogleRelease                 # Google Play AAB
./gradlew bundleFdroidRelease                 # F-Droid AAB

# Module-specific compilation (faster for iteration)
./gradlew :base:compileGoogleDebugKotlin
./gradlew :mobile:compileGoogleDebugKotlin
./gradlew :tv:compileGoogleDebugKotlin

# Clean build (recommended after schema changes)
./gradlew clean && ./gradlew assembleDebug
```

### Install & Launch

**Mobile**:
```bash
./gradlew :mobile:assembleGoogleDebug
"$ANDROID_HOME/platform-tools/adb" install -r mobile/build/outputs/apk/google/debug/mobile-google-debug.apk
"$ANDROID_HOME/platform-tools/adb" shell am start -n com.windscribe.vpn/com.windscribe.mobile.ui.AppStartActivity
```

**TV**:
```bash
./gradlew :tv:assembleGoogleDebug
"$ANDROID_HOME/platform-tools/adb" install -r tv/build/outputs/apk/google/debug/tv-google-debug.apk
"$ANDROID_HOME/platform-tools/adb" shell am start -n com.windscribe.vpn/com.windscribe.tv.splash.SplashActivity
```

### Testing

```bash
# Unit tests
./gradlew test
./gradlew :base:test                          # Specific module

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Lint & formatting
./gradlew ktlintCheck                         # Check Kotlin style
./gradlew ktlintFormat                        # Auto-format
```

---

## Common Development Scenarios

### Adding a New Screen (Mobile — Compose)

See [SKILL.md](SKILL.md) for complete step-by-step guide.

**Quick Overview**:
1. Define route in `Screen.kt`
2. Add to `NavigationStack.kt` with transitions
3. Create Compose screen file
4. Create abstract ViewModel + `@HiltViewModel` implementation
5. Resolve it in `NavigationStack.kt` via `hiltViewModel()` (no module wiring needed)
6. Navigate via `navController.navigate(Screen.NewScreen.route)`

### Adding a Preference

See [SKILL.md](SKILL.md) for complete pattern.

**Steps**:
1. Add constant to `PreferencesKeyConstants.kt`
2. Add property to `PreferencesHelper` interface
3. Implement in `AppPreferencesImpl` with Tray getter/setter
4. Use in ViewModel: `preferencesHelper.newPreference`

### Adding a VPN Feature

**Pattern**:
1. Update `base/backend` — Core VPN logic
2. Update protocol module if needed (openvpn/wgtunnel/strongswan)
3. Add UI controls in `mobile/` (Compose) and `tv/` (XML)
4. Add preference if user-configurable
5. Update database schema if persistent state needed
6. Add tests (unit + integration)

### Database Migration

**Pattern**:
1. Update entity (`@Entity` class)
2. Increment database version in `WindscribeDatabase.kt`
3. Add migration script in `WindscribeDatabase.kt` companion object
4. Export schema to `schemas/` folder for testing
5. Test migration with instrumented test

---

## Agent Workflow Recipes

### Debugging VPN Connection Issues

```bash
# Clear logs
"$ANDROID_HOME/platform-tools/adb" logcat -c

# Monitor VPN logs (real-time)
"$ANDROID_HOME/platform-tools/adb" logcat -v time | grep -E "(WindVPN|OpenVPN|WireGuard|IKEv2)"

# Check protocol-specific logs
"$ANDROID_HOME/platform-tools/adb" logcat | grep -i wireguard

# Capture screenshot for UI debugging
"$ANDROID_HOME/platform-tools/adb" shell screencap -p /sdcard/screenshot.png
"$ANDROID_HOME/platform-tools/adb" pull /sdcard/screenshot.png /tmp/screenshot.png
"$ANDROID_HOME/platform-tools/adb" shell rm /sdcard/screenshot.png
```

### Inspecting Room Database

```bash
# Pull database from device
"$ANDROID_HOME/platform-tools/adb" pull /data/data/com.windscribe.vpn/databases/windscribe.db /tmp/

# Open with sqlite3
sqlite3 /tmp/windscribe.db
sqlite> .tables
sqlite> SELECT * FROM Region LIMIT 5;
```

### Testing Protocol Switching

```bash
# Force protocol via ADB
"$ANDROID_HOME/platform-tools/adb" shell am broadcast \
  -a com.windscribe.vpn.SWITCH_PROTOCOL \
  --es protocol "wireguard"

# Valid protocols: openvpn_udp, openvpn_tcp, ikev2, stealth, wstunnel, wireguard
```

### Clearing App Data (Fresh Start)

```bash
"$ANDROID_HOME/platform-tools/adb" uninstall com.windscribe.vpn
./gradlew :mobile:assembleGoogleDebug
"$ANDROID_HOME/platform-tools/adb" install -r mobile/build/outputs/apk/google/debug/mobile-google-debug.apk
```

---

## Critical Agent Rules

### Always
- **Use Kotlin** for ALL new code (no new Java files)
- **Use coroutines/flows** (no RxJava)
- **Use wsnet** for API calls (no direct Retrofit)
- **Run ktlintFormat** before committing
- **Test on multiple protocols** for VPN features (all 6)
- **Update database schema** properly with migrations
- **Follow MVP architecture** pattern
- **Inject via Hilt** (no manual `new` for core classes)

### Never
- Create circular module dependencies (mobile/tv → base → protocols, NOT base → mobile)
- Use RxJava (use coroutines/flows instead)
- Call APIs directly (use ApiCallManager → wsnet)
- Skip database migrations (will crash on upgrade)
- Modify protocol modules without testing all 6 protocols
- Commit secrets/API keys (use BuildConfig or local.properties)
- Push directly to main/master (use feature branches)

### When Unsure
- Check [SKILL.md](SKILL.md) for operational workflows
- Check [docs/guides/](docs/guides/) for step-by-step workflows
- Search codebase for existing examples (e.g., existing ViewModel/Repository)
- Ask in PR if architectural decision needed

---

## Additional Resources

- **[README.md](README.md)** — Build instructions, tech stack, contribution guide
- **[SKILL.md](SKILL.md)** — Operational workflows for AI agents
- **[docs/architecture/](docs/architecture/)** — Deep-dive architecture documentation
- **[docs/features/](docs/features/)** — Feature-specific documentation (auto-secure, split tunneling, etc.)
- **[docs/guides/](docs/guides/)** — How-to guides (OpenVPN updates, testing, migrations)

---

**Last Updated**: 2026-05-27
**Maintained By**: Engineering Team
**Next Review**: Post-feature additions or major architecture changes