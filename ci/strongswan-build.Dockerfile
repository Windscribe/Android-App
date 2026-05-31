# CI image for the BuildStrongswan job: bakes in the system deps, Gradle, Android
# SDK + NDK and OpenSSL 1.1.1 source that the job used to install at runtime.
# Built & pushed by the manual BuildStrongswanImage job; point BuildStrongswan at it
# via the STRONGSWAN_CI_IMAGE CI/CD variable.

FROM ubuntu:22.04

# Versions kept in sync with .gitlab-ci.yml. SS_NDK_VERSION was a CI/CD variable.
ARG SS_NDK_VERSION=27.2.12479018
ARG GRADLE_VERSION=8.7
ARG ANDROID_PLATFORM=android-34
ARG ANDROID_BUILD_TOOLS=34.0.0
ARG CMDLINE_TOOLS_VERSION=11076708
ARG OPENSSL_TAG=OpenSSL_1_1_1t

ENV ANDROID_SDK_ROOT=/opt/android-sdk \
    ANDROID_HOME=/opt/android-sdk \
    JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
    LANG=en_US.UTF-8 \
    LC_ALL=en_US.UTF-8

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        locales build-essential automake bison flex gettext gperf libtool \
        pkg-config swig jq curl zip unzip tar cmake git perl golang-go \
        ninja-build openjdk-17-jdk wget zlib1g-dev ca-certificates \
    && locale-gen en_US.UTF-8 \
    && rm -rf /var/lib/apt/lists/*

# Gradle
RUN wget -q "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
    && unzip -q "gradle-${GRADLE_VERSION}-bin.zip" -d /opt/gradle \
    && rm "gradle-${GRADLE_VERSION}-bin.zip"
ENV PATH="/opt/gradle/gradle-${GRADLE_VERSION}/bin:${JAVA_HOME}/bin:${PATH}"

# Android SDK command-line tools + platform/build-tools/NDK
RUN mkdir -p "${ANDROID_SDK_ROOT}/cmdline-tools" \
    && wget -q "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" -O /tmp/tools.zip \
    && unzip -q /tmp/tools.zip -d "${ANDROID_SDK_ROOT}/cmdline-tools" \
    && mv "${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools" "${ANDROID_SDK_ROOT}/cmdline-tools/latest" \
    && rm /tmp/tools.zip
ENV PATH="${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:${PATH}"

RUN yes | sdkmanager --licenses > /dev/null || true \
    && sdkmanager --sdk_root="${ANDROID_SDK_ROOT}" \
        "platform-tools" \
        "platforms;${ANDROID_PLATFORM}" \
        "build-tools;${ANDROID_BUILD_TOOLS}" \
        "ndk;${SS_NDK_VERSION}"
ENV ANDROID_NDK_HOME="${ANDROID_SDK_ROOT}/ndk/${SS_NDK_VERSION}" \
    ANDROID_NDK_ROOT="${ANDROID_SDK_ROOT}/ndk/${SS_NDK_VERSION}"

# OpenSSL 1.1.1 source tree; BuildStrongswan reads it from OPENSSL_SRC.
RUN mkdir -p /opt/openssl-src \
    && wget -q "https://github.com/openssl/openssl/archive/refs/tags/${OPENSSL_TAG}.tar.gz" -O /tmp/openssl.tar.gz \
    && tar -xzf /tmp/openssl.tar.gz -C /opt/openssl-src \
    && rm /tmp/openssl.tar.gz
ENV OPENSSL_SRC="/opt/openssl-src/openssl-${OPENSSL_TAG}"