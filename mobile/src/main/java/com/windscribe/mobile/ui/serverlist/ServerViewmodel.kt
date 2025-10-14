package com.windscribe.mobile.ui.serverlist

import android.util.Log
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
import com.windscribe.vpn.serverlist.entity.City
import com.windscribe.vpn.serverlist.entity.ConfigFile
import com.windscribe.vpn.serverlist.entity.Favourite
import com.windscribe.vpn.serverlist.entity.Region
import com.windscribe.vpn.serverlist.entity.StaticRegion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.grandcentrix.tray.core.OnTrayPreferenceChangeListener
import java.util.Locale
import kotlin.collections.map

data class ServerListItem(val id: Int, val region: Region, val cities: List<City>)
data class FavouriteListItem(val id: Int, val city: City, val countryCode: String)
data class StaticListItem(val id: Int, val staticItem: StaticRegion)
data class ConfigListItem(val id: Int, val config: ConfigFile)
data class LatencyListItem(val id: Int, val time: Int)

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
    abstract fun refresh(serverListType: ServerListType)
}

class ServerViewModelImpl(
    private val serverRepository: ServerListRepository,
    private val favouriteRepository: FavouriteRepository,
    private val staticIpRepository: StaticIpRepository,
    private val localDbInterface: LocalDbInterface,
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

    private val _selectedServerListType = MutableStateFlow(when (preferencesHelper.lastSelectedTabIndex) {
        0 -> ServerListType.All
        1 -> ServerListType.Fav
        2 -> ServerListType.Static
        3 -> ServerListType.Config
        else -> ServerListType.All
    })
    override val selectedServerListType: StateFlow<ServerListType> = _selectedServerListType


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

    init {
        fetchAllLists()
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
                            cities = region.cities.updateCityNames().sortCities()
                        )
                    }
                    .updateRegionNames().sortRegions()
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

    private fun List<City>.updateCityNames(): List<City> {
        return map {
            val cityName = serverRepository.getCustomCityName(it.id)
            val nickName = serverRepository.getCustomCityNickName(it.id)
            if (cityName != null && nickName != null) {
                it.apply {
                    this.nodeName = cityName
                    this.nickName = nickName
                }
            } else {
                it
            }
        }
    }

    private fun List<ServerListItem>.updateRegionNames(): List<ServerListItem> {
        return map {
            val countryName = serverRepository.getCustomRegionName(it.id)
            if (countryName != null) {
                it.apply {
                    this.region.name = countryName
                }
            } else {
                it
            }
        }
    }

    private fun List<ServerListItem>.sortRegions(): List<ServerListItem> {
        return when (preferencesHelper.selection) {
            LATENCY_LIST_SELECTION_MODE -> {
                val state = latencyListState.value as? ListState.Success<LatencyListItem>
                if (state != null) {
                    val latencyMap = state.data.associateBy { it.id }
                    sortedBy { item ->
                        // Calculate average latency across cities
                        val (latencySum, count) = item.cities.fold(0 to 0) { acc, city ->
                            val time = latencyMap[city.id]?.time ?: -1
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
        viewModelScope.launch(Dispatchers.IO) {
            favouriteRepository.favourites.map { favourites ->
                favourites.updateCityNames().sortCities().map { city ->
                    FavouriteListItem(
                        city.id,
                        city,
                        localDbInterface.getCountryCode(city.id)
                    )
                }
            }.collect {
                _favouriteListState.value = ListState.Success(it)
            }
        }
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
        viewModelScope.launch(Dispatchers.IO) {
            if (!ignoreLatencyAwait) {
                awaitLatencyIfNeeded()
            }
            repositoryFlow
                .flowOn(Dispatchers.IO)
                .map { transform(it) }
                .catch { e -> stateFlow.value = ListState.Error("$errorMessage: ${e.message}") }
                .collect { result ->
                    stateFlow.value = ListState.Success(result.toList())
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
        preferencesHelper.saveLastSelectedServerTabIndex(when (type) {
            ServerListType.All -> 0
            ServerListType.Fav -> 1
            ServerListType.Static -> 2
            ServerListType.Config -> 3
        })
        _selectedServerListType.value = type
    }

    override fun toggleFavorite(city: City) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = favouriteListState.value
            if (state is ListState.Success) {
                val isFavorite = state.data.any { it.city.id == city.id }
                if (isFavorite) {
                    favouriteRepository.remove(city.id)
                } else {
                    localDbInterface.addToFavouritesAsync(Favourite(city.id))
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
        if (_searchKeyword.value.isEmpty() && query.isEmpty()) {
            return
        }
        _searchKeyword.value = query
        viewModelScope.launch(Dispatchers.IO) {
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