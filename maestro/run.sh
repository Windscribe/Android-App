#!/usr/bin/env bash
# Convenience wrapper: build debug APK, install it, then run Maestro flows.
# Usage:
#   ./maestro/run.sh                                 # build + install + run all flows
#   ./maestro/run.sh --skip-build                    # skip build, use already-installed APK
#   ./maestro/run.sh maestro/flows/01-launch.yaml    # build + install + run single flow
#   ./maestro/run.sh --skip-build maestro/flows/01-launch.yaml

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "error: $ENV_FILE not found. Copy maestro/.env.example to maestro/.env and fill it in."
  exit 1
fi

# shellcheck disable=SC1090
set -a
source "$ENV_FILE"
set +a

if ! command -v maestro >/dev/null 2>&1; then
  if [[ -x "$HOME/.maestro/bin/maestro" ]]; then
    export PATH="$HOME/.maestro/bin:$PATH"
  else
    echo "error: maestro is not on PATH. Install via: curl -Ls https://get.maestro.mobile.dev | bash"
    exit 1
  fi
fi

ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
if [[ ! -x "$ADB" ]]; then
  ADB=adb
fi

DEVICES="$("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}')"
if [[ -z "$DEVICES" ]]; then
  echo "error: no Android device attached. Start an emulator or plug in a device with USB debugging enabled."
  exit 1
fi

MAESTRO_ENV_ARGS=()
[[ -n "${TEST_EMAIL:-}" ]]    && MAESTRO_ENV_ARGS+=(-e "TEST_EMAIL=$TEST_EMAIL")
[[ -n "${TEST_PASSWORD:-}" ]] && MAESTRO_ENV_ARGS+=(-e "TEST_PASSWORD=$TEST_PASSWORD")

# ── Build & install ───────────────────────────────────────────────────────────
SKIP_BUILD=false
FLOW_ARGS=()
for arg in "$@"; do
  if [[ "$arg" == "--skip-build" ]]; then
    SKIP_BUILD=true
  else
    FLOW_ARGS+=("$arg")
  fi
done

if [[ "$SKIP_BUILD" == false ]]; then
  echo "Building google debug APK..."
  cd "$REPO_DIR"
  ./gradlew :mobile:assembleGoogleDebug --no-daemon -q
  APK_PATH=$(find mobile/build/outputs/apk/google/debug -name "*.apk" | head -1)
  if [[ -z "$APK_PATH" ]]; then
    echo "error: APK not found after build."
    exit 1
  fi
  echo "Installing $APK_PATH..."
  "$ADB" install -r "$APK_PATH"
  echo "Installed."
fi

if [[ ${#FLOW_ARGS[@]} -eq 0 ]]; then
  exec maestro test "${MAESTRO_ENV_ARGS[@]}" \
    "$SCRIPT_DIR/flows/01-launch.yaml" \
    "$SCRIPT_DIR/flows/02-login.yaml" \
    "$SCRIPT_DIR/flows/03-connect.yaml" \
    "$SCRIPT_DIR/flows/04-protocol-switch.yaml" \
    "$SCRIPT_DIR/flows/05-tab-navigation.yaml" \
    "$SCRIPT_DIR/flows/06-search.yaml" \
    "$SCRIPT_DIR/flows/08-favourites.yaml" \
    "$SCRIPT_DIR/flows/09-newsfeed.yaml" \
    "$SCRIPT_DIR/flows/10-ip-check.yaml" \
    "$SCRIPT_DIR/flows/11-split-tunnel.yaml" \
    "$SCRIPT_DIR/flows/12-robert-dns.yaml" \
    "$SCRIPT_DIR/flows/07-logout.yaml"
else
  exec maestro test "${MAESTRO_ENV_ARGS[@]}" "${FLOW_ARGS[@]}"
fi
