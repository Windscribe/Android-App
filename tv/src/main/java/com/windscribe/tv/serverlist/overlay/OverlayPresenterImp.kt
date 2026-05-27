/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.overlay

import android.annotation.SuppressLint
import com.windscribe.tv.R
import com.windscribe.tv.serverlist.adapters.FavouriteAdapter
import com.windscribe.tv.serverlist.adapters.ServerAdapter
import com.windscribe.tv.serverlist.adapters.StaticIpAdapter
import com.windscribe.tv.serverlist.customviews.State.FavouriteState
import com.windscribe.tv.serverlist.listeners.DatacenterClickListener
import com.windscribe.tv.sort.ByLatency
import com.windscribe.tv.sort.ByRegionName
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.AZ_LIST_SELECTION_MODE
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.LATENCY_LIST_SELECTION_MODE
import com.windscribe.vpn.commonutils.ResourceHelper
import com.windscribe.vpn.constants.UserStatusConstants
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.repository.LatencyRepository
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.repository.StaticIpRepository
import com.windscribe.vpn.serverlist.entity.Datacenter
import com.windscribe.vpn.serverlist.entity.Favourite
import com.windscribe.vpn.serverlist.entity.Location
import com.windscribe.vpn.serverlist.entity.LocationAndDatacenters
import com.windscribe.vpn.serverlist.entity.ServerListData
import com.windscribe.vpn.serverlist.entity.ServerMapState
import com.windscribe.vpn.serverlist.entity.StaticRegion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class OverlayPresenterImp
    @Inject
    constructor(
        private val localDbInterface: LocalDbInterface,
        private val preferencesHelper: PreferencesHelper,
        private val resourceHelper: ResourceHelper,
        private val locationRepository: LocationRepository,
        private val serverListRepository: ServerListRepository,
        private val staticIpRepository: StaticIpRepository,
        private val latencyRepository: LatencyRepository,
    ) : OverlayPresenter,
        DatacenterClickListener {
        private lateinit var overlayView: OverlayView
        private lateinit var activityScope: CoroutineScope
        private var favouriteAdapter: FavouriteAdapter? = null
        private var serverAdapter: ServerAdapter? = null
        private var staticIpAdapter: StaticIpAdapter? = null
        private var windAdapter: ServerAdapter? = null
        private val logger = LoggerFactory.getLogger("basic")

        override fun bind(
            view: OverlayView,
            scope: CoroutineScope,
        ) {
            this.overlayView = view
            this.activityScope = scope
        }

        override fun onDestroy() {
            logger.debug("Destroying Overlay presenter.")
            favouriteAdapter = null
            staticIpAdapter = null
        }

        override fun onBestLocationClick(cityAndId: Int) {
            overlayView.onNodeSelected(cityAndId)
        }

        override fun onDisabledClick() {
            overlayView.onDisabledNodeClick()
        }

        override fun onFavouriteButtonClick(
            city: Datacenter,
            state: FavouriteState,
        ) {
            if (state == FavouriteState.Favourite) {
                logger.debug("Removed from favourites")
                removeFromFavourite(city.id)
                overlayView.showToast(resourceHelper.getString(com.windscribe.vpn.R.string.remove_from_favourites))
            } else {
                addToFav(city.id)
                logger.debug("Added to favourites")
                overlayView.showToast(resourceHelper.getString(com.windscribe.vpn.R.string.added_to_favourites))
            }
        }

        override fun onFavouriteDatacenterClick(city: Datacenter) {
            logger.debug("Selected favourite datacenter to connect.")
            overlayView.onNodeSelected(city.id)
        }

        override fun onGroupSelected(city: Location) {
            overlayView.onLocationSelected(city.id)
        }

        override fun onStaticIpClick(staticIp: StaticRegion) {
            logger.debug("Selected static Ip to connect.")
            val credentials = staticIp.credentials
            if (credentials == null) {
                overlayView.showToast("No credentials found for static IP")
                return
            }
            overlayView.onStaticSelected(
                staticIp.id ?: 0,
                credentials.userNameEncoded ?: "",
                credentials.passwordEncoded ?: "",
            )
        }

        private fun removeFromFavourite(cityId: Int) {
            activityScope.launch(Dispatchers.IO) {
                try {
                    localDbInterface.deleteFavourite(cityId)
                    withContext(Dispatchers.Main) {
                        logger.debug("Removed from favourites.")
                        overlayView.showToast("Removed from favourites")
                        resetAdapters()
                    }
                } catch (e: Throwable) {
                    withContext(Dispatchers.Main) {
                        logger.debug("Failed to add location to favourites.")
                        overlayView.showToast("Error occurred." + e.localizedMessage)
                    }
                }
            }
        }

        private fun resetAllAdapter(regions: MutableList<LocationAndDatacenters>) {
            overlayView.setState(
                LoadState.Loading,
                R.drawable.ic_all_icon,
                com.windscribe.vpn.R.string.load_loading,
                1,
            )
            val dataDetails = ServerListData()
            activityScope.launch(Dispatchers.IO) {
                try {
                    val pingTimes =
                        try {
                            localDbInterface.getAllPingsAsync()
                        } catch (e: Exception) {
                            emptyList()
                        }
                    dataDetails.pingTimes = pingTimes

                    val favourites =
                        try {
                            localDbInterface.getFavouritesAsync()
                        } catch (e: Exception) {
                            emptyList()
                        }
                    dataDetails.favourites = favourites

                    dataDetails.setShowLatencyInMs(preferencesHelper.showLatencyInMS)
                    dataDetails.isProUser =
                        preferencesHelper.userStatus == UserStatusConstants.USER_STATUS_PREMIUM
                    try {
                        val bestLocation = locationRepository.getBestLocationAsync()
                        dataDetails.bestLocation = bestLocation
                    } catch (_: Exception) {
                    }
                    for (regionAndCity in regions) {
                        val total = getTotal(regionAndCity.datacenters, dataDetails)
                        regionAndCity.latencyTotal = total
                    }

                    val selection = preferencesHelper.selection
                    if (selection == LATENCY_LIST_SELECTION_MODE) {
                        Collections.sort(regions, ByLatency())
                    } else if (selection == AZ_LIST_SELECTION_MODE) {
                        Collections.sort(regions, ByRegionName())
                    }

                    withContext(Dispatchers.Main) {
                        logger.debug("***Successfully received server list.***")
                        regions.add(0, LocationAndDatacenters())
                        if (regions.isNotEmpty()) {
                            serverAdapter =
                                ServerAdapter(regions, dataDetails, this@OverlayPresenterImp, false)
                            serverAdapter?.let {
                                overlayView.setAllAdapter(it)
                                overlayView.setState(LoadState.Loaded, R.drawable.ic_all_icon, 0, 1)
                                logger.debug("All locations loaded successfully")
                            }
                        } else {
                            overlayView.setState(
                                LoadState.NoResult,
                                R.drawable.ic_all_icon,
                                com.windscribe.vpn.R.string.load_nothing_found,
                                1,
                            )
                            logger.debug("No datacenters found.")
                        }
                    }
                } catch (e: Throwable) {
                    withContext(Dispatchers.Main) {
                        overlayView.setState(
                            LoadState.Error,
                            R.drawable.ic_all_icon,
                            com.windscribe.vpn.R.string.load_error,
                            1,
                        )
                        logger.debug("Error loading all locations.")
                    }
                }
            }
        }

        private fun resetFavouriteAdapter() {
            logger.debug("Loading favourite datacenters.")
            overlayView.setState(
                LoadState.Loading,
                R.drawable.ic_fav_nav_icon,
                com.windscribe.vpn.R.string.load_loading,
                2,
            )
            val dataDetails = ServerListData()
            activityScope.launch(Dispatchers.IO) {
                try {
                    val pings = localDbInterface.getAllPingsAsync()
                    dataDetails.pingTimes = pings
                    dataDetails.isProUser = preferencesHelper.userStatus == 1

                    // Load server counts from repository state
                    val serverState = serverListRepository.serversState.value
                    dataDetails.serverCountMap =
                        when (serverState) {
                            is ServerMapState.Success -> {
                                serverState.data.mapValues { it.value.size }
                            }

                            else -> {
                                emptyMap()
                            }
                        }
                } catch (ignored: Exception) {
                }
                val favourites: IntArray =
                    try {
                        dataDetails.favourites = localDbInterface.getFavouritesAsync()
                        val favouritesArray = IntArray(dataDetails.favourites.size)
                        for (i in dataDetails.favourites.indices) {
                            favouritesArray[i] = dataDetails.favourites[i].id
                        }
                        favouritesArray
                    } catch (ignored: Exception) {
                        emptyArray<Int>().toIntArray()
                    }
                withContext(Dispatchers.Main) {
                    if (favourites.isNotEmpty()) {
                        try {
                            val cities = localDbInterface.getDatacenterByID(favourites)
                            favouriteAdapter =
                                FavouriteAdapter(
                                    cities.toMutableList(),
                                    dataDetails,
                                    this@OverlayPresenterImp,
                                )
                            favouriteAdapter?.let {
                                it.setPremiumUser(preferencesHelper.userStatus == 1)
                                overlayView.setFavouriteAdapter(it)
                            }
                            overlayView.setState(
                                LoadState.Loaded,
                                R.drawable.ic_fav_nav_icon,
                                0,
                                2,
                            )
                            logger.debug("Favourite datacenters loaded successfully")
                        } catch (ignored: Exception) {
                            overlayView.setState(
                                LoadState.NoResult,
                                R.drawable.ic_fav_nav_icon,
                                com.windscribe.vpn.R.string.load_nothing_found,
                                2,
                            )
                            logger.debug("No favourite datacenters found.")
                        }
                    } else {
                        overlayView.setState(
                            LoadState.NoResult,
                            R.drawable.ic_fav_nav_icon,
                            com.windscribe.vpn.R.string.load_nothing_found,
                            2,
                        )
                        logger.debug("No favourite datacenters found.")
                    }
                }
            }
        }

        override suspend fun allLocationViewReady() {
            serverListRepository.locationAndDatacenters.collectLatest {
                resetAllAdapter(it.toMutableList())
            }
        }

        override fun favouriteViewReady() {
            resetFavouriteAdapter()
        }

        override suspend fun staticIpViewReady() {
            logger.debug("Static view ready.")
            staticIpRepository.regions.collectLatest {
                resetStaticAdapter(it.toMutableList())
            }
        }

        override suspend fun observeStaticRegions() {
            staticIpRepository.regions.collectLatest {
                logger.debug("Static list Updated: ${it.size}")
                resetStaticAdapter(it.toMutableList())
            }
        }

        override suspend fun observeAllLocations() {
            serverListRepository.locationAndDatacenters.collectLatest {
                resetAllAdapter(it.toMutableList())
                resetFavouriteAdapter()
            }
        }

        private fun resetStaticAdapter(regions: MutableList<StaticRegion>) {
            logger.debug("Loading static IPs.")
            overlayView.setState(
                LoadState.Loading,
                R.drawable.ic_static_ip,
                com.windscribe.vpn.R.string.load_loading,
                4,
            )
            val serverListData = ServerListData()
            activityScope.launch(Dispatchers.IO) {
                try {
                    val pings =
                        try {
                            localDbInterface.getAllPingsAsync()
                        } catch (e: Exception) {
                            emptyList()
                        }
                    serverListData.pingTimes = pings
                    serverListData.setShowLatencyInMs(preferencesHelper.showLatencyInMS)
                    serverListData.isProUser =
                        preferencesHelper.userStatus == UserStatusConstants.USER_STATUS_PREMIUM

                    withContext(Dispatchers.Main) {
                        if (regions.isNotEmpty()) {
                            overlayView.setState(
                                LoadState.Loaded,
                                R.drawable.ic_static_ip,
                                com.windscribe.vpn.R.string.load_nothing_found,
                                4,
                            )
                            staticIpAdapter =
                                StaticIpAdapter(regions, serverListData, this@OverlayPresenterImp)
                            staticIpAdapter?.let { overlayView.setStaticAdapter(it) }
                        } else {
                            overlayView.setState(
                                LoadState.NoResult,
                                R.drawable.ic_static_ip,
                                com.windscribe.vpn.R.string.load_nothing_found,
                                4,
                            )
                            logger.debug("No static ips found.")
                        }
                    }
                } catch (e: Throwable) {
                    withContext(Dispatchers.Main) {
                        overlayView.setState(
                            LoadState.Error,
                            R.drawable.ic_static_ip,
                            com.windscribe.vpn.R.string.load_error,
                            4,
                        )
                        logger.debug("Error loading static ips.")
                    }
                }
            }
        }

        private fun addToFav(cityId: Int) {
            val favourite = Favourite()
            favourite.id = cityId
            activityScope.launch(Dispatchers.IO) {
                try {
                    localDbInterface.addToFavouritesAsync(favourite)
                    withContext(Dispatchers.Main) {
                        logger.debug("Added to favourites.")
                        overlayView.showToast("Added to favourites")
                        resetAdapters()
                    }
                } catch (e: Throwable) {
                    withContext(Dispatchers.Main) {
                        logger.debug("Failed to add location to favourites.")
                        overlayView.showToast("Error occurred.")
                    }
                }
            }
        }

        private fun getTotal(
            cities: List<Datacenter>,
            dataDetails: ServerListData,
        ): Int {
            var total = 0
            var index = 0
            for (city in cities) {
                for (pingTime in dataDetails.pingTimes) {
                    if (pingTime.ping_id == city.id) {
                        total += pingTime.pingTime
                        index++
                    }
                }
            }
            if (index == 0) {
                return 2000
            }
            val average = total / index
            return if (average == -1) {
                2000
            } else {
                average
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        private fun resetAdapters() {
            favouriteAdapter?.notifyDataSetChanged()
        }

        private val latencyAtomic = AtomicBoolean(true)

        override suspend fun observeLatencyChange() {
            latencyRepository.latencyEvent.collectLatest {
                if (latencyAtomic.getAndSet(false)) return@collectLatest
                when (it.second) {
                    LatencyRepository.LatencyType.Servers -> {
                        serverListRepository.load()
                    }

                    LatencyRepository.LatencyType.StaticIp -> {
                        staticIpRepository.load()
                    }

                    LatencyRepository.LatencyType.Config -> {}
                }
            }
        }
    }
