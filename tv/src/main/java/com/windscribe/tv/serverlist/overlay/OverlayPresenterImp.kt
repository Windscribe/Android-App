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
import com.windscribe.tv.serverlist.listeners.NodeClickListener
import com.windscribe.tv.sort.ByLatency
import com.windscribe.tv.sort.ByRegionName
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.constants.PreferencesKeyConstants.AZ_LIST_SELECTION_MODE
import com.windscribe.vpn.constants.PreferencesKeyConstants.LATENCY_LIST_SELECTION_MODE
import com.windscribe.vpn.repository.LatencyRepository
import com.windscribe.vpn.serverlist.entity.*
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.collectLatest
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class OverlayPresenterImp @Inject constructor(
    private var overlayView: OverlayView,
    private var interactor: ActivityInteractor
) : OverlayPresenter, NodeClickListener {
    private var favouriteAdapter: FavouriteAdapter? = null
    private var serverAdapter: ServerAdapter? = null
    private var staticIpAdapter: StaticIpAdapter? = null
    private var windAdapter: ServerAdapter? = null
    private val logger = LoggerFactory.getLogger("overlay:p")
    override fun onDestroy() {
        logger.debug("Destroying Overlay presenter.")
        if (!interactor.getCompositeDisposable().isDisposed) {
            interactor.getCompositeDisposable().dispose()
        }
        favouriteAdapter = null
        staticIpAdapter = null
    }

    override fun onBestLocationClick(cityAndId: Int) {
        overlayView.onNodeSelected(cityAndId)
    }

    override fun onDisabledClick() {
        overlayView.onDisabledNodeClick()
    }

    override fun onFavouriteButtonClick(city: City, state: FavouriteState) {
        if (state == FavouriteState.Favourite) {
            logger.debug("Removed from favourites")
            removeFromFavourite(city.getId())
            overlayView.showToast(interactor.getResourceString(R.string.remove_from_favourites))
        } else {
            addToFav(city.getId())
            logger.debug("Added to favourites")
            overlayView.showToast(interactor.getResourceString(R.string.added_to_favourites))
        }
    }

    override fun onFavouriteNodeCLick(city: City) {
        logger.debug("Selected favourite node to connect.")
        overlayView.onNodeSelected(city.getId())
    }

    override fun onGroupSelected(city: Region) {
        overlayView.onLocationSelected(city.id)
    }

    override fun onStaticIpClick(staticIp: StaticRegion) {
        logger.debug("Selected static Ip to connect.")
        overlayView.onStaticSelected(
            staticIp.id, staticIp.credentials.userNameEncoded,
            staticIp.credentials.passwordEncoded
        )
    }

    private fun removeFromFavourite(cityId: Int) {
        val favourite = Favourite()
        favourite.id = cityId
        interactor.getCompositeDisposable()
            .add(Completable.fromAction { interactor.deleteFavourite(favourite) }
                .andThen(interactor.getFavourites())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<List<Favourite>>() {
                    override fun onError(e: Throwable) {
                        logger.debug("Failed to add location to favourites.")
                        overlayView.showToast("Error occurred." + e.localizedMessage)
                    }

                    override fun onSuccess(favourites: List<Favourite>) {
                        logger.debug("Removed from favourites.")
                        overlayView.showToast("Removed from favourites")
                        resetAdapters()
                    }
                }))
    }

    private fun resetAllAdapter(regions: MutableList<RegionAndCities>) {
        overlayView.setState(LoadState.Loading, R.drawable.ic_all_icon, R.string.load_loading, 1)
        logger.info("****Loading server list from local storage****")
        val dataDetails = ServerListData()
        val oneTimeCompositeDisposable = CompositeDisposable()
        oneTimeCompositeDisposable.add(
                interactor.getAllPings().onErrorReturnItem(ArrayList())
                .flatMap { pingTimes: List<PingTime> ->
                    logger.info("Ping times....")
                    dataDetails.pingTimes = pingTimes
                    interactor.getFavourites()
                }.onErrorReturnItem(ArrayList())
                .flatMap { favourites: List<Favourite> ->
                    logger.info("Favourites...")
                    dataDetails.favourites = favourites
                    interactor.getLocationProvider().bestLocation
                }.flatMap { cityAndRegion: CityAndRegion ->
                    dataDetails.setShowLatencyInMs(interactor.getAppPreferenceInterface().showLatencyInMS)
                    dataDetails.bestLocation = cityAndRegion
                    dataDetails.isProUser = interactor.getAppPreferenceInterface().userStatus == 1
                    for (regionAndCity in regions) {
                        val total = getTotal(regionAndCity.cities, dataDetails)
                        regionAndCity.latencyTotal = total
                    }
                    val selection = interactor.getAppPreferenceInterface().selection
                    if (selection == LATENCY_LIST_SELECTION_MODE) {
                        Collections.sort(regions, ByLatency())
                    } else if (selection == AZ_LIST_SELECTION_MODE) {
                        Collections.sort(regions, ByRegionName())
                    }
                    Single.fromCallable { regions }
                }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<List<RegionAndCities>>() {
                    override fun onError(e: Throwable) {
                        overlayView.setState(
                            LoadState.Error,
                            R.drawable.ic_all_icon,
                            R.string.load_error,
                            1
                        )
                        logger.debug("Error loading all nodes.")
                        if (!oneTimeCompositeDisposable.isDisposed) {
                            oneTimeCompositeDisposable.dispose()
                        }
                    }

                    override fun onSuccess(sortedRegions: List<RegionAndCities>) {
                        logger.debug("***Successfully received server list.***")
                        regions.add(0, RegionAndCities())
                        if (regions.isNotEmpty()) {
                            serverAdapter =
                                ServerAdapter(regions, dataDetails, this@OverlayPresenterImp, false)
                            serverAdapter?.let {
                                overlayView.setAllAdapter(it)
                                overlayView.setState(LoadState.Loaded, R.drawable.ic_all_icon, 0, 1)
                                logger.debug("All node loaded Successfully ")
                            }
                        } else {
                            overlayView
                                .setState(
                                    LoadState.NoResult,
                                    R.drawable.ic_all_icon,
                                    R.string.load_nothing_found,
                                    1
                                )
                            logger.debug("No nodes found.")
                        }
                        if (!oneTimeCompositeDisposable.isDisposed) {
                            oneTimeCompositeDisposable.dispose()
                        }
                    }
                })
        )
    }

    private fun resetFavouriteAdapter() {
        logger.debug("Loading favourite nodes.")
        overlayView.setState(
            LoadState.Loading,
            R.drawable.ic_fav_nav_icon,
            R.string.load_loading,
            2
        )
        val dataDetails = ServerListData()
        interactor.getCompositeDisposable().add(
            interactor.getAllPings()
                .flatMap { pingTimes: List<PingTime> ->
                    dataDetails.pingTimes = pingTimes
                    dataDetails.isProUser = interactor.getAppPreferenceInterface().userStatus == 1
                    interactor.getFavourites()
                }.flatMap { favourites: List<Favourite> ->
                    val favouritesArray = IntArray(favourites.size)
                    for (i in favourites.indices) {
                        favouritesArray[i] = favourites[i].id
                    }
                    Single.fromCallable { favouritesArray }
                }.flatMap { favourites: IntArray ->
                    interactor.getFavouriteRegionAndCities(favourites)
                }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<List<City>>() {
                    override fun onError(e: Throwable) {
                        overlayView.setState(
                            LoadState.NoResult, R.drawable.ic_fav_nav_icon,
                            R.string.load_nothing_found, 2
                        )
                        logger.debug("No favourite nodes found.")
                    }

                    override fun onSuccess(cities: List<City>) {
                        if (cities.isNotEmpty()) {
                            favouriteAdapter = FavouriteAdapter(
                                cities.toMutableList(), dataDetails,
                                this@OverlayPresenterImp
                            )
                            favouriteAdapter?.let {
                                it.setPremiumUser(interactor.isPremiumUser())
                                overlayView.setFavouriteAdapter(it)
                            }
                            overlayView.setState(
                                LoadState.Loaded,
                                R.drawable.ic_fav_nav_icon,
                                0,
                                2
                            )
                            logger.debug("Favourite node loaded Successfully ")
                        } else {
                            overlayView.setState(
                                LoadState.NoResult,
                                R.drawable.ic_fav_nav_icon,
                                R.string.load_nothing_found,
                                2
                            )
                            logger.debug("No favourite nodes found.")
                        }
                    }
                })
        )
    }

    override suspend fun allLocationViewReady() {
        interactor.getServerListUpdater().regions.collectLatest {
            resetAllAdapter(it.toMutableList())
        }
    }

    override fun favouriteViewReady() {
        resetFavouriteAdapter()
    }

    override fun staticIpViewReady() {
        logger.debug("Static view ready.")
        interactor.getStaticListUpdater().load()
    }

    override suspend fun windLocationViewReady() {
        interactor.getServerListUpdater().regions.collectLatest {
            resetWindAdapter(it.toMutableList())
        }
    }

    override suspend fun observeStaticRegions() {
        interactor.getStaticListUpdater().regions.collectLatest {
            logger.debug("Static list Updated: ${it.size}")
            resetStaticAdapter(it.toMutableList())
        }
    }

    override suspend fun observeAllLocations() {
        interactor.getServerListUpdater().regions.collectLatest {
            resetAllAdapter(it.toMutableList())
            resetFavouriteAdapter()
            resetWindAdapter(it.toMutableList())
        }
    }

    private fun resetStaticAdapter(regions: MutableList<StaticRegion>) {
        logger.debug("Loading static nodes.")
        overlayView.setState(
            LoadState.Loading,
            R.drawable.ic_static_ip,
            R.string.load_loading,
            4
        )
        val serverListData = ServerListData()
        interactor.getCompositeDisposable().add(
            interactor.getAllPings()
                .onErrorReturnItem(ArrayList())
                .flatMap { pingTimes: List<PingTime> ->
                    serverListData.pingTimes = pingTimes
                    serverListData
                        .setShowLatencyInMs(interactor.getAppPreferenceInterface().showLatencyInMS)
                    serverListData.isProUser =
                        interactor.getAppPreferenceInterface().userStatus == 1
                    Single.fromCallable { serverListData }
                }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object : DisposableSingleObserver<ServerListData>(){
                        override fun onSuccess(t: ServerListData) {
                            if (regions.size > 0) {
                                overlayView.setState(
                                                LoadState.Loaded,
                                                R.drawable.ic_static_ip,
                                                R.string.load_nothing_found,
                                                4
                                        )
                                staticIpAdapter = StaticIpAdapter(regions, serverListData, this@OverlayPresenterImp)
                                staticIpAdapter?.let { overlayView.setStaticAdapter(it) }
                            } else {
                                overlayView.setState(
                                        LoadState.NoResult, R.drawable.ic_static_ip,
                                        R.string.load_nothing_found, 4
                                )
                                logger.debug("No static ips found.")
                            }
                        }

                        override fun onError(e: Throwable) {
                            overlayView.setState(
                                    LoadState.Error,
                                    R.drawable.ic_static_ip,
                                    R.string.load_error,
                                    4
                            )
                            logger.debug("Error loading static ips.")
                        }

                    }))
    }

    private fun resetWindAdapter(regions: MutableList<RegionAndCities>) {
        logger.debug("Loading wind nodes.")
        overlayView.setState(
            LoadState.Loading,
            R.drawable.ic_flix_icon,
            R.string.load_loading,
            3
        )
        val dataDetails = ServerListData()
        val oneTimeCompositeDisposable = CompositeDisposable()
        oneTimeCompositeDisposable.add(
                interactor.getAllPings().onErrorReturnItem(ArrayList())
                .flatMap { pingTimes: List<PingTime> ->
                    logger.info("Ping times....")
                    dataDetails.pingTimes = pingTimes
                    interactor.getFavourites()
                }.onErrorReturnItem(ArrayList())
                .flatMap { favourites: List<Favourite> ->
                    logger.info("Favourites...")
                    dataDetails.favourites = favourites
                    interactor.getLocationProvider().bestLocation
                }.flatMap { cityAndRegion: CityAndRegion ->
                    dataDetails.setShowLatencyInMs(interactor.getAppPreferenceInterface().showLatencyInMS)
                    dataDetails.bestLocation = cityAndRegion
                    dataDetails.isProUser = interactor.getAppPreferenceInterface().userStatus == 1
                    val streamingGroups: MutableList<RegionAndCities> = ArrayList()
                    for (group in regions) {
                        if (group.region != null && (group.region.locationType
                                    == "streaming")
                        ) {
                            streamingGroups.add(group)
                        }
                    }
                    Single.fromCallable { streamingGroups }
                }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<List<RegionAndCities>>() {
                    override fun onError(e: Throwable) {
                        overlayView.setState(
                            LoadState.Error,
                            R.drawable.ic_flix_icon,
                            R.string.load_error,
                            3
                        )
                        logger.debug("Error loading wind nodes.")
                        if (!oneTimeCompositeDisposable.isDisposed) {
                            oneTimeCompositeDisposable.dispose()
                        }
                    }

                    override fun onSuccess(streamingGroups: List<RegionAndCities>) {
                        logger.debug("***Successfully received server list.***")
                        if (regions.size > 0) {
                            windAdapter = ServerAdapter(
                                streamingGroups, dataDetails, this@OverlayPresenterImp,
                                true
                            )
                            windAdapter?.let {
                                overlayView.setWindAdapter(it)
                                overlayView.setState(LoadState.Loaded, R.drawable.ic_flix_icon, 0, 3)
                                logger.debug("Wind node loaded Successfully ")
                            }
                        } else {
                            overlayView.setState(
                                LoadState.NoResult, R.drawable.ic_flix_icon,
                                R.string.load_nothing_found, 3
                            )
                            logger.debug("No wind nodes found.")
                        }
                        if (!oneTimeCompositeDisposable.isDisposed) {
                            oneTimeCompositeDisposable.dispose()
                        }
                    }
                })
        )
    }

    private fun addToFav(cityId: Int) {
        val favourite = Favourite()
        favourite.id = cityId
        interactor.getCompositeDisposable().add(
            interactor.addToFavourites(favourite)
                .flatMap { interactor.getFavourites() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<List<Favourite>>() {
                    override fun onError(e: Throwable) {
                        logger.debug("Failed to add location to favourites.")
                        overlayView.showToast("Error occurred.")
                    }

                    override fun onSuccess(favourites: List<Favourite>) {
                        logger.debug("Added to favourites.")
                        overlayView.showToast("Added to favourites")
                        resetAdapters()
                    }
                })
        )
    }

    private fun getTotal(cities: List<City>, dataDetails: ServerListData): Int {
        var total = 0
        var index = 0
        for (city in cities) {
            for (pingTime in dataDetails.pingTimes) {
                if (pingTime.id == city.getId()) {
                    total += pingTime.getPingTime()
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
        } else average
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun resetAdapters() {
        favouriteAdapter?.notifyDataSetChanged()
    }

    private val latencyAtomic = AtomicBoolean(true)
    override suspend fun observeLatencyChange() {
        interactor.getLatencyRepository().latencyEvent.collectLatest {
            if (latencyAtomic.getAndSet(false)) return@collectLatest
            when (it.second) {
                LatencyRepository.LatencyType.Servers -> {
                    interactor.getServerListUpdater().load()
                }
                LatencyRepository.LatencyType.StaticIp -> {
                    interactor.getStaticListUpdater().load()
                }
                LatencyRepository.LatencyType.Config -> {}
            }
        }
    }
}