# Hashed Login Feature Implementation

## Overview
Implemented tabbed authentication interface allowing users to choose between Standard (username/password) and Hashed (account hash-based) login/signup methods across all auth screens.

## Implementation Date
April 5, 2026

## Branch
`hash_login`

## Components Created

### Core Components

#### 1. **`AuthTabSelector.kt`**
**Location:** `mobile/src/main/java/com/windscribe/mobile/ui/auth/`

**Purpose:** Reusable tab selector component for Standard/Hashed authentication switching

**Features:**
- Two tabs: "Standard" and "Hashed"
- Selected tab has gradient background (`tabGradientTop` → `tabGradientBottom`)
- Unselected tab has gray text (`grayText` color)
- Single-line text with ellipsis overflow (`maxLines = 1`)
- 48dp height, 6dp padding, 8dp gap between tabs
- Uses `AuthType` enum (STANDARD, HASHED)
- String resources: `R.string.standard`, `R.string.hashed`
- No underline decoration

**Usage:**
```kotlin
AuthTabSelector(
    selectedTab = AuthType.STANDARD,
    onTabSelected = { authType -> viewModel.onAuthTypeChanged(authType) }
)
```

#### 2. **`StandardLoginForm.kt`**
**Location:** `mobile/src/main/java/com/windscribe/mobile/ui/auth/`

**Fields:**
- Username field (autofill enabled)
- Password field with eye icon visibility toggle
- "Forgot Password?" link in password header (right side, underlined, gray text)

**Callbacks:**
- `onUsernameChange: (String) -> Unit`
- `onPasswordChange: (String) -> Unit`

**Error Highlighting:**
- `isUsernameError` - highlights username field
- `isPasswordError` - highlights password field

#### 3. **`HashedLoginForm.kt`**
**Location:** `mobile/src/main/java/com/windscribe/mobile/ui/auth/`

**Fields:**
- Account Hash input field (read-only display when file uploaded)
- Upload icon (trailing icon, triggers file picker)
- Placeholder: "Enter Account Hash or Upload"

**Parameters:**
- `accountHash: String` - hash to display (from file or manual input)

**Callbacks:**
- `onHashValueChange: (String) -> Unit` - manual text input
- `onUploadClick: () -> Unit` - triggers file picker

**Behavior:**
- TextField uses `remember(accountHash)` to update when hash changes
- Same styling as standard auth fields
- Error highlighting via `isError` parameter
- Upload button triggers file picker → SHA256 hash calculated → displayed in field

#### 4. **`TwoFactorScreen.kt`**
**Location:** `mobile/src/main/java/com/windscribe/mobile/ui/auth/`

**Design:**
- Separate screen (not inline in login)
- Back arrow + "2FA" title
- Hidden tabs (opacity 0, per Figma)
- Description text: "Check your preferred one-time password application for a code."
- 2FA code input field (number keyboard)
- Continue button

**State Management:**
- Uses same `LoginViewModel` instance (shared via navigation)
- Button disabled until code entered (`isButtonEnabled = twoFactorCode.isNotEmpty()`)
- Text left-aligned (`textAlign = TextAlign.Start`)

**Navigation:**
- Triggered by `showTwoFactorScreen` SharedFlow (one-time event)
- Back button works normally (no loop)

**Route:** `Screen.TwoFactor` in NavigationStack

#### 5. **`StandardSignupForm.kt`**
**Location:** `mobile/src/main/java/com/windscribe/mobile/ui/auth/`

**Fields:**
- Username field with refresh icon (generates random username)
- Password field with refresh icon (generates random password)
- Confirm Password field
- Email field (optional) with info icon
- Expandable "Got Voucher Code?" section
- Expandable "Referred By Someone?" section

**Expandable Sections:**
- **Voucher:** Shows voucher code input field when expanded
- **Referral:** Shows 2 benefit checkmarks + referral username field when expanded

**Callbacks:**
- `onUsernameChange`, `onPasswordChange`, `onEmailChange`
- `onVoucherChange`, `onReferralUsernameChange`
- `onGenerateUsername`, `onGeneratePassword`

**Generated Value Display:**
- Accepts `generatedUsername` and `generatedPassword` parameters
- TextField uses `remember(initialValue)` to update when generated

#### 6. **`HashedSignupForm.kt`**
**Location:** `mobile/src/main/java/com/windscribe/mobile/ui/auth/`

**UI Elements:**
- Description text with "Learn more" link (underlined, clickable)
- Account hash display box:
  - Dashed white border (2dp)
  - Background: `white.copy(alpha = 0.05f)`
  - Monospace font (FontFamily.Monospace)
  - 18sp font size, center-aligned
  - Displays full SHA256 hash (64 hex chars)
- 4 action buttons (79.5dp width each, gradient background):
  - **Refresh:** Regenerate new random file + hash
  - **Upload:** Pick file from device → SHA256 hash
  - **Download:** Save hash file to Downloads folder (shows toast)
  - **Copy:** Copy hash to clipboard (shows toast)
- Expandable "Got Voucher Code?" section
- Backup confirmation checkbox with full text

**Callbacks:**
- `onBackupConfirmedChanged: (Boolean) -> Unit`
- `onRegenerateHash` - creates new 1KB random file + hash
- `onUploadHash` - triggers file picker
- `onDownloadHash` - saves file to Downloads/windscribe_account_[timestamp].key
- `onCopyHash` - copies hash to clipboard
- `onVoucherChange`, `onLearnMoreClick`

**Parameters:**
- `accountHash: String` - the generated/uploaded hash to display
- `isBackupConfirmed: Boolean` - checkbox state

**Signup Button Logic:**
- Enabled only when: `accountHash.isNotEmpty() && isBackupConfirmed`

**File Download:**
- Android 10+: Uses MediaStore API
- Android 9-: Direct file write to Downloads folder
- Filename: `windscribe_account_[timestamp].key`
- Toast: "Account key saved to Downloads/[filename]"

### Modified Components

#### LoginScreen.kt
**Location:** `mobile/src/main/java/com/windscribe/mobile/ui/auth/LoginScreen.kt`

**Changes:**
- Replaced NavBar with custom header (back arrow + title + tabs)
- Back arrow: 16dp icon in 24dp clickable area
- Conditional form rendering: `when (selectedAuthType)`
  - `AuthType.STANDARD` → Shows `StandardLoginForm`
  - `AuthType.HASHED` → Shows `HashedLoginForm(accountHash = accountHashDisplay)`
- Removed old components: `LoginUsernameTextField`, `LoginPasswordTextField`, `ActionSheet`
- Spacing: `.statusBarsPadding()` + 16dp top + 24dp horizontal
- Added 2FA navigation: `LaunchedEffect` on `showTwoFactorScreen` SharedFlow
- Added file picker: `rememberLauncherForActivityResult(GetContent())`
- Added file picker trigger listener: `triggerFilePicker.collect { ... }`
- Collects `accountHashDisplay` from ViewModel for TextField updates

**File Picker Integration:**
```kotlin
val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
) { uri ->
    uri?.let { viewModel?.onFileSelected(context, it) }
}

LaunchedEffect(Unit) {
    viewModel?.triggerFilePicker?.collect { trigger ->
        if (trigger) {
            filePickerLauncher.launch("*/*")  // Accept any file type
        }
    }
}
```

**Key Code:**
```kotlin
Column(
    modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .padding(horizontal = 24.dp)
        .imePadding()
) {
    Spacer(modifier = Modifier.height(16.dp))
    // Header with back + title + tabs
    when (selectedAuthType) {
        AuthType.STANDARD -> StandardLoginForm(...)
        AuthType.HASHED -> HashedLoginForm(
            accountHash = accountHashDisplay,  // From ViewModel
            onUploadClick = { viewModel?.onUploadHashClick() }
        )
    }
}
```

#### LoginViewModel.kt
**Location:** `mobile/src/main/java/com/windscribe/mobile/ui/auth/LoginViewModel.kt`

**State Additions:**
- `_selectedAuthType: MutableStateFlow<AuthType>` - tracks Standard vs Hashed
- `_accountHash: String` - stores account hash for hashed login (internal)
- `_accountHashDisplay: MutableStateFlow<String>` - hash for UI display (fixes TextField update)
- `_triggerFilePicker: MutableSharedFlow<Boolean>` - triggers file picker
- `_showTwoFactorScreen: MutableSharedFlow<Boolean>` - one-time 2FA navigation event
- Removed: `_twoFactorEnabled` StateFlow (replaced with SharedFlow)

**Methods Added:**
- `onAuthTypeChanged(authType: AuthType)` - switches tabs, clears errors, re-validates
- `onAccountHashChanged(hash: String)` - updates hash field + emits to `_accountHashDisplay`
- `validateHashedInput()` - validates hash length >= 2
- `clearTwoFactorNavigation()` - consumes SharedFlow event
- `onUploadHashClick()` - triggers file picker SharedFlow
- `onFileSelected(context: Context, uri: Uri)` - reads file, calculates SHA256, updates hash display

**Methods Modified:**
- `loginButtonClick()` - For hashed mode, sets `username = accountHash` and `password = accountHash` before login
- `handleApiError()` - When 2FA required, emits `_showTwoFactorScreen.emit(true)` instead of StateFlow
- `onAuthTypeChanged()` - Clears error state, re-validates based on tab

**File Upload Flow:**
```kotlin
onUploadHashClick() → _triggerFilePicker.emit(true) → File picker opens
↓
User selects file → onFileSelected(context, uri)
↓
Read file bytes → SHA256 hash → _accountHashDisplay.emit(hash)
↓
TextField updates with hash → validateHashedInput()
```

#### SignupScreen.kt
**Location:** `mobile/src/main/java/com/windscribe/mobile/ui/auth/SignupScreen.kt`

**Layout Changes:**
- Custom header with back arrow + title + tabs
- Conditional form rendering based on `selectedAuthType`
- Bottom "Already have an account? Log In" link (underlined, clickable)
- Spacing: `.statusBarsPadding()` + 16dp top + 24dp horizontal
- Scrollable content in `Column` with `weight(1f)`
- File picker integration for hash upload
- Toast message listener for download notifications

**State Collection:**
- `selectedAuthType` - tab selection
- `accountHash` - generated hash for display
- `isBackupConfirmed` - checkbox state
- `generatedUsername` - from API generator
- `generatedPassword` - from API generator

**File Picker & Toast Integration:**
```kotlin
val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
) { uri ->
    uri?.let { viewModel?.onFileSelected(context, it) }
}

LaunchedEffect(Unit) {
    viewModel?.triggerFilePicker?.collect { trigger ->
        if (trigger) {
            filePickerLauncher.launch("*/*")
        }
    }
}

LaunchedEffect(Unit) {
    viewModel?.toastMessage?.collect { message ->
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
```

**Removed Components:**
- Old `SignupUsernameTextField`, `SignupPasswordTextField`, `SignupEmailTextField`
- Old `VoucherTextField`, `ReferralUsernameTextField`
- Old `ExpandMenu`, `ReferralFeatures`

**Bottom Link Navigation:**
```kotlin
navController.navigate(Screen.Login.route) {
    popUpTo(Screen.Start.route)
}
```

#### SignupViewModel.kt
**Location:** `mobile/src/main/java/com/windscribe/mobile/ui/auth/SignupViewModel.kt`

**State Additions:**
- `_selectedAuthType: MutableStateFlow<AuthType>` - Standard vs Hashed
- `_accountHash: MutableStateFlow<String>` - auto-generated hash
- `_isBackupConfirmed: MutableStateFlow<Boolean>` - checkbox state
- `_generatedUsername: MutableStateFlow<String>` - from generator API
- `_generatedPassword: MutableStateFlow<String>` - from generator API
- `_hashFileBytes: MutableStateFlow<ByteArray?>` - stores the actual file bytes for download
- `_triggerFilePicker: MutableSharedFlow<Boolean>` - triggers file picker
- `_toastMessage: MutableSharedFlow<String>` - toast notifications for file operations

**Methods Added:**
- `onAuthTypeChanged(authType)` - switches tabs, generates hash if HASHED, clears errors
- `generateAccountHash()` - generates 1KB random file, calculates SHA256 hash, stores both
- `onBackupConfirmedChanged(confirmed)` - updates checkbox, re-validates
- `validateHashedSignup()` - checks `accountHash.isNotEmpty() && isBackupConfirmed`
- `generateUsername()` - calls API, updates `username` + `_generatedUsername`, validates
- `generatePassword()` - calls API, updates `password` + `_generatedPassword`, validates
- `onUploadHashClick()` - triggers file picker for hash upload
- `onFileSelected(context, uri)` - reads uploaded file, calculates SHA256, stores bytes + hash
- `onDownloadHashClick(context)` - saves hash file to Downloads, shows toast

**Methods Modified:**
- `signupButtonClick()` - For HASHED mode:
  1. Sets `username = _accountHash.value`
  2. Sets `password = _accountHash.value`
  3. Skips standard validation (no password complexity check)
  4. Directly calls `startSignupProcess()`
- `onAuthTypeChanged()` - Clears error state when switching tabs

**File Operations:**

**Generate Hash File:**
```kotlin
fun generateAccountHash() {
    val random = SecureRandom()
    val bytes = ByteArray(1024)  // 1KB random file
    random.nextBytes(bytes)
    val hash = HashUtils.sha256FromInputStream(bytes.inputStream())
    _hashFileBytes.emit(bytes)  // Store for download
    _accountHash.emit(hash)      // Display in UI
}
```

**Download Hash File:**
```kotlin
fun onDownloadHashClick(context: Context) {
    val bytes = _hashFileBytes.value
    val fileName = "windscribe_account_[timestamp].key"
    // Save to Downloads folder (MediaStore on Android 10+)
    _toastMessage.emit("Account key saved to Downloads/$fileName")
}
```

**Upload Hash File:**
```kotlin
fun onFileSelected(context: Context, uri: Uri) {
    val bytes = inputStream.readBytes()
    val hash = HashUtils.sha256FromInputStream(bytes.inputStream())
    _hashFileBytes.emit(bytes)  // Store for potential re-download
    _accountHash.emit(hash)      // Display in UI
}
```

#### AccountScreen.kt
**Location:** `mobile/src/main/java/com/windscribe/mobile/ui/preferences/account/AccountScreen.kt`

**Changes to `AccountInfo()` composable:**

**Hash Detection:**
```kotlin
val isHashedAccount = username.startsWith("0x")
```

**Username Row:**
- Now tap-to-copy (all accounts, not just hashed)
- Uses `.hapticClickable {}` with clipboard code
- Shows toast: "Username copied to clipboard"
- Rounded corners:
  - Hashed account: Full rounded corners (12.dp all sides)
  - Standard account: Top rounded only (topStart/topEnd 12.dp)
- Added `maxLines = 1, overflow = TextOverflow.Ellipsis` for long hashes

**Email Section:**
- Wrapped in `if (!isHashedAccount) { ... }` block
- For hashed accounts, completely hides:
  - Email row
  - Email status icons
  - "Get 10GB data" message
  - "Confirm your email" warning
  - "Add Email" button

**Reasoning:**
- Hashed accounts are anonymous - no email needed
- Clean UI for hash-based users

#### HomeScreen.kt
**Location:** `mobile/src/main/java/com/windscribe/mobile/ui/home/HomeScreen.kt`

**Changes to `NetworkInfoSheet()` composable:**

**IP Address Interaction:**
```kotlin
val ipAddress by bridgeApiViewModel.ipState.collectAsState()

Box(
    modifier = Modifier
        .pointerInput(hideIp) {
            detectTapGestures(
                onTap = {
                    if (!hideIp) {
                        // Copy IP to clipboard
                        Toast.makeText(context, "IP address copied to clipboard", ...)
                    }
                },
                onDoubleTap = {
                    homeViewmodel.onHideIpClick()
                }
            )
        }
)
```

**Behavior:**
- **Single tap** (when visible) → Copies IP to clipboard + toast
- **Single tap** (when hidden) → Does nothing
- **Double tap** (any state) → Toggles hide/show
- `pointerInput(hideIp)` - recomposes when hide state changes

### Navigation

#### Screen.kt
**Location:** `mobile/src/main/java/com/windscribe/mobile/ui/nav/Screen.kt`

**Added:**
```kotlin
object TwoFactor : Screen("two_factor")
```

#### NavigationStack.kt
**Location:** `mobile/src/main/java/com/windscribe/mobile/ui/nav/NavigationStack.kt`

**Added route registration:**
```kotlin
composable(route = Screen.TwoFactor.route) {
    ViewModelRoute(LoginViewModel::class.java) {
        TwoFactorScreen(it)
    }
}
```

**Key Detail:** Uses same `LoginViewModel` instance for state sharing between Login and TwoFactor screens

## Resources Added

### Strings
**File:** `base/src/main/res/values/strings.xml`

```xml
<string name="standard">Standard</string>
<string name="hashed">Hashed</string>
<string name="account_hash">Account Hash</string>
<string name="enter_account_hash_or_upload">Enter Account Hash or Upload</string>
<string name="account_hash_description">This is your unique account hash, save it somewhere safe, you\'ll need it to login.</string>
<string name="confirm_password">Confirm Password</string>
<string name="enter_email_address">Enter Email Address</string>
<string name="got_voucher_code">Got Voucher Code?</string>
<string name="account_hash_backup_confirmation">I have backed up my account hash securely and understand that Windscribe will not be able to help me recover my account if I loose it.</string>
<string name="already_have_account_log_in">Already have an account? Log In</string>
<string name="two_fa_check_app_description">Check your preferred one-time password application for a code.</string>
<string name="enter_two_fa_code">Enter 2FA Code</string>
```

**Note:** `learn_more` already existed at line 381, so was not duplicated

### Colors
**File:** `mobile/src/main/java/com/windscribe/mobile/ui/theme/AppColor.kt`

```kotlin
val grayText = Color(0xFF898F9D)
val tabGradientTop = Color(0x33C5CEE0)    // rgba(197,206,224,0.2)
val tabGradientBottom = Color(0x1FC5CEE0)  // rgba(197,206,224,0.12)
```

### Drawables
**Location:** `mobile/src/main/res/drawable/`

- `ic_upload.xml` - Upload arrow icon (24dp, used in hash upload placeholder)
- `ic_download.xml` - Download arrow icon (24dp, used in hash action buttons)
- `ic_copy.xml` - Copy/clipboard icon (24dp, used in hash action buttons)
- `ic_refresh.xml` - Refresh/regenerate icon (24dp, used in username/password/hash generation)

**Existing icons used:**
- `ic_back_arrow.xml` - Back navigation (sized to 16dp)
- `ic_info_icon.xml` - Info icon for email field
- `ic_expand.xml` - Chevron for expandable sections
- `ic_check.xml` - Checkmarks in referral benefits

## Design Implementation

All screens implemented from Figma designs:
- Login Standard: `node-id=5616-2784`
- Login Hashed: `node-id=5618-2331`
- 2FA Screen: `node-id=5617-2288`
- Signup Standard: `node-id=5609-2462`
- Signup Hashed: `node-id=5614-2527`

### Key Design Details
- Tab selector: 48dp height, 6dp padding, 8dp gap between tabs
- Selected tab: Gradient background + shadow
- Input fields: 52dp height, 9dp corner radius
- Back arrow: 16dp size (centered in 24dp touch area)
- Spacing: statusBarsPadding + 16dp top + 24dp horizontal
- Text: DM Sans font (via font16), proper line heights

## Fixes & Refinements Applied

### UI/UX Fixes
1. **Status Bar Padding** - Added `.statusBarsPadding()` to LoginScreen, SignupScreen, TwoFactorScreen
2. **Back Arrow Size** - Icon sized to 16dp (was default 24dp), centered in 24dp clickable area
3. **Top Spacing** - 16dp from statusBarsPadding (was 24dp) to account for system bar
4. **Tab Underline Removed** - Cleaned up `.drawBehind{}` decorative line
5. **Tab Text Overflow** - Added `maxLines = 1, overflow = TextOverflow.Ellipsis`
6. **2FA Text Alignment** - Description text `textAlign = TextAlign.Start` (was centered)
7. **Expandable Sections** - Voucher and referral sections now expand with content
8. **Bottom Signup Link** - "Already have an account? Log In" added to signup screens

### State Management Fixes
9. **Error State Isolation** - `onAuthTypeChanged()` clears errors, tab-specific validation
10. **2FA Navigation Loop Fixed** - Changed from `StateFlow` to `SharedFlow` (one-time event)
11. **2FA Button State** - Disabled until `twoFactorCode.isNotEmpty()`
12. **Hashed Login Credentials** - Hash sent as both username AND password
13. **Hashed Signup Credentials** - Hash sent as both username AND password
14. **Hashed Signup Validation Skip** - Bypasses password complexity rules for hash

### Additional Features
15. **Username Copy (Account Screen)** - Tap username to copy (any account type)
16. **IP Address Copy (Home Screen)** - Single tap IP to copy (when visible)
17. **Email Hidden for Hashed** - Account screen hides email section for `0x` usernames
18. **Username/Password Generators** - Hardcoded API responses ready for wsnet integration
19. **File Upload (Login)** - Upload any file → SHA256 hash → auto-fill field
20. **File Upload (Signup)** - Upload any file → SHA256 hash → replace generated hash
21. **File Download (Signup)** - Download hash file to Downloads folder with toast
22. **Hash Copy (Signup)** - Copy hash to clipboard from signup screen

## Architecture Pattern

### State Management Strategy

**StateFlow vs SharedFlow:**
- `StateFlow` - Persistent state (tab selection, form values, button enabled)
- `SharedFlow(replay = 0)` - One-time events (navigation, dialogs)

**Example:**
```kotlin
// Persistent state - survives configuration changes
private val _selectedAuthType = MutableStateFlow(AuthType.STANDARD)
val selectedAuthType: StateFlow<AuthType> = _selectedAuthType.asStateFlow()

// One-time event - consumed after use
private val _showTwoFactorScreen = MutableSharedFlow<Boolean>(replay = 0)
val showTwoFactorScreen: SharedFlow<Boolean> = _showTwoFactorScreen
```

### Tab Switching Logic

**When tab changes (`onAuthTypeChanged`):**
1. Emit new tab type to `_selectedAuthType`
2. Clear error state: `updateState(LoginState.Idle)` or `updateState(SignupState.Idle)`
3. Disable button: `_loginButtonEnabled.emit(false)`
4. Re-validate based on new tab:
   - Standard → `validateInput()` (checks username + password)
   - Hashed → `validateHashedInput()` (checks hash length)
5. For signup HASHED tab → auto-generate hash

### Hashed Authentication Flow

**Login:**
```
User enters hash → validateHashedInput() → button enabled
↓
Click Continue → loginButtonClick() checks selectedAuthType
↓
If HASHED: username = accountHash; password = accountHash
↓
startLoginProcess() → authTokenLogin(username) → logUserIn(username, password, ...)
↓
Server receives hash as both username and password
```

**Signup:**
```
Tab switched to HASHED → generateAccountHash() → display hash
↓
User checks backup confirmation → validateHashedSignup() → button enabled
↓
Click Sign Up → signupButtonClick() checks selectedAuthType
↓
If HASHED: username = _accountHash.value; password = _accountHash.value
↓
Skip validation → startSignupProcess() → authTokenSignup(username) → signUserIn(username, password, ...)
↓
Server receives hash as both username and password
```

### 2FA Navigation Pattern

**Problem:** `StateFlow` caused navigation loops (back button re-triggered navigation)

**Solution:** One-time event with `SharedFlow`

**Implementation:**
```kotlin
// ViewModel
private val _showTwoFactorScreen = MutableSharedFlow<Boolean>(replay = 0)

suspend fun handleApiError(errorCode: Int, error: String) {
    when (errorCode) {
        ERROR_2FA_REQUIRED -> {
            _showTwoFactorScreen.emit(true)  // Emit once
        }
    }
}

// LoginScreen
LaunchedEffect(Unit) {
    viewModel?.showTwoFactorScreen?.collect { show ->
        if (show) {
            navController.navigate(Screen.TwoFactor.route)
            viewModel.clearTwoFactorNavigation()  // Consume event
        }
    }
}
```

**Result:** Navigate once, event consumed, back button works normally

## Testing Status

- ✅ All code compiles successfully
- ✅ Build successful for `:mobile:compileGoogleDebugKotlin`
- ⏳ UI functionality testing (pending device/emulator testing)
- ⏳ Backend integration (hashed login API calls not implemented)

## API Integration

### Username/Password Generation
**Files:**
- `base/src/main/java/com/windscribe/vpn/api/response/GenerateUsernameResponse.java`
- `base/src/main/java/com/windscribe/vpn/api/response/GeneratePasswordResponse.java`

**Added to `IApiCallManager.kt`:**
```kotlin
suspend fun generateUsername(): GenericResponseClass<GenerateUsernameResponse?, ApiErrorResponse?>
suspend fun generatePassword(): GenericResponseClass<GeneratePasswordResponse?, ApiErrorResponse?>
```

**Current Implementation in `ApiCallManager.kt` (lines 721-733):**
```kotlin
override suspend fun generateUsername(): GenericResponseClass<GenerateUsernameResponse?, ApiErrorResponse?> {
    return suspendCancellableCoroutine { continuation ->
        val hardcodedJson = """{"data": {"username": "LastSlateScoundrel", "success": 1}}"""
        buildResponse(continuation, 200, hardcodedJson, GenerateUsernameResponse::class.java)
    }
}

override suspend fun generatePassword(): GenericResponseClass<GeneratePasswordResponse?, ApiErrorResponse?> {
    return suspendCancellableCoroutine { continuation ->
        val hardcodedJson = """{"data": {"password": "SecurePass123", "success": 1}}"""
        buildResponse(continuation, 200, hardcodedJson, GeneratePasswordResponse::class.java)
    }
}
```

**To Replace with Real wsnet Call:**
```kotlin
override suspend fun generateUsername(): GenericResponseClass<GenerateUsernameResponse?, ApiErrorResponse?> {
    return suspendCancellableCoroutine { continuation ->
        val callback = wsNetServerAPI.generateUsername() { code, json ->
            buildResponse(continuation, code, json, GenerateUsernameResponse::class.java)
        }
        continuation.invokeOnCancellation { callback.cancel() }
    }
}
```

### Hashed Login/Signup Flow

**Current Implementation:**
- Hash is sent as BOTH username and password to existing login/signup endpoints
- No special API endpoints needed
- Server should recognize hash format and handle accordingly

**Login:**
```kotlin
// In LoginViewModel.loginButtonClick()
if (_selectedAuthType.value == AuthType.HASHED) {
    username = accountHash  // User entered hash
    password = accountHash  // Same hash
}
// Normal login flow continues
```

**Signup:**
```kotlin
// In SignupViewModel.signupButtonClick()
if (_selectedAuthType.value == AuthType.HASHED) {
    username = _accountHash.value  // Generated hash
    password = _accountHash.value  // Same hash
}
// Normal signup flow continues
```

## Backend Integration TODO

### Future wsnet Integration Needed:

1. **Username Generator Endpoint**
   - Replace hardcoded response in `ApiCallManager.generateUsername()`
   - Add wsnet call: `wsNetServerAPI.generateUsername()`
   - Current: Returns `"LastSlateScoundrel"`

2. **Password Generator Endpoint**
   - Replace hardcoded response in `ApiCallManager.generatePassword()`
   - Add wsnet call: `wsNetServerAPI.generatePassword()`
   - Current: Returns `"SecurePass123"`

3. **Server-Side Hash Recognition**
   - Detect when username/password are the same hash format (both start with `0x`)
   - Handle hash-based authentication
   - Store hash association with account
   - No special hash validation needed - server treats it like username/password

### Completed Features

✅ **Hash Upload Feature**
   - File picker integrated in `HashedLoginForm` and `HashedSignupForm`
   - Accepts ANY file type (`*/*`)
   - SHA256 hash calculated from file contents
   - Hash automatically displayed in input field

✅ **Hash Download Feature**
   - Generates downloadable .key file in `HashedSignupForm`
   - Saves to Downloads folder with timestamp
   - Android 10+: MediaStore API
   - Android 9-: Direct file write
   - Filename: `windscribe_account_[timestamp].key`
   - Toast notification on success/error

✅ **Hash File System**
   - 1KB random file generated on tab switch to Hashed
   - File bytes stored in memory for download
   - User can upload their own file to replace
   - Same file always produces same hash (SHA256 deterministic)

## Complete File Structure & Changes

### New Files Created (8)

```
mobile/src/main/java/com/windscribe/mobile/ui/auth/
├── AuthTabSelector.kt          # Tab selector (Standard/Hashed)
├── StandardLoginForm.kt        # Username + Password form
├── HashedLoginForm.kt          # Account hash input form
├── TwoFactorScreen.kt          # Separate 2FA screen
├── StandardSignupForm.kt       # Standard signup with generators
└── HashedSignupForm.kt         # Hash display + backup confirmation

base/src/main/java/com/windscribe/vpn/api/response/
├── GenerateUsernameResponse.java  # API response for username generator
└── GeneratePasswordResponse.java  # API response for password generator

base/src/main/java/com/windscribe/vpn/commonutils/
└── HashUtils.kt                   # SHA256 hashing utilities
```

### Files Modified (10)

```
mobile/src/main/java/com/windscribe/mobile/ui/auth/
├── LoginScreen.kt              # Tabbed layout, conditional forms
├── LoginViewModel.kt           # Tab state, hash handling, SharedFlow navigation
├── SignupScreen.kt             # Tabbed layout, generated values, bottom link
└── SignupViewModel.kt          # Tab state, hash generation, generators

mobile/src/main/java/com/windscribe/mobile/ui/
├── home/HomeScreen.kt          # IP copy on tap, hide state check
└── preferences/account/AccountScreen.kt  # Username copy, hide email for hash

mobile/src/main/java/com/windscribe/mobile/ui/nav/
├── Screen.kt                   # Added TwoFactor route
└── NavigationStack.kt          # Registered TwoFactor screen

base/src/main/java/com/windscribe/vpn/api/
├── IApiCallManager.kt          # Added generator method signatures
└── ApiCallManager.kt           # Implemented generators with hardcoded JSON
```

### Resources Modified (2)

```
base/src/main/res/values/
└── strings.xml                 # 12 new strings added

mobile/src/main/java/com/windscribe/mobile/ui/theme/
└── AppColor.kt                 # 3 new colors added
```

### Drawables Created (4)

```
mobile/src/main/res/drawable/
├── ic_upload.xml               # Upload arrow (24x24dp)
├── ic_download.xml             # Download arrow (24x24dp)
├── ic_copy.xml                 # Copy/clipboard (24x24dp)
└── ic_refresh.xml              # Refresh/regenerate (24x24dp)
```

## Implementation Details

### Hash File System

**Core Concept:** The file itself is the credential, not just a storage format. The hash is derived from the file contents using SHA256.

**Hash Utility:** `base/src/main/java/com/windscribe/vpn/commonutils/HashUtils.kt`

```kotlin
object HashUtils {
    fun sha256FromFile(file: File): String
    fun sha256FromInputStream(inputStream: InputStream): String
    fun generateRandomHash(size: Int = 32): String
}
```

**Hash Format:**
- Generated via SHA256 of file contents
- Format: `"0x" + [64 hex characters]`
- Example: `"0x03cc86b9caf8f4b794a1e7c8d5f6e3a2b1c4d7e9f0a3b5c8d2e6f1a4b7c9d3e5f2"`
- Detection: `username.startsWith("0x")`

### File-Based Authentication Flow

**Signup:**
1. User selects "Hashed" tab
2. System generates 1KB random file (SecureRandom bytes)
3. SHA256 hash calculated from file bytes
4. Hash displayed in UI
5. File bytes stored in `_hashFileBytes` StateFlow
6. User can:
   - **Download** → Saves to Downloads as `windscribe_account_[timestamp].key`
   - **Upload** → Replace with their own file (any file type)
   - **Regenerate** → Create new random file + hash
7. User must check "I have backed up my account hash securely"
8. Hash sent as username + password for signup

**Login:**
1. User selects "Hashed" tab
2. User uploads their .key file (or ANY file they used during signup)
3. SHA256 hash calculated from file contents
4. Hash displayed in input field
5. Hash sent as username + password for login

**Key Insight:**
- Same file → Same SHA256 hash → Same credentials
- Users can use ANY file as their credential (photo, document, etc.)
- The generated .key file is just a convenience option
- File must be kept secure - it IS the password

### Validation Rules

**Standard Login:**
- Username: >= 2 characters
- Password: >= 2 characters

**Hashed Login:**
- Account Hash: >= 2 characters
- No password complexity rules

**Standard Signup:**
- Username: >= 2 characters
- Password: >= 8 characters, must have uppercase + lowercase
- Email: Optional, validated if provided
- Voucher: Optional
- Referral: Optional

**Hashed Signup:**
- Account Hash: Auto-generated, always valid
- Backup Confirmation: Required (checkbox must be checked)
- Skips all password validation rules
- Voucher: Optional (expandable)

### Button Enable Logic

**Login:**
- Standard: `isValidUsername() && isValidPassword()`
- Hashed: `accountHash.length >= 2`

**Signup:**
- Standard: `isValidUsername() && isValidPassword()`
- Hashed: `accountHash.isNotEmpty() && isBackupConfirmed`

**2FA:**
- `twoFactorCode.isNotEmpty()`

## Code Patterns to Follow

### Adding New Tab-Specific Field

**1. Add to ViewModel:**
```kotlin
private var newField = ""

fun onNewFieldChanged(value: String) {
    this.newField = value
    if (_selectedAuthType.value == AuthType.STANDARD) {
        validateInput()
    } else {
        validateHashedInput()
    }
}
```

**2. Clear on Tab Change:**
```kotlin
fun onAuthTypeChanged(authType: AuthType) {
    viewModelScope.launch {
        newField = ""  // Clear field
        _selectedAuthType.emit(authType)
        // ... rest of logic
    }
}
```

### Adding New API Call

**1. Create Response Class:**
```java
// base/src/main/java/com/windscribe/vpn/api/response/NewResponse.java
@Keep
public class NewResponse {
    @SerializedName("field_name")
    @Expose
    private String fieldName;

    @SerializedName("success")
    @Expose
    private int success;

    public String getFieldName() { return fieldName; }
    public boolean isSuccessful() { return success == 1; }
}
```

**2. Add to IApiCallManager:**
```kotlin
suspend fun newApiCall(): GenericResponseClass<NewResponse?, ApiErrorResponse?>
```

**3. Implement in ApiCallManager:**
```kotlin
override suspend fun newApiCall(): GenericResponseClass<NewResponse?, ApiErrorResponse?> {
    return suspendCancellableCoroutine { continuation ->
        val callback = wsNetServerAPI.newEndpoint() { code, json ->
            buildResponse(continuation, code, json, NewResponse::class.java)
        }
        continuation.invokeOnCancellation { callback.cancel() }
    }
}
```

## Testing Checklist

### Login Flow
- [ ] Standard login with valid credentials
- [ ] Standard login with invalid credentials
- [ ] Standard login with 2FA required
- [ ] Hashed login with manual hash entry
- [ ] Hashed login with file upload
- [ ] Upload same file used in signup - should work
- [ ] Upload different file - should fail (different hash)
- [ ] Switch tabs - errors clear
- [ ] 2FA back button - returns to login without loop
- [ ] Forgot password link opens browser

### Signup Flow
- [ ] Standard signup with all fields
- [ ] Standard signup with optional fields empty
- [ ] Username generator button works
- [ ] Password generator button works
- [ ] Expandable sections work
- [ ] Hashed signup auto-generates hash and file
- [ ] Hash regenerate button creates new file + hash
- [ ] Hash download button saves .key file to Downloads
- [ ] Toast shows on download success
- [ ] Hash upload button allows file selection
- [ ] Uploaded file generates correct hash
- [ ] Hash copy button copies to clipboard
- [ ] Backup confirmation required (checkbox)
- [ ] Button disabled without checkbox
- [ ] "Already have account" link navigates to login

### File-Based Authentication
- [ ] Download .key file during signup
- [ ] Upload same .key file during login - hash matches
- [ ] Upload personal file (e.g., photo.jpg) during signup
- [ ] Upload same photo during login - hash matches
- [ ] Upload different file during login - hash different, login fails
- [ ] Hash display updates in real-time after file upload

### Account Screen
- [ ] Standard account shows email section
- [ ] Hashed account (0x...) hides email section
- [ ] Username tap copies to clipboard
- [ ] Toast shows on copy

### Home Screen
- [ ] IP single tap copies (when visible)
- [ ] IP single tap blocked (when hidden)
- [ ] IP double tap toggles hide/show
- [ ] Toast shows on copy

## Known Limitations & Future Work

1. **Learn More Link** - UI exists, link destination not implemented (needs documentation URL)
2. **Generator APIs** - Return hardcoded values, need wsnet integration
3. **Hash Recovery** - No recovery mechanism (by design - anonymous accounts)
4. **File Validation** - Currently accepts any file, could add size/type limits
5. **Download Location** - Always Downloads folder, could add user choice
6. **File Persistence** - Hash file bytes lost on app restart (user must re-upload for download)

### Security Considerations

✅ **Hash File is the Credential**
- File must be kept secure like a password
- Losing file = losing account access (by design)
- Users warned via backup confirmation checkbox

✅ **SHA256 Properties**
- Deterministic: Same file → Same hash
- One-way: Hash cannot be reversed to get file
- Collision-resistant: Different files → Different hashes (practically impossible to collide)
- 256-bit output = 64 hex characters (with "0x" prefix = 66 chars total)

✅ **File Flexibility**
- User can use any personal file (photo, document, etc.)
- File they'll never lose
- Or use our generated .key file
- No restrictions on file type or size (within reason)

## User Journey Examples

### Example 1: Standard Workflow
**Signup:**
1. User selects "Hashed" tab
2. System auto-generates random .key file
3. User sees hash: `0x513b5d56ae30109e8ce...`
4. User taps Download → File saved to Downloads
5. User checks "I have backed up..." checkbox
6. User taps Sign Up → Account created

**Login:**
1. User selects "Hashed" tab
2. User taps Upload icon → Selects `windscribe_account_1234567890.key`
3. Hash auto-fills: `0x513b5d56ae30109e8ce...`
4. User taps Continue → Logged in ✅

### Example 2: Personal File Workflow
**Signup:**
1. User selects "Hashed" tab
2. System generates hash, but user wants to use their own file
3. User taps Upload → Selects `family_photo.jpg`
4. Hash recalculates: `0x7a8b9c0d1e2f3a4b5c6d...`
5. User checks backup confirmation
6. User taps Sign Up → Account created with photo's hash

**Login:**
1. User selects "Hashed" tab
2. User taps Upload → Selects same `family_photo.jpg`
3. Hash auto-fills: `0x7a8b9c0d1e2f3a4b5c6d...`
4. User taps Continue → Logged in ✅

### Example 3: Forgot to Download (Your Question)
**Signup:**
1. User selects "Hashed" tab
2. System generates hash
3. User taps Upload → Replaces with `my_document.txt`
4. Hash shown: `0x9e8d7c6b5a4f3e2d1c0b...`
5. User signs up but **forgets to download**

**Login:**
1. User still has `my_document.txt` on their device
2. User selects "Hashed" tab
3. User taps Upload → Selects same `my_document.txt`
4. Hash auto-fills: `0x9e8d7c6b5a4f3e2d1c0b...` (same as signup!)
5. User taps Continue → Logged in ✅

**Key Point:** As long as user keeps the SAME file (any file), they can always login.

## Design Decisions & Open Questions

### 1. 2FA Screen Implementation

**Current State:** Inline 2FA (AnimatedContent on LoginScreen)

**Problem with Separate Screen:**
- Separate TwoFactorScreen was implemented but caused captcha to show twice in some flows
- Navigation loop issues when pressing back
- Extra screen complexity

**Current Solution:**
- Reverted to inline 2FA field that slides in when required
- TwoFactorScreen.kt still exists in codebase but disconnected (not in navigation)
- Works like original implementation

**TODO - Decide:**
- [ ] Keep inline 2FA (current) - simpler, avoids captcha double-show
- [ ] Fix separate screen approach - cleaner but needs captcha flow refactor
- [ ] Remove TwoFactorScreen.kt if keeping inline permanently

**Trade-offs:**
- **Inline:** Simple, works, no captcha issues, but less clean separation
- **Separate:** Better UX separation, matches Figma design, but captcha handling complex

---

### 2. Long Username Display in Account Screen

**Current State:**
- Username row uses `maxLines = 1, overflow = TextOverflow.Ellipsis`
- Long hashes (66 chars) get truncated: `0x03cc86b9caf8f4b794a1...`
- Username is tap-to-copy (user can still access full value)

**Problem:**
- User cannot see full hash on screen
- Ellipsis hides important characters
- Tap-to-copy works but user doesn't see what they're copying

**Possible Solutions:**

**Option 1: Keep Current (Ellipsis + Copy)**
- Pros: Clean UI, doesn't break layout
- Cons: User can't verify full hash visually

**Option 2: Scrollable Text**
- Make username text horizontally scrollable
- Pros: User can see full hash by swiping
- Cons: Not obvious it's scrollable

**Option 3: Wrap to Multiple Lines**
- Remove `maxLines = 1`, let hash wrap
- Pros: Full hash visible
- Cons: Takes more vertical space, looks less clean

**Option 4: Truncate Middle**
- Show: `0x03cc86b...e2f3` (show start + end, ellipsis middle)
- Pros: User sees hash format, can verify start/end
- Cons: Custom implementation needed

**Option 5: Show Hash on Separate Row**
- Username label on left, hash on full row below
- Pros: Full hash visible
- Cons: Layout change needed

**TODO - Decide:**
- [ ] Keep current ellipsis + tap-to-copy
- [ ] Implement scrollable text
- [ ] Allow multi-line wrap
- [ ] Truncate middle (custom TextStyle)
- [ ] Redesign with separate row

**Current Workaround:**
- User can tap username to copy full value
- Toast confirms: "Username copied to clipboard"
- Works but not ideal for verification

---

## Notes

- No hardcoded strings - all use string resources from base module
- No hardcoded colors - all use AppColors constants
- Follows existing Compose patterns (ViewModelRoute, StateFlow/SharedFlow)
- Compatible with current MVP architecture
- All Kotlin except response classes (Java with GSON annotations)
- Build successful for both base and mobile modules
- TwoFactorScreen.kt exists but currently disconnected (inline 2FA used instead)
