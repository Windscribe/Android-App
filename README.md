# 🛡️ Windscribe VPN for Android

<div align="center">

**Your Digital Privacy Companion** 🚀

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-Proprietary-red.svg)](LICENSE)

*Developed by Windscribe Limited - Because your privacy matters*

[Features](#-key-features) • [Building](#-building-from-source) • [Architecture](#-architecture) • [Contributing](#-contributing) • [License](#-license)

</div>

---

## 📱 About

Windscribe VPN is a top-notch VPN application that offers comprehensive privacy and security features for Android devices. Whether you're browsing on your phone, tablet, or Android TV, Windscribe has you covered with military-grade encryption and blazing-fast servers worldwide.

## ✨ Key Features

### 🔐 **Authentication & Account Management**
- **SSO Login** - One-click authentication
- **Captcha Support** - Bot protection that actually works
- **Email Verification** - Get free data when you confirm your email
- **Account Management** - Full control at your fingertips

### 🌐 **Six VPN Protocols** (Because one size doesn't fit all!)
1. **OpenVPN UDP** - Fast and efficient
2. **OpenVPN TCP** - Reliable and stable
3. **IKEv2** - Lightning-fast with StrongSwan implementation
4. **Stealth Protocol** - OpenVPN TCP with a cloak of invisibility
5. **WSTunnel** - OpenVPN over WebSocket for maximum stealth
6. **WireGuard** - The new kid on the block, and it's fast! ⚡

### 🎯 **Advanced Features**
- **Per-Network Configuration** - Different protocols for different networks
- **Network Auto-Detection** - Seamless switching (requires location permissions)
- **Split Tunneling** - Choose which apps use the VPN
- **App Decoy Traffic** - Throw off surveillance with fake traffic
- **Custom Configurations** - Import your own WireGuard/OpenVPN configs
- **R.O.B.E.R.T** - DNS filtering with customizable toggles
- **Static IP Addresses** - Available with pro plans

### 🎨 **Personalization**
- Custom sounds for connections
- Custom wallpapers
- Custom names for server locations
- Location favorites
- Newsfeed with promos and news

### 📊 **Transparency**
- Real-time IP address display
- Connection status monitoring
- Traffic statistics

---

## 🏗️ Building from Source

### Prerequisites

Before you dive in, make sure you have these tools ready:

```bash
☐ Android Studio (Latest stable version)
☐ Android SDK (API 21+)
☐ Android NDK (for native code compilation)
☐ CMake (for building native modules)
☐ SWIG (for generating JNI bindings)
☐ Git (obviously!)
```

### Quick Start

```bash
# Clone the repository
git clone https://github.com/Windscribe/Android-App.git
cd androidapp

# Build debug version
./gradlew assembleDebug

# Or open in Android Studio and hit Run! 🎯
```

### Build Commands Cheat Sheet

```bash
# Debug Builds
./gradlew assembleDebug              # Build debug APK
./gradlew :mobile:assembleGoogleDebug   # Mobile app only
./gradlew :tv:assembleGoogleDebug       # TV app only

# Release Builds
./gradlew assembleRelease            # Build release APK
./gradlew bundleGoogleRelease        # Google Play AAB
./gradlew bundleFdroidRelease        # F-Droid AAB

# Testing
./gradlew test                       # Unit tests
./gradlew connectedAndroidTest       # Instrumented tests

# Code Quality
./gradlew ktlintCheck                # Check Kotlin style
./gradlew ktlintFormat               # Auto-format Kotlin
./gradlew dependencyCheckAnalyze     # Security analysis

# Cleaning
./gradlew clean                      # Fresh start!
```

### Installing & Running

#### Mobile App 📱
```bash
# Build and install
./gradlew :mobile:assembleGoogleDebug
$ANDROID_HOME/platform-tools/adb install -r mobile/build/outputs/apk/google/debug/mobile-google-debug.apk

# Launch the app
$ANDROID_HOME/platform-tools/adb shell am start -n com.windscribe.vpn/com.windscribe.mobile.ui.AppStartActivity
```

#### TV App 📺
```bash
# Build and install
./gradlew :tv:assembleGoogleDebug
$ANDROID_HOME/platform-tools/adb install -r tv/build/outputs/apk/google/debug/tv-google-debug.apk

# Launch the app
$ANDROID_HOME/platform-tools/adb shell am start -n com.windscribe.vpn/com.windscribe.tv.splash.SplashActivity
```

---

## 🏛️ Architecture

### Module Structure (The Big Picture)

```
androidapp/
├── 📦 base/              # Core functionality hub
│   ├── api/             # API communication
│   ├── backend/         # VPN protocol handlers
│   ├── localdatabase/   # Room database
│   ├── repository/      # Data layer
│   └── services/        # Android services
├── 📱 mobile/           # Phone/tablet UI (Jetpack Compose)
├── 📺 tv/               # Android TV UI (XML layouts)
├── 🔌 Protocol Modules
│   ├── openvpn/        # OpenVPN implementation
│   ├── strongswan/     # IKEv2/IPSec
│   └── wgtunnel/       # WireGuard, WSTunnel, Stunnel & ControlD (All Go code compiled to single lib)
├── 🌐 wsnet/           # Networking library
└── 🧪 test/            # Shared test utilities
```

### Technology Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Kotlin (100% of app modules; vendored native VPN modules are C/C++/Go/Java) |
| **UI** | Jetpack Compose (Mobile), XML (TV) |
| **Async** | Coroutines + Kotlin Flows |
| **DI** | Hilt |
| **Database** | Room |
| **Networking** | wsnet (custom library) |
| **Background Tasks** | WorkManager |

### Build Variants

#### 🟢 Google Play (`google` flavor)
- ✅ Google Play Billing
- ✅ In-App Review API
- ✅ Firebase Cloud Messaging
- ✅ Full feature set

#### 🟠 F-Droid (`fdroid` flavor)
- ❌ No proprietary Google dependencies
- ❌ No payment processing
- ❌ No push notifications
- ✅ 100% open source friendly

**For detailed architecture documentation, see [AGENTS.md](AGENTS.md)**

---

## 🎯 StrongSwan Setup

Prebuilt binaries are included, but if you're feeling adventurous:

1. Follow the [official StrongSwan Android build guide](https://wiki.strongswan.org/projects/strongswan/wiki/AndroidVPNClientBuild)
2. Replace contents in `./strongswan/libs`
3. Test thoroughly before committing! 🧪

---

## 💻 Code Style

### Kotlin (Preferred) ✨
We use **ktlint** with default rules:

```bash
# Check your code
./gradlew ktlintCheck

# Auto-fix issues
./gradlew ktlintFormat
```

### General Guidelines
- ✅ Use Kotlin for all new code
- ✅ Prefer coroutines over callbacks
- ✅ Use Kotlin flows for reactive streams
- ✅ Follow MVP pattern
- ✅ Write meaningful commit messages
- ✅ Test your changes!

---

## 🤝 Contributing

We ❤️ contributions! Here's how to get started:

### The Golden Rules

1. **Code Style is Sacred**
  - Run `ktlintFormat` before committing
  - Follow existing patterns
  - Keep it clean and readable

2. **Kotlin First, Always**
  - The app modules (base/mobile/tv) are 100% Kotlin — keep them that way
  - New code MUST be in Kotlin (no new Java files)
  - Use coroutines and flows

3. **Respect the Module Hierarchy**
   ```
   Feature Modules → base → UI Modules
   ```
   NO CIRCULAR DEPENDENCIES! 🚫

4. **Test Your Changes**
  - Write unit tests for business logic
  - Add instrumented tests for UI
  - Manual testing is also important!

### Development Workflow

```bash
# 1. Create a feature branch
git checkout -b feature/awesome-new-feature

# 2. Make your changes
# ... code code code ...

# 3. Format and lint
./gradlew ktlintFormat

# 4. Run tests
./gradlew test

# 5. Commit with meaningful message
git commit -m "feat: add awesome new feature"

# 6. Push and create PR
git push origin feature/awesome-new-feature
```

**For detailed development workflows, see [SKILL.md](SKILL.md)**

---

## 🔢 Versioning

Format: `[major].[minor].[build]`

Example: `3.72.123`
- **3** = Major version
- **72** = Minor version
- **123** = Build number

---

## 📚 Additional Resources

- **[AGENTS.md](AGENTS.md)** — AI-friendly architecture reference
- **[SKILL.md](SKILL.md)** — Development workflows and operational guides
- **[docs/](docs/)** — Comprehensive documentation
  - [docs/architecture/](docs/architecture/) — Architecture deep-dives
  - [docs/features/](docs/features/) — Feature-specific documentation
  - [docs/guides/](docs/guides/) — How-to guides
  - [docs/workflows/](docs/workflows/) — Process documentation
- [Android Developer Docs](https://developer.android.com/)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Room Database](https://developer.android.com/training/data-storage/room)

---

## 🆘 Getting Help

- 💬 Check existing issues
- 📧 Reach out to the team
- 📝 Read the documentation
- 🔍 Search the codebase for examples

---

## 📄 License

Copyright (c) 2021 Windscribe Limited

All rights reserved. This project is proprietary software developed by Windscribe Limited.

---

<div align="center">

**Happy Coding! 🚀**

*Made with ❤️ and lots of ☕*

[⬆ Back to Top](#️-windscribe-vpn-for-android)

</div>