#!/usr/bin/env bash
ROOT_DIR="$PWD"
DST_DIR="$ROOT_DIR/base/src/main/libs"
rm -rf tools/bin
mkdir -p tools/bin
cd tools/bin || exit
export ANDROID_NDK_HOME="$ANDROID_NDK"
git clone https://github.com/Windscribe/Desktop-App.git
cd Desktop-App || exit
git checkout f8fb445b35852e5bf42ea6f178172038ec36f0d9
cd libs/wsnet/tools || exit
./build_android.sh
cp wsnet.aar "$DST_DIR"
cd "$ROOT_DIR" && rm -rf tools/bin