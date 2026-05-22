# Maestro E2E Tests

End-to-end UI tests driven by [Maestro](https://maestro.mobile.dev/). These run
against a real device or emulator, hitting the **real Windscribe API** and
establishing **real VPN tunnels** — they are *not* sandboxed.

Use this for catching:
- Crashes in the native VPN libs (`libwg-go.so`, `libcharon.so`, `libopenvpn.so`).
- Regressions in the connect / disconnect / protocol-switch flow.
- Login, registration, and other UI flows that depend on backend behavior.

CI integration is intentionally out of scope for now — these are local-run only.

## One-time setup

1. **Install Maestro** (if not already):
   ```
   curl -Ls https://get.maestro.mobile.dev | bash
   ```
   Make sure `~/.maestro/bin` is on your `PATH`.

2. **Install the app** on the target device/emulator. Either install a debug
   build via Android Studio or sideload an AAB-derived APK:
   ```
   ./gradlew :mobile:installGoogleDebug
   ```

3. **Configure credentials**:
   ```
   cp maestro/.env.example maestro/.env
   # then edit maestro/.env and fill in real staging credentials
   ```
   `maestro/.env` is gitignored.

4. **Confirm a device is attached**:
   ```
   adb devices
   ```
   You should see one device in the `device` state. If the `adb` command is
   missing, ensure `~/Library/Android/sdk/platform-tools` is on your `PATH`.

## Running flows

Run a single flow:
```
set -a; source maestro/.env; set +a
maestro test maestro/flows/01-launch.yaml
```

Run everything in `flows/`:
```
set -a; source maestro/.env; set +a
maestro test maestro/flows/
```

Use the helper script which sources `.env` and runs all flows:
```
./maestro/run.sh
```

## Layout

```
maestro/
├── config.yaml              # Global onFlowStart hook (dismisses system dialogs)
├── .env.example             # Required env vars (copy to .env)
├── README.md
├── run.sh                   # Convenience wrapper
├── flows/                   # One YAML per scenario; the entry points
│   ├── 01-launch.yaml       # Smoke test: app launches, lands on login
│   ├── 02-login.yaml        # Login with real credentials
│   ├── 03-connect.yaml      # Real VPN tunnel up / down
│   └── 04-protocol-switch.yaml  # Cycle WG / IKEv2 / OpenVPN
└── subflows/                # Reusable steps invoked via runFlow
    ├── login.yaml
    └── allow-system-dialog.yaml
```

## Writing new flows

- Prefer **`id:` selectors** (Compose `Modifier.testTag(...)`) over text — text
  changes when localized.
- Use **`subflows/`** for anything reused (login, dismissing dialogs).
- Set **timeouts explicitly** on `assertVisible` for screens that depend on
  network round-trips (login response, tunnel up).
- Real tunnel flows should run on a clean network state — disconnect any
  existing VPN before starting.

## Debugging a failing flow

```
maestro test --debug-output /tmp/maestro-debug maestro/flows/03-connect.yaml
maestro studio  # interactive UI to inspect the current screen and craft selectors
```
