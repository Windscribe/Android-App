#!/usr/bin/env bash
ROOT_DIR="$PWD"
DST_DIR="$ROOT_DIR/base/src/main/libs"
rm -rf tools/bin
mkdir -p tools/bin
cd tools/bin || exit
export ANDROID_NDK_HOME="$ANDROID_NDK"
git clone https://github.com/Windscribe/Desktop-App.git
cd Desktop-App ||
## WSNet 2.17.1
git checkout 91c51f0737fa1bd951941b17273fa5494d70bf37
cd libs/wsnet/tools || exit
./build_android.sh > /dev/null 2>&1
cp wsnet.aar "$DST_DIR"
cd "$ROOT_DIR" && rm -rf tools/bin