# AmneziaWG Implementation Plan

## Overview
This document outlines the plan to replace the standard WireGuard implementation in the `wgtunnel` module with AmneziaWG (version 1.1.3+), which provides advanced packet obfuscation and anti-detection capabilities.

## Current Architecture Analysis

### Current WireGuard Implementation
- **Module**: `wgtunnel/`
- **Native Library**: `libwg-go.so` (built from Go source)
- **Go Package**: `golang.zx2c4.com/wireguard/android`
- **Current Fork**: `github.com/Windscribe/wireguard` (v0.0.20250523)
- **Build System**: CMake + Makefile (Go 1.21.3)
- **JNI Interface**: `wgtunnel/tools/libwg-go/jni.c`
- **Backend**: `com.wireguard.android.backend.GoBackend`
- **Integration**: `com.windscribe.vpn.backend.wireguard.WireguardBackend`

### Key Components
1. **Native Layer** (`libwg-go.so`):
   - Built from Go WireGuard implementation
   - Exposes JNI methods: `wgTurnOn`, `wgTurnOff`, `wgGetConfig`, `wgGetSocketV4/V6`, `wgVersion`
   - Combined with WSTunnel, Stunnel, and ControlD/ctrld in same module

2. **Java/Kotlin Layer**:
   - `GoBackend.java`: JNI bridge to native WireGuard
   - `WireguardBackend.kt`: Windscribe-specific VPN backend implementation
   - Config parsing and management in `com.wireguard.config.*`

3. **Integration Points**:
   - VPN service integration via `WireGuardWrapperService`
   - Connection state management via flows
   - Health monitoring via handshake checks
   - DNS proxy integration via `VPNTunnelWrapper`

## AmneziaWG Key Differences

### Protocol Enhancements
1. **Junk Packets**: Random packets before handshakes (4-12 packets recommended)
2. **Message Padding**: Custom padding for handshake and transport messages
3. **Custom Signature Packets**: Configurable packet injection with static/random bytes
4. **Traffic Obfuscation**: Designed to blend with regular internet traffic

### Configuration Changes
AmneziaWG adds new configuration parameters to standard WireGuard config:
```ini
[Interface]
PrivateKey = ...
Address = ...
Jc = 4                    # Junk packet count
Jmin = 50                 # Minimum junk packet size
Jmax = 1000              # Maximum junk packet size
S1 = 10                   # Init packet magic header size
S2 = 20                   # Response packet magic header size
H1 = 1234567890          # Init packet magic header
H2 = 9876543210          # Response packet magic header
H3 = 5555555555          # Cookie packet magic header
H4 = 3333333333          # Transport packet magic header

[Peer]
PublicKey = ...
Endpoint = ...
AllowedIPs = ...
```

## Implementation Strategy

### Phase 1: Repository Integration
**Goal**: Integrate AmneziaWG-Go as a dependency

1. **Update `wgtunnel/tools/libwg-go/go.mod`**:
   ```go
   // Replace this:
   replace golang.zx2c4.com/wireguard => github.com/Windscribe/wireguard v0.0.20250523-0.20250728222226-b33441abef7c

   // With:
   replace golang.zx2c4.com/wireguard => github.com/amnezia-vpn/amneziawg-go v1.1.3
   ```

2. **Verify Compatibility**:
   - Check if AmneziaWG-Go maintains API compatibility with standard WireGuard-Go
   - Identify any breaking changes in native interface
   - Test if existing JNI methods are preserved

### Phase 2: Configuration Support
**Goal**: Extend configuration parsing to support AmneziaWG parameters

1. **Update Config Models** (`wgtunnel/src/main/java/com/wireguard/config/`):
   - Add AmneziaWG-specific parameters to `Interface.java` class
   - Add parsing logic for new configuration fields
   - Maintain backward compatibility with standard WireGuard configs

2. **Config Builder Example**:
   ```kotlin
   data class AmneziaConfig(
       val junkPacketCount: Int = 4,           // Jc
       val junkPacketMinSize: Int = 50,        // Jmin
       val junkPacketMaxSize: Int = 1000,      // Jmax
       val initPacketMagicSize: Int = 10,      // S1
       val responsePacketMagicSize: Int = 20,  // S2
       val initPacketMagicHeader: Long = 0,    // H1
       val responsePacketMagicHeader: Long = 0, // H2
       val cookiePacketMagicHeader: Long = 0,  // H3
       val transportPacketMagicHeader: Long = 0 // H4
   )
   ```

3. **Update `Interface.java`**:
   ```java
   public final class Interface {
       private final Optional<Integer> junkPacketCount;
       private final Optional<Integer> junkPacketMinSize;
       private final Optional<Integer> junkPacketMaxSize;
       // ... other Amnezia fields

       // Parser updates to handle new keys
   }
   ```

### Phase 3: Backend Integration
**Goal**: Update Windscribe backend to support AmneziaWG features

1. **Update `WireGuardVpnProfile.java`** (`base/src/main/java/com/windscribe/vpn/backend/wireguard/`):
   - Add AmneziaWG configuration fields
   - Update config string generation to include Amnezia parameters
   - Add methods to enable/disable obfuscation

2. **Update `VPNProfileCreator.kt`** (`base/src/main/java/com/windscribe/vpn/backend/utils/`):
   - Add logic to generate AmneziaWG configs based on server requirements
   - Add API support for fetching Amnezia-specific server configs
   - Implement fallback to standard WireGuard if Amnezia not supported

3. **Update `WireguardBackend.kt`**:
   - No major changes needed (abstraction layer should handle it)
   - May need to update logging to reflect AmneziaWG status

### Phase 4: API & Server Configuration
**Goal**: Integrate with backend API to fetch AmneziaWG configs

1. **API Response Updates**:
   - Work with backend team to add AmneziaWG parameters to WireGuard config endpoint
   - Add capability flag to indicate AmneziaWG support on server side
   - Version config response format

2. **Repository Updates** (`base/src/main/java/com/windscribe/vpn/repository/WgConfigRepository.kt`):
   - Parse AmneziaWG-specific configuration from API
   - Store Amnezia parameters in local database
   - Update config caching logic

### Phase 5: UI & User Preferences
**Goal**: Allow users to enable/disable AmneziaWG features

1. **Add Preference Key** (`base/src/main/java/com/windscribe/vpn/constants/PreferencesKeyConstants.kt`):
   ```kotlin
   const val USE_AMNEZIA_WG = "use_amnezia_wg"
   const val AMNEZIA_JUNK_PACKETS = "amnezia_junk_packets"
   ```

2. **Add UI Setting** (Mobile & TV):
   - Add toggle in Connection preferences for "Enhanced Obfuscation"
   - Add advanced settings for Amnezia parameters (optional)
   - Show status indicator when AmneziaWG is active

3. **Update `ConnectionViewModel.kt`** (`mobile/`):
   - Add state for AmneziaWG settings
   - Expose preference controls

### Phase 6: Build System Updates
**Goal**: Ensure AmneziaWG builds correctly for all architectures

1. **Update `wgtunnel/tools/libwg-go/Makefile`**:
   - Update Go version if required by AmneziaWG
   - Verify all build flags are compatible
   - Test builds for all Android architectures (arm, arm64, x86, x86_64)

2. **Update `wgtunnel/build.gradle.kts`**:
   - Update any version constraints
   - Ensure NDK compatibility

3. **Test Build**:
   ```bash
   ./gradlew :wgtunnel:clean
   ./gradlew :wgtunnel:assembleDebug
   ```

### Phase 7: Testing & Validation
**Goal**: Ensure AmneziaWG works correctly and provides expected benefits

1. **Unit Tests**:
   - Add tests for AmneziaWG config parsing
   - Test config generation with various parameters
   - Test backward compatibility with standard WireGuard configs

2. **Integration Tests**:
   - Test connection establishment with AmneziaWG
   - Verify obfuscation parameters are applied
   - Test fallback to standard WireGuard
   - Test connection stability and handshake health monitoring

3. **Performance Tests**:
   - Measure connection speed with various obfuscation levels
   - Test battery impact
   - Compare with standard WireGuard baseline

4. **Security Tests**:
   - Verify traffic obfuscation effectiveness
   - Test against DPI detection
   - Validate encryption remains intact

## Migration Strategy

### Backward Compatibility
- Standard WireGuard configs should continue to work
- Existing connections should not break
- AmneziaWG features should be opt-in initially

### Rollout Plan
1. **Phase 1**: Internal testing with development builds
2. **Phase 2**: Beta testing with select users
3. **Phase 3**: Gradual rollout with feature flag
4. **Phase 4**: Enable by default for regions with VPN blocking

## API Changes Required

### Server-Side Changes
1. **WireGuard Config Endpoint**: Add AmneziaWG parameters
2. **Capability Endpoint**: Add flag for AmneziaWG support
3. **Server List**: Indicate which servers support AmneziaWG

### Example API Response
```json
{
  "wg_config": {
    "private_key": "...",
    "address": "10.0.0.2/32",
    "public_key": "...",
    "endpoint": "server.example.com:51820",
    "allowed_ips": "0.0.0.0/0",
    "amnezia": {
      "enabled": true,
      "junk_packet_count": 6,
      "junk_packet_min_size": 50,
      "junk_packet_max_size": 1000,
      "init_packet_magic_size": 10,
      "response_packet_magic_size": 20,
      "init_packet_magic_header": "1234567890",
      "response_packet_magic_header": "9876543210",
      "cookie_packet_magic_header": "5555555555",
      "transport_packet_magic_header": "3333333333"
    }
  }
}
```

## File Changes Summary

### Files to Modify
1. `wgtunnel/tools/libwg-go/go.mod` - Update dependency
2. `wgtunnel/tools/libwg-go/go.sum` - Update checksums (run `go mod tidy`)
3. `wgtunnel/src/main/java/com/wireguard/config/Interface.java` - Add Amnezia fields
4. `wgtunnel/src/main/java/com/wireguard/config/Config.java` - Update parsing
5. `base/src/main/java/com/windscribe/vpn/backend/wireguard/WireGuardVpnProfile.java` - Add Amnezia support
6. `base/src/main/java/com/windscribe/vpn/backend/utils/VPNProfileCreator.kt` - Generate Amnezia configs
7. `base/src/main/java/com/windscribe/vpn/repository/WgConfigRepository.kt` - API integration
8. `base/src/main/java/com/windscribe/vpn/constants/PreferencesKeyConstants.kt` - Add prefs
9. `base/src/main/java/com/windscribe/vpn/apppreference/PreferencesHelper.kt` - Add preference getters/setters
10. `mobile/src/main/java/com/windscribe/mobile/ui/preferences/connection/ConnectionViewModel.kt` - UI state
11. `base/src/main/res/values/strings.xml` - Add UI strings

### Files to Create
1. `base/src/main/java/com/windscribe/vpn/backend/wireguard/AmneziaConfig.kt` - Config data class
2. `docs/AMNEZIA_WG_GUIDE.md` - User documentation

## Risks & Mitigations

### Risk 1: API Compatibility
- **Risk**: AmneziaWG-Go may have breaking changes from WireGuard-Go
- **Mitigation**: Test thoroughly, maintain wrapper layer, consider version pinning

### Risk 2: Performance Impact
- **Risk**: Obfuscation may slow down connections
- **Mitigation**: Make obfuscation opt-in, provide tunable parameters, performance testing

### Risk 3: Server Support
- **Risk**: Not all servers may support AmneziaWG
- **Mitigation**: Implement graceful fallback, capability detection, clear user messaging

### Risk 4: Build Complexity
- **Risk**: Build process may break due to Go module changes
- **Mitigation**: Pin Go version, comprehensive build testing, CI/CD validation

## Success Criteria
1. ✅ AmneziaWG builds successfully for all Android architectures
2. ✅ Standard WireGuard configs continue to work
3. ✅ AmneziaWG configs connect successfully
4. ✅ Traffic obfuscation is verified to work
5. ✅ Performance impact is within acceptable range (<10% overhead)
6. ✅ No regression in connection stability
7. ✅ UI properly reflects AmneziaWG status

## Timeline Estimate
- **Phase 1**: Repository Integration - 1 day
- **Phase 2**: Configuration Support - 2-3 days
- **Phase 3**: Backend Integration - 2-3 days
- **Phase 4**: API & Server Configuration - 2-3 days (+ backend team time)
- **Phase 5**: UI & Preferences - 2-3 days
- **Phase 6**: Build System - 1-2 days
- **Phase 7**: Testing & Validation - 3-5 days

**Total Estimated Time**: 2-3 weeks (client-side only, excluding backend API work)

## Implementation Progress

### ✅ Phase 1: Repository Integration (COMPLETED)
**Date**: 2025-12-16

1. **Branch Created**: `feature/amnezia-wireguard` from `develop`

2. **Go Dependencies Updated**:
   - File: `wgtunnel/tools/libwg-go/go.mod`
   - Changed: `github.com/Windscribe/wireguard` → `github.com/amnezia-vpn/amneziawg-go v0.2.16`
   - Ran: `go mod tidy` successfully
   - Go version updated: `1.24.0` → `1.24.4`

3. **JNI Bridge Fixed**:
   - File: `wgtunnel/tools/libwg-go/api-android.go`
   - Updated imports from `golang.zx2c4.com/wireguard/*` to `github.com/amnezia-vpn/amneziawg-go/*`
   - Fixed `CreateUnmonitoredTUNFromFD` call: removed second parameter (now only takes `fd`)
   - Updated `wgVersion()` to detect `github.com/amnezia-vpn/amneziawg-go` dependency

4. **Build Status**: ✅ **SUCCESS** - All architectures compile (arm64-v8a, armeabi-v7a, x86, x86_64)

### ✅ Phase 2: Configuration Support (COMPLETED)
**Date**: 2025-12-16

1. **Interface.java - Complete Replacement**:
   - File: `wgtunnel/src/main/java/com/wireguard/config/Interface.java`
   - Source: Copied from official AmneziaWG Android project at `/Users/gindersingh/Downloads/amneziawg-android/tunnel/src/main/java/org/amnezia/awg/config/Interface.java`
   - Package updated: `org.amnezia.awg.config` → `com.wireguard.config`
   - Methods renamed for compatibility:
     - `toAwgQuickString()` → `toWgQuickString()`
     - `toAwgUserspaceString()` → `toWgUserspaceString()`

2. **AmneziaWG Parameters Added** (20 total):

   **Junk Packet Configuration**:
   - `junkPacketCount` (Jc) - Number of junk packets
   - `junkPacketMinSize` (Jmin) - Minimum junk packet size
   - `junkPacketMaxSize` (Jmax) - Maximum junk packet size

   **Packet Junk Sizes** (S1-S4):
   - `initPacketJunkSize` (S1) - Init packet padding
   - `responsePacketJunkSize` (S2) - Response packet padding
   - `cookieReplyPacketJunkSize` (S3) - Cookie reply packet padding
   - `transportPacketJunkSize` (S4) - Transport packet padding

   **Magic Headers** (H1-H4):
   - `initPacketMagicHeader` (H1) - Init packet signature (String)
   - `responsePacketMagicHeader` (H2) - Response packet signature (String)
   - `underloadPacketMagicHeader` (H3) - Underload packet signature (String)
   - `transportPacketMagicHeader` (H4) - Transport packet signature (String)

   **Special Junk Injection** (I1-I5):
   - `specialJunkI1` through `specialJunkI5` - Custom junk injection points (String)

3. **BadConfigException Updated**:
   - File: `wgtunnel/src/main/java/com/wireguard/config/BadConfigException.java`
   - Added 16 new `Location` enum values for all AmneziaWG parameters:
     - `JUNK_PACKET_COUNT`, `JUNK_PACKET_MIN_SIZE`, `JUNK_PACKET_MAX_SIZE`
     - `INIT_PACKET_JUNK_SIZE`, `RESPONSE_PACKET_JUNK_SIZE`, `COOKIE_REPLY_PACKET_JUNK_SIZE`, `TRANSPORT_PACKET_JUNK_SIZE`
     - `INIT_PACKET_MAGIC_HEADER`, `RESPONSE_PACKET_MAGIC_HEADER`, `UNDERLOAD_PACKET_MAGIC_HEADER`, `TRANSPORT_PACKET_MAGIC_HEADER`
     - `SPECIAL_JUNK_I1` through `SPECIAL_JUNK_I5`

4. **Peer.java - Removed udp_stuffing**:
   - File: `wgtunnel/src/main/java/com/wireguard/config/Peer.java`
   - **Issue**: `udp_stuffing` property was causing IPC error: `IpcSet: IPC error -22: invalid UAPI peer key: udp_stuffing`
   - **Fix**: Completely removed `udp_stuffing` property (not part of WireGuard/AmneziaWG UAPI spec)
   - Removed from: field declaration, constructor, parse() switch, equals(), hashCode(), toWgQuickString(), toWgUserspaceString(), Builder class

5. **Build Status**: ✅ **SUCCESS** - wgtunnel module compiles without errors

### Configuration Format Examples

**Standard WireGuard Config** (still supported):
```ini
[Interface]
PrivateKey = <base64-key>
Address = 10.0.0.2/32
DNS = 1.1.1.1

[Peer]
PublicKey = <base64-key>
Endpoint = server.example.com:51820
AllowedIPs = 0.0.0.0/0
```

**AmneziaWG Config** (new parameters):
```ini
[Interface]
PrivateKey = <base64-key>
Address = 10.0.0.2/32
DNS = 1.1.1.1
Jc = 4
Jmin = 50
Jmax = 1000
S1 = 10
S2 = 20
S3 = 15
S4 = 25
H1 = 1234567890
H2 = 9876543210
H3 = 5555555555
H4 = 3333333333

[Peer]
PublicKey = <base64-key>
Endpoint = server.example.com:51820
AllowedIPs = 0.0.0.0/0
```

### Key Technical Details

1. **AmneziaWG Version**: v0.2.16 (latest stable as of 2025-12-16)
2. **Go Module Path**: `github.com/amnezia-vpn/amneziawg-go`
3. **Reference Implementation**: Official amneziawg-android project located at `/Users/gindersingh/Downloads/amneziawg-android`

### Files Modified So Far

#### Phase 1 Files:
- `wgtunnel/tools/libwg-go/go.mod` - Dependency update
- `wgtunnel/tools/libwg-go/go.sum` - Checksums updated
- `wgtunnel/tools/libwg-go/api-android.go` - JNI bridge fixes

#### Phase 2 Files:
- `wgtunnel/src/main/java/com/wireguard/config/Interface.java` - Complete AmneziaWG support
- `wgtunnel/src/main/java/com/wireguard/config/BadConfigException.java` - New Location enums
- `wgtunnel/src/main/java/com/wireguard/config/Peer.java` - Removed udp_stuffing

### Important Notes for Phase 3

1. **Backward Compatibility**: All changes maintain backward compatibility. Standard WireGuard configs without AmneziaWG parameters will continue to work.

2. **Parameter Types**:
   - Integer parameters (Jc, Jmin, Jmax, S1-S4): Use `Optional<Integer>`
   - Magic header parameters (H1-H4, I1-I5): Use `Optional<String>` (not Long, to support various formats)

3. **UAPI Serialization**: The `toWgUserspaceString()` method correctly serializes all AmneziaWG parameters using lowercase keys (e.g., `jc=4`, `s1=10`, `h1=1234567890`)

4. **No udp_stuffing**: This property was causing IPC errors and is not part of the WireGuard/AmneziaWG specification. It has been completely removed.

### ✅ Phase 1 (Updated): Windscribe Fork Integration with customTun
**Date**: 2025-12-16
**Status**: RESOLVED & COMPLETED

**Challenge**: The Windscribe fork of amneziawg-go (android branch) declares its module path as `github.com/amnezia-vpn/amneziawg-go` rather than matching the repository URL.

**Solution Applied**:
Instead of trying to change the module path, we work with the path the fork declares:
1. Require the package by its declared name: `github.com/amnezia-vpn/amneziawg-go v0.2.16`
2. Use a replace directive to point to Windscribe fork:
   ```go
   replace github.com/amnezia-vpn/amneziawg-go => github.com/Windscribe/amneziawg-go v0.0.0-20251216202551-98ababa2da28
   ```

**Final Configuration**:
- **Repository**: `github.com/Windscribe/amneziawg-go`
- **Branch**: `android`
- **Commit**: `98ababa2da2833b56aae151e8c0b9282e65d5215`
- **Pseudo-version**: `v0.0.0-20251216202551-98ababa2da28`

**Files Modified**:
1. `wgtunnel/tools/libwg-go/go.mod`:
   ```go
   replace github.com/amnezia-vpn/amneziawg-go => github.com/Windscribe/amneziawg-go v0.0.0-20251216202551-98ababa2da28

   require (
       github.com/amnezia-vpn/amneziawg-go v0.2.16
       // ... other deps
   )
   ```

2. `wgtunnel/tools/libwg-go/api-android.go`:
   - Line 76: `func wgTurnOn(interfaceName string, tunFd int32, settings string, customTun bool) int32`
   - Line 83: `tun, name, err := tun.CreateUnmonitoredTUNFromFD(int(tunFd), customTun)`
   - customTun parameter restored and functional

**Build Status**: ✅ **SUCCESS**
```
BUILD SUCCESSFUL in 19s
41 actionable tasks: 12 executed, 29 up-to-date
```
All architectures built successfully: arm64-v8a, armeabi-v7a, x86, x86_64

**CustomTun Feature**: ✅ **ENABLED**
The Windscribe fork's android branch includes custom TUN device support, allowing the wrapper TUN functionality to be enabled via the boolean parameter.

### Next Steps for Phase 3: Backend Integration

1. **Update WireGuardVpnProfile.java**:
   - Location: `base/src/main/java/com/windscribe/vpn/backend/wireguard/WireGuardVpnProfile.java`
   - Add AmneziaWG configuration fields
   - Update config string generation to include Amnezia parameters

2. **Update VPNProfileCreator.kt**:
   - Location: `base/src/main/java/com/windscribe/vpn/backend/utils/VPNProfileCreator.kt`
   - Add logic to generate AmneziaWG configs based on server requirements
   - Add API support for fetching Amnezia-specific server configs

3. **Database Schema** (if needed):
   - May need to add Amnezia parameter storage to local database
   - Check `base/schemas/` for any required updates

## References
- [AmneziaWG-Android Repository](https://github.com/amnezia-vpn/amneziawg-android)
- [AmneziaWG-Go Repository](https://github.com/amnezia-vpn/amneziawg-go)
- [WireGuard Specification](https://www.wireguard.com/protocol/)
- Current WireGuard implementation: `github.com/Windscribe/wireguard`

## Notes
- Use tag v1.1.3 or later from `amnezia-vpn/amneziawg-go`
- Latest available version is v1.1.6 (as of Dec 2025)
- Consider using latest stable version for best obfuscation features
- Maintain backward compatibility at all times
- Document all configuration parameters for support team
