# CI image for the Deploy-stage jobs: bakes in the system deps, Go, Android NDK +
# CMake and fastlane that the Alpine deploy jobs used to install at runtime.
# Built & pushed by the manual BuildDeployImage job; point the pipeline at it via
# the BASE_CI_IMAGE CI/CD variable.

FROM alvrme/alpine-android:android-36-jdk17

# Versions kept in sync with .gitlab-ci.yml.
ARG NDK_VERSION=27.2.12479018
ARG CMAKE_VERSION=3.22.1
ARG GO_VERSION=1.21.10-r0
ARG GO_REPO=http://dl-cdn.alpinelinux.org/alpine/v3.19/community

RUN apk add --no-cache \
        curl flock jq swig \
        git make musl-dev build-base cmake ninja zip unzip perl

# Go pinned to the Alpine v3.19 community version.
RUN apk add --no-cache "go=${GO_VERSION}" --repository="${GO_REPO}"

# Android NDK + CMake via the base image's `extras` helper.
RUN extras ndk -n "${NDK_VERSION}" -c "${CMAKE_VERSION}"

# fastlane via `extras`, with a ruby-gem fallback.
RUN extras fastlane || (apk --no-cache add ruby ruby-dev && gem install fastlane --force)