DST_DIR="$PWD/wstunnel/src/main/jniLibs"
rm -rf tools/bin
mkdir -p tools/bin
cd tools/bin || exit
git clone https://github.com/Windscribe/wstunnel.git
cd wstunnel || exit
git apply clib.patch
rm -rf build
./build_android.sh
ls build/android
cp -R build/android/arm64-v8a "$DST_DIR"
cp -R build/android/armeabi-v7a "$DST_DIR"
cp -R build/android/x86 "$DST_DIR"
cp -R build/android/x86_64 "$DST_DIR"
cd ../../ && rm -rf bin