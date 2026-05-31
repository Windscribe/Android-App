# Maestro E2E Tests

End-to-end UI tests driven by [Maestro](https://maestro.mobile.dev/). These run
against a real device or emulator, hitting the **real Windscribe staging API**
and establishing **real VPN tunnels** — they are *not* sandboxed.

Used to test critical flows of the app.

## One-time setup

1. **Install Maestro** (if not already):
   ```
   curl -Ls https://get.maestro.mobile.dev | bash
   ```
   Make sure `~/.maestro/bin` is on your `PATH`.

2. **Configure credentials** — export the test account in your shell (or add to
   `~/.zshrc` / `~/.bashrc`):
   ```
   export TEST_EMAIL="..."
   export TEST_PASSWORD="..."
   ```
   In CI these come from GitLab CI/CD variables.

3. **Confirm a device is attached**:
   ```
   adb devices
   ```
   You should see one device in the `device` state. If `adb` is missing from
   your `PATH`, add `~/Library/Android/sdk/platform-tools`.

## Running flows

The helper script builds the arm64 staging APK, installs it, and runs the full
suite:
```
./maestro/run.sh
```

Skip the build and use whatever APK is already installed:
```
./maestro/run.sh --skip-build
```

Run a single flow (still builds + installs unless `--skip-build`):
```
./maestro/run.sh maestro/flows/03-connect.yaml
./maestro/run.sh --skip-build maestro/flows/03-connect.yaml
```

### Environment variables

- `TEST_EMAIL` _(required)_ — staging account.
- `TEST_PASSWORD` _(required)_ — staging account.
- `TEST_VIDEO` (default `true`) — record one MP4 per flow via `adb screenrecord` → `maestro-reports/videos/`. Set to `false` to disable.
- `DEV` (unset locally) — set to `true` so the build points at staging. CI does this automatically in `MaestroBuildApk`.
- `ADB` (auto-detected) — override the adb binary if auto-detection fails.

## CI

Two GitLab jobs in `.gitlab-ci.yml`:

- **`MaestroBuildApk`** (manual) — Builds an arm64-v8a debug APK with `DEV=true`
  (staging). Artifact retained 1 day.
- **`MaestroUITests`** (auto, on success of `MaestroBuildApk`) — Runs on the
  arm64 macOS runner (tag `arm64`), boots the `ws_test_avd` emulator, installs
  the APK, and runs the full suite. Uploads emulator log + Maestro reports +
  per-flow videos. Artifact retained 1 day.

Click `MaestroBuildApk` to trigger; `MaestroUITests` follows automatically.

## Layout

```
maestro/
├── README.md
├── config.yaml                              # Global onFlowStart hook
├── run.sh                                   # Build + install + run wrapper
├── flows/                                   # Top-level scenarios
│   ├── 01-launch.yaml                       # App launches, lands on login
│   ├── 02-login.yaml                        # Login with staging creds
│   ├── 03-connect.yaml                      # Tunnel up / down (default protocol)
│   ├── 04-protocol-switch.yaml              # WG → IKEv2 → OpenVPN UDP
│   ├── 05-tab-navigation.yaml
│   ├── 06-search.yaml
│   ├── 07-logout.yaml
│   ├── 08-favourites.yaml
│   ├── 09-newsfeed.yaml
│   ├── 10-ip-check.yaml + 10-ip-disconnect.yaml
│   ├── 11-split-tunnel.yaml + 11-split-tunnel-cleanup.yaml
│   └── 12-robert-dns.yaml + 12-robert-dns-disconnect.yaml + 12-robert-dns-cleanup.yaml
└── subflows/                                # Reusable steps invoked via runFlow
    ├── login.yaml
    ├── connect.yaml
    ├── wait-for-connected.yaml              # Poll + sweep popups until connected
    ├── wait-for-disconnected.yaml           # Poll + sweep until disconnected
    ├── allow-system-dialog.yaml             # OS "Allow" / "OK" buttons
    ├── dismiss-popups.yaml                  # Sweeps the two in-app popups below
    ├── dismiss-preferred-protocol-popup.yaml
    ├── dismiss-battery-optimization-popup.yaml
    ├── dismiss-wg-key-popup.yaml            # WG "key limit reached" retry
    └── clear-favourites.yaml
```

Some scenarios are split across multiple files (e.g. `10-ip-check` +
`10-ip-disconnect`) because `run.sh` interleaves them with `adb shell` calls to
measure the device's public IP before, during, and after the tunnel — see the
suite block at the bottom of `run.sh` for the orchestration.

## Writing new flows

- Prefer **`id:` selectors** (Compose `Modifier.testTag(...)`) over text — text
  changes when localized.
- Use **`subflows/`** for anything reused (login, dismissing dialogs).
- For anything that involves connecting, use `subflows/wait-for-connected.yaml`
  instead of a bare `extendedWaitUntil`. It polls and sweeps intermittent
  popups (preferred-protocol, battery-optimization, OS VPN consent) until the
  connected indicator is visible. Same for `wait-for-disconnected.yaml`.
- Set **timeouts explicitly** on `extendedWaitUntil` for screens that depend on
  network round-trips (login response, tunnel up).
- Real tunnel flows should run on a clean network state — disconnect any
  existing VPN before starting.

## Debugging a failing flow

```
maestro test --debug-output /tmp/maestro-debug maestro/flows/03-connect.yaml
maestro studio  # interactive UI to inspect the current screen and craft selectors
```

When a CI run fails, grab the artifact bundle from the `MaestroUITests` job:
- `emulator.log` — boot + runtime log of the AVD
- `maestro-reports/` — Maestro's own HTML/XML reports
- `maestro-reports/videos/<flow-name>.mp4` — full screen recording of each flow
