# Auto-Secure Network Whitelist System

## Overview

The Auto-Secure Network Whitelist is a session-based mechanism that prevents VPN auto-connection after a user manually disconnects on a network with auto-secure enabled. This prevents the frustrating experience of the VPN immediately reconnecting after the user intentionally disconnects.

**Key Behavior**: The whitelist is temporary (session-based) and automatically clears when the user changes networks, allowing auto-connect to resume normally when they return to the network later.

---

## User Experience Flow

### Scenario 1: User Manual Disconnect on Auto-Secure Network

```
1. User connected to "Coffee Shop WiFi"
2. Auto-Secure: ON
3. VPN: Connected
4. User taps "Disconnect"
   → Network "Coffee Shop WiFi" added to whitelist
   → Auto-connect BLOCKED while on this network
5. User remains on "Coffee Shop WiFi"
   → VPN stays disconnected (whitelist active)
6. User switches to "Home WiFi"
   → Whitelist CLEARED (network changed)
   → Auto-connect resumes normally
7. User returns to "Coffee Shop WiFi" later
   → Auto-connect works (whitelist was cleared in step 6)
```

### Scenario 2: System Disconnect (Protocol Change)

```
1. User connected to "Coffee Shop WiFi"
2. Auto-Secure: ON
3. VPN: Connected via OpenVPN
4. User changes protocol to WireGuard in settings
   → System disconnect (protocol switching)
   → Network NOT whitelisted (system action, not user intent)
5. VPN automatically reconnects with WireGuard
   → Auto-connect continues normally
```

### Scenario 3: Auto-Secure Turned OFF

```
1. User connected to "Coffee Shop WiFi"
2. Auto-Secure: ON → User switches to OFF
3. VPN: Connected
4. Auto-Secure setting change triggers disconnect
   → System disconnect (not user disconnect)
   → Network NOT whitelisted
5. VPN stays disconnected (auto-secure is off)
6. Auto-connect will NOT trigger (feature disabled)
```

---

## Implementation Details

### Key Components

**1. DeviceStateManager** (`base/src/main/java/com/windscribe/vpn/state/DeviceStateManager.kt`)
- Tracks whitelist state (in-memory)
- Detects network changes
- Clears whitelist on network change
- Provides whitelist check method

**2. WindVpnController** (`base/src/main/java/com/windscribe/vpn/backend/utils/WindVpnController.kt`)
- Whitelist set/clear logic
- Distinguishes user disconnect vs system disconnect
- Service lifecycle optimization (stops service when whitelisted)

**3. AutoConnectService** (`base/src/main/java/com/windscribe/vpn/services/AutoConnectService.kt`)
- Checks whitelist before auto-connecting
- Skips auto-connect if current network is whitelisted

**4. VpnBackend** (`base/src/main/java/com/windscribe/vpn/backend/openvpn/VpnBackend.kt`)
- System disconnect handling
- Always passes `error = null` for system disconnects to prevent whitelisting

**5. Windscribe.kt** (`base/src/main/java/com/windscribe/vpn/Windscribe.kt`)
- Network change observer
- Preference sync on network change

---

## State Management

### Whitelist Data Structure

```kotlin
// DeviceStateManager.kt
private val whitelistedNetworks = mutableSetOf<String>()  // SSID or network identifier

fun addToWhitelist(networkId: String) {
    whitelistedNetworks.add(networkId)
}

fun clearWhitelist() {
    whitelistedNetworks.clear()
}

fun isWhitelisted(networkId: String): Boolean {
    return whitelistedNetworks.contains(networkId)
}
```

### Session-Based Lifecycle

- **Created**: When user manually disconnects on auto-secure enabled network
- **Active**: While user remains on the same network
- **Cleared**: Immediately when network changes (WiFi → Mobile, WiFi A → WiFi B, etc.)
- **Persisted**: NO — in-memory only, lost on app restart

---

## Decision Logic

### When to Whitelist

```kotlin
fun onUserDisconnect(currentNetwork: String) {
    if (preferencesHelper.isAutoSecureOn) {
        deviceStateManager.addToWhitelist(currentNetwork)
        stopVpnService()  // Optimize: no need to keep service alive
    }
}
```

**Conditions**:
1. ✅ User initiated disconnect (taps disconnect button)
2. ✅ Auto-secure is enabled
3. ✅ Connected to a network (not airplane mode)

**NOT whitelisted**:
1. ❌ System disconnect (protocol change, auto-secure toggle)
2. ❌ Auto-secure is OFF
3. ❌ Connection error/failure (different flow)

### When to Clear Whitelist

```kotlin
fun onNetworkChanged(oldNetwork: String, newNetwork: String) {
    deviceStateManager.clearWhitelist()
    checkAutoConnect()  // Resume auto-connect logic
}
```

**Triggers**:
1. ✅ WiFi → Mobile data
2. ✅ WiFi A → WiFi B
3. ✅ Mobile data → WiFi
4. ✅ Network disconnect → Network connect
5. ✅ Airplane mode → Network restored

### When to Check Whitelist

```kotlin
// AutoConnectService.kt
fun onNetworkAvailable(network: String) {
    if (deviceStateManager.isWhitelisted(network)) {
        // Skip auto-connect
        return
    }

    if (preferencesHelper.isAutoSecureOn) {
        // Proceed with auto-connect
        windVpnController.connect()
    }
}
```

**Check Points**:
1. Network change detected (WiFi/mobile data connects)
2. Auto-secure setting enabled
3. VPN currently disconnected

---

## Code Flow

### Flow 1: User Manual Disconnect

```
User taps "Disconnect" (UI)
    ↓
HomeViewModel.disconnect() called
    ↓
WindVpnController.disconnect(isUserInitiated = true)
    ↓
VpnBackend.stop()
    ↓
onDisconnected callback
    ↓
DeviceStateManager.getCurrentNetwork()
    ↓
PreferencesHelper.isAutoSecureOn → true
    ↓
DeviceStateManager.addToWhitelist(currentNetwork)
    ↓
WindVpnService.stopSelf()  // Service optimization
    ↓
UI shows "Disconnected" state
    ↓
[User remains on same network]
    ↓
AutoConnectService receives network broadcast
    ↓
deviceStateManager.isWhitelisted(network) → true
    ↓
Auto-connect SKIPPED
```

### Flow 2: Network Change (Whitelist Clear)

```
User switches from WiFi to Mobile Data
    ↓
DeviceStateReceiverWrapper.onNetworkChanged()
    ↓
DeviceStateManager.clearWhitelist()  // CRITICAL
    ↓
Windscribe.networkObserver.onNetworkChanged()
    ↓
AutoConnectService.checkAutoConnect()
    ↓
deviceStateManager.isWhitelisted(network) → false
    ↓
preferencesHelper.isAutoSecureOn → true
    ↓
windVpnController.connect()  // Auto-connect resumed
```

### Flow 3: System Disconnect (Protocol Change)

```
User changes protocol in settings
    ↓
SettingsViewModel.setProtocol(newProtocol)
    ↓
WindVpnController.switchProtocol(newProtocol)
    ↓
VpnBackend.stop(error = null)  // error = null indicates system disconnect
    ↓
onDisconnected callback (error = null)
    ↓
WindVpnController checks: error == null → System disconnect
    ↓
NO whitelist action (intentional)
    ↓
VpnBackend.start(newProtocol)
    ↓
Auto-reconnect continues normally
```

---

## Important Notes

### Protocol/Port Switching on Network Change

**Requires `autoConnect` Setting Enabled**:

The feature that automatically switches protocols or ports based on network change requires the `autoConnect` preference to be enabled. This is because protocol/port switching is fundamentally an "auto" feature — it automatically adapts the VPN configuration without user intervention.

```kotlin
fun onNetworkChanged(newNetwork: String) {
    if (!preferencesHelper.autoConnect) {
        // Skip protocol/port switching — not enabled
        return
    }

    val networkProfile = getNetworkProfile(newNetwork)
    if (networkProfile.protocol != currentProtocol) {
        switchProtocol(networkProfile.protocol)
    }
}
```

**User Experience**:
- Auto-connect ON + Per-network config → Protocol/port switches automatically on network change
- Auto-connect OFF + Per-network config → User must manually connect after network change (protocol/port switching does not occur)

### System Disconnects Always Pass error = null

**Critical Implementation Detail**:

All system-initiated disconnects (protocol change, auto-secure toggle, port change) MUST pass `error = null` to the disconnect callback. This signals that the disconnect was intentional and should NOT trigger whitelisting.

```kotlin
// ❌ WRONG — Will whitelist and break auto-reconnect
vpnBackend.stop()  // error defaults to VpnError.USER_DISCONNECT

// ✅ CORRECT — System disconnect, no whitelisting
vpnBackend.stop(error = null)
```

**System Disconnect Scenarios**:
- Protocol switching (OpenVPN → WireGuard)
- Port switching (UDP → TCP)
- Auto-secure toggle (ON → OFF or OFF → ON)
- Connection mode change (manual → auto)

### Service Lifecycle Optimization

When a network is whitelisted (user disconnected on auto-secure network), the `WindVpnService` can be stopped entirely since auto-connect is blocked for that session.

**Benefits**:
1. ✅ Reduced battery consumption (no background service running)
2. ✅ No persistent notification (service not foreground)
3. ✅ Better user experience (clear signal VPN is off)

```kotlin
fun onUserDisconnect() {
    if (preferencesHelper.isAutoSecureOn) {
        deviceStateManager.addToWhitelist(currentNetwork)
        stopService(Intent(this, WindVpnService::class.java))  // Stop service
    }
}

fun onNetworkChanged() {
    deviceStateManager.clearWhitelist()
    if (shouldAutoConnect()) {
        startService(Intent(this, WindVpnService::class.java))  // Restart service
    }
}
```

---

## Edge Cases & Handling

### Edge Case 1: User Disconnects Multiple Times

**Scenario**: User disconnects, reconnects manually, disconnects again

**Handling**:
- Each disconnect re-adds network to whitelist (idempotent)
- Whitelist uses `Set<String>`, so duplicate adds are no-op
- Behavior remains consistent

### Edge Case 2: Rapid Network Changes

**Scenario**: User switches networks rapidly (WiFi A → WiFi B → WiFi A)

**Handling**:
- Each network change clears whitelist
- When returning to WiFi A, whitelist is already clear
- Auto-connect resumes normally

### Edge Case 3: App Restart

**Scenario**: User disconnects on WiFi, kills app, reopens app on same WiFi

**Handling**:
- Whitelist is in-memory only (lost on app restart)
- On app reopen, whitelist is empty
- Auto-connect resumes normally (desired behavior — user restarted app, intent reset)

### Edge Case 4: Airplane Mode

**Scenario**: User disconnects on WiFi, enables airplane mode, disables airplane mode

**Handling**:
- Airplane mode triggers network change (network lost)
- Whitelist cleared when airplane mode enabled
- When airplane mode disabled, auto-connect resumes

---

## Testing

### Manual Test Cases

**Test 1: Basic Whitelist Behavior**
1. Connect to WiFi with auto-secure ON
2. Tap disconnect
3. Verify VPN does not auto-reconnect
4. Switch to mobile data
5. Verify VPN auto-connects

**Test 2: Protocol Switching**
1. Connect to WiFi with OpenVPN
2. Change protocol to WireGuard in settings
3. Verify VPN reconnects automatically with WireGuard
4. Confirm no whitelisting occurred

**Test 3: Auto-Secure Toggle**
1. Connect to WiFi with auto-secure ON
2. Toggle auto-secure OFF
3. Verify VPN disconnects
4. Verify VPN does not auto-reconnect (feature disabled)

**Test 4: Return to Network**
1. Connect to WiFi A with auto-secure ON
2. Tap disconnect
3. Switch to WiFi B (auto-connects)
4. Return to WiFi A
5. Verify VPN auto-connects (whitelist was cleared)

### Automated Test Cases

```kotlin
// DeviceStateManagerTest.kt
@Test
fun `whitelist is cleared on network change`() {
    val manager = DeviceStateManager()

    manager.addToWhitelist("WiFi_A")
    assertTrue(manager.isWhitelisted("WiFi_A"))

    manager.onNetworkChanged("WiFi_A", "WiFi_B")

    assertFalse(manager.isWhitelisted("WiFi_A"))
    assertFalse(manager.isWhitelisted("WiFi_B"))
}

@Test
fun `system disconnect does not whitelist`() {
    val controller = WindVpnController()

    controller.disconnect(error = null)  // System disconnect

    assertFalse(deviceStateManager.isWhitelisted(currentNetwork))
}

@Test
fun `user disconnect whitelists network`() {
    val controller = WindVpnController()
    preferencesHelper.isAutoSecureOn = true

    controller.disconnect(isUserInitiated = true)

    assertTrue(deviceStateManager.isWhitelisted(currentNetwork))
}
```

---

## Troubleshooting

### Problem: Auto-connect not working after returning to network

**Diagnosis**:
```bash
adb logcat | grep -i "whitelist"
```

**Common Causes**:
1. Whitelist not cleared on network change
2. `DeviceStateManager.onNetworkChanged()` not called
3. Network identifier mismatch (SSID vs network hash)

**Fix**: Ensure `Windscribe.networkObserver` properly calls `deviceStateManager.clearWhitelist()` on network change.

### Problem: VPN auto-reconnects immediately after user disconnect

**Diagnosis**:
```bash
adb logcat | grep -E "(AutoConnect|DeviceState)"
```

**Common Causes**:
1. System disconnect passed instead of user disconnect
2. Auto-secure preference not checked before whitelisting
3. Whitelist check skipped in `AutoConnectService`

**Fix**: Verify `WindVpnController.disconnect()` receives correct `isUserInitiated` flag.

### Problem: Protocol switching fails to auto-reconnect

**Diagnosis**:
```bash
adb logcat | grep -E "(VpnBackend|WindVpnController)"
```

**Common Causes**:
1. `error != null` passed on system disconnect (triggers whitelisting)
2. Auto-connect setting disabled
3. Protocol switching logic broken

**Fix**: Ensure `VpnBackend.stop(error = null)` for all system disconnects.

---

## References

- [CLAUDE.md](../../CLAUDE.md) — Auto-secure whitelist summary
- [AGENTS.md](../../AGENTS.md) — Architecture overview
- [DeviceStateManager.kt](../../base/src/main/java/com/windscribe/vpn/state/DeviceStateManager.kt) — Implementation
- [WindVpnController.kt](../../base/src/main/java/com/windscribe/vpn/backend/utils/WindVpnController.kt) — Disconnect logic
- [AutoConnectService.kt](../../base/src/main/java/com/windscribe/vpn/services/AutoConnectService.kt) — Auto-connect logic

---

**Last Updated**: 2026-04-22
**Feature Version**: Introduced in v3.60
**Maintained By**: Engineering Team