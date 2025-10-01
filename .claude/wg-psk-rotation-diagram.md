# WireGuard PSK Rotation & Retry System

## Overview
This diagram illustrates the intelligent PSK (Pre-Shared Key) rotation and retry mechanism in WgConfigRepository.

## Core Data Structures

```
┌─────────────────────────────────────────────────────────────┐
│                     GlobalPskState                          │
├─────────────────────────────────────────────────────────────┤
│ - latestPsk: String                                         │
│   └─> Most recently rotated PSK from API                    │
│ - timestamp: Long                                           │
│   └─> When latestPsk was generated                          │
│ - perServerStates: Map<String, PerServerPskState>           │
│   └─> Per-hostname PSK tracking                             │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ contains
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                  PerServerPskState                          │
├─────────────────────────────────────────────────────────────┤
│ - hostname: String                                          │
│ - currentPsk: String                                        │
│   └─> PSK that should work for this server                  │
│ - previousPsk: String?                                      │
│   └─> Fallback PSK to try if currentPsk fails               │
│ - lastHandshakeTime: Long                                   │
│   └─> Last successful handshake (used to track inactive peers)│
└─────────────────────────────────────────────────────────────┘
```

## System Initialization

```
┌───────────────────────┐
│   App Starts (init)   │
└───────────┬───────────┘
            │
            ▼
┌───────────────────────┐
│ rotatePskOnAppStart() │
└───────────┬───────────┘
            │
            ▼
┌────────────────────────────────────┐
│   shouldRotatePsk()                │
├────────────────────────────────────┤
│ 1. Check if > 5 min since rotation │
│ 2. Check if latestPsk is being used│
│    by any server                   │
└────────┬───────────────┬───────────┘
         │               │
    Yes (both)        No (either)
         │               │
         │               └──> Skip rotation
         │                   (Keep unused PSK)
         ▼
┌────────────────────┐
│   rotatePsk()      │
├────────────────────┤
│ Call wgRekey API   │
│ Update latestPsk   │
│ Keep perServerStates│
└────────────────────┘
```

## Connection Flow with PSK Selection

```
┌──────────────────────────────────────────────────────────────────────┐
│                      getWgParams(hostname)                           │
└────────────────────────────┬─────────────────────────────────────────┘
                             │
                             ▼
                  ┌──────────────────────┐
                  │  generateKeys()      │
                  │  (get or create)     │
                  └──────────┬───────────┘
                             │
                             ▼
                  ┌──────────────────────────┐
                  │  getPskForServer()       │◄──────────┐
                  ├──────────────────────────┤           │
                  │ 1. cleanupExpiredStates()│           │
                  │    Remove servers > 1hr  │           │
                  │ 2. Select PSK            │           │
                  └──────────┬───────────────┘           │
                             │                           │
         ┌───────────────────┼────────────────┐          │
         │                   │                │          │
         ▼                   ▼                ▼          │
    ┌─────────┐      ┌──────────────┐   ┌──────────────┴┐
    │ No state│      │ Has state    │   │ Has state     │
    │ for this│      │ No retry     │   │ IS retry      │
    │ server  │      │              │   │               │
    └────┬────┘      └──────┬───────┘   └──────┬────────┘
         │                  │                   │
         │                  │                   │
         ▼                  ▼                   ▼
    Use latestPsk    Use previousPsk     Use currentPsk
                     (if set) else        (the newer one)
                     currentPsk
                             │
                             └──────────────────┘
                                      │
                                      ▼
                             ┌─────────────────┐
                             │  wgConnect()    │
                             │  (get server    │
                             │   IP & DNS)     │
                             └────────┬────────┘
                                      │
                                      ▼
                             ┌─────────────────┐
                             │ createRemoteParams()│
                             │ Return config   │
                             └─────────────────┘
```

## Two-PSK Retry Mechanism

```
┌──────────────────────────────────────────────────────────────────────┐
│                    Connection Attempt Flow                           │
└──────────────────────────────────────────────────────────────────────┘

SCENARIO 1: First connection to a server
┌─────────────────────────────────────────────┐
│ Server: atl-331-wg.whiskergalaxy.com  │
│ PerServerPskState: null (no state)          │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
         Use latestPsk (ABC123)
                   │
                   ├──── SUCCESS ─────┐
                   │                  │
                   │                  ▼
                   │         ┌─────────────────────┐
                   │         │ onHandshakeSuccess()│
                   │         ├─────────────────────┤
                   │         │ Create state:       │
                   │         │  currentPsk: ABC123 │
                   │         │  previousPsk: null  │
                   │         │  lastHandshake: now │
                   │         └─────────────────────┘
                   │
                   └──── FAILURE ─────┐
                                      │
                                      ▼
                              (No retry available,
                               try different server)


SCENARIO 2: Reconnection with PSK rotation
┌─────────────────────────────────────────────┐
│ Time passes, PSK rotated: XYZ789            │
│ PerServerPskState:                          │
│  currentPsk: ABC123                         │
│  previousPsk: null                          │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
         Use previousPsk (null) → ABC123
                   │
                   ├──── SUCCESS ─────┐
                   │                  │
                   │                  ▼
                   │         ┌─────────────────────┐
                   │         │ onHandshakeSuccess()│
                   │         ├─────────────────────┤
                   │         │ Keep state:         │
                   │         │  currentPsk: ABC123 │
                   │         │  previousPsk: null  │
                   │         │  lastHandshake: now │
                   │         └─────────────────────┘
                   │
                   └──── FAILURE ─────┐
                                      │
                                      ▼
                              ┌────────────────┐
                              │ onPskFailure() │
                              ├────────────────┤
                              │ Update state:  │
                              │  currentPsk: XYZ789│
                              │  previousPsk: ABC123│
                              └────────┬───────┘
                                       │
                                       ▼
                              ┌────────────────────┐
                              │ nextHostnameToTry()│
                              ├────────────────────┤
                              │ Returns: hostname  │
                              │ (retry available)  │
                              └────────┬───────────┘
                                       │
                                       ▼
                              ┌────────────────────┐
                              │ AutoConnection     │
                              │ retries same server│
                              └────────┬───────────┘
                                       │
                                       ▼
                         getPskForServer() [IS RETRY]
                                       │
                                       ▼
                              Use currentPsk (XYZ789)
                                       │
                                       ├──── SUCCESS ─────┐
                                       │                  │
                                       │                  ▼
                                       │         ┌─────────────────────┐
                                       │         │ onHandshakeSuccess()│
                                       │         ├─────────────────────┤
                                       │         │ Update state:       │
                                       │         │  currentPsk: XYZ789 │
                                       │         │  previousPsk: null  │
                                       │         │  lastHandshake: now │
                                       │         └─────────────────────┘
                                       │
                                       └──── FAILURE ─────┐
                                                          │
                                                          ▼
                                                  (Both PSKs failed,
                                                   try different server)
```

## PSK State Lifecycle

```
┌─────────────────────────────────────────────────────────────────────┐
│                        State Transitions                            │
└─────────────────────────────────────────────────────────────────────┘

STATE 1: No state (first connection)
┌──────────────────────┐
│ Server: example.com  │
│ State: null          │
└──────────┬───────────┘
           │
           │ Connect with latestPsk
           ▼
┌──────────────────────┐
│ Handshake Success    │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────────────┐
│ currentPsk: ABC123           │
│ previousPsk: null            │
│ lastHandshakeTime: T0        │
└──────────┬───────────────────┘
           │
           │ Connection stable
           ▼
STATE 2: Stable connection
┌──────────────────────────────┐
│ currentPsk: ABC123           │
│ previousPsk: null            │
│ lastHandshakeTime: T0        │
└──────────┬───────────────────┘
           │
           │ Periodic handshakes
           │ (lastHandshakeTime updated)
           ▼
┌──────────────────────────────┐
│ currentPsk: ABC123           │
│ previousPsk: null            │
│ lastHandshakeTime: T1        │
└──────────┬───────────────────┘
           │
           │ PSK rotated globally
           │ (latestPsk → XYZ789)
           ▼
STATE 3: PSK rotated, but not failed yet
┌──────────────────────────────┐
│ currentPsk: ABC123           │
│ previousPsk: null            │
│ lastHandshakeTime: T1        │
└──────────┬───────────────────┘
           │
           │ Disconnect & reconnect
           │ Old PSK fails (server peer expired)
           ▼
┌──────────────────────┐
│ onPskFailure()       │
└──────────┬───────────┘
           │
           ▼
STATE 4: Retry state (two PSKs available)
┌──────────────────────────────┐
│ currentPsk: XYZ789 (new)     │
│ previousPsk: ABC123 (old)    │
│ lastHandshakeTime: T1        │
└──────────┬───────────────────┘
           │
           │ Retry with currentPsk (XYZ789)
           ▼
┌──────────────────────┐
│ Handshake Success    │
└──────────┬───────────┘
           │
           ▼
STATE 5: Back to stable with new PSK
┌──────────────────────────────┐
│ currentPsk: XYZ789           │
│ previousPsk: null            │
│ lastHandshakeTime: T2        │
└──────────────────────────────┘
```

## Cleanup Mechanism

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Expired State Cleanup                            │
└─────────────────────────────────────────────────────────────────────┘

On every connection attempt (getPskForServer):
┌─────────────────────────────┐
│ cleanupExpiredServerStates()│
└──────────────┬──────────────┘
               │
               ▼
    ┌──────────────────────────────────┐
    │ For each server in perServerStates│
    └──────────────┬───────────────────┘
                   │
                   ▼
    ┌─────────────────────────────────────────┐
    │ Check: (now - lastHandshakeTime) > 1hr? │
    └──────────────┬────────────┬──────────────┘
                   │            │
              YES  │            │ NO
                   │            │
                   ▼            ▼
         ┌──────────────┐   ┌─────────────┐
         │ Remove state │   │ Keep state  │
         │ (Server has  │   │             │
         │  removed peer)│   │             │
         └──────────────┘   └─────────────┘

Example Timeline:
├─────┬─────────────┬──────────────┬────────────────────────┤
T0    T1            T2             T3                       T4
│     │             │              │                        │
│     │             │              │                        │
Connect │             │              │                        User tries to
Server A│             Handshake      │                        connect again
        │             (update T1)    │                        │
        │                            │                        getPskForServer()
        Connect with                 Last handshake          │
        Server B                     Server B                cleanupExpiredServerStates()
                                     (update T2)             │
                                                             ├─ Server A: T4-T1 > 1hr → REMOVE
                                                             └─ Server B: T4-T2 < 1hr → KEEP
```

## Key Functions

### PSK Rotation Logic
```
shouldRotatePsk():
  if no global state:
    → return true (need initial PSK)

  if time elapsed > 5 minutes:
    Check if latestPsk is used by any server:
      - Look through all perServerStates
      - Check if any server has currentPsk == latestPsk
      - OR if any server has previousPsk == latestPsk

    if latestPsk NOT used by any server:
      → return false (skip rotation, keep unused PSK)

    if latestPsk IS used:
      → return true (rotate to new PSK)

  else:
    → return false (within time threshold)
```

### PSK Selection Logic
```
getPskForServer(hostname):
  Clean up expired server states (>1 hour)

  if no global state → use local PSK

  if no server state:
    → use latestPsk (first time)

  if is retry:
    → use currentPsk (newer PSK)
  else:
    → use previousPsk ?? currentPsk
```

### Retry Detection
```
nextHostnameToTry():
  Get current profile PSK (triedPsk)
  Get server state currentPsk

  if currentPsk != triedPsk:
    → return hostname (retry available)
  else:
    → return null (no retry)
```

### State Updates
```
onHandshakeSuccess():
  Determine which PSK was used
  Always update state:
    - currentPsk = usedPsk
    - previousPsk = null
    - lastHandshakeTime = now

onPskFailure(hostname):
  Update state:
    - previousPsk = old currentPsk
    - currentPsk = latestPsk (from global)
    - lastHandshakeTime = unchanged
```

## Time Constants

```
┌────────────────────────────────────────┐
│ pskRotationThresholdMs = 5 minutes     │
│ → How often to rotate PSK globally     │
└────────────────────────────────────────┘

┌────────────────────────────────────────┐
│ peerExpiryThresholdMs = 60 minutes     │
│ → Server peer cache expiry time        │
│ → Used for cleanup                     │
└────────────────────────────────────────┘
```

## Summary

**Two-PSK Strategy:**
- Each server tracks 2 PSKs: current (newer) and previous (older)
- On first failure, switch to newer PSK
- Allows one automatic retry per server

**Rotation Timing:**
- Global PSK rotates every 5 minutes (if current PSK is being used)
- Skips rotation if latestPsk hasn't been adopted by any server yet
- Per-server states track which PSK each server expects
- Server peers expire after 1 hour → need latest PSK
- Efficient rotation: only rotate when necessary

**State Management:**
- Always update on handshake (keeps lastHandshakeTime fresh)
- Cleanup expired states (>1 hour) on every connection attempt
- Handles peer expiry during active app sessions
- Prevents memory buildup from unused servers