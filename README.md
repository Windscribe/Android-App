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
| **Language** | Kotlin (primary), Java (legacy) |
| **UI** | Jetpack Compose (Mobile), XML (TV) |
| **Async** | Coroutines + Kotlin Flows |
| **DI** | Dagger 2 |
| **Database** | Room |
| **Networking** | wsnet (custom library) |
| **Background Tasks** | WorkManager |

### Architecture Pattern

We follow **MVP (Model-View-Presenter)** with a twist:

```
UI Layer (Activity/Fragment)
    ↓
Presenter (Business Logic)
    ↓
Repository (Data Management)
    ↓
Data Sources (API/Database)
```

### Data Flow

```
App → ApiCallManager → wsnet → API Endpoints
                              ↓
                         Response
                              ↓
                        Repository
                              ↓
                           ViewModel
                              ↓
                             UI
```

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

### Java (Legacy) ☕
Following [grandcentrix-AndroidCodeStyle](https://github.com/GCX-HCI/grandcentrix-AndroidCodeStyle)

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
  - We're migrating from Java to Kotlin
  - New code MUST be in Kotlin
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

### Common Development Tasks

#### Adding a New VPN Feature
1. Update `base` module for core functionality
2. Modify `mobile`/`tv` UI as needed
3. Add appropriate tests
4. Update database schema if required

#### Modifying UI
- **Mobile**: Update Compose components
- **TV**: Update XML layouts with data binding
- Use existing design patterns

#### API Changes
- Update `wsnet` integration (no direct Retrofit!)
- Add data models
- Update Room entities if needed

---

## 📋 Project Statistics

### Code Distribution
- **Base Module**: 65 Java files (mostly data models)
- **Mobile Module**: 5 Java files (billing interfaces)
- **TV Module**: 0 Java files (100% Kotlin! 🎉)
- **Total Kotlin Files**: Majority of codebase
- **Protocols Supported**: 6
- **Countries Supported**: Global coverage

### Module Details
| Module | Purpose | Primary Language |
|--------|---------|------------------|
| base | Core logic | Kotlin + Java |
| mobile | Phone/Tablet UI | Kotlin (Compose) |
| tv | Android TV UI | Kotlin |
| openvpn | Protocol impl | Native C + Kotlin |
| strongswan | IKEv2 impl | Native + Kotlin |
| wgtunnel | WireGuard, WSTunnel, Stunnel & ControlD | Go → Kotlin |

---

## 🔢 Versioning

Format: `[major].[minor].[build]`

Example: `3.72.123`
- **3** = Major version
- **72** = Minor version
- **123** = Build number

---

## 🐛 Debugging Tips

### Common Issues

**Build fails with NDK errors?**
```bash
# Verify NDK installation
echo $ANDROID_NDK_HOME

# Clean and rebuild
./gradlew clean
./gradlew assembleDebug
```

**App crashes on VPN connection?**
```bash
# Check logs
adb logcat -s "vpn" -v time

# Look for protocol-specific logs
adb logcat | grep -i wireguard
```

**Database migration issues?**
```bash
# Clear app data and reinstall
adb uninstall com.windscribe.vpn
./gradlew :mobile:assembleGoogleDebug
adb install -r mobile/build/outputs/apk/google/debug/mobile-google-debug.apk
```

---

## 🧪 Testing

### Running Tests
```bash
# All tests
./gradlew test

# Specific module
./gradlew :base:test

# Instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest

# Test with coverage
./gradlew testDebugUnitTestCoverage
```

### Writing Tests
- Unit tests go in `src/test/`
- Instrumented tests in `src/androidTest/`
- Use MockK for mocking
- Follow AAA pattern (Arrange, Act, Assert)

---

## 📚 Additional Resources

- [Android Developer Docs](https://developer.android.com/)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Room Database](https://developer.android.com/training/data-storage/room)

---

## 🎓 Learning Path

New to the codebase? Start here:

1. 📖 Read `CLAUDE.md` for detailed architecture
2. 🔍 Explore `base/api/` to understand networking
3. 🎨 Check `mobile/` for Compose UI examples
4. 🔌 Review protocol modules for VPN logic
5. 🧪 Read existing tests to understand patterns

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

## 🙏 Acknowledgments

Built with ❤️ by the Windscribe team

Special thanks to:
- The Android Open Source Project
- StrongSwan developers
- WireGuard team
- All our contributors!

---

<div align="center">

**Happy Coding! 🚀**

*Made with ❤️ and lots of ☕*

[⬆ Back to Top](#️-windscribe-vpn-for-android)

</div>