# Auto-Secure Network Whitelist System

## Overview

The Auto-Secure Network Whitelist System is a session-based mechanism that prevents unwanted VPN auto-connection after a user manually disconnects on a network with auto-secure enabled. This ensures the VPN respects user intent while maintaining automatic protection across network changes.

### Key Behavior

- **User disconnects on Network A (auto-secure ON)** → VPN will NOT auto-connect while on Network A
- **User switches to Network B** → Whitelist cleared, auto-connect resumes normally
- **User returns to Network A** → VPN WILL auto-connect (whitelist was cleared when they left)
- **System disconnects** (protocol change, auto-secure OFF) → Whitelist NOT set, auto-connect resumes

## System Architecture

### Components

| Component | Role | Responsibilities |
|-----------|------|------------------|
| **DeviceStateManager** | Network State & Whitelist Tracking | - Monitors network changes<br>- Maintains whitelist state<br>- Clears whitelist on network change<br>- Exposes `isCurrentNetworkWhitelisted` StateFlow |
| **WindVpnController** | VPN Control & Whitelist Logic | - Sets whitelist on user disconnect<br>- Clears whitelist on user connect<br>- Distinguishes user vs system disconnects<br>- Optimizes AutoConnectService startup |
| **AutoConnectService** | Auto-Connect Execution | - Checks whitelist before auto-connecting<br>- Skips auto-connect on whitelisted networks<br>- Monitors network changes for auto-secure ON networks |
| **VpnBackend** | Protocol Management | - Handles protocol/port switching<br>- System disconnects (passes null error)<br>- Monitors network changes for connected VPN |
| **Windscribe.kt** | App Lifecycle & Coordination | - Clears preferences whitelist on network change<br>- Starts AutoConnectService when needed<br>- Syncs whitelist state on network transitions |
| **PreferencesHelper** | Persistence | - Stores whitelisted network name<br>- Survives app restarts (cleared on network change) |

### Data Flow

```
Network Change (DeviceStateManager)
    ↓
Whitelist Cleared (if network changed)
    ↓
AutoConnectService Started (if needed)
    ↓
Check: Auto-Secure ON? + Not Whitelisted? + Disconnected?
    ↓
Connect VPN → Clear Whitelist
```

```
User Disconnect (WindVpnController)
    ↓
Set Whitelist (current network)
    ↓
AutoConnectService Started (if auto-secure OFF or not whitelisted)
    ↓
Check: Whitelisted?
    ↓
Skip Auto-Connect
```

## Behavior Matrix

| Scenario | Auto-Secure | Action | Whitelist | Auto-Connect | Service Started |
|----------|-------------|--------|-----------|--------------|-----------------|
| User connects manually | ON/OFF | User Connect | **Cleared** ✓ | N/A | N/A |
| User disconnects | ON | User Disconnect | **Set** ✓ | **Blocked** ✗ | **No** (whitelisted + auto-secure ON) |
| User disconnects | OFF | User Disconnect | **Set** ✓ | Allowed (auto-secure OFF) | **Yes** (auto-secure OFF) |
| System disconnect (protocol change) | ON | System Disconnect | **Not Set** ✗ | Allowed | **Yes** |
| System disconnect (auto-secure OFF) | OFF | System Disconnect | **Not Set** ✗ | Allowed | **Yes** |
| Network change (away from whitelisted) | Any | Network Change | **Cleared** ✓ | Allowed | **Yes** (if new network is auto-secure ON) |
| Return to whitelisted network | ON | Network Change | **Cleared** ✓ | **Allowed** ✓ | **Yes** |
| Protocol/Port switch | ON | System Disconnect + Reconnect | **Not Set** ✗ | Allowed (auto-feature) | N/A (reconnecting) |

## Implementation Details

### 1. DeviceStateManager - Whitelist Tracking

**File**: `base/src/main/java/com/windscribe/vpn/state/DeviceStateManager.kt`

```kotlin
// Whitelisted network state - indicates if current network is whitelisted
private val _isCurrentNetworkWhitelisted = MutableStateFlow(false)
val isCurrentNetworkWhitelisted: StateFlow<Boolean> = _isCurrentNetworkWhitelisted.asStateFlow()

// Store the whitelisted network name for comparison
private var whitelistedNetworkName: String? = null

/**
 * Sets the whitelisted network name and updates the state.
 * Pass null to clear the whitelist.
 */
fun setWhitelistedNetwork(networkName: String?) {
    whitelistedNetworkName = networkName
    logger.info("Whitelisted network set to: $networkName")
    scope.launch {
        updateWhitelistedNetworkState(_networkDetail.value)
    }
}

// Auto-clear whitelist when network changes
private suspend fun processNetworkChange(force: Boolean = false) {
    val currentDetail = _networkDetail.value
    val detail = getCurrentNetworkDetail()

    if (force || currentDetail != detail) {
        _networkDetail.emit(detail)

        // Clear whitelist when network changes (so user can auto-connect when returning)
        if (currentDetail?.name != detail?.name && whitelistedNetworkName != null) {
            logger.info("Network changed from ${currentDetail?.name} to ${detail?.name} - clearing whitelist")
            whitelistedNetworkName = null
        }
    }

    updateWhitelistedNetworkState(detail)
}
```

**Key Points**:
- Session-based: Whitelist only applies while on the same network
- Auto-clears: When user leaves network, whitelist is removed
- StateFlow: Reactive updates for AutoConnectService and other observers

### 2. WindVpnController - Whitelist Logic

**File**: `base/src/main/java/com/windscribe/vpn/backend/utils/WindVpnController.kt`

#### On Connect - Clear Whitelist

```kotlin
private suspend fun createProfileAndLaunchService(...) {
    // Clear whitelist - user wants VPN, so enable future auto-connect
    preferencesHelper.whiteListedNetwork = null
    deviceStateManager.setWhitelistedNetwork(null)

    // ... connection logic
}
```

**Reasoning**: User connecting means they want VPN protection. Clear whitelist to allow auto-connect in future.

#### On Disconnect - Set Whitelist (User Only)

```kotlin
private suspend fun disconnect(
    waitForNextProtocol: Boolean = false,
    reconnecting: Boolean = false,
    error: VPNState.Error? = null
) {
    // Only whitelist on user-initiated disconnect (not system errors or reconnecting)
    val isUserDisconnect = error?.error == VPNState.ErrorType.UserDisconnect

    if (!reconnecting && isUserDisconnect) {
        // Whitelist current network - user doesn't want VPN here, block future auto-connect
        val networkName = deviceStateManager.getCurrentNetworkName()
        preferencesHelper.whiteListedNetwork = networkName
        deviceStateManager.setWhitelistedNetwork(networkName)
        logger.debug("User disconnected - whitelisted network: $networkName")
    } else {
        logger.debug("System disconnect (reconnecting=$reconnecting, error=${error?.error}) - not whitelisting")
    }
}
```

**Error Types for Disconnect Detection**:
- `VPNState.ErrorType.UserDisconnect` → User clicked disconnect → **Whitelist**
- `null` → System disconnect (protocol change, auto-secure OFF) → **Don't whitelist**
- Other errors → Connection failures → **Don't whitelist**

#### Optimize AutoConnectService Startup

```kotlin
private fun checkForReconnect() {
    scope.launch {
        val currentNetworkName = deviceStateManager.getCurrentNetworkName()
        val isWhitelisted = deviceStateManager.isCurrentNetworkWhitelisted.value
        val networkInfo = localDbInterface.getNetwork(currentNetworkName)

        // Only start AutoConnectService if:
        // 1. Auto-secure is OFF (service will reconnect when network changes to ON network)
        // 2. Network is not whitelisted (might need to reconnect on this network)
        if (networkInfo?.isAutoSecureOn == false || !isWhitelisted) {
            appContext.startAutoConnectService()
        } else {
            logger.debug("Not starting AutoConnectService - auto-secure ON and whitelisted")
        }
    }
}
```

**Optimization**: Don't start foreground service if:
- Network is auto-secure ON AND whitelisted (user just disconnected, won't auto-connect anyway)

### 3. AutoConnectService - Whitelist Check

**File**: `base/src/main/java/com/windscribe/vpn/services/AutoConnectService.kt`

```kotlin
networkInfoManager.networkInfo.collectLatest { networkInfo ->
    val isWhitelisted = deviceStateManager.isCurrentNetworkWhitelisted.value
    logger.debug("Network: ${networkInfo?.networkName}, AutoSecure: ${networkInfo?.isAutoSecureOn}, Whitelisted: $isWhitelisted")

    if (networkInfo?.isAutoSecureOn == true &&
        !isWhitelisted &&  // ← Prevents auto-connect on whitelisted networks
        vpnConnectionStateManager.state.value.status == VPNState.Status.Disconnected &&
        userRepository.user.value?.accountStatus == User.AccountStatus.Okay) {
        vpnController.connectAsync()
    } else if (networkInfo?.isAutoSecureOn == true && isWhitelisted) {
        logger.debug("Auto secure ON but network is whitelisted - skipping auto-connect")
    }
}
```

**Key Points**:
- Checks whitelist before auto-connecting
- Logs when skipping auto-connect due to whitelist
- Only affects auto-secure ON networks

### 4. VpnBackend - System Disconnects

**File**: `base/src/main/java/com/windscribe/vpn/backend/VpnBackend.kt`

#### Protocol/Port Switching on Network Change

```kotlin
// IMPORTANT: Protocol/port switching on network change requires autoConnect to be enabled
// This ensures we only auto-switch protocols when user has auto-connect features enabled
if (vpnState.status == VPNState.Status.Connected &&
    preferencesHelper.autoConnect &&  // ← Protocol switching is an "auto" feature
    networkInfo?.isAutoSecureOn == true &&
    networkInfo.isPreferredOn &&
    networkInfo.protocol != null &&
    networkInfo.port != null) {

    val currentProtocol = protocolInformation?.protocol
    val currentPort = protocolInformation?.port
    val networkProtocol = networkInfo.protocol
    val networkPort = networkInfo.port

    if (currentProtocol != networkProtocol || currentPort != networkPort) {
        isHandlingNetworkChange = true
        vpnLogger.debug("Network change detected while connected. Reconnecting with correct protocol/port...")
        // System disconnect to change protocol - don't whitelist
        appContext.vpnController.disconnectAsync(error = null)
    }
}
```

**Important**: Protocol switching requires `preferencesHelper.autoConnect == true`. This is an "auto" feature - if user has disabled auto-connect globally, we respect that and don't auto-switch protocols.

#### Auto-Secure OFF Disconnect

```kotlin
if (networkInfo?.isAutoSecureOn == false && vpnState.status == VPNState.Status.Connected) {
    isHandlingNetworkChange = true
    vpnLogger.debug("Auto-secure OFF for ${networkInfo.networkName} - system disconnecting")
    // System disconnect due to auto-secure OFF - don't whitelist
    appContext.vpnController.disconnectAsync(error = null)
}
```

**Key Points**:
- System disconnects always pass `error = null`
- This prevents whitelisting when disconnect is system-initiated
- User can still manually disconnect and whitelist the network

### 5. Windscribe.kt - Network Change Observer

**File**: `base/src/main/java/com/windscribe/vpn/Windscribe.kt`

```kotlin
// Sync preferences when network changes (auto-clear whitelist)
applicationScope.launch {
    var previousNetwork: String? = null
    deviceStateManager.networkDetail.collect { detail ->
        val currentNetwork = detail?.name

        // Clear preferences whitelist when network changes
        if (previousNetwork != null && previousNetwork != currentNetwork && preference.whiteListedNetwork != null) {
            preference.whiteListedNetwork = null
            logger.debug("Network changed from $previousNetwork to $currentNetwork - cleared preferences whitelist")
        }

        // Start AutoConnectService if needed when network changes
        if (previousNetwork != null && previousNetwork != currentNetwork && currentNetwork != null) {
            applicationScope.launch {
                try {
                    val networkInfo = windscribeDatabase.networkInfoDao().getNetwork(currentNetwork)
                    val isVpnActive = vpnConnectionStateManager.isVPNActive()

                    // Start service if: auto-secure ON, VPN not active, and autoConnect enabled
                    if (networkInfo?.isAutoSecureOn == true &&
                        !isVpnActive &&
                        preference.autoConnect &&
                        canAccessNetworkName()) {
                        logger.debug("Network changed to auto-secure ON network - starting AutoConnectService")
                        startAutoConnectService()
                    }
                } catch (e: Exception) {
                    logger.debug("Error checking network for AutoConnectService: ${e.message}")
                }
            }
        }

        previousNetwork = currentNetwork
    }
}
```

**Key Points**:
- Clears preferences whitelist on network change (backup to DeviceStateManager)
- Starts AutoConnectService when switching to auto-secure ON network
- Only starts if VPN not already active and autoConnect enabled

## Code Flow Examples

### Example 1: User Disconnects on Auto-Secure ON Network

**Scenario**: User is on "MyWiFi" (auto-secure ON), VPN connected, clicks disconnect

```
1. User clicks disconnect button
   → WindVpnController.disconnectAsync(error = UserDisconnect)

2. WindVpnController.disconnect()
   → Checks: isUserDisconnect = true, reconnecting = false
   → Gets current network: "MyWiFi"
   → Sets whitelist:
      - preferencesHelper.whiteListedNetwork = "MyWiFi"
      - deviceStateManager.setWhitelistedNetwork("MyWiFi")
   → Logs: "User disconnected - whitelisted network: MyWiFi"

3. DeviceStateManager.setWhitelistedNetwork("MyWiFi")
   → whitelistedNetworkName = "MyWiFi"
   → Emits: _isCurrentNetworkWhitelisted = true

4. WindVpnController.checkForReconnect()
   → Checks: auto-secure ON + whitelisted = true
   → Skips starting AutoConnectService
   → Logs: "Not starting AutoConnectService - auto-secure ON and whitelisted"

Result: VPN disconnected, network whitelisted, no service running
```

### Example 2: User Switches Networks (Whitelist Cleared)

**Scenario**: User on "MyWiFi" (whitelisted), switches to "OfficeWiFi" (auto-secure ON)

```
1. Network changes from "MyWiFi" to "OfficeWiFi"
   → DeviceStateManager detects network change

2. DeviceStateManager.processNetworkChange()
   → Checks: currentDetail.name ("MyWiFi") != detail.name ("OfficeWiFi")
   → Whitelist clearing logic:
      - whitelistedNetworkName != null (was "MyWiFi")
      - Sets whitelistedNetworkName = null
   → Emits: _networkDetail = NetworkDetail("OfficeWiFi", WIFI)
   → Logs: "Network changed from MyWiFi to OfficeWiFi - clearing whitelist"

3. Windscribe.kt network observer
   → Checks: previousNetwork ("MyWiFi") != currentNetwork ("OfficeWiFi")
   → Clears: preference.whiteListedNetwork = null
   → Checks database: OfficeWiFi has auto-secure ON
   → Starts AutoConnectService
   → Logs: "Network changed to auto-secure ON network - starting AutoConnectService"

4. AutoConnectService.onHandleWork()
   → Checks: auto-secure ON + not whitelisted + disconnected
   → Connects VPN
   → WindVpnController clears whitelist on connect

Result: Whitelist cleared, VPN auto-connected to OfficeWiFi
```

### Example 3: User Returns to Whitelisted Network

**Scenario**: User returns to "MyWiFi" (was whitelisted before leaving)

```
1. Network changes to "MyWiFi"
   → DeviceStateManager detects network change

2. DeviceStateManager.processNetworkChange()
   → Checks: whitelistedNetworkName = null (was cleared when user left)
   → Emits: _networkDetail = NetworkDetail("MyWiFi", WIFI)
   → Does NOT set whitelist (it was cleared)

3. Windscribe.kt network observer
   → Checks database: MyWiFi has auto-secure ON
   → Starts AutoConnectService

4. AutoConnectService.onHandleWork()
   → Checks: auto-secure ON + not whitelisted + disconnected
   → Connects VPN

Result: VPN auto-connects (whitelist was cleared when user left network)
```

### Example 4: System Disconnect (Protocol Change)

**Scenario**: User on "MyWiFi" (auto-secure ON, preferred protocol: WireGuard:443), VPN connected with OpenVPN:443

```
1. VpnBackend detects protocol mismatch
   → Current: OpenVPN:443
   → Required: WireGuard:443

2. VpnBackend.handleNetworkInfoUpdate()
   → Checks: connected + autoConnect enabled + protocol mismatch
   → Disconnects: appContext.vpnController.disconnectAsync(error = null)
   → Logs: "Network change detected while connected. Reconnecting with correct protocol/port..."

3. WindVpnController.disconnect()
   → Checks: error = null (not UserDisconnect)
   → Skips whitelist logic
   → Logs: "System disconnect (reconnecting=false, error=null) - not whitelisting"

4. WindVpnController.checkForReconnect()
   → Checks: auto-secure ON + not whitelisted
   → Starts AutoConnectService

5. AutoConnectService reconnects with WireGuard:443

Result: VPN switches protocol without whitelisting network
```

## Logging Guide

### DeviceStateManager Logs

```
INFO  - Whitelisted network set to: MyWiFi
INFO  - Network changed from MyWiFi to OfficeWiFi - clearing whitelist
DEBUG - Whitelisted network state changed: true (current: MyWiFi, whitelisted: MyWiFi)
```

### WindVpnController Logs

```
DEBUG - User disconnected - whitelisted network: MyWiFi
DEBUG - System disconnect (reconnecting=false, error=null) - not whitelisting
DEBUG - Not starting AutoConnectService - auto-secure ON and whitelisted
```

### AutoConnectService Logs

```
DEBUG - Network: MyWiFi, AutoSecure: true, Whitelisted: true
DEBUG - Auto secure ON but network is whitelisted - skipping auto-connect
```

### VpnBackend Logs

```
DEBUG - Network change detected while connected. Current: OpenVPN:443, Required: WireGuard:443. Reconnecting...
DEBUG - Auto-secure OFF for MyWiFi - system disconnecting
```

### Windscribe.kt Logs

```
DEBUG - Network changed from MyWiFi to OfficeWiFi - cleared preferences whitelist
DEBUG - Network changed to auto-secure ON network - starting AutoConnectService
```

## Troubleshooting

### Problem: VPN keeps auto-connecting after user disconnect

**Symptoms**: User disconnects, but VPN reconnects immediately

**Possible Causes**:
1. ✗ Whitelist not being set on user disconnect
2. ✗ Whitelist being cleared too early
3. ✗ Error type not being passed correctly

**Debug Steps**:
```bash
adb logcat | grep -E "vpn_controller|device-state-manager|auto-connect-service"
```

Look for:
- "User disconnected - whitelisted network: XXX" (should appear)
- "Whitelisted network state changed: true" (should appear)
- "Auto secure ON but network is whitelisted - skipping auto-connect" (should appear)

### Problem: VPN not auto-connecting when returning to network

**Symptoms**: User returns to network, VPN doesn't auto-connect (but should)

**Possible Causes**:
1. ✓ Whitelist not being cleared on network change (intended behavior)
2. ✗ Auto-secure setting is OFF for network
3. ✗ Global autoConnect setting is disabled

**Debug Steps**:
```bash
adb logcat | grep -E "device-state-manager|auto-connect-service"
```

Look for:
- "Network changed from XXX to YYY - clearing whitelist" (should appear when leaving network)
- "Network changed to auto-secure ON network - starting AutoConnectService" (should appear when returning)

### Problem: AutoConnectService keeps running unnecessarily

**Symptoms**: Foreground notification stays after user disconnect

**Possible Causes**:
1. ✗ checkForReconnect() not checking whitelist
2. ✗ Service not stopping when whitelist is set

**Debug Steps**:
```bash
adb logcat | grep "vpn_controller"
```

Look for:
- "Not starting AutoConnectService - auto-secure ON and whitelisted" (should appear after user disconnect)

### Problem: Protocol doesn't switch on network change

**Symptoms**: User has preferred protocol for network, but VPN doesn't switch

**Possible Causes**:
1. ✗ Global autoConnect setting is disabled (protocol switching requires this)
2. ✗ Network doesn't have preferred protocol set
3. ✗ VpnBackend not detecting protocol mismatch

**Debug Steps**:
```bash
adb logcat | grep "vpn_backend"
```

Look for:
- "Network change detected while connected. Current: XXX, Required: YYY. Reconnecting..." (should appear)

**Fix**: Enable Settings → Auto-Connect (protocol switching is an "auto" feature)

## Design Decisions

### Why session-based whitelist (clears on network change)?

**Problem**: User disconnects on "MyWiFi" because they don't want VPN there. Later, they change to "OfficeWiFi" and back to "MyWiFi". Should VPN auto-connect?

**Options**:
1. **Persistent whitelist**: Never auto-connect on "MyWiFi" again (until user manually connects)
2. **Session-based whitelist**: Clear whitelist when leaving network, allow auto-connect on return

**Decision**: Session-based (Option 2)

**Reasoning**:
- User's intent is temporary: "I don't want VPN *right now* on this network"
- If they wanted permanent whitelist, they would disable auto-secure in settings
- Leaving and returning to network suggests changed circumstances (e.g., user was home, went to cafe, came back home - now they want VPN)
- Simpler mental model: "Whitelist lasts while I'm on this network"

### Why distinguish user vs system disconnects?

**Problem**: System disconnects for protocol switching or auto-secure OFF. Should these whitelist the network?

**Answer**: No

**Reasoning**:
- System disconnects are not user intent
- Whitelisting on protocol switch would break auto-reconnect
- Whitelisting on auto-secure OFF would prevent future auto-connect when user re-enables auto-secure

### Why require autoConnect for protocol switching?

**Problem**: User has disabled global auto-connect. Should VPN still auto-switch protocols on network change?

**Answer**: No

**Reasoning**:
- Protocol switching is an "auto" feature (automatic behavior)
- If user has disabled auto-connect, they want manual control
- Respects user's global preference for automatic vs manual VPN control
- Consistent with other auto features (auto-secure, auto-connect on network change)

## Future Considerations

### Persistent Whitelist Mode

If users request permanent network whitelisting:

**Option 1**: Add "Never auto-connect on this network" toggle in per-network settings
- Stores in NetworkInfo database table
- Separate from session-based whitelist
- Persists across network changes

**Option 2**: Add whitelist duration preference (session, 1 hour, 24 hours, permanent)
- More flexible but more complex UI

### Smart Whitelist Suggestions

Detect patterns and suggest whitelisting:
- User disconnects 3+ times on same network → Suggest disabling auto-secure
- User disconnects frequently on work hours → Suggest schedule-based auto-secure

### Whitelist Export/Import

For enterprise deployments:
- Export whitelist as JSON
- Import across devices
- MDM integration

## Related Files

| File | Lines | Purpose |
|------|-------|---------|
| `base/src/main/java/com/windscribe/vpn/state/DeviceStateManager.kt` | 69-218 | Whitelist state tracking and network change detection |
| `base/src/main/java/com/windscribe/vpn/backend/utils/WindVpnController.kt` | Multiple | Whitelist set/clear logic and service optimization |
| `base/src/main/java/com/windscribe/vpn/services/AutoConnectService.kt` | 92-134 | Whitelist check before auto-connect |
| `base/src/main/java/com/windscribe/vpn/backend/VpnBackend.kt` | 98-135 | System disconnect logic (protocol switching, auto-secure OFF) |
| `base/src/main/java/com/windscribe/vpn/Windscribe.kt` | 152-186 | Network change observer, preference sync, service startup |
| `base/src/main/java/com/windscribe/vpn/apppreference/PreferencesHelper.kt` | - | Whitelist persistence (whiteListedNetwork property) |

## Testing Checklist

### Manual Testing

- [ ] User disconnects on auto-secure ON network → VPN doesn't auto-connect
- [ ] User switches networks → Whitelist cleared
- [ ] User returns to whitelisted network → VPN auto-connects
- [ ] User connects manually → Whitelist cleared
- [ ] System disconnect (protocol change) → VPN reconnects, no whitelist
- [ ] System disconnect (auto-secure OFF) → VPN disconnects, no whitelist
- [ ] AutoConnectService doesn't start after user disconnect on whitelisted network
- [ ] Protocol switching requires autoConnect enabled
- [ ] Whitelist persists across app restart (until network change)

### Automated Testing

```kotlin
@Test
fun `user disconnect on auto-secure ON network sets whitelist`() {
    // Given: Connected on auto-secure ON network
    // When: User disconnects
    // Then: Whitelist should be set, auto-connect blocked
}

@Test
fun `network change clears whitelist`() {
    // Given: Network whitelisted
    // When: Network changes
    // Then: Whitelist should be cleared
}

@Test
fun `system disconnect does not set whitelist`() {
    // Given: Connected
    // When: System disconnect (error = null)
    // Then: Whitelist should not be set
}
```

## Summary

The Auto-Secure Network Whitelist System provides intelligent VPN auto-connection that respects user intent:

1. **User Control**: User disconnect prevents auto-connect (session-based)
2. **Smart Clearing**: Whitelist clears when leaving network (allows future auto-connect)
3. **System Intelligence**: System disconnects don't whitelist (maintains auto-reconnect)
4. **Optimized Performance**: Service doesn't run unnecessarily on whitelisted networks
5. **Protocol Flexibility**: Auto-switches protocols only when autoConnect enabled

This system balances automatic protection with user autonomy, providing a seamless VPN experience.
