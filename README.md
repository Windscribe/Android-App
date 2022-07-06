# **Windscribe for Android**

# **How to build:**

Install
- SDK
- NDK
- Cmake
- Swig
- Clone this repository and run ./gradlew
assembleDebug or open project in android studio.

# **Strongswan:**

Prebuilt Strongswan binaries are included. If required build Strongswan
using these instructions and replace contents of ./strongswan/libs
https://wiki.strongswan.org/projects/strongswan/wiki/AndroidVPNClientBuild


# **Code style:**

- ktlint with default rules is used for Kotlin.
- Java uses
  grandcentrix-AndroidCodeStyle(https://github.com/GCX-HCI/grandcentrix-AndroidCodeStyle)

# **Contributing**

- Stick to ktlint code style.
- Most of the code is written in Java. We are in the process to move
  everything to kotlin. Our preferred stack includes kotlin ,
  coroutines, Kotlin flows, and MVP. All future changes should respect
  it where possible.
- Code is structured into multiple feature modules(Openvpn, Strongswan,
  Stealth, Test), base module(Core Logic) and UI Modules(Tv, Mobile).
  Avoid circular dependencies between modules. It should always follow
  this path: Feature > base > UI

# **Versioning**

format: [major][minor][BuildNumber]


Copyright (c) 2021 Windscribe Limited
