#!/usr/bin/env bash
# Runs Maestro UI tests for the Windscribe Android app.
# Usage:
#   ./maestro/run.sh                                  # build + install + run all flows
#   ./maestro/run.sh --skip-build                     # skip build, use already-installed APK
#   ./maestro/run.sh maestro/flows/03-connect.yaml    # build + install + run one flow
#   ./maestro/run.sh --skip-build maestro/flows/03-connect.yaml

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

# ── Env & tools ───────────────────────────────────────────────────────────────

[[ ! -f "$ENV_FILE" ]] && { echo "error: $ENV_FILE not found."; exit 1; }

# shellcheck disable=SC1090
set -a; source "$ENV_FILE"; set +a

if ! command -v maestro >/dev/null 2>&1; then
  [[ -x "$HOME/.maestro/bin/maestro" ]] \
    && export PATH="$HOME/.maestro/bin:$PATH" \
    || { echo "error: maestro not on PATH. Install: curl -Ls https://get.maestro.mobile.dev | bash"; exit 1; }
fi

ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
[[ ! -x "$ADB" ]] && ADB=adb
"$ADB" devices | awk 'NR>1 && $2=="device"' | grep -q . || { echo "error: no Android device attached."; exit 1; }

MAESTRO_ENV_ARGS=()
[[ -n "${TEST_EMAIL:-}" ]]    && MAESTRO_ENV_ARGS+=(-e "TEST_EMAIL=$TEST_EMAIL")
[[ -n "${TEST_PASSWORD:-}" ]] && MAESTRO_ENV_ARGS+=(-e "TEST_PASSWORD=$TEST_PASSWORD")

# ── Parse args ────────────────────────────────────────────────────────────────

SKIP_BUILD=false
FLOW_ARGS=()
for arg in "$@"; do
  [[ "$arg" == "--skip-build" ]] && SKIP_BUILD=true || FLOW_ARGS+=("$arg")
done

# ── Helpers ───────────────────────────────────────────────────────────────────

build_and_install() {
  echo "Building google debug APK..."
  cd "$REPO_DIR"
  ./gradlew :mobile:assembleGoogleDebug --no-daemon -q
  local APK
  APK=$(find mobile/build/outputs/apk/google/debug -name "*.apk" | head -1)
  [[ -z "$APK" ]] && { echo "error: APK not found."; exit 1; }
  echo "Installing $APK..."
  "$ADB" install -r "$APK"
  echo "Installed."
}

run_flows() {
  maestro test "${MAESTRO_ENV_ARGS[@]}" "$@" || SUITE_EXIT=1
}

get_public_ip() {
  "$ADB" shell "(printf 'GET / HTTP/1.0\r\nHost: api.ipify.org\r\nConnection: close\r\n\r\n'; sleep 1) \
    | nc -w 3 api.ipify.org 80 \
    | tr -d '\r' \
    | grep -Eo '([0-9]{1,3}\.){3}[0-9]{1,3}' \
    | tail -n 1" 2>/dev/null | tr -d '[:space:]'
}

resolve_dns() {
  # Returns the resolved IP for a domain, empty string if blocked/failed
  "$ADB" shell "ping -c 1 $1 2>/dev/null" | grep '^PING' | grep -Eo '([0-9]{1,3}\.){3}[0-9]{1,3}' | head -1 | tr -d '[:space:]'
}


check_ip_unchanged() {
  local before=$1 after=$2
  if [[ -z "$before" || -z "$after" ]]; then
    echo "[Warning] split-tunnel ip-check: could not fetch device IP — skipped"
  elif [[ "$before" != "$after" ]]; then
    echo "[Failed]  split-tunnel ip-check: IP changed but should not have (before=$before after=$after)"
    SUITE_EXIT=1
  else
    echo "[Passed]  split-tunnel ip-check: IP unchanged $before (Windscribe excluded from tunnel)"
  fi
}

check_dns_blocked() {
  local before=$1 after=$2 domain=$3
  if [[ -z "$before" || "$before" == "127.0.0.1" ]]; then
    echo "[Failed]  robert-dns: $domain should resolve before connect but got: $before"
    SUITE_EXIT=1
  elif [[ "$after" != "127.0.0.1" ]]; then
    echo "[Failed]  robert-dns: $domain should resolve to 127.0.0.1 when blocked but got: $after"
    SUITE_EXIT=1
  else
    echo "[Passed]  robert-dns: $domain resolved ($before) before connect, blocked (127.0.0.1) while connected"
  fi
}

check_ip_changed() {
  local before=$1 after=$2
  if [[ -z "$before" || -z "$after" ]]; then
    echo "[Warning] ip-check: could not fetch device IP — skipped"
  elif [[ "$before" == "$after" ]]; then
    echo "[Failed]  ip-check: IP did not change (before=$before after=$after)"
    SUITE_EXIT=1
  else
    echo "[Passed]  ip-check: IP changed $before → $after"
  fi
}

# ── Main ──────────────────────────────────────────────────────────────────────

[[ "$SKIP_BUILD" == false ]] && build_and_install

# Single flow mode
[[ ${#FLOW_ARGS[@]} -gt 0 ]] && exec maestro test "${MAESTRO_ENV_ARGS[@]}" "${FLOW_ARGS[@]}"

# Full suite
SUITE_EXIT=0

run_flows \
  "$SCRIPT_DIR/flows/01-launch.yaml" \
  "$SCRIPT_DIR/flows/02-login.yaml" \
  "$SCRIPT_DIR/flows/03-connect.yaml" \
  "$SCRIPT_DIR/flows/04-protocol-switch.yaml" \
  "$SCRIPT_DIR/flows/05-tab-navigation.yaml" \
  "$SCRIPT_DIR/flows/06-search.yaml" \
  "$SCRIPT_DIR/flows/08-favourites.yaml" \
  "$SCRIPT_DIR/flows/09-newsfeed.yaml"

IP_BEFORE=$(get_public_ip)
run_flows "$SCRIPT_DIR/flows/10-ip-check.yaml"
IP_AFTER=$(get_public_ip)
run_flows "$SCRIPT_DIR/flows/10-ip-disconnect.yaml"
check_ip_changed "$IP_BEFORE" "$IP_AFTER"

run_flows "$SCRIPT_DIR/flows/11-split-tunnel.yaml"
ST_IP_BEFORE=$(get_public_ip)
run_flows "$SCRIPT_DIR/flows/10-ip-check.yaml"
ST_IP_AFTER=$(get_public_ip)
run_flows "$SCRIPT_DIR/flows/10-ip-disconnect.yaml"
run_flows "$SCRIPT_DIR/flows/11-split-tunnel-cleanup.yaml"
check_ip_unchanged "$ST_IP_BEFORE" "$ST_IP_AFTER"

DNS_BEFORE=$(resolve_dns facebook.com)                     # disconnected — should resolve
run_flows "$SCRIPT_DIR/flows/12-robert-dns.yaml"           # connect + social filter on
DNS_AFTER=$(resolve_dns facebook.com)                      # connected + blocked — should be 127.0.0.1
run_flows "$SCRIPT_DIR/flows/12-robert-dns-disconnect.yaml"
run_flows "$SCRIPT_DIR/flows/12-robert-dns-cleanup.yaml"
check_dns_blocked "$DNS_BEFORE" "$DNS_AFTER" "facebook.com"

run_flows "$SCRIPT_DIR/flows/07-logout.yaml"

exit $SUITE_EXIT
