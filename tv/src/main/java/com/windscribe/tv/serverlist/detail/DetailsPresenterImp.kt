/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.detail

import com.windscribe.tv.serverlist.adapters.DetailViewAdapter
import com.windscribe.tv.serverlist.customviews.State.FavouriteState
import com.windscribe.tv.serverlist.overlay.LoadState
import com.windscribe.vpn.api.response.ServerNodeListOverLoaded
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.commonutils.FlagIconResource
import com.windscribe.vpn.commonutils.ResourceHelper
import com.windscribe.vpn.constants.UserStatusConstants
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.repository.LatencyRepository
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.serverlist.entity.Datacenter
import com.windscribe.vpn.serverlist.entity.Favourite
import com.windscribe.vpn.serverlist.entity.ServerListData
import com.windscribe.vpn.serverlist.entity.ServerMapState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class DetailsPresenterImp
    @Inject
    constructor(
        private val localDbInterface: LocalDbInterface,
        private val preferencesHelper: PreferencesHelper,
        private val resourceHelper: ResourceHelper,
        private val latencyRepository: LatencyRepository,
        private val serverListRepository: ServerListRepository,
        private val userRepository: UserRepository,
    ) : DetailPresenter,
        DetailListener {
        private val logger = LoggerFactory.getLogger("basic")
        private lateinit var detailView: DetailView
        private lateinit var activityScope: CoroutineScope
        private var detailViewAdapter: DetailViewAdapter? = null

        override fun bind(
            view: DetailView,
            scope: CoroutineScope,
        ) {
            this.detailView = view
            this.activityScope = scope
        }

        override fun onDestroy() {
            logger.debug("Destroying detail presenter")
            // Coroutine scope will be cancelled by the activity
        }

        override fun init(regionId: Int) {
            logger.debug("Loading detail view for group.")
            detailView.setState(LoadState.Loading, 0, com.windscribe.vpn.R.string.load_loading)

            activityScope.launch(Dispatchers.IO) {
                try {
                    // Load cities
                    val cities = localDbInterface.getAllDatacentersAsync(regionId)
                    logger.info("Regions and cities...")
                    val sortedCities = cities.sortedBy { it.nodeName }

                    // Load ping times
                    val pingTimes =
                        try {
                            localDbInterface.getAllPingsAsync()
                        } catch (e: Exception) {
                            logger.debug("Error loading ping times: ${e.message}")
                            emptyList()
                        }
                    logger.info("Ping times....")

                    // Load favourites
                    val favourites =
                        try {
                            localDbInterface.getFavouritesAsync()
                        } catch (e: Exception) {
                            logger.debug("Error loading favourites: ${e.message}")
                            emptyList()
                        }

                    // Resolve the region's country code and the user's ALC access list
                    val countryCode =
                        try {
                            localDbInterface.getLocationAsync(regionId).location?.countryCode
                        } catch (e: Exception) {
                            null
                        }
                    val alcCountryCodes = userRepository.user.value?.alcCountryCodes ?: emptySet()

                    // Load server counts from repository state
                    val serverState = serverListRepository.serversState.value
                    val serverCountMap =
                        when (serverState) {
                            is ServerMapState.Success -> {
                                serverState.data.mapValues { it.value.size }
                            }

                            else -> {
                                emptyMap()
                            }
                        }

                    withContext(Dispatchers.Main) {
                        logger.debug("***Successfully received server list.***")
                        val serverListData = ServerListData()
                        serverListData.setShowLatencyInMs(preferencesHelper.showLatencyInMS)
                        serverListData.isProUser = preferencesHelper.userStatus == UserStatusConstants.USER_STATUS_PREMIUM
                        serverListData.pingTimes = pingTimes
                        serverListData.favourites = favourites
                        serverListData.serverCountMap = serverCountMap

                        if (sortedCities.isNotEmpty()) {
                            setBackground(regionId)
                            detailViewAdapter =
                                DetailViewAdapter(
                                    sortedCities,
                                    serverListData,
                                    this@DetailsPresenterImp,
                                )
                            detailViewAdapter?.setPremiumUser(preferencesHelper.userStatus == UserStatusConstants.USER_STATUS_PREMIUM)
                            detailViewAdapter?.setAlcAccess(countryCode, alcCountryCodes)
                            detailViewAdapter?.let { detailView.setDetailAdapter(it) }
                            setFavouriteStates()
                            detailView.setState(LoadState.Loaded, 0, 0)
                            logger.debug("Successfully loaded detail view.")
                        } else {
                            detailView.setState(LoadState.NoResult, 0, com.windscribe.vpn.R.string.load_nothing_found)
                            logger.debug("No datacenters found under this location.")
                        }
                    }
                } catch (e: Throwable) {
                    withContext(Dispatchers.Main) {
                        logger.debug("Error loading group view.")
                        detailView.setState(LoadState.Error, 0, com.windscribe.vpn.R.string.load_error)
                    }
                }
            }
        }

        override fun onConnectClick(city: Datacenter) {
            logger.debug("Selected group item to connect.")
            detailView.onNodeSelected(city.id)
        }

        override fun onDisabledClick() {
            detailView.onDisabledNodeClick()
        }

        override fun onFavouriteClick(
            city: Datacenter,
            state: FavouriteState,
        ) {
            if (state == FavouriteState.Favourite) {
                logger.debug("Removing from favourites.")
                removeFromFavourite(city.id)
            } else {
                logger.debug("Adding to favourites.")
                addToFav(city.id)
            }
        }

        private fun removeFromFavourite(cityId: Int) {
            val favourite = Favourite()
            favourite.id = cityId
            activityScope.launch(Dispatchers.IO) {
                try {
                    localDbInterface.deleteFavourite(cityId)
                    val favourites = localDbInterface.getFavouritesAsync()

                    withContext(Dispatchers.Main) {
                        logger.debug("Removed from favourites.")
                        detailView.showToast(resourceHelper.getString(com.windscribe.vpn.R.string.remove_from_favourites))
                        detailViewAdapter?.setFavourites(favourites)
                    }
                } catch (e: Throwable) {
                    withContext(Dispatchers.Main) {
                        logger.debug("Failed to add location to favourites.")
                        detailView.showToast("Error occurred." + e.localizedMessage)
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
                    val favourites = localDbInterface.getFavouritesAsync()

                    withContext(Dispatchers.Main) {
                        detailView.showToast(resourceHelper.getString(com.windscribe.vpn.R.string.added_to_favourites))
                        detailViewAdapter?.setFavourites(favourites)
                    }
                } catch (e: Throwable) {
                    withContext(Dispatchers.Main) {
                        logger.debug("Failed to add location to favourites.")
                        detailView.showToast("Error occurred.")
                    }
                }
            }
        }

        private fun setBackground(regionId: Int) {
            activityScope.launch(Dispatchers.IO) {
                try {
                    val cities = localDbInterface.getLocationAsync(regionId)

                    withContext(Dispatchers.Main) {
                        detailView.setCountryFlagBackground(
                            FlagIconResource.getFlag(cities.location?.countryCode),
                        )
                        detailView.setTitle(cities.location?.name ?: "")
                        detailView.setCount("" + cities.datacenters.size)
                    }
                } catch (e: Throwable) {
                }
            }
        }

        private fun setFavouriteStates() {
            activityScope.launch(Dispatchers.IO) {
                try {
                    val jsonString = preferencesHelper.favoriteServerList
                    val serverNodeList =
                        com.google.gson.Gson().fromJson<List<ServerNodeListOverLoaded>>(
                            jsonString,
                            object : com.google.gson.reflect.TypeToken<List<ServerNodeListOverLoaded>>() {}.type,
                        )

                    withContext(Dispatchers.Main) {
                        detailViewAdapter?.addFav(serverNodeList)
                    }
                } catch (e: Throwable) {
                }
            }
        }

        private val latencyAtomic = AtomicBoolean(true)

        override suspend fun observeLatencyChange() {
            latencyRepository.latencyEvent.collectLatest {
                if (latencyAtomic.getAndSet(false)) return@collectLatest
                when (it.second) {
                    LatencyRepository.LatencyType.Servers -> {
                        updateLatency()
                    }

                    else -> {}
                }
            }
        }

        private fun updateLatency() {
            activityScope.launch(Dispatchers.IO) {
                try {
                    val pings = localDbInterface.getAllPingsAsync()

                    withContext(Dispatchers.Main) {
                        detailViewAdapter?.setPings(pings)
                    }
                } catch (e: Throwable) {
                }
            }
        }
    }
