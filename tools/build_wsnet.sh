#!/usr/bin/env bash
ROOT_DIR="$PWD"
DST_DIR="$ROOT_DIR/base/src/main/libs"
rm -rf tools/bin
mkdir -p tools/bin
cd tools/bin || exit
export ANDROID_NDK_HOME="$ANDROID_NDK"
git clone https://github.com/Windscribe/Desktop-App.git
cd Desktop-App || exit
## WSNet 2.21.1
git checkout c0d606d6c2bf17bdecd9ef05e2691b3bc81ab48b
cd libs/wsnet/tools || exit
./build_android.sh > /dev/null 2>&1
cp wsnet.aar "$DST_DIR"
cd "$ROOT_DIR" && rm -rf tools/bin