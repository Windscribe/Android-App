#!/usr/bin/env bash
ROOT_DIR="$PWD"
DST_DIR="$ROOT_DIR/base/src/main/libs"
rm -rf tools/bin
mkdir -p tools/bin
cd tools/bin || exit
rm -rf vcpkg
git clone https://github.com/microsoft/vcpkg.git
cd vcpkg || exit
./bootstrap-vcpkg.sh
export VCPKG_ROOT="$PWD"
export ANDROID_NDK_HOME="$ANDROID_NDK"
echo "$ANDROID_NDK_HOME"
echo "$VCPKG_ROOT"
cd ..
git clone https://github.com/Windscribe/Desktop-App.git
cd Desktop-App || exit
git checkout fb88aab1964717301e9a482e4cb9c592bed1369b
cd libs/wsnet/tools || exit
./build_android.sh
cp wsnet.aar "$DST_DIR"
cd "$ROOT_DIR" && rm -rf tools/bin