strongswanLibsSrc="$PWD/../strongswan-src/src/frontends/android/app/src/main/libs/*"
strongswanLibsDst="$PWD/../strongswan/libs"
mkdir -p $strongswanLibsDst
strongswanRoot="$PWD/../strongswan-src"
cd $strongswanRoot
lib="$PWD/src/frontends/android/app/src/main/jni/openssl"
PATH=$ANDROID_NDK/toolchains/llvm/prebuilt/linux-x86_64/bin:$PATH
pushd $OpenSSL
CC=armv7a-linux-androideabi16-clang ./Configure android-arm no-shared no-ssl3 no-engine no-dso no-asm no-hw no-comp no-stdio -fPIC -DOPENSSL_PIC -D__ANDROID_API__=16 -ffast-math -O3 -funroll-loops
make -j $(nproc) > config.log
mkdir -p "${lib}/armeabi-v7a"
cp libcrypto.a ${lib}/armeabi-v7a/
make distclean
CC=aarch64-linux-android21-clang ./Configure android-arm64 no-shared no-ssl3 no-engine no-dso no-asm no-hw no-comp no-stdio -fPIC -DOPENSSL_PIC -D__ANDROID_API__=21 -ffast-math -O3 -funroll-loops
make -j $(nproc) > config.log
mkdir -p "${lib}/arm64-v8a"
cp libcrypto.a ${lib}/arm64-v8a/
make distclean
CC=i686-linux-android16-clang ./Configure android-x86 no-shared no-ssl3 no-engine no-dso no-asm no-hw no-comp no-stdio -fPIC -DOPENSSL_PIC -D__ANDROID_API__=16 -ffast-math -O3 -funroll-loops
make -j $(nproc) > config.log
mkdir -p "${lib}/x86"
cp libcrypto.a ${lib}/x86/
make distclean
CC=x86_64-linux-android21-clang ./Configure android-x86_64 no-shared no-ssl3 no-engine no-dso no-asm no-hw no-comp no-stdio -fPIC -DOPENSSL_PIC -D__ANDROID_API__=21 -ffast-math -O3 -funroll-loops
make -j $(nproc) > config.log
mkdir -p "${lib}/x86_64"
cp libcrypto.a ${lib}/x86_64/
cp -R include/ ${lib}/
make distclean
rm config.log
popd
echo "LOCAL_PATH := \$(call my-dir)" >"${lib}/Android.mk"
echo "include \$(CLEAR_VARS)" >>"${lib}/Android.mk"
echo "LOCAL_MODULE := libcrypto_static" >>"${lib}/Android.mk"
echo "LOCAL_SRC_FILES := \$(TARGET_ARCH_ABI)/libcrypto.a" >>"${lib}/Android.mk"
echo "LOCAL_EXPORT_C_INCLUDES := \$(LOCAL_PATH)/include" >>"${lib}/Android.mk"
echo "include \$(PREBUILT_STATIC_LIBRARY)" >>"${lib}/Android.mk"
cd $strongswanRoot
./autogen.sh > config.log
./configure --disable-defaults > config.log
make dist -j $(nproc) > config.log
rm config.log
