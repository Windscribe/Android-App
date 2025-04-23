package com.windscribe.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.constants.PreferencesKeyConstants.AZ_LIST_SELECTION_MODE
import com.windscribe.vpn.constants.PreferencesKeyConstants.LATENCY_LIST_SELECTION_MODE
import com.windscribe.vpn.constants.PreferencesKeyConstants.SELECTION_KEY
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.repository.FavouriteRepository
import com.windscribe.vpn.repository.LatencyRepository
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.repository.StaticIpRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.serverlist.entity.City
import com.windscribe.vpn.serverlist.entity.ConfigFile
import com.windscribe.vpn.serverlist.entity.Node
import com.windscribe.vpn.serverlist.entity.Region
import com.windscribe.vpn.serverlist.entity.StaticRegion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.grandcentrix.tray.core.OnTrayPreferenceChangeListener
import java.util.Locale
import java.util.logging.Logger

data class ServerListItem(val id: Int, val region: Region, val cities: List<City>)
data class FavouriteListItem(val id: Int, val city: City)
data class StaticListItem(val id: Int, val staticItem: StaticRegion)
data class ConfigListItem(val id: Int, val config: ConfigFile)
data class LatencyListItem(val id: Int, val time: Int)
sealed class UserState() {
    object Loading : UserState()
    object Pro : UserState()
    data class Free(val dataLeft: String, val dataLeftAngle: Float) : UserState()
}

sealed class ListState<out T> {
    object Loading : ListState<Nothing>()
    data class Success<T>(val data: List<T>) : ListState<T>()
    data class Error(val message: String) : ListState<Nothing>()
}

enum class ServerListType {
    All, Fav, Static, Config
}

abstract class ServerViewModel : ViewModel() {
    abstract val serverListState: StateFlow<ListState<ServerListItem>>
    abstract val favouriteListState: StateFlow<ListState<FavouriteListItem>>
    abstract val staticListState: StateFlow<ListState<StaticListItem>>
    abstract val configListState: StateFlow<ListState<ConfigListItem>>
    abstract val latencyListState: StateFlow<ListState<LatencyListItem>>
    abstract val selectedServerListType: StateFlow<ServerListType>
    abstract val showSearchView: StateFlow<Boolean>
    abstract val searchKeyword: StateFlow<String>
    abstract val searchListState: StateFlow<ListState<ServerListItem>>
    abstract val searchItemsExpandState: StateFlow<HashMap<String, Boolean>>
    abstract val refreshState: StateFlow<Boolean>
    abstract fun setSelectedType(type: ServerListType)
    abstract fun toggleFavorite(city: City)
    abstract fun deleteFavourite(id: Int)
    abstract fun toggleSearch()
    abstract fun onQueryTextChange(query: String)
    abstract fun onExpandStateChanged(id: String, expanded: Boolean)
    abstract val userState: StateFlow<UserState>
    abstract fun refresh(serverListType: ServerListType)
}

fun mockServerViewModel(): ServerViewModel {
    val node = Node()
    val city1 = City(1, 1, "Toronto", "Comfort Zone", 1, "Coordinates", "TZ", listOf(node))
    val city2 = City(1, 1, "Montreal", "Bagels", 1, "Coordinates", "TZ", listOf(node))
    val city3 = City(1, 1, "Ottawa", "Parliament", 1, "Coordinates", "TZ", listOf(node))
    val region1 = Region(1, "Canada East", "CA", 1, 1, "canada-east", 1, "", "", "", 1, "")
    val region2 = Region(2, "Canada West", "US", 1, 1, "canada-east", 1, "", "", "", 1, "")
    val region3 = Region(3, "India", "IN", 1, 1, "canada-east", 1, "", "", "", 1, "")
    val serverListItem1 = ServerListItem(1, region1, listOf(city1, city2, city3))
    val serverListItem2 = ServerListItem(2, region2, listOf(city1, city2, city3))
    val serverListItem3 = ServerListItem(3, region3, listOf(city1, city2, city3))
    val serverList = listOf(serverListItem1, serverListItem2, serverListItem3)
    return object : ServerViewModel() {
        override val serverListState: StateFlow<ListState<ServerListItem>>
            get() = MutableStateFlow(ListState.Success(serverList))
        override val favouriteListState: StateFlow<ListState<FavouriteListItem>>
            get() = MutableStateFlow(ListState.Loading)
        override val staticListState: StateFlow<ListState<StaticListItem>>
            get() = MutableStateFlow(ListState.Loading)
        override val configListState: StateFlow<ListState<ConfigListItem>>
            get() = MutableStateFlow(ListState.Loading)
        override val selectedServerListType: StateFlow<ServerListType>
            get() = MutableStateFlow(ServerListType.All)
        override val latencyListState: StateFlow<ListState<LatencyListItem>>
            get() = MutableStateFlow(ListState.Loading)
        override val searchListState: StateFlow<ListState<ServerListItem>>
            get() = MutableStateFlow(ListState.Loading)
        override val showSearchView: StateFlow<Boolean>
            get() = MutableStateFlow(false)
        override val searchKeyword: StateFlow<String>
            get() = MutableStateFlow("")
        override val searchItemsExpandState: StateFlow<HashMap<String, Boolean>>
            get() = MutableStateFlow(hashMapOf())
        override val userState: StateFlow<UserState>
            get() = MutableStateFlow(UserState.Loading)
        override val refreshState: StateFlow<Boolean>
            get() = MutableStateFlow(false)

        override fun setSelectedType(type: ServerListType) {}
        override fun toggleFavorite(city: City) {}
        override fun deleteFavourite(id: Int) {}
        override fun toggleSearch() {}
        override fun onQueryTextChange(query: String) {}
        override fun onExpandStateChanged(id: String, expanded: Boolean) {}
        override fun refresh(serverListType: ServerListType) {}
    }
}

class ServerViewModelImpl(
    private val serverRepository: ServerListRepository,
    private val favouriteRepository: FavouriteRepository,
    private val staticIpRepository: StaticIpRepository,
    private val localDbInterface: LocalDbInterface,
    private val userRepository: UserRepository,
    private val preferencesHelper: PreferencesHelper,
    private val latencyRepository: LatencyRepository
) : ServerViewModel() {

    private val _serverListState = MutableStateFlow<ListState<ServerListItem>>(ListState.Loading)
    override val serverListState: StateFlow<ListState<ServerListItem>> = _serverListState

    private val _favouriteListState =
        MutableStateFlow<ListState<FavouriteListItem>>(ListState.Loading)
    override val favouriteListState: StateFlow<ListState<FavouriteListItem>> = _favouriteListState

    private val _staticListState = MutableStateFlow<ListState<StaticListItem>>(ListState.Loading)
    override val staticListState: StateFlow<ListState<StaticListItem>> = _staticListState

    private val _configListState = MutableStateFlow<ListState<ConfigListItem>>(ListState.Loading)
    override val configListState: StateFlow<ListState<ConfigListItem>> = _configListState

    private val _latencyListState = MutableStateFlow<ListState<LatencyListItem>>(ListState.Loading)
    override val latencyListState: StateFlow<ListState<LatencyListItem>> = _latencyListState

    private val _searchListState = MutableStateFlow<ListState<ServerListItem>>(ListState.Loading)
    override val searchListState: StateFlow<ListState<ServerListItem>> = _searchListState

    private val _selectedServerListType = MutableStateFlow(ServerListType.All)
    override val selectedServerListType: StateFlow<ServerListType> = _selectedServerListType

    private val _userState = MutableStateFlow<UserState>(UserState.Loading)
    override val userState: StateFlow<UserState> = _userState

    private val _showSearchView = MutableStateFlow(false)
    override val showSearchView: StateFlow<Boolean> = _showSearchView

    private val _searchKeyword = MutableStateFlow("")
    override val searchKeyword: StateFlow<String> = _searchKeyword
    private val _searchItemsExpandState = MutableStateFlow<HashMap<String, Boolean>>(hashMapOf())
    override val searchItemsExpandState: StateFlow<HashMap<String, Boolean>> =
        _searchItemsExpandState
    private val _refreshState = MutableStateFlow(false)
    override val refreshState: StateFlow<Boolean> = _refreshState
    private var preferenceChangeListener: OnTrayPreferenceChangeListener? = null


    private val logger = Logger.getLogger("ServerViewModel")

    init {
        fetchAllLists()
        fetchUserState()
        fetchUserPreferences()
    }

    private fun fetchAllLists() {
        fetchServerList()
        fetchFavouriteList()
        fetchStaticList()
        fetchConfigList()
        fetchLatencyList()
    }

    private fun fetchLatencyList() {
        fetchData(
            ignoreLatencyAwait = true,
            stateFlow = _latencyListState,
            repositoryFlow = localDbInterface.getLatency(),
            transform = { latency -> latency.map { LatencyListItem(it.ping_id, it.pingTime) } },
            errorMessage = "Failed to load latency times."
        )
    }

    private fun fetchUserPreferences() {
        viewModelScope.launch {
            preferenceChangeListener = OnTrayPreferenceChangeListener {
                val locationOrderChanged = it.count { item -> item.key() == SELECTION_KEY } > 0
                if (locationOrderChanged) {
                    fetchServerList()
                    fetchFavouriteList()
                    fetchStaticList()
                    fetchConfigList()
                }
            }
            preferencesHelper.addObserver(preferenceChangeListener!!)
        }
    }

    private fun fetchUserState() {
        viewModelScope.launch {
            userRepository.userInfo.collect {
                if (it.isPro) {
                    _userState.emit(UserState.Pro)
                } else {
                    val dataLeft = it.maxData - it.dataUsed
                    val angle = (dataLeft.toFloat() / it.maxData.toFloat()) * 360f
                    logger.info("Data left: $dataLeft, Angle: $angle Max: ${it.maxData}")
                    _userState.emit(
                        UserState.Free(
                            String.format(
                                Locale.getDefault(),
                                "%.2f GB",
                                dataLeft.toDouble() / (1024 * 1024 * 1024)
                            ),
                            angle
                        )
                    )
                }
            }
        }
    }

    private fun fetchServerList() {
        fetchData(
            stateFlow = _serverListState,
            repositoryFlow = serverRepository.regions,
            transform = { regions ->
                if (regions.isNotEmpty()) {
                    preferencesHelper.migrationRequired = false
                }

                regions
                    .map { region ->
                        ServerListItem(
                            id = region.region.id,
                            region = region.region,
                            cities = region.cities.sortCities()
                        )
                    }
                    .sortRegions()
            },
            errorMessage = "Failed to load servers"
        )
    }

    private fun List<City>.sortCities(): List<City> {
        return when (preferencesHelper.selection) {
            LATENCY_LIST_SELECTION_MODE -> {
                val state = latencyListState.value as? ListState.Success<LatencyListItem>
                if (state != null) {
                    sortedBy { city ->
                        state.data.firstOrNull { it.id == city.id }?.time
                    }
                } else {
                    sortedBy { it.nodeName }
                }
            }

            else -> sortedBy { it.nodeName }
        }
    }

    private fun List<ServerListItem>.sortRegions(): List<ServerListItem> {
        return when (preferencesHelper.selection) {
            LATENCY_LIST_SELECTION_MODE -> {
                val state = latencyListState.value as? ListState.Success<LatencyListItem>
                if (state != null) {
                    val latencyMap = state.data.associateBy { it.id }
                    sortedBy { item ->
                        // Calculate average latency; fallback to max to sort these to the bottom
                        val (latencySum, count) = item.cities.fold(0 to 0) { acc, _ ->
                            val time = latencyMap[item.id]?.time ?: -1
                            if (time > 0) {
                                acc.first + time to acc.second + 1
                            } else {
                                acc
                            }
                        }
                        if (count > 0) latencySum / count else Int.MAX_VALUE
                    }
                } else {
                    this
                }
            }
            AZ_LIST_SELECTION_MODE -> sortedBy { it.region.name }
            else -> this
        }
    }

    private fun List<StaticListItem>.sortStaticRegions(): List<StaticListItem> {
        return when (preferencesHelper.selection) {
            LATENCY_LIST_SELECTION_MODE -> {
                val state = latencyListState.value as? ListState.Success<LatencyListItem>
                if (state != null) {
                    sortedBy { item ->
                        state.data.find { it.id == item.id }?.time ?: -1
                    }
                } else {
                    sortedBy { it.staticItem.cityName }
                }
            }

            else -> sortedBy { it.staticItem.cityName }
        }
    }

    private fun List<ConfigListItem>.sortConfigs(): List<ConfigListItem> {
        return when (preferencesHelper.selection) {
            LATENCY_LIST_SELECTION_MODE -> {
                val state = latencyListState.value as? ListState.Success<LatencyListItem>
                if (state != null) {
                    sortedBy { item ->
                        state.data.find { it.id == item.id }?.time ?: -1
                    }
                } else {
                    sortedBy { it.config.name }
                }
            }

            else -> sortedBy { it.config.name }
        }
    }

    private fun fetchFavouriteList() {
        fetchData(
            stateFlow = _favouriteListState,
            repositoryFlow = favouriteRepository.favourites,
            transform = { favourites ->
                favourites.sortCities().map { FavouriteListItem(it.id, it) }
            },
            errorMessage = "Failed to load favourites"
        )
    }

    private fun fetchStaticList() {
        fetchData(
            stateFlow = _staticListState,
            repositoryFlow = staticIpRepository.regions,
            transform = { regions ->
                regions.map { StaticListItem(it.id, it) }.sortStaticRegions()
            },
            errorMessage = "Failed to load static IPs"
        )
    }

    private fun fetchConfigList() {
        fetchData(
            stateFlow = _configListState,
            repositoryFlow = localDbInterface.getConfigs(),
            transform = { configs ->
                configs.map { ConfigListItem(it.getPrimaryKey(), it) }.sortConfigs()
            },
            errorMessage = "Failed to load custom configs"
        )
    }

    private fun <T, R> fetchData(
        ignoreLatencyAwait: Boolean = false,
        stateFlow: MutableStateFlow<ListState<R>>,
        repositoryFlow: Flow<List<T>>,
        transform: (List<T>) -> List<R>,
        errorMessage: String
    ) {
        stateFlow.value = ListState.Loading
        viewModelScope.launch {
            if (!ignoreLatencyAwait) {
                awaitLatencyIfNeeded()
            }
            repositoryFlow
                .flowOn(Dispatchers.IO)
                .map { transform(it) }
                .catch { e -> stateFlow.value = ListState.Error("$errorMessage: ${e.message}") }
                .collect { result ->
                    stateFlow.value = ListState.Success(result)
                }
        }
    }

    private suspend fun awaitLatencyIfNeeded() {
        if (preferencesHelper.selection == LATENCY_LIST_SELECTION_MODE) {
            latencyListState
                .filterIsInstance<ListState.Success<*>>()
                .first()
        }
    }

    override fun setSelectedType(type: ServerListType) {
        _selectedServerListType.value = type
    }

    override fun toggleFavorite(city: City) {
        viewModelScope.launch {
            val state = favouriteListState.value
            if (state is ListState.Success) {
                val isFavorite = state.data.any { it.city.id == city.id }
                if (isFavorite) {
                    favouriteRepository.remove(city.id)
                } else {
                    favouriteRepository.add(city)
                }
            }
        }
    }

    override fun deleteFavourite(id: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                localDbInterface.deleteFavourite(id)
            }
        }
    }

    override fun toggleSearch() {
        val searchViewState = _showSearchView.value
        _showSearchView.value = !searchViewState

        if (!searchViewState) {
            viewModelScope.launch(Dispatchers.IO) {
                _searchListState.emit(ListState.Loading)
                val items = (serverListState.value as? ListState.Success<ServerListItem>)?.data
                    ?: emptyList()
                _searchListState.emit(ListState.Success(items))
                _searchItemsExpandState.value = hashMapOf()
            }
        }
    }

    override fun onQueryTextChange(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _searchKeyword.value = query
            _searchListState.emit(ListState.Loading)
            val items =
                (serverListState.value as? ListState.Success<ServerListItem>)?.data ?: emptyList()
            val sortedItems = items.sortedWith(getComparator(query))
            val updatedList = sortedItems.mapNotNull { it.filterIfContains(query) }
            val searchItems = if (updatedList.size < items.size) updatedList else items
            _searchItemsExpandState.value =
                HashMap(searchItems.associate { it.region.name to true })
            _searchListState.emit(ListState.Success(searchItems))
        }
    }

    private fun ServerListItem.filterIfContains(keyword: String): ServerListItem? {
        val lowerKeyword = keyword.lowercase(Locale.getDefault())

        val filteredCities = cities.filter {
            it.nickName.lowercase(Locale.getDefault()).contains(lowerKeyword) ||
                    it.nodeName.lowercase(Locale.getDefault()).contains(lowerKeyword)
        }

        return when {
            filteredCities.isNotEmpty() -> copy(cities = filteredCities)
            region.name.lowercase(Locale.getDefault()).contains(lowerKeyword) -> this
            else -> null
        }
    }

    private fun ServerListItem.startsWithKeyword(keyword: String): Boolean {
        val lowerKeyword = keyword.lowercase(Locale.getDefault())

        return region.name.lowercase(Locale.getDefault()).startsWith(lowerKeyword) ||
                cities.any {
                    it.nickName.lowercase(Locale.getDefault()).startsWith(lowerKeyword) ||
                            it.nodeName.lowercase(Locale.getDefault()).startsWith(lowerKeyword)
                }
    }

    private fun getComparator(part: String): Comparator<ServerListItem> {
        return compareByDescending { it.startsWithKeyword(part) }
    }

    override fun onExpandStateChanged(id: String, expanded: Boolean) {
        viewModelScope.launch {
            _searchItemsExpandState.value = HashMap(_searchItemsExpandState.value).apply {
                this[id] = expanded
            }
        }
    }

    override fun refresh(serverListType: ServerListType) {
        viewModelScope.launch(Dispatchers.IO) {
            _refreshState.emit(true)
            when (serverListType) {
                ServerListType.All -> latencyRepository.updateAllServerLatencies()
                ServerListType.Fav -> latencyRepository.updateFavouriteCityLatencies()
                ServerListType.Static -> latencyRepository.updateStaticIpLatency()
                ServerListType.Config -> latencyRepository.updateConfigLatencies()
            }
            latencyRepository.latencyEvent.first()
            _refreshState.emit(false)
        }
    }

    override fun onCleared() {
        preferencesHelper.removeObserver(preferenceChangeListener!!)
        super.onCleared()
    }
}