# Windscribe WSTunnel for mobile apps.
This project is a fork of [https://github.com/trazfr/tcp-over-websocket](https://github.com/trazfr/tcp-over-websocket). Only relevent source are kept for easier maintence.

## Build
1. Run `go mod tidy` in ws directory.
2. Install gomobile tools if not already installed.
   [Download](https://github.com/golang/mobile).
3. Run`gomobile bind -o ../../base/libs/wstunnel.aar  -javapkg com.windscribe.websockettunnel`
This builds .aar library and platform specific bindings from exported functions.
This library is then copied to base module's libs directory.
4. Exported binding are used by [WSTunnelManager](https://gitlab.int.windscribe.com/ws/client/androidapp/-/blob/300-ws-tunnel/base/src/main/java/com/windscribe/vpn/backend/openvpn/WsTunnelManager.kt) class.

## Run
1. Select WSTunnel protocol from android app settings.
2. Connect and enjoy.
3. Log file is saved to Android app's internal storage.

## What to do next
1. Test , test and test more.
2. Implement Ws tunnel protocol for iOS.