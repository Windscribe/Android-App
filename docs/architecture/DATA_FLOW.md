# Data Flow Patterns — Windscribe Android App

## Overview

This document illustrates how data flows through the Windscribe Android app for common operations. Understanding these patterns is essential for adding features, debugging issues, and maintaining architectural consistency.

---

## Core Patterns

###  Pattern 1: API Request → Database → UI

**Use Case**: Fetching server list, user data, notifications

**Flow**:
```
┌─────────────┐
│  UI Layer   │  User taps "Refresh Servers"
│ (Composable)│
└──────┬──────┘
       │ observes StateFlow
       ↓
┌──────▼──────┐
│  ViewModel  │  viewModel.refreshServers()
└──────┬──────┘
       │ calls repository
       ↓
┌──────▼──────────────┐
│   Repository        │  serverListRepository.updateServerList()
│ (Business Logic)    │
└──┬──────────────┬───┘
   │              │
   │ API call     │ Database write
   ↓              ↓
┌──▼───────┐  ┌──▼──────────┐
│   API    │  │  Database   │
│ Manager  │  │  (Room)     │
└──┬───────┘  └──┬──────────┘
   │             │
   │ wsnet      │ DAO
   ↓             ↓
┌──▼─────────────▼───┐
│   Local Database   │  Data persisted
└────────────────────┘
       │
       │ Flow emission
       ↓
┌──────▼──────┐
│  ViewModel  │  Updates state
└──────┬──────┘
       │ StateFlow
       ↓
┌──────▼──────┐
│  UI Layer   │  UI re-renders
└─────────────┘
```

**Code Example**:

```kotlin
// 1. UI observes ViewModel state
@Composable
fun LocationsScreen(viewModel: LocationsViewModel) {
    val state by viewModel.state.collectAsState()

    when (state) {
        is LocationsState.Loading -> LoadingSpinner()
        is LocationsState.Success -> ServerList(state.data)
        is LocationsState.Error -> ErrorMessage(state.message)
    }

    Button(onClick = { viewModel.refreshServers() }) {
        Text("Refresh")
    }
}

// 2. ViewModel manages state
class LocationsViewModel(
    private val repository: ServerListRepository
) : ViewModel() {
    private val _state = MutableStateFlow<LocationsState>(LocationsState.Loading)
    val state: StateFlow<LocationsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.regions.collect { regions ->
                _state.value = LocationsState.Success(regions)
            }
        }
    }

    fun refreshServers() {
        viewModelScope.launch {
            _state.value = LocationsState.Loading
            when (val result = repository.updateServerList()) {
                is CallResult.Success -> { /* Flow will emit new data */ }
                is CallResult.Error -> _state.value = LocationsState.Error(result.errorMessage)
            }
        }
    }
}

// 3. Repository orchestrates API + Database
class ServerListRepository(
    private val apiCallManager: IApiCallManager,
    private val localDbInterface: LocalDbInterface
) {
    private var _events = MutableSharedFlow<List<RegionAndCities>>(replay = 1)
    val regions: SharedFlow<List<RegionAndCities>> = _events

    init {
        loadFromDatabase()
    }

    private fun loadFromDatabase() {
        scope.launch {
            _events.emit(localDbInterface.getAllRegionAsync())
        }
    }

    suspend fun updateServerList(): CallResult<Unit> {
        val apiResult = result<ServerListResponse> {
            apiCallManager.getServerList(userName)
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

// 4. API Manager calls wsnet
class ApiCallManager(
    private val wsNetServerAPI: WSNetServerAPI
) : IApiCallManager {
    override suspend fun getServerList(userName: String) =
        suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.serverLocations(userName) { code, json ->
                buildResponse(continuation, code, json, ServerListResponse::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
}

// 5. Database interface wraps DAO
class LocalDatabaseImpl(
    private val regionDao: RegionDao,
    private val cityDao: CityDao
) : LocalDbInterface {
    override suspend fun getAllRegionAsync(): List<RegionAndCities> {
        return regionAndCitiesDao.getAllRegionAsync()
    }

    override suspend fun addToRegions(regions: List<Region>) {
        regionDao.deleteAll()
        regionDao.addAll(regions)
    }
}
```

**Key Points**:
- Repository is source of truth
- Database acts as cache (offline support)
- Flow provides reactive updates
- UI automatically updates when data changes

---

### Pattern 2: VPN Connection Flow

**Use Case**: User connects to VPN

**Flow**:
```
┌─────────────┐
│  UI Layer   │  User taps "Connect" button
└──────┬──────┘
       ↓
┌──────▼──────┐
│  ViewModel  │  viewModel.connect()
└──────┬──────┘
       ↓
┌──────▼──────────────────┐
│  WindVpnController      │  Main VPN orchestrator
│  - Select backend       │
│  - Prepare config       │
└──────┬──────────────────┘
       ↓
┌──────▼──────────────────┐
│  VpnBackend             │  Protocol-specific backend
│  (OpenVPN/IKEv2/WG)    │
└──────┬──────────────────┘
       ↓
┌──────▼──────────────────┐
│  Native Library         │  JNI call to C++/Go
│  (OpenVPN/StrongSwan/  │
│   WireGuard-go)         │
└──────┬──────────────────┘
       ↓
┌──────▼──────────────────┐
│  WindVpnService         │  Foreground service started
│  - Show notification    │
│  - Create VPN interface │
└──────┬──────────────────┘
       ↓
┌──────▼──────────────────────┐
│  VPNConnectionStateManager  │  State: CONNECTING
└──────┬──────────────────────┘
       │ (after connection established)
       ↓
┌──────▼──────────────────────┐
│  VPNConnectionStateManager  │  State: CONNECTED
└──────┬──────────────────────┘
       │ StateFlow emission
       ↓
┌──────▼──────┐
│  ViewModel  │  Updates state
└──────┬──────┘
       ↓
┌──────▼──────┐
│  UI Layer   │  Shows "Connected" + server info
└─────────────┘
```

**Code Example**:

```kotlin
// 1. UI triggers connection
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val vpnState by viewModel.vpnState.collectAsState()

    ConnectButton(
        state = vpnState,
        onClick = {
            when (vpnState) {
                is VPNState.Disconnected -> viewModel.connect()
                is VPNState.Connected -> viewModel.disconnect()
                else -> { /* Connecting/Disconnecting - disabled */ }
            }
        }
    )
}

// 2. ViewModel delegates to controller
class HomeViewModel(
    private val windVpnController: WindVpnController,
    private val vpnConnectionStateManager: VPNConnectionStateManager
) : ViewModel() {
    val vpnState: StateFlow<VPNState> = vpnConnectionStateManager.state

    fun connect() {
        viewModelScope.launch {
            windVpnController.connect()
        }
    }
}

// 3. WindVpnController orchestrates connection
class WindVpnController(
    private val preferencesHelper: PreferencesHelper,
    private val serverRepository: ServerListRepository
) {
    fun connect() {
        val protocol = preferencesHelper.selectedProtocol
        val server = serverRepository.getSelectedServer()
        val config = buildConfig(protocol, server)

        // Select backend based on protocol
        val backend = when (protocol) {
            Protocol.OPENVPN_UDP, Protocol.OPENVPN_TCP -> vpnBackend
            Protocol.IKEV2 -> ikev2Backend
            Protocol.WIREGUARD -> wireGuardBackend
            else -> vpnBackend
        }

        // Start VPN service
        startVpnService()

        // Connect via backend
        backend.connect(config)
    }
}

// 4. VpnBackend calls native library
class VpnBackend {
    fun connect(config: OpenVPNConfig) {
        vpnConnectionStateManager.setState(VPNState.Connecting)
        openVPNManager.startVPN(config.toNativeString())
    }

    // Called by native code when connected
    @JvmStatic
    fun onConnected() {
        vpnConnectionStateManager.setState(VPNState.Connected(serverIP, publicIP))
    }
}

// 5. State propagates to UI
class VPNConnectionStateManager {
    private val _state = MutableStateFlow<VPNState>(VPNState.Disconnected)
    val state: StateFlow<VPNState> = _state.asStateFlow()

    fun setState(newState: VPNState) {
        _state.value = newState
    }
}
```

---

### Pattern 3: Auto-Connect on Network Change

**Use Case**: Network changes, auto-connect triggers

**Flow**:
```
┌──────────────────────┐
│  Network Change      │  WiFi connects / Mobile data enabled
└──────┬───────────────┘
       ↓
┌──────▼─────────────────────────┐
│  DeviceStateReceiverWrapper    │  BroadcastReceiver
│  - Detects network change      │
└──────┬─────────────────────────┘
       ↓
┌──────▼─────────────────────────┐
│  DeviceStateManager            │
│  - Clear auto-secure whitelist │
│  - Get new network info         │
└──────┬─────────────────────────┘
       ↓
┌──────▼─────────────────────────┐
│  AutoConnectService            │
│  - Check whitelist              │
│  - Check auto-connect setting   │
│  - Load network profile         │
└──────┬─────────────────────────┘
       │ (if should auto-connect)
       ↓
┌──────▼─────────────────────────┐
│  WindVpnController             │
│  - Select protocol (network     │
│    profile or user default)    │
│  - Connect                      │
└────────────────────────────────┘
```

**Code Example**:

```kotlin
// 1. BroadcastReceiver detects network change
class DeviceStateReceiverWrapper : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ConnectivityManager.CONNECTIVITY_ACTION -> {
                val networkInfo = getActiveNetworkInfo(context)
                deviceStateManager.onNetworkChanged(networkInfo)
            }
        }
    }
}

// 2. DeviceStateManager processes change
class DeviceStateManager {
    private val whitelistedNetworks = mutableSetOf<String>()

    fun onNetworkChanged(networkInfo: NetworkInfo) {
        // Clear whitelist on network change
        clearWhitelist()

        // Update current network
        currentNetwork = networkInfo

        // Trigger auto-connect check
        autoConnectService.checkAutoConnect(networkInfo)
    }

    fun clearWhitelist() {
        whitelistedNetworks.clear()
    }

    fun isWhitelisted(networkId: String): Boolean {
        return whitelistedNetworks.contains(networkId)
    }
}

// 3. AutoConnectService determines if should connect
class AutoConnectService {
    fun checkAutoConnect(networkInfo: NetworkInfo) {
        // Check whitelist
        if (deviceStateManager.isWhitelisted(networkInfo.id)) {
            return  // User manually disconnected on this network
        }

        // Check auto-connect setting
        if (!preferencesHelper.autoConnect) {
            return  // Auto-connect disabled
        }

        // Check if already connected
        if (vpnConnectionStateManager.isConnected()) {
            return  // Already connected
        }

        // Load network profile (per-network protocol preference)
        val networkProfile = networkInfoRepository.getNetworkProfile(networkInfo.id)
        val protocol = networkProfile?.preferredProtocol ?: preferencesHelper.selectedProtocol

        // Connect with appropriate protocol
        windVpnController.connect(protocol)
    }
}
```

---

### Pattern 4: Preference Changes → Behavior Updates

**Use Case**: User changes setting, app behavior updates

**Flow**:
```
┌─────────────┐
│  UI Layer   │  User toggles "Auto-Secure" ON
└──────┬──────┘
       ↓
┌──────▼──────┐
│  ViewModel  │  viewModel.setAutoSecure(true)
└──────┬──────┘
       ↓
┌──────▼──────────────────┐
│  PreferencesHelper      │  Writes to Tray storage
│  isAutoSecureOn = true  │
└──────┬──────────────────┘
       │ (preference change broadcast)
       ↓
┌──────▼──────────────────┐
│  DeviceStateManager     │  Reacts to preference change
│  - Check current network│
│  - Trigger auto-connect │
│    if needed            │
└──────┬──────────────────┘
       ↓ (if unsafe network + VPN off)
┌──────▼──────────────────┐
│  WindVpnController      │  Connects VPN
└─────────────────────────┘
```

**Code Example**:

```kotlin
// 1. UI triggers preference change
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val autoSecure by viewModel.autoSecure.collectAsState()

    Switch(
        checked = autoSecure,
        onCheckedChange = { viewModel.setAutoSecure(it) }
    )
}

// 2. ViewModel updates preference
class SettingsViewModel(
    private val preferencesHelper: PreferencesHelper
) : ViewModel() {
    val autoSecure: StateFlow<Boolean> = preferencesHelper.autoSecureFlow

    fun setAutoSecure(enabled: Boolean) {
        preferencesHelper.isAutoSecureOn = enabled
    }
}

// 3. PreferencesHelper persists + broadcasts change
class AppPreferencesImpl(
    private val appPreferences: TrayAppPreferences
) : PreferencesHelper {
    private val _autoSecureFlow = MutableStateFlow(false)
    override val autoSecureFlow: StateFlow<Boolean> = _autoSecureFlow.asStateFlow()

    override var isAutoSecureOn: Boolean
        get() = appPreferences.getBoolean(PreferencesKeyConstants.AUTO_SECURE, false)
        set(value) {
            appPreferences.putBoolean(PreferencesKeyConstants.AUTO_SECURE, value)
            _autoSecureFlow.value = value
            // Broadcast preference change
            broadcastPreferenceChange(PreferencesKeyConstants.AUTO_SECURE, value)
        }
}

// 4. DeviceStateManager reacts
class DeviceStateManager {
    init {
        preferencesHelper.autoSecureFlow.collect { enabled ->
            onAutoSecureChanged(enabled)
        }
    }

    private fun onAutoSecureChanged(enabled: Boolean) {
        if (enabled) {
            // Auto-secure enabled - check if should connect
            val network = currentNetwork
            if (network.isTrusted()) {
                // Trusted network (home) - no action
            } else {
                // Untrusted network - connect if VPN off
                if (!vpnConnectionStateManager.isConnected()) {
                    windVpnController.connect()
                }
            }
        }
    }
}
```

---

## Advanced Flows

### Flow 5: User Login → Session Management

```
User enters credentials
    ↓
LoginViewModel.login(username, password)
    ↓
UserRepository.login()
    ↓
ApiCallManager.login() → wsnet
    ↓
API returns session token + user data
    ↓
PreferencesHelper.sessionToken = token
LocalDb.insertUser(userData)
    ↓
Repository emits User object via Flow
    ↓
ViewModel updates state
    ↓
UI navigates to HomeScreen
    ↓
Background: ServerListRepository.updateServerList()
```

### Flow 6: Protocol Fallback on Connection Failure

```
User connects → Preferred protocol: WireGuard
    ↓
WindVpnController.connect(WireGuard)
    ↓
WireGuardBackend.connect()
    ↓
Connection fails (timeout after 30s)
    ↓
VpnBackend emits ConnectionError
    ↓
ProtocolConnectionManager receives error
    ↓
Attempt fallback: OpenVPN TCP (port 443)
    ↓
WindVpnController.connect(OpenVPN_TCP)
    ↓
VpnBackend.connect()
    ↓
Connection succeeds
    ↓
UI shows "Connected via OpenVPN TCP"
```

### Flow 7: Database Migration

```
App upgrade (v41 → v42)
    ↓
WindscribeDatabase instantiated
    ↓
Room detects version mismatch (41 < 42)
    ↓
Executes MIGRATION_41_42
    ↓
SQL: ALTER TABLE SomeEntity ADD COLUMN newField TEXT
    ↓
Database schema updated to v42
    ↓
App continues normally
```

---

## Data Sources

### 1. Remote (API)
- Server lists
- User account data
- Notifications
- Static IP configurations
- Session tokens

**Access Pattern**: `Repository` → `ApiCallManager` → `wsnet`

### 2. Local Database (Room)
- Cached server lists
- Ping times
- Network profiles (per-network configs)
- Notification history
- User preferences (subset for queries)

**Access Pattern**: `Repository` → `LocalDbInterface` → `DAO`

### 3. Preferences (Tray)
- User settings (50+ preferences)
- Protocol selection
- Auto-connect state
- UI preferences (theme, language)

**Access Pattern**: `PreferencesHelper` → `TrayAppPreferences`

### 4. Native State (In-Memory)
- VPN connection state
- Current IP address
- Network info (SSID, type)
- Auto-secure whitelist

**Access Pattern**: State managers (`VPNConnectionStateManager`, `DeviceStateManager`)

---

## State Flow Patterns

### StateFlow for Single Value State

```kotlin
class HomeViewModel {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadData() {
        _isLoading.value = true
        // ... load data
        _isLoading.value = false
    }
}
```

### SharedFlow for Events

```kotlin
class ServerListRepository {
    private val _events = MutableSharedFlow<List<Region>>(replay = 1)
    val regions: SharedFlow<List<Region>> = _events

    suspend fun updateServerList() {
        val newRegions = api.fetchRegions()
        _events.emit(newRegions)  // All collectors receive update
    }
}
```

### Sealed Classes for Complex State

```kotlin
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel {
    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun login(username: String, password: String) {
        _state.value = LoginState.Loading
        viewModelScope.launch {
            when (val result = userRepository.login(username, password)) {
                is CallResult.Success -> _state.value = LoginState.Success(result.data)
                is CallResult.Error -> _state.value = LoginState.Error(result.errorMessage)
            }
        }
    }
}
```

---

## Common Anti-Patterns (Avoid)

### ❌ UI directly accessing Repository

```kotlin
// DON'T DO THIS
@Composable
fun BadScreen(repository: ServerListRepository) {
    val regions by repository.regions.collectAsState(emptyList())
    // UI should NOT directly observe Repository
}
```

**Why**: Violates MVVM separation, makes testing harder, couples UI to data layer

**Instead**: Use ViewModel
```kotlin
@Composable
fun GoodScreen(viewModel: LocationsViewModel) {
    val state by viewModel.state.collectAsState()
    // ViewModel mediates between UI and Repository
}
```

### ❌ Blocking calls on Main Thread

```kotlin
// DON'T DO THIS
fun loadServers() {
    val servers = runBlocking {  // Blocks UI thread!
        apiCallManager.getServerList()
    }
}
```

**Why**: Freezes UI, causes ANR (Application Not Responding)

**Instead**: Use coroutines with proper scope
```kotlin
fun loadServers() {
    viewModelScope.launch {  // Non-blocking
        val servers = apiCallManager.getServerList()
    }
}
```

### ❌ Direct API calls bypassing Repository

```kotlin
// DON'T DO THIS
class BadViewModel(private val api: IApiCallManager) {
    fun loadData() {
        api.getServerList()  // Bypasses repository caching
    }
}
```

**Why**: No caching, no offline support, violates architecture

**Instead**: Use Repository
```kotlin
class GoodViewModel(private val repository: ServerListRepository) {
    fun loadData() {
        repository.updateServerList()  // Uses cache, handles errors
    }
}
```

---

## References

- [AGENTS.md](../../AGENTS.md) — Architecture overview
- [docs/architecture/MODULE_STRUCTURE.md](MODULE_STRUCTURE.md) — Module details
- [docs/architecture/VPN_PROTOCOLS.md](VPN_PROTOCOLS.md) — Protocol flows
- [SKILL.md](../../SKILL.md) — Implementation workflows

---

**Last Updated**: 2026-04-22
**Maintained By**: Engineering Team
