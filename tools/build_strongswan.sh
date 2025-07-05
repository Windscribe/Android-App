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
gradle assembleRelease

echo "===> Copying output libs to: $STRONGSWAN_LIBS_DST"
mkdir -p "$STRONGSWAN_LIBS_DST"
cp -r "$APP_DIR"/build/intermediates/merged_native_libs/release/mergeReleaseNativeLibs/out/lib/* "$STRONGSWAN_LIBS_DST/"

echo "✅ Build completed successfully."