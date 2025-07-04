#!/usr/bin/env bash
ROOT_DIR="$PWD"
DST_DIR="$ROOT_DIR/base/src/main/libs"
rm -rf tools/bin
mkdir -p tools/bin
cd tools/bin || exit
export ANDROID_NDK_HOME="$ANDROID_NDK"
git clone https://github.com/Windscribe/Desktop-App.git
cd Desktop-App || exit
git checkout 21a348471cca5c65ba6100c181da4b483f6fa8c8
cd libs/wsnet/tools || exit
./build_android.sh
cp wsnet.aar "$DST_DIR"
cd "$ROOT_DIR" && rm -rf tools/bin