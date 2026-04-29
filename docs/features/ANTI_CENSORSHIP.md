# Anti-Censorship Options

This document explains what each anti-censorship option does and how it helps users connect in restrictive environments.

---

## Overview

The Anti-Censorship screen provides three independent options designed to help users connect to Windscribe VPN when normal connections fail due to network restrictions or censorship.

**Location**: Settings → Anti-Censorship

**Warning**: All options degrade performance (increased latency, bandwidth overhead). Only enable if normal connection fails.

---

## 1. Protocol Tweaks

### Description
Protocol-level changes made to WireGuard, OpenVPN, and Stealth protocols.

### Type
Dropdown with 3 options

### Options

#### Auto
- Server automatically recommends obfuscation based on your location
- Uses `amneziaWgConfigId` field from API session response
- If API recommends a configuration, it's automatically applied
- If no recommendation, no obfuscation is used

#### Enabled
- You explicitly enable protocol obfuscation
- Shows additional "Configuration" dropdown with 10 presets
- Full control over which Amnezia preset is used
- Prevents automatic changes from API

#### Disabled
- No obfuscation applied to any protocol
- Standard VPN protocol behavior
- Fastest performance but may be blocked

### What It Impacts

**WireGuard Protocol**:
- Adds packet obfuscation using AmneziaWG parameters
- Junk packet injection (random data packets)
- Magic headers (disguise WireGuard handshake)
- Special protocol injections (fake SNMP, BitTorrent, etc.)
- Applied via `applyUnblockWgParams()` in `VPNProfileCreator.kt`

**OpenVPN TCP/UDP**:
- Enables `udp-stuffing` (sends random packets before reset)
- Enables `tcp-split-reset` (splits reset packets into pieces)
- Anti-DPI patches applied to server config
- Applied in `VPNProfileCreator.kt:264-282`

**Stealth & WSTunnel**:
- Applies TLS padding to tunnel traffic
- Applied via `ProxyTunnelManager.kt:36-44`

### Use Case
Enable when your government/ISP blocks VPN protocols by detecting WireGuard or OpenVPN signatures.

### Technical Details
- **Amnezia Presets**: Fetched from `/v2/UnblockWgParams` API
- **Storage**: `protocolTweaksMode` in DataStore
- **Database**: `UnBlockWgParam` table stores 10+ presets
- **Parameters**: jc, jMin, jMax, s1-s4, h1-h4, i1-i5

---

## 2. Server Routing

### Description
Increases latency, but improves chances of being able to connect.

### Type
Dropdown with 3 options

### Options

#### Auto
- Server automatically chooses best routing
- Sends `backup = -1` parameter to API
- Lets backend decide optimal infrastructure

#### Regular
- Uses primary/direct server infrastructure
- Sends `backup = 0` parameter to API
- Fastest route but may be blocked in censored regions

#### Alternate
- Uses backup infrastructure with different IP ranges/ASNs
- Sends `backup = 1` parameter to API
- Slower but better for restrictive networks where primary IPs are blocked

### What It Impacts

**API Calls**:
- Session requests (`getSessionGeneric`)
- Server list requests (`getServers`)
- All API endpoints affected by backup parameter

**Server Infrastructure**:
- Which backend servers handle your requests
- Geographic routing paths
- IP address ranges used

**Side Effect**:
- Changing routing triggers automatic server list refresh
- You get a fresh list of available servers for the new infrastructure

### Use Case
Enable Alternate when primary Windscribe servers/IPs are blocked in your region.

### Technical Details
- **Mapping**: `getBackupParameter()` converts mode to integer
- **Applied in**: `UserRepository.kt`, `ServerListRepository.kt`, `WgConfigRepository.kt`
- **Storage**: `serverRoutingMode` in DataStore

---

## 3. Large TLS

### Description
Artificially enlarge TLS packets, helps to circumvent censorship in some cases.

### Type
Toggle (On/Off)

### Options

#### On
- All API requests use enlarged TLS packets
- Extra padding bytes added to TLS handshake
- Makes traffic harder to fingerprint

#### Off
- Standard TLS packet sizes
- No padding applied
- Faster but may be detectable

### What It Impacts

**All API Requests**:
- Session updates
- Server list fetches
- Credential requests
- Location lists
- All traffic through WSNet library

**How It Works**:
- Sets `isAPIExtraTLSPadding` flag in WSNet native library
- WSNet adds random padding to TLS records
- Prevents DPI from fingerprinting Windscribe API traffic

### Auto-Enable Behavior

**Automatically enabled on first start for users in**:
- Belarus (be)
- Iran (fa)
- Russia (ru)
- Turkey (tr)
- China (zh)

Detected via system locale language code.

### Use Case
Enable when API requests are being blocked or fingerprinted by DPI systems.

### Technical Details
- **Observer**: `BridgeApiRepository.observeExtraTlsPaddingStatus()`
- **Applied**: `WSNetWrapper.configureAdvancedParameters()`
- **Storage**: `extraTlsPaddingEnabled` in DataStore
- **Default**: `appContext.isRegionRestricted`

---

## How They Work Together

All three options can be used simultaneously for maximum censorship evasion:

```
Protocol Tweaks (Manual + Preset 5)
    ↓ Obfuscates WireGuard/OpenVPN packets

Large TLS (On)
    ↓ Pads API TLS requests

Server Routing (Alternate)
    ↓ Uses backup infrastructure

= Maximum evasion but highest latency
```

**Recommended Approach**:
1. Try each option individually first
2. Only combine if single options don't work
3. Start with Auto modes before Manual

---

## File Reference

### Core Implementation
- `VPNProfileCreator.kt:264-282, 562-621` - Protocol obfuscation application
- `ProxyTunnelManager.kt:36-44` - Stealth/WSTunnel padding
- `BridgeApiRepository.kt:71-81` - Large TLS observer
- `UserRepository.kt:209-231` - Protocol Tweaks Auto mode
- `DataStorePreferenceHelper.kt:1061-1067` - Server routing parameter

### Preferences
- `PreferencesHelper.kt:107, 164` - Property definitions
- `PreferencesKeyConstants.kt:122-133` - Constants
- `Windscribe.kt:256-266` - Region detection

### Repositories
- `UnblockWgParamsRepository.kt` - Amnezia preset management
- `AdvanceParameterRepository.kt` - Advanced parameters
- `ServerListRepository.kt` - Server list with routing

### UI
- Mobile: `mobile/src/.../anticensorship/`
- TV: `tv/src/.../settings/`

---

## API Integration

### Protocol Tweaks Auto Mode

**API Field**: `server_inventory.amneziawg_config_id`

**Example Response**:
```json
{
  "server_inventory": {
    "amneziawg_config_id": "preset_5"
  }
}
```

**Behavior**:
- If field is present → selects that preset automatically
- If field is null/empty → no obfuscation in Auto mode
- Only applies when `protocolTweaksMode == "auto"`

### Amnezia Presets API

**Endpoint**: `/v2/UnblockWgParams`

**Returns**: List of obfuscation configurations with:
- Preset ID and title
- Junk packet parameters
- Magic headers
- Special injections

**Storage**: Saved to local database, synced on app start

### Server Routing Parameter

**API Parameter**: `backup` (integer)

**Values**:
- `-1` = Auto (server decides)
- `0` = Regular (primary infrastructure)
- `1` = Alternate (backup infrastructure)

**Used In**: `session()`, `getServers()` API calls

---

## Testing Guide

### Protocol Tweaks

1. Set to Manual, select a preset
2. Connect via WireGuard
3. Check connection succeeds in restricted environment
4. Try different presets if one doesn't work

### Large TLS

1. Toggle On
2. Refresh server list or login
3. Verify API calls succeed
4. Check in censored region for effectiveness

### Server Routing

1. Note current servers (Regular mode)
2. Switch to Alternate
3. Server list automatically refreshes
4. Verify different servers appear
5. Test connection quality

---

## Performance Impact

| Option | Latency | Bandwidth | When to Use |
|--------|---------|-----------|-------------|
| Protocol Tweaks | Low-Medium | Medium | Protocol detection/blocking |
| Server Routing (Alternate) | High | None | IP/infrastructure blocking |
| Large TLS | Low | Low | API fingerprinting |
