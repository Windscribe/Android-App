# VPN Protocols

The six protocols the app ships, what implements each, and the order they are tried in.

---

## The list

`Util.getAppSupportedProtocolList()` builds this order, and it is what you see in the log at startup:

```
[wg:443, ikev2:500, udp:443, tcp:443, stunnel:443, wstunnel:443]
```

| # | Key | Port | Engine | Module |
|---|---|---|---|---|
| 1 | `wg` | 443 | `WireGuardEngine` | `:wgtunnel` (wg-go) |
| 2 | `ikev2` | 500 | `IKev2Engine` | `:strongswan` (charon) |
| 3 | `udp` | 443 | `WindscribeOpenVpnEngine` | `:openvpn` |
| 4 | `tcp` | 443 | `WindscribeOpenVpnEngine` | `:openvpn` |
| 5 | `stunnel` | 443 | `WindscribeOpenVpnEngine` + `WindStunnelUtility` | `:openvpn` |
| 6 | `wstunnel` | 443 | `WindscribeOpenVpnEngine` + `WSTunnelLib` | `:openvpn` |

Constants are in `PreferencesKeyConstants` (`PROTO_WIRE_GUARD` = `"wg"`, `PROTO_STEALTH` = `"stunnel"`
— the keys do not all match the user-facing names). Ports listed are the defaults; all six are
user-configurable, and the port map from the API supplies the choices.

The last four are all OpenVPN. Stealth is OpenVPN wrapped in stunnel; WSTunnel is OpenVPN over a
WebSocket. They are separate entries in the list because the app treats them as separate protocols to
try, not because they are separate implementations.

---

## Default

**WireGuard on port 443.**

```kotlin
// DataStorePreferenceHelper.getDefaultProtoInfo()
return Pair(PreferencesKeyConstants.PROTO_WIRE_GUARD, DEFAULT_WIRE_GUARD_PORT)
```

With one override: if the API has supplied a **suggested protocol** and port, that is used instead,
and `getAppSupportedProtocolList` moves it to the front of the list. So the effective first attempt is
the suggested protocol when there is one, and WireGuard otherwise.

Two more things reorder the list at connect time, in `AutoConnectionManager.reloadProtocols()`:

| Setting | Effect |
|---|---|
| Connection mode is **manual** | The user's saved protocol moves to the top. |
| **Preferred protocol** is on for this network | That network's protocol moves to the top, above the manual one. |

---

## Per protocol, as we use it

**WireGuard** — the default. Handshake-based, so a dead tunnel is silent; `TunnelTrafficMonitor`
watches the mux's packet counters for a run with no ingress, and is the only protocol with that
monitoring. `PinIpRecovery` handles the pinned-IP case. Connect budget is 20 s, shorter than the
others.

**IKEv2** — charon, driven through JNI. The interface is negotiated: charon fills the builder and
calls `requestTun()` from `establish()`, so the profile is built before the config is known.
Self-excluded from the blocking interface because charon fetches CRLs over plain HTTP on a socket
nothing protects.

**OpenVPN UDP / TCP** — the OpenVPN 3 C++ core. Its own `connect-timeout` defaults to 120 s, four
times our budget, so it will neither retry nor report inside our window. We derive progress from the
state stream instead — see [VPN_BACKEND.md](VPN_BACKEND.md) §7 for the 12 s / 30 s split.

**Stealth (`stunnel`)** — OpenVPN inside a TLS wrapper, so it looks like ordinary HTTPS. For networks
that block or fingerprint OpenVPN directly.

**WSTunnel** — OpenVPN over WebSocket, the last resort for heavily filtered networks. Known issue:
`WSTunnelLib.stop()` blocks for roughly 90 s on shutdown; an upstream fix is needed.

---

## Where the rest lives

Failover, budgets, protocol switching and the decision rules are backend concerns, not protocol ones:
[VPN_BACKEND.md](VPN_BACKEND.md) — §7 for failover, §8 for who decides.

Options that change protocol behaviour — protocol tweaks, server routing, extra TLS padding — are in
[ANTI_CENSORSHIP.md](../features/ANTI_CENSORSHIP.md).

Updating the OpenVPN native build: [OPENVPN_UPDATE.md](../guides/OPENVPN_UPDATE.md).
