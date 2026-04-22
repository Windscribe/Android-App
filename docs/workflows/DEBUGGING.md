# Debugging Guide — Windscribe Android App

## Overview

Comprehensive debugging guide for common issues in the Windscribe Android app. Covers VPN connection problems, build failures, database issues, protocol-specific debugging, and performance analysis.

---

## Table of Contents

1. [VPN Connection Issues](#vpn-connection-issues)
2. [Build & Compilation Problems](#build--compilation-problems)
3. [Database Issues](#database-issues)
4. [Protocol-Specific Debugging](#protocol-specific-debugging)
5. [Auto-Connect Problems](#auto-connect-problems)
6. [UI Issues (Compose/XML)](#ui-issues)
7. [Performance Debugging](#performance-debugging)
8. [LogCat Filtering](#logcat-filtering)
9. [ADB Commands Reference](#adb-commands-reference)

---

## VPN Connection Issues

### Problem: VPN Fails to Connect

**Symptoms**:
- Connection stuck at "Connecting..."
- Immediate disconnect after connection
- "Connection failed" error

**Debugging Steps**:

1. **Check LogCat for errors**:
```bash
"$ANDROID_HOME/platform-tools/adb" logcat -v time | grep -E "(WindVPN|ERROR)"
```

Look for:
- `Connection timeout`
- `Authentication failed`
- `TLS handshake failed`
- `No route to host`

2. **Verify server selection**:
```bash
"$ANDROID_HOME/platform-tools/adb" logcat | grep "WindVPNController.*server"
```

Output should show:
```
WindVPNController: Selected server: us-east-001.windscribe.com
WindVPNController: Server IP: 192.0.2.1
```

3. **Check protocol selection**:
```bash
"$ANDROID_HOME/platform-tools/adb" logcat | grep "protocol"
```

Output:
```
WindVPNController: Selected protocol: WIREGUARD
WireGuardBackend: Starting WireGuard connection
```

4. **Test protocol fallback**:
```bash
# Force OpenVPN TCP (most reliable)
"$ANDROID_HOME/platform-tools/adb" shell am broadcast \
  -a com.windscribe.vpn.SWITCH_PROTOCOL \
  --es protocol "openvpn_tcp"
```

5. **Check network connectivity**:
```bash
"$ANDROID_HOME/platform-tools/adb" shell ping -c 4 8.8.8.8
```

**Common Causes & Solutions**:

| Issue | Cause | Solution |
|-------|-------|----------|
| Timeout | Firewall blocking UDP | Switch to OpenVPN TCP or Stealth |
| Auth failed | Expired session | Re-login to app |
| TLS handshake failed | Certificate issue | Update app, clear data |
| No route | Network disconnected | Check WiFi/mobile data |

---

### Problem: VPN Disconnects Randomly

**Symptoms**:
- Connection drops after few minutes
- Frequent reconnections
- "Connection lost" notifications

**Debugging Steps**:

1. **Monitor state changes**:
```bash
"$ANDROID_HOME/platform-tools/adb" logcat -v time | grep "VPNConnectionState"
```

Output shows state transitions:
```
VPNConnectionStateManager: State changed: CONNECTED → DISCONNECTED
VPNConnectionStateManager: Reason: Network unreachable
```

2. **Check for network changes**:
```bash
"$ANDROID_HOME/platform-tools/adb" logcat | grep "DeviceStateManager.*network"
```

3. **Verify auto-reconnect**:
```bash
"$ANDROID_HOME/platform-tools/adb" logcat | grep "AutoConnectService"
```

**Common Causes**:
- **Network switching (WiFi ↔ Mobile)** — Normal, should auto-reconnect
- **App killed by system** — Enable "Don't optimize" in battery settings
- **DNS timeout** — Try different DNS servers in settings
- **Protocol instability** — Try different protocol (e.g., IKEv2 more stable on mobile)

---

## Build & Compilation Problems

### Problem: Gradle Build Fails

**Error**: `Could not resolve all dependencies`

**Solution**:
```bash
# Stop Gradle daemon
./gradlew --stop

# Clean build
./gradlew clean

# Rebuild
./gradlew assembleDebug
```

---

### Problem: NDK Errors (OpenVPN/StrongSwan)

**Error**: `NDK not found` or `CMake executable not found`

**Debugging**:
```bash
# Verify NDK installation
echo $ANDROID_NDK_HOME
ls $ANDROID_HOME/ndk/

# Check CMake
ls $ANDROID_HOME/cmake/
```

**Solution**:
1. Open Android Studio → SDK Manager
2. Install NDK (Side by side)
3. Install CMake
4. Update `local.properties`:
```properties
ndk.dir=/Users/username/Library/Android/sdk/ndk/25.1.8937393
cmake.dir=/Users/username/Library/Android/sdk/cmake/3.22.1
```

5. Rebuild:
```bash
./gradlew clean
./gradlew :openvpn:assembleDebug
```

---

### Problem: Kotlin Compilation Errors

**Error**: `Unresolved reference` or `Type mismatch`

**Debugging**:
```bash
# Compile specific module
./gradlew :base:compileGoogleDebugKotlin --console=plain

# Check for errors
./gradlew :mobile:compileGoogleDebugKotlin --console=plain
```

**Common Causes**:
- **Stale cache** — `./gradlew clean`
- **Dagger graph issue** — Rebuild project
- **Kotlin version mismatch** — Check `build.gradle.kts` versions

---

## Database Issues

### Problem: App Crashes on Startup

**Error**: `IllegalStateException: Migration didn't properly handle`

**Cause**: Database migration missing or incorrect

**Debugging**:
```bash
# Check database version
"$ANDROID_HOME/platform-tools/adb" shell \
  "sqlite3 /data/data/com.windscribe.vpn/databases/windscribe.db 'PRAGMA user_version;'"
```

**Solution**:
```bash
# Option 1: Clear app data (loses local data)
"$ANDROID_HOME/platform-tools/adb" uninstall com.windscribe.vpn
./gradlew :mobile:assembleGoogleDebug
"$ANDROID_HOME/platform-tools/adb" install -r mobile/build/outputs/apk/google/debug/mobile-google-debug.apk

# Option 2: Add migration
# See docs/guides/DATABASE_MIGRATIONS.md
```

---

### Problem: Data Not Persisting

**Symptoms**:
- Server list disappears on app restart
- Preferences reset

**Debugging**:
```bash
# Check database file exists
"$ANDROID_HOME/platform-tools/adb" shell ls /data/data/com.windscribe.vpn/databases/

# Inspect database
"$ANDROID_HOME/platform-tools/adb" pull /data/data/com.windscribe.vpn/databases/windscribe.db /tmp/
sqlite3 /tmp/windscribe.db

sqlite> .tables
sqlite> SELECT COUNT(*) FROM Region;
```

**Common Causes**:
- **Database locked** — Close all Room queries
- **Transaction not committed** — Verify `@Transaction` usage
- **App killed during write** — Use `WAL` mode (already enabled)

---

## Protocol-Specific Debugging

### OpenVPN Debugging

**Enable verbose logging**:
```bash
"$ANDROID_HOME/platform-tools/adb" logcat -v time | grep -i "openvpn"
```

**Check config generation**:
```bash
"$ANDROID_HOME/platform-tools/adb" logcat | grep "OpenVPNConfig"
```

Output shows config:
```
OpenVPNConfig: proto=udp, remote=192.0.2.1 443, cipher=AES-256-GCM
```

**Common OpenVPN errors**:

| Log Message | Cause | Solution |
|-------------|-------|----------|
| `RESOLVE: Cannot resolve host` | DNS issue | Check network DNS |
| `TLS handshake failed` | Certificate issue | Update app |
| `AUTH_FAILED` | Invalid credentials | Re-login |
| `Connection timeout` | Firewall | Try TCP or Stealth |

---

### IKEv2 Debugging

**Check StrongSwan logs**:
```bash
"$ANDROID_HOME/platform-tools/adb" logcat | grep -E "(strongswan|charon)"
```

**Common IKEv2 errors**:

| Log Message | Cause | Solution |
|-------------|-------|----------|
| `no IKE config found` | Server unreachable | Check network |
| `peer not responding` | Firewall blocking UDP 500/4500 | Try different protocol |
| `authentication failed` | Wrong credentials | Re-login |

---

### WireGuard Debugging

**Check WireGuard logs**:
```bash
"$ANDROID_HOME/platform-tools/adb" logcat | grep -i "wireguard"
```

**Verify key exchange**:
```bash
"$ANDROID_HOME/platform-tools/adb" logcat | grep "WireGuard.*handshake"
```

Output:
```
WireGuardBackend: Handshake completed with peer
WireGuardBackend: Receiving keepalive packets
```

**Common WireGuard errors**:
- `Handshake timeout` → Server unreachable or blocked
- `Invalid key` → Re-fetch configuration
- `Endpoint resolution failed` → DNS issue

---

## Auto-Connect Problems

### Problem: VPN Not Auto-Connecting

**Debugging**:

1. **Check auto-connect setting**:
```bash
"$ANDROID_HOME/platform-tools/adb" shell \
  "cat /data/data/com.windscribe.vpn/shared_prefs/windscribe.preferences.xml" | grep auto_connect
```

2. **Monitor AutoConnectService**:
```bash
"$ANDROID_HOME/platform-tools/adb" logcat | grep "AutoConnectService"
```

Expected output when network changes:
```
AutoConnectService: Network changed: WiFi connected
AutoConnectService: Checking whitelist...
AutoConnectService: Network not whitelisted, proceeding
AutoConnectService: Auto-connect enabled, initiating connection
```

3. **Check whitelist state**:
```bash
"$ANDROID_HOME/platform-tools/adb" logcat | grep "whitelist"
```

**Common Causes**:

| Issue | Cause | Solution |
|-------|-------|----------|
| Auto-connect disabled | User setting | Enable in Settings → Connection |
| Network whitelisted | User disconnected on this network | Switch networks to clear whitelist |
| Battery optimization | System killing service | Disable battery optimization for app |

---

### Problem: Auto-Connect Triggers Incorrectly

**Symptoms**:
- VPN connects on trusted networks (home WiFi)
- Connects when user didn't want it

**Debugging**:
```bash
"$ANDROID_HOME/platform-tools/adb" logcat | grep -E "(DeviceStateManager|AutoConnectService)"
```

**Check**:
1. Is auto-secure enabled? (should NOT connect on trusted networks)
2. Is network marked as trusted?
3. Is whitelist cleared properly on network change?

**See**: [docs/features/AUTO_SECURE_WHITELIST.md](../features/AUTO_SECURE_WHITELIST.md)

---

## UI Issues

### Compose UI Not Updating

**Symptoms**:
- UI shows stale data
- State changes don't reflect

**Debugging**:

1. **Verify StateFlow collection**:
```kotlin
// ✅ Correct
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val state by viewModel.state.collectAsState()
    // UI uses state
}

// ❌ Wrong
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val state = viewModel.state  // Not collected!
}
```

2. **Check for recomposition**:
Add logging to Composable:
```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    Log.d("Recomposition", "HomeScreen recomposed")
    // ...
}
```

3. **Verify ViewModel scope**:
```kotlin
class HomeViewModel : ViewModel() {
    fun loadData() {
        viewModelScope.launch {  // ✅ Correct scope
            // ...
        }
        // NOT GlobalScope.launch ❌
    }
}
```

---

### TV UI (XML) Issues

**Problem**: Data binding not working

**Debugging**:
```xml
<!-- Enable data binding in layout -->
<layout xmlns:android="http://schemas.android.com/apk/res/android">
    <data>
        <variable
            name="viewModel"
            type="com.windscribe.tv.home.HomeViewModel" />
    </data>

    <TextView
        android:text="@{viewModel.serverName}"  <!-- Verify variable name -->
        ... />
</layout>
```

**Check binding generation**:
```bash
./gradlew :tv:compileGoogleDebugKotlin --console=plain | grep "Binding"
```

---

## Performance Debugging

### High CPU Usage

**Monitoring**:
```bash
# Monitor CPU usage
"$ANDROID_HOME/platform-tools/adb" shell top | grep com.windscribe.vpn
```

**Profile with Android Studio**:
1. Run app in debug mode
2. Android Studio → View → Tool Windows → Profiler
3. Select CPU timeline
4. Look for hot spots

**Common Causes**:
- **Protocol overhead** — Stealth/WSTunnel use more CPU (double encryption)
- **Infinite loops** — Check coroutines for improper `while(true)`
- **UI recomposition storms** — Verify StateFlow usage

---

### High Battery Drain

**Check wakelocks**:
```bash
"$ANDROID_HOME/platform-tools/adb" shell dumpsys batterystats | grep com.windscribe.vpn
```

**Common Causes**:
- **Foreground service running when unnecessary** — Verify service stops on disconnect
- **Frequent reconnections** — Check network stability
- **GPS usage** — Only request location for auto-secure if really needed

---

### Memory Leaks

**Use LeakCanary** (already integrated in debug builds):
```bash
./gradlew :mobile:assembleGoogleDebug
```

LeakCanary will show leaks in notification.

**Common Leaks**:
- **Activity references in ViewModels** — Use `Application` context
- **Uncancelled coroutines** — Use `viewModelScope` (auto-cancelled)
- **BroadcastReceiver not unregistered** — Unregister in `onDestroy`

---

## LogCat Filtering

### Filter by Tag

```bash
# VPN controller
"$ANDROID_HOME/platform-tools/adb" logcat -s WindVPNController

# All VPN related
"$ANDROID_HOME/platform-tools/adb" logcat | grep -E "(WindVPN|VPN|OpenVPN|WireGuard|IKEv2)"

# Errors only
"$ANDROID_HOME/platform-tools/adb" logcat *:E
```

### Filter by Process

```bash
# Only Windscribe app logs
"$ANDROID_HOME/platform-tools/adb" logcat --pid=$(adb shell pidof -s com.windscribe.vpn)
```

### Filter by Time

```bash
# Logs from last 5 minutes
"$ANDROID_HOME/platform-tools/adb" logcat -t '05:00.0'
```

### Save Logs to File

```bash
# Save all logs
"$ANDROID_HOME/platform-tools/adb" logcat -d > /tmp/windscribe-logs.txt

# Save VPN logs only
"$ANDROID_HOME/platform-tools/adb" logcat -d | grep -E "(WindVPN|VPN)" > /tmp/vpn-logs.txt
```

---

## ADB Commands Reference

### App Management

```bash
# Install APK
"$ANDROID_HOME/platform-tools/adb" install -r mobile/build/outputs/apk/google/debug/mobile-google-debug.apk

# Uninstall (clears data)
"$ANDROID_HOME/platform-tools/adb" uninstall com.windscribe.vpn

# Clear app data (keep app installed)
"$ANDROID_HOME/platform-tools/adb" shell pm clear com.windscribe.vpn

# Launch app
"$ANDROID_HOME/platform-tools/adb" shell am start -n com.windscribe.vpn/com.windscribe.mobile.ui.AppStartActivity

# Stop app
"$ANDROID_HOME/platform-tools/adb" shell am force-stop com.windscribe.vpn
```

### Screenshots

```bash
# Capture screenshot
"$ANDROID_HOME/platform-tools/adb" shell screencap -p /sdcard/screenshot.png
"$ANDROID_HOME/platform-tools/adb" pull /sdcard/screenshot.png /tmp/screenshot.png
"$ANDROID_HOME/platform-tools/adb" shell rm /sdcard/screenshot.png
```

### Database Inspection

```bash
# Pull database
"$ANDROID_HOME/platform-tools/adb" pull /data/data/com.windscribe.vpn/databases/windscribe.db /tmp/

# Open in sqlite3
sqlite3 /tmp/windscribe.db

# Common queries
sqlite> .tables
sqlite> SELECT * FROM Region LIMIT 5;
sqlite> SELECT * FROM City WHERE region_id = 1;
sqlite> PRAGMA user_version;  # Check DB version
```

### Network Debugging

```bash
# Check network state
"$ANDROID_HOME/platform-tools/adb" shell dumpsys connectivity

# Check active VPN
"$ANDROID_HOME/platform-tools/adb" shell dumpsys connectivity | grep -A 20 "VPN"

# Ping test
"$ANDROID_HOME/platform-tools/adb" shell ping -c 4 8.8.8.8

# DNS resolution
"$ANDROID_HOME/platform-tools/adb" shell nslookup google.com
```

### Preferences

```bash
# View preferences
"$ANDROID_HOME/platform-tools/adb" shell \
  "cat /data/data/com.windscribe.vpn/shared_prefs/windscribe.preferences.xml"

# Search for specific preference
"$ANDROID_HOME/platform-tools/adb" shell \
  "cat /data/data/com.windscribe.vpn/shared_prefs/windscribe.preferences.xml" | grep auto_connect
```

---

## Debugging Checklist

When facing an issue, follow this checklist:

- [ ] **Reproduce** — Can you consistently reproduce the issue?
- [ ] **LogCat** — Check logs for errors/exceptions
- [ ] **Network** — Verify network connectivity (WiFi/mobile data working)
- [ ] **Version** — Is app up to date? Check build version
- [ ] **Clean Build** — `./gradlew clean && ./gradlew assembleDebug`
- [ ] **Clear Data** — Uninstall/reinstall or clear app data
- [ ] **Protocol** — Try different VPN protocol
- [ ] **Device** — Test on different device/emulator
- [ ] **Permissions** — Verify all required permissions granted
- [ ] **Battery** — Check battery optimization not killing app

---

## Getting Help

If stuck after following this guide:

1. **Collect logs**:
```bash
"$ANDROID_HOME/platform-tools/adb" logcat -d > /tmp/windscribe-full-logs.txt
```

2. **Capture screenshot** of error/issue

3. **Document**:
   - Device model & Android version
   - App version (from About screen)
   - Steps to reproduce
   - Expected vs actual behavior

4. **Open issue** on GitLab with collected information

---

## References

- [SKILL.md](../../SKILL.md) — Development workflows
- [docs/architecture/VPN_PROTOCOLS.md](../architecture/VPN_PROTOCOLS.md) — Protocol details
- [docs/features/AUTO_SECURE_WHITELIST.md](../features/AUTO_SECURE_WHITELIST.md) — Auto-connect debugging
- [Android Developer Docs](https://developer.android.com/studio/debug)

---

**Last Updated**: 2026-04-22
**Maintained By**: Engineering Team
