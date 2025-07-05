#!/bin/bash

set -euo pipefail

# === INPUT ===
APK_UNSIGNED_PATH="$1"

if [ ! -f "$APK_UNSIGNED_PATH" ]; then
  echo "❌ APK not found at: $APK_UNSIGNED_PATH"
  exit 1
fi

# === CONFIGURATION ===
BUILD_TOOLS_VERSION=35.0.0
KEYSTORE_PATH=temp-keystore.jks
KEY_ALIAS=tempkey
KEYSTORE_PASSWORD=temp123
KEY_PASSWORD=temp123

# === SDK Tools ===
ZIPALIGN="$ANDROID_SDK_ROOT/build-tools/$BUILD_TOOLS_VERSION/zipalign"
APKSIGNER="$ANDROID_SDK_ROOT/build-tools/$BUILD_TOOLS_VERSION/apksigner"

# === TEMP FILES ===
APK_ALIGNED_PATH="${APK_UNSIGNED_PATH%.apk}-aligned.apk"
APK_SIGNED_PATH="${APK_UNSIGNED_PATH%.apk}-signed.apk"

echo "🧹 Cleaning up temporary files..."
rm -f "$KEYSTORE_PATH" "$APK_ALIGNED_PATH"

# === GENERATE TEMP KEYSTORE ===
echo "🔐 Creating temporary keystore..."
keytool -genkey -v \
  -keystore "$KEYSTORE_PATH" \
  -alias "$KEY_ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass "$KEYSTORE_PASSWORD" \
  -keypass "$KEY_PASSWORD" \
  -dname "CN=Temp, OU=Dev, O=Example, L=City, S=State, C=US" \
  -noprompt

# === ZIPALIGN ===
echo "📦 Aligning APK..."
"$ZIPALIGN" -v -p 4 "$APK_UNSIGNED_PATH" "$APK_ALIGNED_PATH"

# === SIGN ===
echo "✍️  Signing APK (v2 only)..."
"$APKSIGNER" sign \
  --ks "$KEYSTORE_PATH" \
  --ks-key-alias "$KEY_ALIAS" \
  --ks-pass "pass:$KEYSTORE_PASSWORD" \
  --key-pass "pass:$KEY_PASSWORD" \
  --v1-signing-enabled false \
  --v2-signing-enabled true \
  --v3-signing-enabled false \
  --v4-signing-enabled false \
  --out "$APK_SIGNED_PATH" \
  "$APK_ALIGNED_PATH"

echo "✅ Verifying signed APK..."
"$APKSIGNER" verify "$APK_SIGNED_PATH"

echo "📁 Replacing original APK with signed version..."
mv -f "$APK_SIGNED_PATH" "$APK_UNSIGNED_PATH"
echo "🎉 APK successfully signed with v2 and saved at: $APK_UNSIGNED_PATH"