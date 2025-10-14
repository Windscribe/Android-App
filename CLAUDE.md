# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

## App Overview

Windscribe VPN - Android application with comprehensive privacy and security features.

### Key Features
- **Authentication**: SSO, captcha, login/signup, account management
- **VPN Protocols**: OpenVPN (UDP/TCP), IKEv2, Stealth, WSTunnel, WireGuard
- **Per-Network Configuration**: Different protocols for different networks
- **Advanced**: Split tunneling, decoy traffic, custom configs, R.O.B.E.R.T DNS filtering, static IPs

## Build Commands

```bash
# Debug/Release builds
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew bundleGoogleRelease    # Google Play AAB
./gradlew bundleFdroidRelease    # F-Droid AAB

# Module-specific
./gradlew :mobile:assembleGoogleDebug
./gradlew :tv:assembleGoogleDebug

# Testing
./gradlew test
./gradlew connectedAndroidTest

# Code quality
./gradlew ktlintCheck
./gradlew ktlintFormat
```

### Install & Launch

```bash
# Mobile app
./gradlew :mobile:assembleGoogleDebug
"$ANDROID_HOME/platform-tools/adb" install -r mobile/build/outputs/apk/google/debug/mobile-google-debug.apk
"$ANDROID_HOME/platform-tools/adb" shell am start -n com.windscribe.vpn/com.windscribe.mobile.ui.AppStartActivity

# TV app
./gradlew :tv:assembleGoogleDebug
"$ANDROID_HOME/platform-tools/adb" install -r tv/build/outputs/apk/google/debug/tv-google-debug.apk
"$ANDROID_HOME/platform-tools/adb" shell am start -n com.windscribe.vpn/com.windscribe.tv.splash.SplashActivity
```

## Architecture

### Module Structure

**Core Modules**
- **base/**: Core functionality - managers, repositories, state management
  - api/: wsnet library integration
  - backend/: VPN functionality, protocol communication
  - localdatabase/: Room database
  - repository/: Data layer (API + database)
  - services/: Android services
- **mobile/**: Phone/tablet UI (Jetpack Compose, 100% Kotlin)
- **tv/**: Android TV UI (XML layouts, 100% Kotlin)
- **common/**: Tunnel wrapper + DNS traffic separation

**VPN Protocol Modules**
- **openvpn/**: OpenVPN implementation (built from source)
- **strongswan/**: IKEv2/IPSec (prebuilt binaries)
- **wgtunnel/**: Single native library from Go code containing:
  - WireGuard tunnel
  - WSTunnel (WebSocket tunneling)
  - Stunnel (Stealth Protocol)
  - ControlD/ctrld (DoH/DoT DNS)

**Supporting**
- **wsnet/**: In-house networking library - handles ALL API calls
- **test/**: Shared test utilities

### Tech Stack
- **Language**: Kotlin (preferred), Java (legacy)
- **DI**: Dagger 2
- **Database**: Room
- **Async**: Coroutines + Flows (RxJava fully removed)
- **UI**: Jetpack Compose (mobile), XML (TV)
- **Networking**: wsnet library

### Build Variants
- **google**: Google Play (billing, FCM, in-app review)
- **fdroid**: F-Droid (no proprietary Google dependencies)

## Development

### Code Style
- **Use Kotlin for all new code** (TV is 100% Kotlin)
- **Use coroutines/flows** (no RxJava)
- ktlint for Kotlin formatting
- MVP architecture pattern

### Architecture Pattern Example

Complete flow: Define Screen → Add to Navigation → Compose UI → ViewModel (Dagger) → Repository → API/Database

```kotlin
// 1. Define Screen Route (mobile/src/main/java/.../nav/Screen.kt)
sealed class Screen(val route: String) {
    object General: Screen("general")
    // ... other screens
}

// 2. Add Screen to Navigation (mobile/src/main/java/.../nav/NavigationStack.kt)
@Composable
fun NavigationStack(startDestination: Screen) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination.route) {
        addNavigationScreens()
    }
}

private fun NavGraphBuilder.addNavigationScreens() {
    composable(route = Screen.General.route,
        enterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
    ) {
        ViewModelRoute(GeneralViewModel::class.java) {
            GeneralScreen(it)
        }
    }
}

@Composable
private inline fun <reified VM : ViewModel> ViewModelRoute(
    viewModelClass: Class<VM>,
    content: @Composable (VM) -> Unit
) {
    val composeComponent = (LocalContext.current as? AppStartActivity)?.di
    val viewModel: VM = viewModel(factory = composeComponent.getViewModelFactory())
    content(viewModel)
}

// 3. Navigate to Screen
val navController = LocalNavController.current
navController.navigate(Screen.General.route)

// 4. Compose Screen receives ViewModel via Dagger
@Composable
fun GeneralScreen(viewModel: GeneralViewModel) {
    val state by viewModel.state.collectAsState()

    when (state) {
        is ListState.Loading -> LoadingIndicator()
        is ListState.Success -> Content(state.data)
        is ListState.Error -> ErrorMessage(state.message)
    }
}

// 5. ViewModel Implementation (abstract base + concrete impl)
abstract class GeneralViewModel : ViewModel() {
    abstract val state: StateFlow<GeneralState>
    abstract fun loadSettings()
}

class GeneralViewModelImpl(
    private val preferencesHelper: PreferencesHelper,
    private val userRepository: UserRepository
) : GeneralViewModel() {

    private val _state = MutableStateFlow<GeneralState>(GeneralState.Loading)
    override val state: StateFlow<GeneralState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    override fun loadSettings() {
        viewModelScope.launch {
            _state.value = GeneralState.Loading
            val settings = preferencesHelper.getGeneralSettings()
            _state.value = GeneralState.Success(settings)
        }
    }
}

sealed class GeneralState {
    object Loading : GeneralState()
    data class Success(val settings: GeneralSettings) : GeneralState()
    data class Error(val message: String) : GeneralState()
}

// 6. Dagger Factory (mobile/src/main/java/.../di/ComposeModule.kt)
@Module
class ComposeModule {
    @Provides
    fun getViewModelFactory(
        preferencesHelper: PreferencesHelper,
        userRepository: UserRepository
        // ... other dependencies
    ): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(GeneralViewModel::class.java)) {
                    return GeneralViewModelImpl(preferencesHelper, userRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}

// 7. Repository (base/src/main/java/com/windscribe/vpn/repository/...)
class ServerListRepository @Inject constructor(
    private val scope: CoroutineScope,
    private val apiCallManager: IApiCallManager,
    private val localDbInterface: LocalDbInterface,
    private val preferencesHelper: PreferencesHelper
) {
    private var _events = MutableSharedFlow<List<RegionAndCities>>(replay = 1)
    val regions: SharedFlow<List<RegionAndCities>> = _events

    init {
        load()
    }

    fun load() {
        scope.launch {
            _events.emit(localDbInterface.getAllRegionAsync())
        }
    }

    suspend fun updateServerList(): CallResult<Unit> {
        val userName = preferencesHelper.userName
        val apiResult = result<ServerListResponse> {
            apiCallManager.getServerList(userName)
        }

        return when (apiResult) {
            is CallResult.Success -> {
                localDbInterface.addToRegions(apiResult.data.regions)
                _events.emit(localDbInterface.getAllRegionAsync())
                CallResult.Success(Unit)
            }
            is CallResult.Error -> apiResult
        }
    }
}

// 8. LocalDbInterface (base/src/main/java/com/windscribe/vpn/localdatabase/...)
interface LocalDbInterface {
    suspend fun getAllRegionAsync(): List<RegionAndCities>
    suspend fun addToRegions(regions: List<Region>)
}

// Implementation wraps DAOs
class LocalDatabaseImpl @Inject constructor(
    private val regionAndCitiesDao: RegionAndCitiesDao,
    private val regionDao: RegionDao,
    // ... other DAOs
) : LocalDbInterface {
    override suspend fun getAllRegionAsync(): List<RegionAndCities> {
        return regionAndCitiesDao.getAllRegionAsync()
    }

    override suspend fun addToRegions(regions: List<Region>) {
        return regionDao.addRegions(regions)
    }
}

// 9. DAO (base/src/main/java/com/windscribe/vpn/serverlist/dao/...)
@Dao
abstract class RegionDao {
    @Insert(onConflict = REPLACE)
    abstract suspend fun addAll(regions: List<Region>)

    suspend fun addRegions(regions: List<Region>) {
        deleteAll()
        addAll(regions)
    }

    @Query("Delete from Region")
    abstract suspend fun deleteAll()
}

// 10. Entity (base/src/main/java/com/windscribe/vpn/serverlist/entity/...)
@Entity(tableName = "Region", indices = [Index(value = ["region_id"], unique = true)])
class Region(
    @SerializedName("id")
    @ColumnInfo(name = "region_id")
    @PrimaryKey
    val id: Int,

    @SerializedName("name")
    @ColumnInfo(name = "name")
    val name: String
)

// 11. API Manager Interface (base/src/main/java/com/windscribe/vpn/api/...)
interface IApiCallManager {
    suspend fun getServerList(userName: String): GenericResponseClass<String?, ApiErrorResponse?>
}

// 12. API Manager Implementation
@Singleton
class ApiCallManager @Inject constructor(
    private val wsNetServerAPI: WSNetServerAPI,
    private val preferencesHelper: PreferencesHelper
) : IApiCallManager {

    override suspend fun getServerList(userName: String): GenericResponseClass<String?, ApiErrorResponse?> {
        return suspendCancellableCoroutine { continuation ->
            val callback = wsNetServerAPI.serverLocations(userName) { code, json ->
                buildResponse(continuation, code, json, String::class.java)
            }
            continuation.invokeOnCancellation { callback.cancel() }
        }
    }
}

// 13. Preferences Key Constants (base/src/main/java/com/windscribe/vpn/constants/...)
object PreferencesKeyConstants {
    const val USER_NAME = "user_name"
}

// 14. Preferences Interface (base/src/main/java/com/windscribe/vpn/apppreference/...)
@Singleton
interface PreferencesHelper {
    var userName: String
}

// 15. Preferences Implementation (uses Tray library for storage)
@Singleton
class AppPreferencesImpl @Inject constructor(
    private val appPreferences: TrayAppPreferences
) : PreferencesHelper {

    override var userName: String
        get() = appPreferences.getString(PreferencesKeyConstants.USER_NAME, "")
        set(value) = appPreferences.put(PreferencesKeyConstants.USER_NAME, value)
}

// Usage in ViewModel: preferencesHelper.userName
```

### Common Tasks
- **VPN Features**: Modify base → Update UI modules → Add tests
- **UI Changes**: Mobile (Compose), TV (XML layouts)
- **API Changes**: Update wsnet integration (no direct Retrofit)

### Key Components
1. Multi-protocol VPN controller (base)
2. Auto-connection with protocol fallback
3. DNS routing (default VPN DNS or custom DoH/DoT via ctrld)
4. Centralized state management
5. Server/location selection logic

## Migration Status

- **TV Module**: 100% Kotlin
- **Mobile Module**: ~95% Kotlin (5 Java files - billing interfaces)
- **Base Module**: ~85% Kotlin (65 Java files - data models, API responses)
- **RxJava**: Fully removed, all coroutines/flows
