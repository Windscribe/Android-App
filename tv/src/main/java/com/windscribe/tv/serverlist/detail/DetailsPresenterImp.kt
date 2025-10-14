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
import com.windscribe.vpn.serverlist.entity.City
import com.windscribe.vpn.serverlist.entity.Favourite
import com.windscribe.vpn.serverlist.entity.ServerListData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class DetailsPresenterImp @Inject constructor(
    private val detailView: DetailView,
    private val activityScope: CoroutineScope,
    private val localDbInterface: LocalDbInterface,
    private val preferencesHelper: PreferencesHelper,
    private val resourceHelper: ResourceHelper,
    private val latencyRepository: LatencyRepository
) : DetailPresenter, DetailListener {
    private val logger = LoggerFactory.getLogger("basic")
    private var detailViewAdapter: DetailViewAdapter? = null
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
                val cities = localDbInterface.getAllCitiesAsync(regionId)
                logger.info("Regions and cities...")
                val sortedCities = cities.sortedBy { it.nodeName }

                // Load ping times
                val pingTimes = try {
                    localDbInterface.getAllPingsAsync()
                } catch (e: Exception) {
                    logger.debug("Error loading ping times: ${e.message}")
                    emptyList()
                }
                logger.info("Ping times....")

                // Load favourites
                val favourites = try {
                    localDbInterface.getFavouritesAsync()
                } catch (e: Exception) {
                    logger.debug("Error loading favourites: ${e.message}")
                    emptyList()
                }

                withContext(Dispatchers.Main) {
                    logger.debug("***Successfully received server list.***")
                    val serverListData = ServerListData()
                    serverListData.setShowLatencyInMs(preferencesHelper.showLatencyInMS)
                    serverListData.isProUser = preferencesHelper.userStatus == UserStatusConstants.USER_STATUS_PREMIUM
                    serverListData.pingTimes = pingTimes
                    serverListData.favourites = favourites

                    if (sortedCities.isNotEmpty()) {
                        setBackground(regionId)
                        detailViewAdapter = DetailViewAdapter(
                            sortedCities,
                            serverListData,
                            this@DetailsPresenterImp
                        )
                        detailViewAdapter?.setPremiumUser(preferencesHelper.userStatus == UserStatusConstants.USER_STATUS_PREMIUM)
                        detailViewAdapter?.let { detailView.setDetailAdapter(it) }
                        setFavouriteStates()
                        detailView.setState(LoadState.Loaded, 0, 0)
                        logger.debug("Successfully loaded detail view.")
                    } else {
                        detailView.setState(LoadState.NoResult, 0, com.windscribe.vpn.R.string.load_nothing_found)
                        logger.debug("No nodes found under this group.")
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

    override fun onConnectClick(city: City) {
        logger.debug("Selected group item to connect.")
        activityScope.launch(Dispatchers.IO) {
            preferencesHelper.setFutureSelectCity(city.getId())
        }
        detailView.onNodeSelected(city.getId())
    }

    override fun onDisabledClick() {
        detailView.onDisabledNodeClick()
    }

    override fun onFavouriteClick(city: City, state: FavouriteState) {
        if (state == FavouriteState.Favourite) {
            logger.debug("Removing from favourites.")
            removeFromFavourite(city.getId())
        } else {
            logger.debug("Adding to favourites.")
            addToFav(city.getId())
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
                val cities = localDbInterface.getRegionAsync(regionId)

                withContext(Dispatchers.Main) {
                    detailView.setCountryFlagBackground(
                        FlagIconResource.getFlag(cities.region.countryCode)
                    )
                    detailView.setTitle(cities.region.name)
                    detailView.setCount("" + cities.cities.size)
                }
            } catch (e: Throwable) {
            }
        }
    }

    private fun setFavouriteStates() {
        activityScope.launch(Dispatchers.IO) {
            try {
                val jsonString = preferencesHelper.getResponseString(com.windscribe.vpn.constants.PreferencesKeyConstants.FAVORITE_SERVER_LIST)
                val serverNodeList = com.google.gson.Gson().fromJson<List<ServerNodeListOverLoaded>>(
                    jsonString,
                    object : com.google.gson.reflect.TypeToken<List<ServerNodeListOverLoaded>>() {}.type
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
                LatencyRepository.LatencyType.Servers -> updateLatency()
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