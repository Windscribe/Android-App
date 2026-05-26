# AGENTS.md — Windscribe Android App

## Project Context

**Repository**: ws/client/androidapp (GitLab)
**Purpose**: Windscribe VPN application for Android devices (phones, tablets, Android TV)
**Tech Stack**: Kotlin (primary), Java (legacy), Jetpack Compose (mobile UI), XML (TV UI), Native C/C++/Go (protocols)
**Owner**: Windscribe Engineering Team
**Platform**: Android API 21+ (Lollipop and above)

## Quick Links

- [README.md](README.md) — Human-friendly overview and build instructions
- [SKILL.md](SKILL.md) — Operational workflows for AI agents
- [docs/](docs/) — Supplementary documentation

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

### File Tree with Annotations

```
androidapp/
├── base/                                    # Core functionality — ALL business logic
│   ├── api/                                # wsnet integration — ONLY way to call APIs
│   │   ├── IApiCallManager.kt              # API interface (all endpoints)
│   │   └── ApiCallManager.kt               # Implementation using wsnet
│   ├── backend/                            # VPN protocol communication & control
│   │   ├── utils/
│   │   │   └── WindVpnController.kt        # 🔑 Main VPN controller (start/stop/switch)
│   │   ├── openvpn/
│   │   │   ├── VpnBackend.kt               # OpenVPN backend (UDP/TCP/Stealth/WSTunnel)
│   │   │   └── DeviceStateReceiverWrapper.kt # Network change handling
│   │   ├── ikev2/
│   │   │   └── IKev2VpnBackend.kt          # IKEv2 backend
│   │   └── wireguard/
│   │       └── WireGuardBackend.kt         # WireGuard backend
│   ├── localdatabase/                      # Room database (all persistent data)
│   │   ├── WindscribeDatabase.kt           # Database instance
│   │   ├── entities/                       # Data models
│   │   │   ├── Region.kt                   # VPN server region
│   │   │   ├── City.kt                     # City within region
│   │   │   └── StaticRegion.kt             # Static IP region
│   │   └── dao/                            # Database access objects
│   │       ├── RegionDao.kt
│   │       ├── CityDao.kt
│   │       └── ...
│   ├── repository/                         # Data layer (API + Database)
│   │   ├── ServerListRepository.kt         # 🔑 Server list management
│   │   ├── UserRepository.kt               # User account data
│   │   └── NotificationRepository.kt       # In-app notifications
│   ├── services/                           # Android services (background work)
│   │   ├── WindVpnService.kt               # 🔑 Main VPN service (always-alive)
│   │   ├── AutoConnectService.kt           # 🔑 Network change auto-connect
│   │   └── UpdateService.kt                # App update checks
│   ├── state/                              # State managers (app-level state)
│   │   ├── DeviceStateManager.kt           # 🔑 Network state, whitelist logic
│   │   └── VPNConnectionStateManager.kt    # VPN connection state
│   ├── apppreference/                      # Preferences (Tray library wrapper)
│   │   ├── PreferencesHelper.kt            # Interface for all preferences
│   │   └── AppPreferencesImpl.kt           # Implementation
│   ├── managers/                           # Feature managers
│   │   ├── ProtocolConnectionManager.kt    # Protocol switching logic
│   │   └── LocationManager.kt              # Server location selection
│   └── constants/
│       └── PreferencesKeyConstants.kt      # All preference key constants
│
├── mobile/                                 # Phone/Tablet UI (100% Jetpack Compose)
│   └── src/main/java/com/windscribe/mobile/
│       ├── ui/
│       │   └── AppStartActivity.kt         # 🔑 Main activity (Compose entry point)
│       ├── nav/
│       │   ├── Screen.kt                   # Screen route definitions
│       │   └── NavigationStack.kt          # Navigation graph (hiltViewModel() per screen)
│       └── [screens]/                      # Compose UI screens
│           ├── HomeScreen.kt
│           ├── LocationsScreen.kt
│           └── SettingsScreen.kt
│
├── tv/                                     # Android TV UI (100% Kotlin, XML layouts)
│   └── src/main/java/com/windscribe/tv/
│       ├── splash/
│       │   └── SplashActivity.kt           # 🔑 TV entry point
│       ├── home/
│       │   └── HomeActivity.kt             # Main TV interface
│       └── [features]/                     # Feature activities
│
├── openvpn/                                # OpenVPN implementation (built from source)
│   ├── src/main/cpp/                       # Native C++ code (OpenVPN core)
│   │   ├── openvpn3/                       # OpenVPN 3 library
│   │   └── jni/                            # JNI bindings
│   └── src/main/java/                      # Kotlin wrapper
│       └── com/windscribe/vpn/openvpn/
│           └── OpenVPNManager.kt
│
├── strongswan/                             # IKEv2/IPSec (prebuilt binaries)
│   ├── libs/                               # Prebuilt .so files (armeabi-v7a, arm64-v8a, x86, x86_64)
│   └── src/main/java/
│       └── org/strongswan/android/
│           └── logic/VpnStateService.kt
│
├── wgtunnel/                               # Single native library (Go code compiled to .so)
│   │                                       # Contains: WireGuard, WSTunnel, Stunnel, ControlD (ctrld)
│   ├── src/main/go/                        # Go source code
│   │   ├── wireguard/                      # WireGuard implementation
│   │   ├── wstunnel/                       # WebSocket tunnel (OpenVPN over WS)
│   │   ├── stunnel/                        # Stealth protocol wrapper
│   │   └── ctrld/                          # ControlD DNS (DoH/DoT)
│   └── src/main/java/
│       └── com/windscribe/vpn/wgtunnel/
│           └── WgTunnelManager.kt          # Kotlin interface to Go lib
│
├── common/                                 # Tunnel wrapper + DNS traffic separation
│   └── src/main/java/
│       └── com/windscribe/vpn/common/
│           ├── TunnelManager.kt            # Tunnel abstraction layer
│           └── DnsResolver.kt              # DNS routing logic
│
├── wsnet/                                  # In-house networking library (ALL API calls)
│   └── src/main/java/
│       └── com/windscribe/wsnet/
│           ├── WSNetServerAPI.kt           # API client interface
│           └── [endpoints]/                # API endpoint implementations
│
├── test/                                   # Shared test utilities
│   └── src/main/java/
│       └── com/windscribe/vpn/test/
│           └── TestHelpers.kt
│
├── docs/                                   # 📚 Supplementary documentation
│   ├── architecture/                       # Architecture deep-dives
│   ├── features/                           # Feature-specific docs
│   ├── guides/                             # How-to guides
│   ├── workflows/                          # Process documentation
│   ├── api/                                # API references
│   └── security/                           # Security documentation
│
├── AGENTS.md                               # This file (AI architecture reference)
├── SKILL.md                                # AI operational workflows
├── README.md                               # Human-friendly overview
└── build.gradle.kts                        # Root build configuration
```

**Key Files** (🔑 marked above):
- **WindVpnController**: Main VPN controller — start/stop/switch protocols
- **VpnBackend**: OpenVPN backend — handles UDP/TCP/Stealth/WSTunnel
- **WindVpnService**: Main VPN service — runs as foreground service
- **AutoConnectService**: Network change detection → auto-connect logic
- **DeviceStateManager**: Network state tracking, auto-secure whitelist
- **ServerListRepository**: Server list management (API → Database → UI)
- **AppStartActivity** (mobile): Main Compose entry point
- **SplashActivity** (tv): Android TV entry point

---

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

### 5. Coroutines + Flows (RxJava Fully Removed)

**Async Operations**: 100% Kotlin coroutines + Flows.

**Patterns**:
- `suspend fun` for one-shot async operations
- `Flow<T>` for streams (replacing RxJava `Observable`)
- `SharedFlow<T>` for hot streams (replacing `BehaviorSubject`)
- `StateFlow<T>` for state (replacing `BehaviorSubject` with initial value)

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

### 6. Auto-Secure Network Whitelist System

**Purpose**: Prevent auto-reconnect after user manually disconnects on an auto-secure network.

**Behavior**:
- User disconnect on auto-secure ON network → Network whitelisted, auto-connect blocked
- Network change → Whitelist cleared, auto-connect resumes normally
- Return to network → Auto-connect works (whitelist was cleared when user left)
- System disconnect (protocol change, auto-secure OFF) → No whitelist, auto-reconnect continues

**Key Components**:
- `DeviceStateManager` (base/state/) — Whitelist state tracking, network change detection
- `WindVpnController` (base/backend/utils/) — Whitelist set/clear logic
- `AutoConnectService` (base/services/) — Whitelist check before auto-connect
- `VpnBackend` (base/backend/) — System disconnect handling

**Implementation**: Session-based (in-memory), cleared on network change. See [docs/features/AUTO_SECURE_WHITELIST.md](docs/features/AUTO_SECURE_WHITELIST.md).

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
// mobile/src/google/java/com/windscribe/mobile/billing/
class BillingManagerImpl : BillingManager { ... }

// mobile/src/fdroid/java/com/windscribe/mobile/billing/
class BillingManagerImpl : BillingManager {
    override fun purchase() { /* no-op */ }
}
```

### 8. Migration Status: Java → Kotlin

**Current State**:
- **TV Module**: 100% Kotlin ✅
- **Mobile Module**: ~95% Kotlin (5 Java files — billing interfaces)
- **Base Module**: ~85% Kotlin (65 Java files — data models, API responses)
- **Target**: 100% Kotlin (ongoing migration)

**Rule**: ALL new code MUST be in Kotlin. No new Java files.

### 9. UI Architecture: Compose (Mobile) vs XML (TV)

**Mobile**: 100% Jetpack Compose
- Screen-based navigation (NavHost)
- ViewModels provided by Hilt (`@HiltViewModel` + `hiltViewModel()`)
- State management via `StateFlow` → `collectAsState()`

**TV**: XML layouts + view binding
- Activity-based navigation
- Traditional MVP pattern
- ViewModels with LiveData (being migrated to StateFlow)

### 10. Protocol Connection Flow

**Start Connection**:
1. User selects location/protocol → `WindVpnController.connect()`
2. Controller selects backend based on protocol → `VpnBackend` / `IKev2VpnBackend` / `WireGuardBackend`
3. Backend prepares config → Calls native library (OpenVPN/StrongSwan/WireGuard)
4. `WindVpnService` started as foreground service → Notification shown
5. VPN interface created → Traffic routed through tunnel
6. `VPNConnectionStateManager` emits state updates → UI reflects connection status

**Auto-Connect Flow**:
1. Network change detected → `DeviceStateReceiverWrapper` broadcasts
2. `AutoConnectService` receives broadcast → Checks whitelist
3. If not whitelisted + auto-connect enabled → `WindVpnController.connect()`
4. Same flow as manual connection

**Protocol Fallback**:
1. Connection fails → Backend emits error
2. `ProtocolConnectionManager` receives error → Tries next protocol in preference order
3. Max 3 retries → Show error to user if all fail

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

**Code Example** (from CLAUDE.md):
```kotlin
// ViewModel
class LocationsViewModel(
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
Tray preference written to storage
    ↓
DeviceStateManager.onAutoSecureChanged()
    ↓
AutoConnectService checks current network
    ↓
If unsafe network + VPN disconnected → Connect
```

---

## Module Interaction Patterns

### base/backend ↔ Protocol Modules

**OpenVPN Example**:
```kotlin
// base/backend/openvpn/VpnBackend.kt
class VpnBackend {
    private val openVPNManager = OpenVPNManager() // from openvpn module

    fun startVPN(config: OpenVPNConfig) {
        openVPNManager.startVPN(config.toNativeConfig())
    }
}

// openvpn/src/main/java/com/windscribe/vpn/openvpn/OpenVPNManager.kt
class OpenVPNManager {
    external fun startVPN(config: String): Int // JNI call to C++
}
```

### mobile/tv → base (Repository Pattern)

**Mobile (Compose)**:
```kotlin
// mobile/src/main/java/com/windscribe/mobile/ui/HomeScreen.kt
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val state by viewModel.state.collectAsState()
    // UI rendering
}

// Provided by Hilt
@HiltViewModel
class HomeViewModelImpl @Inject constructor(
    private val serverListRepository: ServerListRepository // from base/
) : HomeViewModel()
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

See [docs/guides/ADDING_VPN_FEATURE.md](docs/guides/ADDING_VPN_FEATURE.md).

**Pattern**:
1. Update `base/backend` — Core VPN logic
2. Update protocol module if needed (openvpn/wgtunnel/strongswan)
3. Add UI controls in `mobile/` (Compose) and `tv/` (XML)
4. Add preference if user-configurable
5. Update database schema if persistent state needed
6. Add tests (unit + integration)

### Database Migration

See [docs/guides/DATABASE_MIGRATIONS.md](docs/guides/DATABASE_MIGRATIONS.md).

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
- Use RxJava (fully removed, use coroutines/flows)
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

**Last Updated**: 2026-04-22
**Maintained By**: Engineering Team
**Next Review**: Post-feature additions or major architecture changes