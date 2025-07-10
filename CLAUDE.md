# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## App Overview

**Windscribe VPN** is a top-notch VPN application developed by Windscribe Limited, offering comprehensive privacy and security features.

### Key Features
- **Authentication**: SSO login, captcha support, login/signup, account management
- **Email Verification**: Free data allocation upon email confirmation
- **VPN Protocols (6 total)**:
  - OpenVPN UDP
  - OpenVPN TCP
  - IKEv2 (StrongSwan implementation)
  - Stealth Protocol (OpenVPN TCP wrapper)
  - WSTunnel (OpenVPN TCP WebSocket wrapper)
  - WireGuard
- **Per-Network Configuration**: Choose specific protocols and ports for different networks
- **Network Detection**: Requires location permissions (foreground/background) to access network SSID
- **Advanced Features**:
  - Split tunneling
  - App decoy traffic mode
  - Custom sounds
  - Custom wallpapers
  - Custom names for server locations
  - Custom configs (import your own WireGuard or OpenVPN configurations)
  - Location favorites
  - Newsfeed notifications with promos and news
  - IP address display (connected/disconnected state on home screen)
  - R.O.B.E.R.T DNS filtering with customizable filter toggles
  - Static IP addresses (separate section on home screen, purchasable with pro plans)
  

## Build Commands

### Building the App
```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew bundleGoogleRelease    # Build Google Play AAB
./gradlew bundleFdroidRelease    # Build F-Droid AAB
```

### Testing
```bash
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumented tests
./gradlew testDebug             # Run debug unit tests
```

### Code Quality
```bash
./gradlew ktlintCheck           # Check Kotlin code style
./gradlew ktlintFormat          # Format Kotlin code
./gradlew dependencyCheckAnalyze # Security dependency analysis
```

### Cleaning
```bash
./gradlew clean                 # Clean build artifacts
```

## Project Architecture

### Module Structure

#### Core Modules
- **base/**: Core functionality hub - exposes managers/repositories, handles state management
  - **api/**: Handles all API login and wsnet library interactions
  - **apppreference/**: Manages key-value pair preferences and settings
  - **backend/**: Handles all VPN functionality and communicates with VPN protocol modules
  - **localdatabase/**: Manages structured data using Room (server lists, favorites, etc.)
  - **state/**: Handles universal state management (current network info, etc.)
  - **repository/**: Communicates with both API and database, exposes data to other components
  - **services/**: Android services implementation
  - **di/**: Dependency injection setup with Dagger components
  - **autoconnection/**: Handles auto-connection mode with protocol fallback and error reporting
- **mobile/**: Phone/tablet UI (99% Jetpack Compose)
- **tv/**: Android TV UI (XML-based layouts)
- **common/**: Tunnel wrapper + DNS traffic separation for custom DNS options

#### VPN Protocol Modules
- **openvpn/**: OpenVPN protocol implementation (built from source)
- **strongswan/**: IKEv2/IPSec implementation with prebuilt binaries
  - **strongswan-src/**: Submodule with full source (for custom builds if needed)
- **wgtunnel/**: Go projects compilation module containing:
  - WireGuard tunnel implementation
  - ctrld CLI for custom DNS (DoH/DoT)
  - Stealth Protocol (OpenVPN TCP wrapped in custom tunneling)
  - WebSocket Protocol (OpenVPN TCP wrapped in WebSocket traffic)

#### Supporting Modules
- **wsnet/**: In-house networking library (.aar) - handles ALL API communication
- **test/**: Shared test utilities and mocks
- **fastlane/**: CI/CD automation scripts
- **config/**: Build and signing properties (loaded from secret vault during CI/CD)
- **tools/**: Build scripts (e.g., native StrongSwan compilation)

### Application Classes
- **Windscribe** (base): Main application class with dependency injection
- **PhoneApplication** (mobile): Mobile-specific implementation
- **TVApplication** (tv): TV-specific implementation

### Key Technologies
- **Dependency Injection**: Dagger 2 for component management
- **Database**: Room for local data persistence
- **Networking**: App → ApiCallManager → wsnet library → API endpoints
- **VPN Protocols**: OpenVPN, WireGuard, IKEv2/IPSec
- **UI**: Mix of traditional Android Views and Jetpack Compose
- **Background Processing**: WorkManager for scheduled tasks
- **Languages**: Kotlin (preferred) and Java (legacy)

### Build Variants
- **google**: Google Play Store version with Firebase and billing
- **fdroid**: F-Droid version without proprietary dependencies

#### Build Flavor Differences
- **Google Play Store (google flavor)**: 
  - Includes payment processing via Google Play Billing
  - App review prompts using Google Play In-App Review API
  - Push notifications via Firebase Cloud Messaging
  - Full feature set with Google APIs integration

- **F-Droid (fdroid flavor)**:
  - Missing payment processing (no Google Play Billing)
  - No app review prompts (no Google Play In-App Review)
  - No push notifications (no Firebase dependency)
  - Open-source friendly build without proprietary Google dependencies

## Development Workflow

### Code Style
- Use ktlint with default rules for Kotlin code
- Follow grandcentrix-AndroidCodeStyle for Java code
- Prefer Kotlin over Java for new code
- Use coroutines and Kotlin flows for async operations

### Testing Strategy
- Unit tests for business logic
- Instrumented tests for Android components
- Mock test data available in `test/` module
- Test runner: `com.windscribe.vpn.CustomRunner`

### Key Components to Understand
1. **VPN Management**: Multi-protocol VPN controller in base module
2. **Connection State**: Centralized connection state management
3. **Server Selection**: Location and server selection logic
4. **User Authentication**: Account management and authentication
5. **Settings**: App preferences and configuration
6. **Auto-Connection**: Intelligent connection management with protocol fallback
   - Automatically tries different protocols if one fails
   - Provides debug log dialog and support contact when all protocols fail
7. **DNS Routing**: 
   - Default: DNS queries → VPN's DNS resolver
   - Custom DNS enabled: DNS queries → custom DoH/DoT resolver via ctrld
   - Common module handles traffic separation

### Architecture Principles
- **Clear Separation**: wsnet (API only) | Protocol modules (VPN logic) | base (orchestration) | UI modules (presentation)
- **Protocol Independence**: Each VPN protocol handles its own connection logic and encryption
- **Centralized State**: Base module provides unified interface for all VPN functionality
- **Modular Compilation**: Go projects compiled into wgtunnel, native binaries prebuilt for StrongSwan

## Common Development Tasks

### Adding New VPN Features
1. Modify base module for core functionality
2. Update mobile/TV UIs as needed
3. Add appropriate tests
4. Update database schema if needed

### UI Changes
- Mobile: Update fragments and activities in mobile module
- TV: Update leanback components in tv module
- Use existing design patterns and components

### Network/API Changes
- Update wsnet library integration (API calls go through wsnet, not direct Retrofit)
- Add corresponding data models
- Update database entities if needed

### Adding Dependencies
- Add to base module build.gradle for shared dependencies
- Use appropriate product flavors for platform-specific dependencies
- Update dependency check configuration if needed

## Security Considerations
- VPN credentials and certificates are stored securely
- Network security config varies by build type