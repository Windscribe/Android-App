# Debugging

How to get logs off a device, what the modules mean, and the build commands worth knowing.

---

## 1. The app log

The app logs through slf4j/logback to a file on the device, **not** to logcat tags. Grepping logcat for
class names will not find anything — every line is tagged with a logger *module* name instead.

```
/data/user/0/com.windscribe.vpn/files/applog.txt
```

Rolling, 100 KB per file, one archived generation (`applog.1.log.zip`). Root level is `DEBUG`, written
to both the file and logcat.

### Pull it

```bash
adb -s <serial> shell run-as com.windscribe.vpn cat files/applog.txt > applog.txt
```

### Clear it before a run

```bash
adb -s <serial> shell run-as com.windscribe.vpn rm -f files/applog.txt
adb -s <serial> shell run-as com.windscribe.vpn ls files/applog.txt   # confirm it is gone
```

Confirm the delete. `run-as` fails silently in some states, and a stale log read as a fresh one wastes
more time than the check costs.

### Format

One JSON object per line:

```json
{"tm":"2026-09-05 15:54:30.238","lvl":"error","mod":"vpn_backend","msg":"IKEv2: Authentication failed."}
```

Which makes it filterable:

```bash
# everything the VPN did, minus the noisy network library
python3 -c "
import json,sys
for l in open('applog.txt'):
    try: d=json.loads(l)
    except: continue
    if d['mod'] in ('wsnet-wrapper','migration'): continue
    print(d['tm'][11:], d['lvl'][:1], d['mod'], '|', d['msg'])
"
```

---

## 2. Logger modules

| Module | Covers |
|---|---|
| `vpn` | The service, the TUN, state transitions, connect decisions |
| `vpn_backend` | Per-protocol engines and their tunnel state |
| `auto-connect-watcher` | Network changes and whether they trigger a connect |
| `connection-policy` | Policy decisions |
| `tunnel-health-check` | WireGuard handshake age monitoring |
| `network-info-manager`, `device-state-manager` | Network identity and connectivity |
| `worker` | WorkManager jobs — session, server list, notifications |
| `data`, `server_list_repository` | Data layer and server list updates |
| `wsnet`, `wsnet-wrapper` | Networking library, including API failover probes |
| `ui`, `app`, `windscribe-main` | UI and app lifecycle |
| `billing`, `sso`, `play-integrity`, `fcm` | Feature areas |

Full list: `grep -rhoE 'getLogger\("[a-z_-]+"\)' --include=*.kt base/ common/`

---

## 3. Reading a connection

A successful connect looks like this — the shape is worth knowing, because a failure is usually a
missing line rather than an error line:

```
vpn          | Connecting to VPN with connectionId: <uuid>
vpn          | VPN state changed to Connecting (connectionId=<uuid>, error=null)
vpn          | TUN established: mtu=1280 routes=1 addrs=1 exceptApps=1
vpn          | Blocking traffic until the connection comes up.
vpn          | Trying to connect: ikev2:500:NextUp
vpn          | Server(id=…, hostname=…, ip=…)
vpn_backend  | Holding the live TUN while the engine negotiates.
vpn          | IKEv2 engine started on a mux descriptor
vpn          | TUN REPLACED: mtu=1300 routes=1 addrs=1
vpn_backend  | Starting connectivity test.
vpn_backend  | Connectivity test successful: <ip>
vpn          | VPN state changed to Connected (connectionId=<uuid>, error=null)
```

`TUN established` then `TUN REPLACED` with no `TUN closed` in between is the design working: one
interface for the session, replaced in place rather than torn down. See
[VPN_BACKEND.md](../architecture/VPN_BACKEND.md).

Things to look for when it goes wrong:

| Symptom | Look for |
|---|---|
| Connect never starts | `auto-connect-watcher` line showing the inputs — `AutoSecure`, `StoodDown`, `AutoConnect`, `State` |
| Connected but no traffic | `Connectivity test` lines; a held TUN with no engine drops everything |
| Reconnect loop | Repeated `connectionId` changes; check for an auth or timeout error between them |
| Notification wrong | `Session over; …` or `Waiting for the session refresh …` — these name the service mode |
| Stuck on "Awaiting session update" | `worker` lines: the verdict is one of `location was at fault`, `Account is …`, or `server list is being replaced` |
| Interface leaked | `TUN held 90000ms with no engine` — a reconnect was lost |

---

## 4. Build

```bash
./gradlew :mobile:assembleFdroidDebug          # debug APK
./gradlew :base:testFdroidDebugUnitTest        # unit tests
./gradlew ktlintFormat                         # fix formatting
./gradlew ktlintCheck                          # verify formatting
```

Install to a specific device:

```bash
adb -s <serial> install -r -d mobile/build/outputs/apk/fdroid/debug/mobile-fdroid-debug.apk
```

`adb devices -l` lists serials.

### When Gradle misbehaves

```bash
./gradlew --stop        # kill the daemon
./gradlew clean
```

Native build problems (`:openvpn`, `:strongswan`) are usually a toolchain mismatch rather than a code
error — check the NDK and CMake versions declared in the module's `build.gradle.kts` against what is
installed.

---

## 5. Device state

```bash
# is the VPN service running?
adb -s <serial> shell dumpsys activity services com.windscribe

# routing table inside the tunnel
adb -s <serial> shell ip route

# is the app's always-on / lockdown set?
adb -s <serial> shell settings get global always_on_vpn_app
adb -s <serial> shell settings get global always_on_vpn_lockdown
```

---

## Related

- [VPN_BACKEND.md](../architecture/VPN_BACKEND.md) — how the backend works
- [DNS_INTERCEPTION.md](../architecture/DNS_INTERCEPTION.md) — DNS through Control D
- [OPENVPN_UPDATE.md](../guides/OPENVPN_UPDATE.md) — updating the OpenVPN native build
