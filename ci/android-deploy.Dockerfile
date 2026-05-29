# Custom CI image for the Deploy-stage jobs (formerly .docker_android_toolchain).
#
# Bakes in everything the Alpine deploy jobs install at runtime via before_script:
# system packages, Go, the Android NDK + CMake, and fastlane. This turns a
# multi-minute `apk add` / NDK download / gem install on every pipeline run into a
# pre-built layer, removing a recurring source of slowness and network flakiness.
#
# Hosted in this project's GitLab Container Registry. Built & pushed by the manual
# `BuildDeployImage` job in .gitlab-ci.yml (uses the built-in $CI_REGISTRY* creds —
# no PAT needed). The runner then pulls it automatically via CI_JOB_TOKEN.
#
# After the job runs once, point the pipeline at it by setting the BASE_CI_IMAGE
# CI/CD variable to:
#   $CI_REGISTRY_IMAGE/android-deploy:latest
#
# To build locally instead (optional):
#   docker login <your-gitlab-registry-host>
#   docker build -f ci/android-deploy.Dockerfile \
#     -t <registry-host>/<group>/<project>/android-deploy:latest .
#   docker push <registry-host>/<group>/<project>/android-deploy:latest

# Public Alpine image that ships the Android SDK, JDK 17, sdkmanager on PATH, and
# the `extras` helper (extras ndk / extras fastlane). Base for the current CI too.
FROM alvrme/alpine-android:android-36-jdk17

# Versions kept in sync with .gitlab-ci.yml (.docker_android_toolchain).
ARG NDK_VERSION=27.2.12479018
ARG CMAKE_VERSION=3.22.1
ARG GO_VERSION=1.21.10-r0
ARG GO_REPO=http://dl-cdn.alpinelinux.org/alpine/v3.19/community

# System build dependencies (was: two `apk add` lines in before_script).
RUN apk add --no-cache \
        curl flock jq swig \
        git make musl-dev build-base cmake ninja zip unzip perl

# Go pinned to the Alpine v3.19 community version (was: third `apk add` line).
RUN apk add --no-cache "go=${GO_VERSION}" --repository="${GO_REPO}"

# Android NDK + CMake via the base image's `extras` helper (was: `extras ndk ...`).
RUN extras ndk -n "${NDK_VERSION}" -c "${CMAKE_VERSION}"

# fastlane via `extras`, with the same ruby-gem fallback the before_script used.
RUN extras fastlane || (apk --no-cache add ruby ruby-dev && gem install fastlane --force)