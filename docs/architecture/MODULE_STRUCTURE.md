# Module Structure — Windscribe Android App

## Overview

The Windscribe Android app is organized into a multi-module architecture with clear separation of concerns. This document provides a deep-dive into each module's responsibility, key components, and interaction patterns.

---

## Module Dependency Hierarchy

```
┌─────────────────────────────────────────────────┐
│              Application Layer                   │
│  ┌──────────────────┬─────────────────────┐    │
│  │   mobile/        │   tv/               │    │
│  │   (Compose UI)   │   (XML UI)          │    │
│  └────────┬─────────┴──────┬──────────────┘    │
│           │                │                     │
│           └────────┬───────┘                     │
│                    │                             │
│  ┌─────────────────▼──────────────────────┐    │
│  │           base/                         │    │
│  │   (Core business logic & state)        │    │
│  └──┬──────┬──────┬──────┬─────────────┬──┘    │
│     │      │      │      │             │        │
├─────┼──────┼──────┼──────┼─────────────┼────────┤
│     │      │      │      │             │        │
│  ┌──▼──┐ ┌▼───┐ ┌▼───┐ ┌▼──────┐  ┌───▼───┐   │
│  │wsnet│ │ovpn│ │wg  │ │strong │  │common │   │
│  │     │ │    │ │tun │ │swan   │  │       │   │
│  └─────┘ └────┘ └────┘ └───────┘  └───────┘   │
└─────────────────────────────────────────────────┘
```

**Key Principles**:
1. **UI modules depend on base** — Never the reverse
2. **base orchestrates protocols** — Protocol modules are independent
3. **wsnet is the ONLY API client** — No direct Retrofit/OkHttp
4. **No circular dependencies** — Strict unidirectional flow

---

## 1. base/ — Core Module

**Purpose**: All business logic, state management, data persistence, and VPN orchestration.

**Size**: ~85% Kotlin, 15% Java (legacy data models)

**Responsibility**: Acts as the "brain" of the application. Contains all core functionality that both mobile and TV UIs consume.

### Submodules

#### api/
**Purpose**: API integration via wsnet library

**Key Files**:
- `IApiCallManager.kt` — Interface defining all API endpoints
- `ApiCallManager.kt` — Implementation using wsnet
- `GenericResponseClass.kt` — API response wrapper

**Pattern**:
```kotlin
interface IApiCallManager {
    suspend fun getServerList(userName: String): GenericResponseClass<ServerListResponse?, ApiErrorResponse?>
    suspend fun login(username: String, password: String): GenericResponseClass<LoginResponse?, ApiErrorResponse?>
    // ... all other endpoints
}
```

**No Direct HTTP Calls**: ALL API requests MUST go through wsnet. This centralizes:
- Authentication headers
- Retry logic
- SSL pinning
- Error handling
- Analytics
- Circuit breaking

---

#### backend/
**Purpose**: VPN protocol communication and control

**Structure**:
```
backend/
├── utils/
│   └── WindVpnController.kt        # Main VPN controller
├── openvpn/
│   ├── VpnBackend.kt               # OpenVPN backend
│   └── DeviceStateReceiverWrapper.kt  # Network changes
├── ikev2/
│   └── IKev2VpnBackend.kt          # IKEv2 backend
└── wireguard/
    └── WireGuardBackend.kt         # WireGuard backend
```

**Key Responsibilities**:
1. **Protocol Selection** — Choose backend based on user preference
2. **Connection Management** — Start/stop/reconnect VPN
3. **State Propagation** — Emit connection state changes
4. **Error Handling** — Protocol fallback on failure
5. **Network Monitoring** — React to network changes

**Main Class**: `WindVpnController`
- Entry point for all VPN operations
- Manages protocol backends (OpenVPN, IKEv2, WireGuard)
- Handles auto-connect logic
- Whitelist management (auto-secure)

**Backend Pattern**:
Each protocol has its own backend class implementing a common interface:
```kotlin
interface VpnBackend {
    fun connect(config: VPNConfig)
    fun disconnect()
    fun getState(): VPNState
}
```

---

#### localdatabase/
**Purpose**: Room database for all persistent data

**Structure**:
```
localdatabase/
├── WindscribeDatabase.kt           # Database instance + migrations
├── entities/                       # Data models (@Entity classes)
│   ├── Region.kt
│   ├── City.kt
│   ├── StaticRegion.kt
│   ├── PingTime.kt
│   └── NetworkInfo.kt
└── dao/                            # Data Access Objects
    ├── RegionDao.kt
    ├── CityDao.kt
    ├── StaticRegionDao.kt
    └── NetworkInfoDao.kt
```

**Key Entities**:
- **Region** — VPN server regions (continents)
- **City** — Cities within regions (server locations)
- **StaticRegion** — Static IP configurations
- **PingTime** — Server latency measurements
- **NetworkInfo** — Saved network profiles (per-network configs)

**Migration Strategy**:
- Version tracked in `WindscribeDatabase.kt`
- Migration scripts in companion object
- Schema exports in `base/schemas/` folder
- ALWAYS test migrations with instrumented tests

---

#### repository/
**Purpose**: Data layer — orchestrates API calls and database operations

**Key Classes**:
- `ServerListRepository` — Server/region management
- `UserRepository` — User account data
- `NotificationRepository` — In-app notifications
- `EmergencyConnectRepository` — Emergency connect fallback
- `LatencyRepository` — Server ping times

**Pattern** (Example: ServerListRepository):
```kotlin
class ServerListRepository @Inject constructor(
    private val scope: CoroutineScope,
    private val apiCallManager: IApiCallManager,
    private val localDbInterface: LocalDbInterface,
    private val preferencesHelper: PreferencesHelper
) {
    private var _events = MutableSharedFlow<List<RegionAndCities>>(replay = 1)
    val regions: SharedFlow<List<RegionAndCities>> = _events

    init {
        load()  // Load from database on initialization
    }

    fun load() {
        scope.launch {
            _events.emit(localDbInterface.getAllRegionAsync())
        }
    }

    suspend fun updateServerList(): CallResult<Unit> {
        val apiResult = result<ServerListResponse> {
            apiCallManager.getServerList(preferencesHelper.userName)
        }

        return when (apiResult) {
            is CallResult.Success -> {
                localDbInterface.addToRegions(apiResult.data.regions)
                _events.emit(localDbInterface.getAllRegionAsync())
                CallResult.Success(Unit)
            }
            is CallResult.Error -> apiResult
        }
    }
}
```

**Responsibilities**:
1. Fetch data from API
2. Store in Room database
3. Emit updates via SharedFlow
4. Handle errors and retry logic
5. Manage cache invalidation

---

#### services/
**Purpose**: Android services for background operations

**Key Services**:

**WindVpnService** (`WindVpnService.kt`)
- **Type**: Foreground service (persistent notification)
- **Purpose**: Main VPN service, runs while VPN is connected
- **Lifecycle**: Started on VPN connect, stopped on disconnect
- **Responsibilities**:
  - Maintain VPN connection
  - Handle protocol switching
  - Emit state changes
  - Manage notification

**AutoConnectService** (`AutoConnectService.kt`)
- **Type**: Background service (triggered by broadcasts)
- **Purpose**: Auto-connect on network changes
- **Triggers**:
  - WiFi connected
  - Mobile data connected
  - Airplane mode disabled
- **Responsibilities**:
  - Check auto-secure whitelist
  - Verify auto-connect setting
  - Trigger connection if conditions met
  - Apply per-network protocol preferences

**UpdateService** (`UpdateService.kt`)
- **Type**: WorkManager periodic task
- **Purpose**: Check for app updates
- **Schedule**: Every 24 hours
- **Responsibilities**:
  - Fetch latest version from API
  - Show update notification if newer version available
  - Handle in-app update flow (Google Play only)

---

#### state/
**Purpose**: App-level state management

**Key Classes**:

**DeviceStateManager** (`DeviceStateManager.kt`)
- Network state tracking
- Auto-secure whitelist management
- Network change detection
- Network type identification (WiFi, Mobile, Unknown)

**VPNConnectionStateManager** (`VPNConnectionStateManager.kt`)
- VPN connection state (Disconnected, Connecting, Connected, Disconnecting)
- State flow emissions
- Error state handling

**NetworkInformationManager** (`NetworkInformationManager.kt`)
- Current network information (SSID, type, IP)
- Network capability checking
- Per-network configuration lookup

---

#### apppreference/
**Purpose**: Preferences storage via Tray library

**Key Classes**:
- `PreferencesHelper` — Interface for all preferences
- `AppPreferencesImpl` — Implementation using Tray

**Pattern**:
```kotlin
@Singleton
interface PreferencesHelper {
    var userName: String
    var isAutoSecureOn: Boolean
    var selectedProtocol: String
    var autoConnect: Boolean
    // ... ~50 more preferences
}

@Singleton
class AppPreferencesImpl @Inject constructor(
    private val appPreferences: TrayAppPreferences
) : PreferencesHelper {
    override var userName: String
        get() = appPreferences.getString(PreferencesKeyConstants.USER_NAME, "")
        set(value) = appPreferences.put(PreferencesKeyConstants.USER_NAME, value)

    // ... implementations for all preferences
}
```

**Why Tray?**: Multi-process support (VPN service runs in separate process)

---

#### managers/
**Purpose**: Feature-specific managers

**Key Classes**:
- `ProtocolConnectionManager` — Protocol switching and fallback
- `LocationManager` — Server location selection
- `BillingManager` — In-app purchases (Google Play variant)
- `FirebaseManager` — Push notifications (Google Play variant)

---

#### constants/
**Purpose**: Constant definitions

**Key Files**:
- `PreferencesKeyConstants.kt` — All preference keys
- `NetworkKeyConstants.kt` — Network-related constants
- `VPNStateConstants.kt` — VPN state values

---

## 2. mobile/ — Phone/Tablet UI Module

**Purpose**: Jetpack Compose UI for phones and tablets

**Language**: 100% Kotlin

**Architecture**: MVVM with Compose

**Size**: ~95% Kotlin, 5% Java (billing interfaces for Google Play variant)

### Structure

```
mobile/src/main/java/com/windscribe/mobile/
├── ui/
│   ├── AppStartActivity.kt        # Main entry point
│   ├── HomeScreen.kt
│   ├── LocationsScreen.kt
│   ├── SettingsScreen.kt
│   └── ... (all Compose screens)
├── nav/
│   ├── Screen.kt                  # Route definitions
│   └── NavigationStack.kt         # NavHost setup; resolves @HiltViewModels via hiltViewModel()
└── viewmodel/
    ├── HomeViewModel.kt
    ├── LocationsViewModel.kt
    └── ... (all ViewModels)
```

### Key Patterns

**Navigation**:
```kotlin
sealed class Screen(val route: String) {
    object Home: Screen("home")
    object Locations: Screen("locations")
    object Settings: Screen("settings")
}
```

**ViewModel Injection**:
```kotlin
@Composable
fun NavigationStack() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            ViewModelRoute(HomeViewModel::class.java) { viewModel ->
                HomeScreen(viewModel)
            }
        }
    }
}
```

**State Management**:
```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val serverListRepository: ServerListRepository
) : ViewModel() {
    private val _state = MutableStateFlow<HomeState>(HomeState.Loading)
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            serverListRepository.regions.collect { regions ->
                _state.value = HomeState.Success(regions)
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val state by viewModel.state.collectAsState()
    // Render UI based on state
}
```

---

## 3. tv/ — Android TV UI Module

**Purpose**: Android TV UI (leanback experience)

**Language**: 100% Kotlin

**Architecture**: MVP with XML layouts

**UI Framework**: XML + View Binding

### Structure

```
tv/src/main/java/com/windscribe/tv/
├── splash/
│   └── SplashActivity.kt          # Entry point
├── home/
│   ├── HomeActivity.kt
│   └── HomePresenter.kt
├── serverlist/
│   ├── ServerListActivity.kt
│   └── ServerListPresenter.kt
└── settings/
    ├── SettingsActivity.kt
    └── SettingsPresenter.kt
```

### Key Differences from Mobile

| Aspect | Mobile | TV |
|--------|--------|-----|
| UI Framework | Jetpack Compose | XML layouts |
| Architecture | MVVM | MVP |
| Navigation | NavController | Activity-based |
| Input | Touch | D-pad/remote |
| Design | Material 3 | Leanback |

**Why XML for TV?**: Leanback library better supports XML. TV module may migrate to Compose for TV in future.

---

## 4. openvpn/ — OpenVPN Protocol Module

**Purpose**: OpenVPN implementation (UDP, TCP, Stealth, WSTunnel)

**Language**: Native C++ (OpenVPN 3 library) + Kotlin wrapper

**Build**: Compiled from source (CMake + NDK)

### Structure

```
openvpn/
├── src/main/cpp/                  # Native code
│   ├── openvpn3/                  # OpenVPN 3 library
│   ├── jni/                       # JNI bindings
│   └── CMakeLists.txt
└── src/main/java/
    └── com/windscribe/vpn/openvpn/
        └── OpenVPNManager.kt      # Kotlin ↔ JNI interface
```

### Supported Variants

1. **OpenVPN UDP** — Fast, best for most networks
2. **OpenVPN TCP** — Reliable, works through restrictive firewalls
3. **Stealth** — OpenVPN TCP wrapped with stunnel (obfuscation)
4. **WSTunnel** — OpenVPN over WebSocket (maximum firewall bypass)

### JNI Interface

```kotlin
class OpenVPNManager {
    external fun startVPN(config: String): Int
    external fun stopVPN()
    external fun getStatus(): String

    companion object {
        init {
            System.loadLibrary("openvpn3")
        }
    }
}
```

---

## 5. strongswan/ — IKEv2 Protocol Module

**Purpose**: IKEv2/IPSec implementation

**Language**: Native C (StrongSwan) + Kotlin wrapper

**Build**: Prebuilt binaries (`.so` files included)

### Structure

```
strongswan/
├── libs/                          # Prebuilt .so files
│   ├── armeabi-v7a/
│   ├── arm64-v8a/
│   ├── x86/
│   └── x86_64/
└── src/main/java/
    └── org/strongswan/android/
        └── logic/VpnStateService.kt
```

### Why Prebuilt?

- StrongSwan build process is complex (requires autotools, many dependencies)
- Binaries are stable and rarely need updates
- Reduces build time significantly

### Updating Binaries

See [docs/guides/STRONGSWAN_UPDATE.md](../guides/STRONGSWAN_UPDATE.md) (to be created)

---

## 6. wgtunnel/ — Multi-Protocol Native Module

**Purpose**: WireGuard, WSTunnel, Stunnel, and ControlD (ctrld) in single library

**Language**: Go (compiled to single `.so` file)

**Build**: Go → gomobile → Android native library

### Structure

```
wgtunnel/
├── src/main/go/                   # Go source code
│   ├── wireguard/                 # WireGuard implementation
│   ├── wstunnel/                  # WebSocket tunnel
│   ├── stunnel/                   # Stealth protocol (obfuscation)
│   └── ctrld/                     # ControlD DNS (DoH/DoT)
└── src/main/java/
    └── com/windscribe/vpn/wgtunnel/
        └── WgTunnelManager.kt     # Kotlin ↔ Go interface
```

### Why Single Library?

- **Code Sharing** — Common DNS routing logic
- **Smaller APK** — One `.so` vs multiple
- **Simpler Build** — Single Go build produces all protocols
- **Faster Switching** — No library loading overhead

### Protocols Included

1. **WireGuard** — Modern, fast VPN protocol
2. **WSTunnel** — OpenVPN over WebSocket wrapper
3. **Stunnel** — TLS wrapper for obfuscation (Stealth protocol)
4. **ControlD (ctrld)** — DNS-over-HTTPS/DNS-over-TLS client

---

## 7. common/ — Tunnel Wrapper Module

**Purpose**: Tunnel abstraction and DNS traffic separation

**Language**: Kotlin

### Structure

```
common/src/main/java/com/windscribe/vpn/common/
├── TunnelManager.kt               # Tunnel abstraction
└── DnsResolver.kt                 # DNS routing logic
```

### Responsibilities

**TunnelManager**:
- Tunnel interface creation
- Split tunneling logic
- Per-app VPN routing
- Traffic statistics

**DnsResolver**:
- DNS query routing (VPN DNS vs custom DoH/DoT)
- DNS leak prevention
- R.O.B.E.R.T integration

---

## 8. wsnet/ — Networking Library Module

**Purpose**: In-house HTTP client for ALL API calls

**Language**: Kotlin + Java (legacy)

**Why Custom Library?**:
- Centralized authentication
- Automatic retry with exponential backoff
- SSL certificate pinning
- Request/response analytics
- Circuit breaker pattern
- Error standardization

### Key Classes

**WSNetServerAPI** — Main API client
```kotlin
class WSNetServerAPI {
    fun serverLocations(
        userName: String,
        callback: (code: Int, json: String?) -> Unit
    ): CancellableCall

    fun login(
        username: String,
        password: String,
        twoFACode: String?,
        callback: (code: Int, json: String?) -> Unit
    ): CancellableCall

    // ... all other API endpoints
}
```

**Features**:
- Automatic auth header injection
- SSL pinning with backup pins
- Network change handling
- Request queuing
- Analytics events

---

## 9. test/ — Shared Test Utilities Module

**Purpose**: Common test helpers and utilities

**Language**: Kotlin

### Structure

```
test/src/main/java/com/windscribe/vpn/test/
├── TestHelpers.kt                 # Common test utilities
├── MockData.kt                    # Test data generators
└── FakeRepositories.kt            # Fake implementations for testing
```

---

## Module Size Comparison

| Module | Lines of Code (est.) | Primary Language |
|--------|---------------------|------------------|
| base | ~45,000 | Kotlin (85%), Java (15%) |
| mobile | ~15,000 | Kotlin (100%) |
| tv | ~12,000 | Kotlin (100%) |
| openvpn | ~8,000 (Kotlin), ~50,000 (C++) | C++, Kotlin |
| strongswan | ~2,000 (Kotlin) | Kotlin wrapper |
| wgtunnel | ~10,000 | Go, Kotlin |
| common | ~3,000 | Kotlin |
| wsnet | ~8,000 | Kotlin, Java |
| test | ~1,000 | Kotlin |

**Total**: ~150,000 lines of Kotlin/Java, ~60,000 lines of C++/Go

---

## Inter-Module Communication

### Pattern: UI → Repository → API/Database

```
[mobile/HomeScreen.kt]
    ↓ (observes StateFlow)
[mobile/HomeViewModel.kt]
    ↓ (calls repository methods)
[base/repository/ServerListRepository.kt]
    ↓ (API call via)
[base/api/ApiCallManager.kt]
    ↓ (HTTP request via)
[wsnet/WSNetServerAPI.kt]
    ↓ (network call)
API Server
```

### Pattern: VPN Connection

```
[mobile/HomeScreen.kt] — User taps "Connect"
    ↓
[mobile/HomeViewModel.kt] — viewModel.connect()
    ↓
[base/backend/utils/WindVpnController.kt] — selectBackend() + connect()
    ↓
[base/backend/openvpn/VpnBackend.kt] — prepare config
    ↓
[openvpn/OpenVPNManager.kt] — startVPN() (JNI call)
    ↓
[openvpn/src/main/cpp/jni/] — Native OpenVPN start
    ↓
[base/services/WindVpnService.kt] — Foreground service started
    ↓
[base/state/VPNConnectionStateManager.kt] — Emit CONNECTED state
    ↓
[mobile/HomeViewModel.kt] — Observe state change
    ↓
[mobile/HomeScreen.kt] — UI updates to "Connected"
```

---

## Build Configuration

### Build Variants

| Variant | Module | Features |
|---------|--------|----------|
| googleDebug | mobile, tv | Google Play services, FCM, debug logging |
| googleRelease | mobile, tv | Google Play services, FCM, ProGuard |
| fdroidDebug | mobile | No Google services, debug logging |
| fdroidRelease | mobile | No Google services, ProGuard |

### Variant-Specific Source Sets

```
mobile/
├── src/
│   ├── main/                      # Common code
│   ├── google/                    # Google Play variant
│   │   └── java/.../billing/
│   │       └── BillingManagerImpl.kt  # Real billing
│   └── fdroid/                    # F-Droid variant
│       └── java/.../billing/
│           └── BillingManagerImpl.kt  # No-op billing
```

---

## Module Migration Status

| Module | Kotlin % | Java % | Target |
|--------|----------|--------|--------|
| tv | 100% | 0% | ✅ Complete |
| mobile | 95% | 5% | 99% (billing interfaces) |
| base | 85% | 15% | 95% (data models) |
| common | 100% | 0% | ✅ Complete |
| test | 100% | 0% | ✅ Complete |

**Migration Rule**: ALL new code MUST be in Kotlin. No new Java files allowed.

---

## References

- [AGENTS.md](../../AGENTS.md) — Architecture overview
- [SKILL.md](../../SKILL.md) — Development workflows
- [docs/architecture/VPN_PROTOCOLS.md](VPN_PROTOCOLS.md) — Protocol implementations
- [docs/architecture/DATA_FLOW.md](DATA_FLOW.md) — Data flow patterns

---

**Last Updated**: 2026-04-22
**Maintained By**: Engineering Team
