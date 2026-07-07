#!/usr/bin/env bash

# Usage: ./build_wsnet.sh [flavor] [version] [output_dir]
#   flavor: google (uses GitLab) or fdroid (uses GitHub) - defaults to fdroid
#   version: wsnet version/tag - defaults to 1.5.4
#   output_dir: where to place the AAR - defaults to base/src/main/libs
# Examples:
#   ./tools/build_wsnet.sh google
#   ./tools/build_wsnet.sh google 1.5.4
#   ./tools/build_wsnet.sh google 1.5.4 ./base/src/main/libs

set -e  # Exit on error

# Get the absolute path to the project root (script is in tools/ subdirectory)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

FLAVOR="${1:-fdroid}"
WSNET_VERSION="${2:-1.5.27}"
DEFAULT_DST_DIR="$ROOT_DIR/base/src/main/libs"
DST_DIR="${3:-$DEFAULT_DST_DIR}"

# Convert DST_DIR to absolute path if it's relative
if [[ "$DST_DIR" != /* ]]; then
    DST_DIR="$(cd "$(dirname "$DST_DIR")" 2>/dev/null && pwd)/$(basename "$DST_DIR")" || DST_DIR="$ROOT_DIR/$DST_DIR"
fi

# Validate flavor
if [[ "$FLAVOR" != "google" && "$FLAVOR" != "fdroid" ]]; then
    echo "Error: Invalid flavor '$FLAVOR'. Must be 'google' or 'fdroid'"
    exit 1
fi

# VCPKG registry is always on GitHub
VCPKG_REGISTRY_URL="https://github.com/Windscribe/ws-vcpkg-registry.git"

# Set WSNet repository URL based on flavor
if [[ "$FLAVOR" == "google" ]]; then
    echo "Building for Google flavor (using internal GitLab)"
    WSNET_URL="https://gitlab.int.windscribe.com/ws/client/client-libs/wsnet.git"
else
    echo "Building for F-Droid flavor (using public GitHub)"
    WSNET_URL="https://github.com/Windscribe/wsnet.git"
fi

echo "Configuration:"
echo "  Flavor: $FLAVOR"
echo "  Output Directory: $DST_DIR"
echo "  WSNet Version: $WSNET_VERSION"
echo "  VCPKG Registry: $VCPKG_REGISTRY_URL (always GitHub)"
echo "  WSNet Repository: $WSNET_URL"

# Create output directory if it doesn't exist
mkdir -p "$DST_DIR"

# Clean and prepare build directory
rm -rf tools/bin
mkdir -p tools/bin
cd tools/bin || exit

# Setup environment
if [[ "$FLAVOR" == "fdroid" ]]; then
    export ANDROID_NDK_HOME="$ANDROID_NDK"
fi
export VCPKG_ROOT="$HOME/vcpkg"

# Clone repositories
echo "Cloning VCPKG registry..."
git clone "$VCPKG_REGISTRY_URL"
./ws-vcpkg-registry/install-vcpkg/vcpkg_install.sh "$VCPKG_ROOT" --configure-git

echo "Cloning WSNet..."
git clone "$WSNET_URL"
cd wsnet || exit

# Checkout specific version
echo "Checking out WSNet $WSNET_VERSION..."
git checkout "$WSNET_VERSION"

# Build
echo "Building WSNet for Android..."
cd tools || exit
./build_android.sh > /dev/null 2>&1

# Copy AAR to destination
echo "Copying wsnet.aar to $DST_DIR"
cp wsnet.aar "$DST_DIR"

# Cleanup
cd "$ROOT_DIR" && rm -rf tools/bin

echo "Build complete! AAR saved to: $DST_DIR/wsnet.aar"