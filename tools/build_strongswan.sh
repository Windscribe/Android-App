#!/usr/bin/env bash

set -euo pipefail

# === Usage ===
# ./build_strongswan.sh /path/to/strongswan-src /path/to/openssl /path/to/output-libs
# ================================

if [ "$#" -ne 3 ]; then
    echo "Usage: $0 <strongswan-dir> <openssl-dir> <strongswan-libs-dst>"
    exit 1
fi

# === Parameters ===
STRONGSWAN_DIR="$1"
OPENSSL_SRC="$2"
STRONGSWAN_LIBS_DST="$3"

# === Derived Paths ===
ANDROID_OPENSSL_DIR="$STRONGSWAN_DIR/src/frontends/android/openssl"
export OUT_DIR="$STRONGSWAN_DIR/src/frontends/android/app/src/main/jni/openssl"
APP_DIR="$STRONGSWAN_DIR/src/frontends/android/app"

# OPENSSL_SRC comes from the strongswan-build image ENV, not from .gitlab-ci.yml,
# so a stale image would otherwise link an old OpenSSL without any sign of it.
# Fail loudly instead: an empty value here would make the cp below read "/.".
if [ -z "$OPENSSL_SRC" ] || [ ! -d "$OPENSSL_SRC" ]; then
    echo "ERROR: OPENSSL_SRC is empty or not a directory: '$OPENSSL_SRC'" >&2
    echo "It is set by ci/strongswan-build.Dockerfile; rebuild that image." >&2
    exit 1
fi

# VERSION.dat exists only from OpenSSL 3.0 on, so this also rejects a 1.x tree.
if [ ! -f "$OPENSSL_SRC/VERSION.dat" ]; then
    echo "ERROR: no VERSION.dat in $OPENSSL_SRC -- expected OpenSSL 3.x." >&2
    echo "A pre-3.0 source tree means the strongswan-build image is stale." >&2
    exit 1
fi
# shellcheck source=/dev/null
. "$OPENSSL_SRC/VERSION.dat"
echo "===> OpenSSL source: ${MAJOR}.${MINOR}.${PATCH}"
if [ "${MAJOR}" != "3" ]; then
    echo "ERROR: expected OpenSSL 3.x, found ${MAJOR}.${MINOR}.${PATCH}." >&2
    exit 1
fi

echo "===> Copying OpenSSL sources..."
cp -a "$OPENSSL_SRC/." "$ANDROID_OPENSSL_DIR/"

echo "===> Compiling OpenSSL..."
cd "$ANDROID_OPENSSL_DIR"
./compile.sh

echo "===> Generating Android.mk for libcrypto_static..."
mkdir -p "$OUT_DIR"
cat <<EOF > "${OUT_DIR}/Android.mk"
LOCAL_PATH := \$(call my-dir)
include \$(CLEAR_VARS)
LOCAL_MODULE := libcrypto_static
LOCAL_SRC_FILES := \$(TARGET_ARCH_ABI)/libcrypto.a
LOCAL_EXPORT_C_INCLUDES := \$(LOCAL_PATH)/include
include \$(PREBUILT_STATIC_LIBRARY)
EOF

echo "===> Building Strongswan..."
cd "$STRONGSWAN_DIR"
./autogen.sh > config.log
./configure --disable-defaults >> config.log
make dist -j "$(nproc)" >> config.log
rm config.log

echo "===> Updating Gradle build config..."
cd "$APP_DIR"
sed -i '/arguments '\''-j'\'' \+ Runtime\.runtime\.availableProcessors()/a\        arguments "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON"' build.gradle

echo "===> Building release APK..."
# Use the project's wrapper, not the image's gradle: each strongSwan tag pins
# the Gradle version its AGP requires (6.1.0 needs 8.13), so the wrapper keeps
# this in step automatically instead of failing on the next version bump.
chmod +x "$APP_DIR/../gradlew"
"$APP_DIR/../gradlew" assembleRelease

echo "===> Copying output libs to: $STRONGSWAN_LIBS_DST"
mkdir -p "$STRONGSWAN_LIBS_DST"
cp -r "$APP_DIR"/build/intermediates/merged_native_libs/release/mergeReleaseNativeLibs/out/lib/* "$STRONGSWAN_LIBS_DST/"

echo "✅ Build completed successfully."