# Windscribe Android App — AI Agent Skill

You are an AI agent working with the Windscribe Android app codebase. This skill defines HOW to perform common development, debugging, and maintenance tasks.

For architecture reference (WHAT the system is), see [AGENTS.md](AGENTS.md).
For human-friendly overview, see [README.md](README.md).

---

## Prerequisites

Before starting any work session:

```bash
# 1. Verify Android environment
echo $ANDROID_HOME
./gradlew --version

# 2. Pull latest changes (avoid conflicts)
git pull --rebase

# 3. Check current branch
git branch --show-current

# 4. Clean build if switching branches or after schema changes
./gradlew clean
```

---

## Development Workflows

### Adding a New Screen (Mobile — Jetpack Compose)

**Step 1: Define Screen Route**

```kotlin
// mobile/src/main/java/com/windscribe/mobile/nav/Screen.kt
sealed class Screen(val route: String) {
    // Existing screens...
    object NewFeature: Screen("new_feature")
}
```

**Step 2: Add to Navigation Graph**

```kotlin
// mobile/src/main/java/com/windscribe/mobile/nav/NavigationStack.kt
private fun NavGraphBuilder.addNavigationScreens() {
    // Existing routes...

    composable(
        route = Screen.NewFeature.route,
        enterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
    ) {
        ViewModelRoute(NewFeatureViewModel::class.java) {
            NewFeatureScreen(it)
        }
    }
}
```

**Step 3: Create Compose Screen**

```kotlin
// mobile/src/main/java/com/windscribe/mobile/ui/NewFeatureScreen.kt
@Composable
fun NewFeatureScreen(viewModel: NewFeatureViewModel) {
    val state by viewModel.state.collectAsState()

    when (state) {
        is NewFeatureState.Loading -> LoadingIndicator()
        is NewFeatureState.Success -> {
            val data = (state as NewFeatureState.Success).data
            Column {
                Text("Feature Content: $data")
                Button(onClick = { viewModel.performAction() }) {
                    Text("Action")
                }
            }
        }
        is NewFeatureState.Error -> {
            ErrorMessage((state as NewFeatureState.Error).message)
        }
    }
}
```

**Step 4: Create Abstract ViewModel + Implementation**

```kotlin
// mobile/src/main/java/com/windscribe/mobile/ui/NewFeatureViewModel.kt

// Abstract interface (allows easier testing)
abstract class NewFeatureViewModel : ViewModel() {
    abstract val state: StateFlow<NewFeatureState>
    abstract fun performAction()
}

// Implementation with dependencies (Hilt constructs it)
@HiltViewModel
class NewFeatureViewModelImpl @Inject constructor(
    private val preferencesHelper: PreferencesHelper,
    private val repository: SomeRepository
) : NewFeatureViewModel() {

    private val _state = MutableStateFlow<NewFeatureState>(NewFeatureState.Loading)
    override val state: StateFlow<NewFeatureState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.value = NewFeatureState.Loading
            val data = repository.fetchData()
            _state.value = NewFeatureState.Success(data)
        }
    }

    override fun performAction() {
        viewModelScope.launch {
            // Perform action
        }
    }
}

// State definition
sealed class NewFeatureState {
    object Loading : NewFeatureState()
    data class Success(val data: String) : NewFeatureState()
    data class Error(val message: String) : NewFeatureState()
}
```

**Step 5: Resolve the ViewModel in the Navigation Graph**

No DI module wiring is needed — Hilt builds the `@HiltViewModel` from its
`@Inject constructor`. Just request it inside the screen's `composable { }` block
in `NavigationStack.kt`:

```kotlin
// mobile/src/main/java/com/windscribe/mobile/ui/nav/NavigationStack.kt
composable(route = Screen.NewFeature.route, /* transitions */) {
    ViewModelRoute(NewFeatureViewModelImpl::class.java) { viewModel ->
        NewFeatureScreen(viewModel)
    }
}
```

`ViewModelRoute` calls `hiltViewModel()` under the hood. Any new dependency you
add to the constructor just needs to be provided somewhere in the Hilt graph
(usually already true for repositories/helpers).

**Step 6: Navigate to Screen**

```kotlin
// From any Composable with access to navController
val navController = LocalNavController.current
Button(onClick = { navController.navigate(Screen.NewFeature.route) }) {
    Text("Go to New Feature")
}
```

---

### Adding a Preference

**Step 1: Define Constant**

```kotlin
// base/src/main/java/com/windscribe/vpn/constants/PreferencesKeyConstants.kt
object PreferencesKeyConstants {
    // Existing constants...
    const val NEW_PREFERENCE = "new_preference_key"
}
```

**Step 2: Add to PreferencesHelper Interface**

```kotlin
// base/src/main/java/com/windscribe/vpn/apppreference/PreferencesHelper.kt
@Singleton
interface PreferencesHelper {
    // Existing properties...
    var newPreference: String
}
```

**Step 3: Implement in AppPreferencesImpl**

```kotlin
// base/src/main/java/com/windscribe/vpn/apppreference/AppPreferencesImpl.kt
@Singleton
class AppPreferencesImpl @Inject constructor(
    private val appPreferences: TrayAppPreferences
) : PreferencesHelper {

    // Existing implementations...

    override var newPreference: String
        get() = appPreferences.getString(PreferencesKeyConstants.NEW_PREFERENCE, "default_value")
        set(value) = appPreferences.put(PreferencesKeyConstants.NEW_PREFERENCE, value)
}
```

**Step 4: Use in ViewModel**

```kotlin
class SomeViewModel(
    private val preferencesHelper: PreferencesHelper
) : ViewModel() {

    fun updatePreference(value: String) {
        preferencesHelper.newPreference = value
    }

    fun getPreference(): String {
        return preferencesHelper.newPreference
    }
}
```

---

### Adding a Repository Method

**Step 1: Update DAO**

```kotlin
// base/src/main/java/com/windscribe/vpn/localdatabase/dao/SomeDao.kt
@Dao
abstract class SomeDao {

    @Query("SELECT * FROM SomeEntity WHERE id = :id")
    abstract suspend fun getById(id: Int): SomeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: SomeEntity)
}
```

**Step 2: Add to LocalDbInterface**

```kotlin
// base/src/main/java/com/windscribe/vpn/localdatabase/LocalDbInterface.kt
interface LocalDbInterface {
    // Existing methods...
    suspend fun getSomeEntityById(id: Int): SomeEntity?
    suspend fun insertSomeEntity(entity: SomeEntity)
}
```

**Step 3: Implement in LocalDatabaseImpl**

```kotlin
// base/src/main/java/com/windscribe/vpn/localdatabase/LocalDatabaseImpl.kt
class LocalDatabaseImpl @Inject constructor(
    private val someDao: SomeDao,
    // ... other DAOs
) : LocalDbInterface {

    override suspend fun getSomeEntityById(id: Int): SomeEntity? {
        return someDao.getById(id)
    }

    override suspend fun insertSomeEntity(entity: SomeEntity) {
        someDao.insert(entity)
    }
}
```

**Step 4: Add Repository Method**

```kotlin
// base/src/main/java/com/windscribe/vpn/repository/SomeRepository.kt
class SomeRepository @Inject constructor(
    private val scope: CoroutineScope,
    private val apiCallManager: IApiCallManager,
    private val localDbInterface: LocalDbInterface
) {

    suspend fun fetchAndSaveEntity(id: Int): CallResult<SomeEntity> {
        // Fetch from API
        val apiResult = result<SomeEntityResponse> {
            apiCallManager.getSomeEntity(id)
        }

        return when (apiResult) {
            is CallResult.Success -> {
                val entity = apiResult.data.toEntity()
                localDbInterface.insertSomeEntity(entity)
                CallResult.Success(entity)
            }
            is CallResult.Error -> apiResult
        }
    }
}
```

**Step 5: Use in ViewModel**

```kotlin
class SomeViewModel(
    private val repository: SomeRepository
) : ViewModel() {

    fun loadEntity(id: Int) {
        viewModelScope.launch {
            when (val result = repository.fetchAndSaveEntity(id)) {
                is CallResult.Success -> {
                    // Update UI state
                }
                is CallResult.Error -> {
                    // Show error
                }
            }
        }
    }
}
```

---

### Adding a VPN Feature

**Pattern**: Modify base → Update UI modules → Add tests

**Step 1: Update Core Logic in base/backend**

Example: Adding a new protocol option

```kotlin
// base/src/main/java/com/windscribe/vpn/backend/utils/WindVpnController.kt
class WindVpnController {

    fun connectWithNewFeature(config: VPNConfig, enableFeature: Boolean) {
        if (enableFeature) {
            // Apply feature-specific configuration
            config.customOption = "feature_value"
        }

        // Proceed with normal connection
        connect(config)
    }
}
```

**Step 2: Update Protocol Module (if needed)**

If the feature requires native protocol changes:

```kotlin
// openvpn/src/main/java/com/windscribe/vpn/openvpn/OpenVPNManager.kt
class OpenVPNManager {
    fun setCustomOption(value: String) {
        nativeSetOption(value)  // JNI call to C++
    }

    private external fun nativeSetOption(value: String)
}
```

**Step 3: Add Preference (if user-configurable)**

Follow "Adding a Preference" workflow above.

**Step 4: Update Mobile UI (Compose)**

```kotlin
// mobile/src/main/java/com/windscribe/mobile/ui/SettingsScreen.kt
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    Switch(
        checked = viewModel.isNewFeatureEnabled.collectAsState().value,
        onCheckedChange = { viewModel.setNewFeature(it) }
    )
}
```

**Step 5: Update TV UI (XML)**

```xml
<!-- tv/src/main/res/layout/settings_fragment.xml -->
<Switch
    android:id="@+id/new_feature_switch"
    android:text="Enable New Feature"
    android:checked="@{viewModel.newFeatureEnabled}" />
```

**Step 6: Add Tests**

```kotlin
// base/src/test/java/com/windscribe/vpn/backend/WindVpnControllerTest.kt
@Test
fun `connectWithNewFeature applies configuration when enabled`() {
    val controller = WindVpnController()
    val config = VPNConfig()

    controller.connectWithNewFeature(config, enableFeature = true)

    assertEquals("feature_value", config.customOption)
}
```

---

### Database Migration

**When Needed**: Adding/removing columns, changing types, adding tables

**Step 1: Update Entity**

Room entities live in `base/src/main/java/com/windscribe/vpn/serverlist/entity/` (server-list
entities like `Location`, `Datacenter`, `StaticRegion`, `PingTime`) and
`.../localdatabase/tables/` (e.g. `NetworkInfo`, `UserStatusTable`). They are plain Kotlin
classes with mutable `var` properties + defaults (NOT `data class`) so Room keeps its no-arg
constructor + setters.

```kotlin
// base/src/main/java/com/windscribe/vpn/localdatabase/tables/SomeEntity.kt
@Entity(tableName = "SomeEntity")
class SomeEntity(
    @PrimaryKey var id: Int = 0,
    var existingField: String? = null,
    var newField: String = ""  // NEW FIELD
)
```

**Step 2: Increment Database Version**

```kotlin
// base/src/main/java/com/windscribe/vpn/localdatabase/WindscribeDatabase.kt
@Database(
    entities = [Location::class, Datacenter::class, SomeEntity::class],
    version = 42,  // INCREMENT THIS
    exportSchema = true
)
@Singleton
abstract class WindscribeDatabase : RoomDatabase() {
    // abstract DAO accessors...
}
```

**Step 3: Add the Migration**

Migrations live in a standalone `object Migrations` (Kotlin), one `val` per step:

```kotlin
// base/src/main/java/com/windscribe/vpn/localdatabase/Migrations.kt
object Migrations {
    val migration_41_42: Migration = object : Migration(41, 42) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE SomeEntity ADD COLUMN newField TEXT NOT NULL DEFAULT ''"
            )
        }
    }
    // ... existing migration_X_Y vals
}
```

**Step 4: Register it where the database is built** (Hilt `@Provides`, NOT a companion `getInstance`):

```kotlin
// base/src/main/java/com/windscribe/vpn/di/BaseApplicationModule.kt
Room.databaseBuilder(app, WindscribeDatabase::class.java, "wind_db")
    // ... existing .addMigrations(...) chain
    .addMigrations(Migrations.migration_41_42)
    .build()
```

**Step 5: Test Migration**

```kotlin
// base/src/androidTest/java/com/windscribe/vpn/localdatabase/MigrationTest.kt
@Test
fun migrate41To42() {
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WindscribeDatabase::class.java
    )

    // Create database at version 41
    val db = helper.createDatabase("test.db", 41)
    db.execSQL("INSERT INTO SomeEntity (id, existingField) VALUES (1, 'test')")
    db.close()

    // Run migration
    helper.runMigrationsAndValidate("test.db", 42, true, Migrations.migration_41_42)

    // Verify new column exists
    val migratedDb = helper.runMigrationsAndValidate("test.db", 42, true, MIGRATION_41_42)
    val cursor = migratedDb.query("SELECT * FROM SomeEntity WHERE id = 1")
    cursor.moveToFirst()
    assertEquals("", cursor.getString(cursor.getColumnIndex("newField")))
}
```

---

## Build & Release

### Building Different Variants

```bash
# Mobile — Google Play (default)
./gradlew :mobile:assembleGoogleDebug          # Debug APK
./gradlew :mobile:assembleGoogleRelease        # Release APK (requires signing)
./gradlew :mobile:bundleGoogleRelease          # AAB for Play Store

# Mobile — F-Droid (no Google dependencies)
./gradlew :mobile:assembleFdroidDebug
./gradlew :mobile:assembleFdroidRelease

# TV — Google Play
./gradlew :tv:assembleGoogleDebug
./gradlew :tv:assembleGoogleRelease

# All modules, all variants
./gradlew assembleDebug
./gradlew assembleRelease
```

### Module-Specific Compilation (Faster Iteration)

```bash
# Compile Kotlin only (no full APK build)
./gradlew :base:compileGoogleDebugKotlin
./gradlew :mobile:compileGoogleDebugKotlin
./gradlew :tv:compileGoogleDebugKotlin

# Compile all together
./gradlew :base:compileGoogleDebugKotlin :mobile:compileGoogleDebugKotlin :tv:compileGoogleDebugKotlin --console=plain
```

### Release Checklist

See [docs/workflows/RELEASE_PROCESS.md](docs/workflows/RELEASE_PROCESS.md) for full checklist.

**Quick Reference**:
1. Update version in `build.gradle.kts` (major.minor.build)
2. Update changelog
3. Run full test suite (`./gradlew test connectedAndroidTest`)
4. Test all 6 protocols on real devices
5. Build release AAB (`./gradlew bundleGoogleRelease`)
6. Sign and upload to Play Console
7. Tag release in Git

---

## Debugging

### VPN Connection Issues

```bash
# Clear logcat buffer
"$ANDROID_HOME/platform-tools/adb" logcat -c

# Monitor VPN logs in real-time
"$ANDROID_HOME/platform-tools/adb" logcat -v time | grep -E "(WindVPN|OpenVPN|WireGuard|IKEv2)"

# Filter by specific protocol
"$ANDROID_HOME/platform-tools/adb" logcat -v time | grep -i "wireguard"

# Check connection state
"$ANDROID_HOME/platform-tools/adb" logcat -v time | grep "VPNConnectionState"

# Monitor network changes (auto-connect debugging)
"$ANDROID_HOME/platform-tools/adb" logcat -v time | grep "DeviceStateManager"
```

### Capture Screenshot (UI Debugging)

```bash
# Capture screenshot
"$ANDROID_HOME/platform-tools/adb" shell screencap -p /sdcard/screenshot.png

# Pull to local machine
"$ANDROID_HOME/platform-tools/adb" pull /sdcard/screenshot.png /tmp/screenshot.png

# Clean up
"$ANDROID_HOME/platform-tools/adb" shell rm /sdcard/screenshot.png
```

### Inspecting Room Database

```bash
# Pull database from device (requires root or debuggable app)
"$ANDROID_HOME/platform-tools/adb" pull /data/data/com.windscribe.vpn/databases/windscribe.db /tmp/

# Open with sqlite3
sqlite3 /tmp/windscribe.db

# Common queries
sqlite> .tables                                    # List all tables
sqlite> .schema Region                             # Show table schema
sqlite> SELECT * FROM Region LIMIT 5;              # View data
sqlite> SELECT COUNT(*) FROM City;                 # Count rows
```

### Debugging Auto-Secure Whitelist

```bash
# Check whitelist state
"$ANDROID_HOME/platform-tools/adb" logcat -v time | grep -i "whitelist"

# Monitor network changes
"$ANDROID_HOME/platform-tools/adb" logcat -v time | grep "DeviceStateManager"

# Force network change (requires root)
"$ANDROID_HOME/platform-tools/adb" shell svc wifi disable
"$ANDROID_HOME/platform-tools/adb" shell svc wifi enable
```

### Protocol-Specific Debugging

**OpenVPN**:
```bash
"$ANDROID_HOME/platform-tools/adb" logcat | grep -i "openvpn"
```

**WireGuard**:
```bash
"$ANDROID_HOME/platform-tools/adb" logcat | grep -i "wireguard"
```

**IKEv2**:
```bash
"$ANDROID_HOME/platform-tools/adb" logcat | grep -i "ikev2\|strongswan"
```

### Build Failures

**NDK Errors**:
```bash
# Verify NDK installation
echo $ANDROID_NDK_HOME

# Clean and rebuild
./gradlew clean
./gradlew assembleDebug
```

**Gradle Daemon Issues**:
```bash
# Stop Gradle daemon
./gradlew --stop

# Clean and rebuild
./gradlew clean assembleDebug
```

**Database Migration Crash**:
```bash
# Uninstall app (clears database)
"$ANDROID_HOME/platform-tools/adb" uninstall com.windscribe.vpn

# Reinstall
./gradlew :mobile:assembleGoogleDebug
"$ANDROID_HOME/platform-tools/adb" install -r mobile/build/outputs/apk/google/debug/mobile-google-debug.apk
```

---

## Testing

### Running Tests

```bash
# All unit tests
./gradlew test

# Specific module
./gradlew :base:test
./gradlew :mobile:test

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Specific test class
./gradlew :base:test --tests "WindVpnControllerTest"

# Test with coverage
./gradlew testDebugUnitTestCoverage
```

### Writing Unit Tests

**Pattern**: Arrange, Act, Assert (AAA)

```kotlin
class SomeRepositoryTest {
    private lateinit var repository: SomeRepository
    private lateinit var mockApiCallManager: IApiCallManager
    private lateinit var mockLocalDb: LocalDbInterface

    @Before
    fun setup() {
        mockApiCallManager = mockk()
        mockLocalDb = mockk()
        repository = SomeRepository(
            scope = TestCoroutineScope(),
            apiCallManager = mockApiCallManager,
            localDbInterface = mockLocalDb
        )
    }

    @Test
    fun `updateServerList saves to database on success`() = runTest {
        // Arrange
        val mockResponse = ServerListResponse(regions = listOf(...))
        coEvery { mockApiCallManager.getServerList(any()) } returns
            GenericResponseClass(dataClass = mockResponse)
        coEvery { mockLocalDb.addToRegions(any()) } just Runs
        coEvery { mockLocalDb.getAllRegionAsync() } returns listOf(...)

        // Act
        val result = repository.updateServerList()

        // Assert
        assertTrue(result is CallResult.Success)
        coVerify { mockLocalDb.addToRegions(mockResponse.regions) }
    }
}
```

### Writing Instrumented Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WindscribeDatabase::class.java
    )

    @Test
    fun migrateAll() {
        // Create database at version 1
        helper.createDatabase("test.db", 1).apply {
            close()
        }

        // Run all migrations up to current version
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            WindscribeDatabase::class.java,
            "test.db"
        ).build().apply {
            openHelper.writableDatabase.close()
        }
    }
}
```

---

## Code Quality

### Kotlin Linting

```bash
# Check code style (reports violations)
./gradlew ktlintCheck

# Auto-fix style issues
./gradlew ktlintFormat

# Run before every commit
./gradlew ktlintFormat && git add -A
```

### Security Scanning

**Strix** (Agentic red team):
```bash
# One-time comprehensive audit
strix scan /Users/gindersingh/Documents/Apps/gitlab/androidapp \
  --output-format markdown \
  --output-file docs/security/STRIX_AUDIT_$(date +%Y-%m-%d).md

# Quick scan (faster, less comprehensive)
strix scan --quick .
```

**Shannon** (Vulnerability analysis):
```bash
# Analyze for vulnerabilities
shannon analyze /Users/gindersingh/Documents/Apps/gitlab/androidapp \
  --report docs/security/SHANNON_AUDIT_$(date +%Y-%m-%d).md
```

**OWASP Dependency Check**:
```bash
# Check for known vulnerabilities in dependencies
./gradlew dependencyCheckAnalyze

# Report generated in build/reports/dependency-check-report.html
```

---

## Git Workflow

### Commit Standards

**Format**: `<type>(<scope>): <description>`

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `refactor`: Code refactoring (no behavior change)
- `perf`: Performance improvement
- `test`: Adding/updating tests
- `docs`: Documentation changes
- `build`: Build system changes
- `ci`: CI/CD changes
- `chore`: Maintenance tasks

**Scopes**:
- `mobile`: Mobile UI
- `tv`: TV UI
- `base`: Core functionality
- `openvpn`: OpenVPN protocol
- `wireguard`: WireGuard protocol
- `ikev2`: IKEv2 protocol
- `db`: Database changes
- `api`: API integration

**Examples**:
```bash
git commit -m "feat(mobile): add new settings screen for protocol selection"
git commit -m "fix(base): resolve auto-connect whitelist not clearing on network change"
git commit -m "refactor(wireguard): extract config parsing to separate class"
git commit -m "docs: update AGENTS.md with protocol switching workflow"
```

### Branch Naming

**Pattern**: `<type>/<short-description>`

```bash
# Features
git checkout -b feature/split-tunneling-ui
git checkout -b feature/wireguard-protocol

# Bug fixes
git checkout -b bugfix/connection-crash-on-wifi-change
git checkout -b bugfix/database-migration-38-39

# Hotfixes (for production issues)
git checkout -b hotfix/vpn-service-memory-leak
```

### Creating Pull Requests

**Before creating PR**:
```bash
# 1. Format code
./gradlew ktlintFormat

# 2. Run tests
./gradlew test

# 3. Commit changes
git add -A
git commit -m "feat(mobile): add feature X"

# 4. Push to remote
git push origin feature/my-feature

# 5. Create PR via GitLab UI
```

**PR Checklist** (see [docs/workflows/CODE_REVIEW_CHECKLIST.md](docs/workflows/CODE_REVIEW_CHECKLIST.md)):
- [ ] Code formatted (ktlintFormat)
- [ ] Tests pass (./gradlew test)
- [ ] New tests added for new features
- [ ] VPN features tested on all 6 protocols
- [ ] Database migration tested if schema changed
- [ ] No new Java files (use Kotlin)
- [ ] No direct API calls (use wsnet via ApiCallManager)
- [ ] Description explains WHAT and WHY

---

## Critical Agent Rules

### Always

1. **Use Kotlin** for ALL new code
   - No new Java files (base/mobile/tv are already 100% Kotlin)
   - The only Java left is the vendored native VPN modules (openvpn/strongswan/wgtunnel) — leave them alone unless updating upstream

2. **Use coroutines/flows** for async operations
   - `suspend fun` for one-shot async
   - `Flow<T>` for streams
   - `StateFlow<T>` for state
   - NO RxJava

3. **Use wsnet** for API calls
   - NEVER use Retrofit/OkHttp directly
   - All API calls via `ApiCallManager` → `wsnet`

4. **Run ktlintFormat** before every commit
   ```bash
   ./gradlew ktlintFormat && git add -A && git commit
   ```

5. **Test VPN features on all 6 protocols**
   - OpenVPN UDP, OpenVPN TCP, IKEv2, Stealth, WSTunnel, WireGuard
   - Protocol switching logic affects all

6. **Update database schema properly**
   - Increment version in `WindscribeDatabase.kt`
   - Add migration script
   - Test migration with instrumented test
   - Export schema to `schemas/` folder

7. **Follow MVP architecture**
   - Activity/Fragment (View) → Presenter/ViewModel → Repository → API/Database

8. **Inject via Hilt**
   - No manual `new` for singletons or core classes
   - Use `@Inject` constructor or `@Provides` methods in an `@InstallIn` module

9. **Write tests**
   - Unit tests for business logic (repositories, managers)
   - Instrumented tests for database migrations and UI

10. **Update CHANGELOG** for user-facing changes

### Never

1. **Create circular dependencies**
   - ✅ mobile/tv → base → protocols
   - ❌ base → mobile (breaks module hierarchy)

2. **Use RxJava**
   - Fully removed from codebase
   - Use coroutines/flows instead

3. **Call APIs directly**
   - ❌ `Retrofit.Builder()...`
   - ✅ `ApiCallManager.getServerList()`

4. **Skip database migrations**
   - Will crash on app upgrade
   - Always add migration for schema changes

5. **Modify protocol modules without testing**
   - Test all 6 protocols if changing base/backend
   - Protocol fallback logic depends on all working

6. **Commit secrets/keys**
   - Use `BuildConfig` for build-time secrets
   - Use `local.properties` for developer keys (git-ignored)
   - No hardcoded API keys, tokens, passwords

7. **Push directly to main/master**
   - Always use feature branches
   - Create PR for review

8. **Ignore ktlint violations**
   - CI will fail
   - Run `ktlintFormat` before committing

### When Unsure

1. **Check [AGENTS.md](AGENTS.md)** for architectural patterns
2. **Check [docs/guides/](docs/guides/)** for step-by-step workflows
3. **Search codebase** for existing examples
   ```bash
   # Find existing ViewModel implementations
   find . -name "*ViewModel.kt" | head -5

   # Find Repository examples
   find . -name "*Repository.kt" | head -5
   ```
4. **Ask in PR** if architectural decision needed
5. **Reference [AGENTS.md](AGENTS.md)** for architecture overview

---

## Common Pitfalls

### Protocol Switching

**Problem**: Connection fails after switching protocols

**Solution**:
1. Ensure old connection fully stopped before starting new
2. Clear VPN interface state
3. Wait for state machine to reach DISCONNECTED before reconnecting

```kotlin
// ❌ Don't do this
vpnBackend.stop()
vpnBackend.start(newConfig)  // May fail if old connection not fully stopped

// ✅ Do this
vpnBackend.stop()
vpnBackend.waitForDisconnect(timeout = 5.seconds)
vpnBackend.start(newConfig)
```

### Auto-Secure Whitelist

**Problem**: Auto-connect not working after returning to network

**Solution**: Ensure whitelist is cleared on network change

```kotlin
// DeviceStateManager must clear whitelist when network changes
override fun onNetworkChanged(newNetwork: Network) {
    clearAutoSecureWhitelist()  // Critical!
    checkAutoConnect()
}
```

### Database Migration

**Problem**: App crashes on upgrade with "Migration not found" error

**Solution**: Add migration for EVERY schema change

```kotlin
// ALWAYS add migration when incrementing version.
// Version lives on @Database; the migration step goes in the Migrations object,
// and is registered in BaseApplicationModule's Room.databaseBuilder(...).addMigrations(...).
@Database(version = 42)  // Incremented from 41 (in WindscribeDatabase.kt)
abstract class WindscribeDatabase : RoomDatabase()

object Migrations {
    val migration_41_42: Migration = object : Migration(41, 42) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Migration SQL here
        }
    }
}
```

### Compose State

**Problem**: UI not updating when data changes

**Solution**: Ensure ViewModel uses `StateFlow` and UI collects as state

```kotlin
// ViewModel
private val _state = MutableStateFlow<State>(State.Loading)
val state: StateFlow<State> = _state.asStateFlow()

// Composable
val state by viewModel.state.collectAsState()
```

---

## Additional Resources

- **[AGENTS.md](AGENTS.md)** — Architecture reference (modules, patterns, flows, code examples)
- **[README.md](README.md)** — Build instructions, features, tech stack
- **[docs/guides/](docs/guides/)** — How-to guides (OpenVPN updates, testing, etc.)
- **[docs/architecture/](docs/architecture/)** — Deep-dive architecture docs
- **[docs/workflows/](docs/workflows/)** — Release process, debugging, code review

---

**Last Updated**: 2026-05-27
**Maintained By**: Engineering Team