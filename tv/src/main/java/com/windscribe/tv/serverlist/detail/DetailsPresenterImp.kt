/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.detail

import com.windscribe.tv.R
import com.windscribe.tv.serverlist.adapters.DetailViewAdapter
import com.windscribe.tv.serverlist.customviews.State.FavouriteState
import com.windscribe.tv.serverlist.overlay.LoadState
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.Windscribe.Companion.getExecutorService
import com.windscribe.vpn.api.response.ServerNodeListOverLoaded
import com.windscribe.vpn.commonutils.FlagIconResource
import com.windscribe.vpn.repository.LatencyRepository
import com.windscribe.vpn.serverlist.entity.*
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.collectLatest
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

class DetailsPresenterImp(
    private val detailView: DetailView,
    private val interactor: ActivityInteractor
) : DetailPresenter, DetailListener {
    private val logger = LoggerFactory.getLogger("detail:p")
    private var detailViewAdapter: DetailViewAdapter? = null
    override fun onDestroy() {
        logger.debug("Destroying detail presenter")
        interactor.getCompositeDisposable()
        interactor.getCompositeDisposable().dispose()
    }

    override fun init(regionId: Int) {
        logger.debug("Loading detail view for group.")
        detailView.setState(LoadState.Loading, 0, R.string.load_loading)
        val cities: MutableList<City> = ArrayList()
        val serverListData = ServerListData()
        val oneTimeCompositeDisposable = CompositeDisposable()
        oneTimeCompositeDisposable.add(
            interactor.getAllCities(regionId)
                .flatMap { updatedCities: List<City> ->
                    logger.info("Regions and cities...")
                    cities.clear()
                    cities.addAll(updatedCities)
                    interactor.getAllPings()
                }.onErrorReturnItem(ArrayList())
                .flatMap { pingTimes: List<PingTime> ->
                    logger.info("Ping times....")
                    serverListData.pingTimes = pingTimes
                    interactor.getFavourites()
                }.onErrorReturnItem(ArrayList())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<List<Favourite>>() {
                    override fun onError(e: Throwable) {
                        logger.debug("Error loading group view.")
                        detailView.setState(LoadState.Error, 0, R.string.load_error)
                        if (!oneTimeCompositeDisposable.isDisposed) {
                            oneTimeCompositeDisposable.dispose()
                        }
                    }

                    override fun onSuccess(favourites: List<Favourite>) {
                        logger.debug("***Successfully received server list.***")
                        serverListData.setShowLatencyInMs(
                            interactor.getAppPreferenceInterface().showLatencyInMS
                        )
                        serverListData.isProUser =
                            interactor.getAppPreferenceInterface().userStatus == 1
                        serverListData.favourites = favourites
                        if (cities.size > 0) {
                            setBackground(regionId)
                            detailViewAdapter = DetailViewAdapter(
                                cities,
                                serverListData, this@DetailsPresenterImp
                            )
                            detailViewAdapter?.setPremiumUser(interactor.isPremiumUser())
                            detailViewAdapter?.let { detailView.setDetailAdapter(it) }
                            setFavouriteStates()
                            detailView.setState(LoadState.Loaded, 0, 0)
                            logger.debug("Successfully loaded detail view.")
                        } else {
                            detailView.setState(LoadState.NoResult, 0, R.string.load_nothing_found)
                            logger.debug("No nodes found under this group.")
                        }
                        if (!oneTimeCompositeDisposable.isDisposed) {
                            oneTimeCompositeDisposable.dispose()
                        }
                    }
                })
        )
    }

    override fun onConnectClick(city: City) {
        logger.debug("Selected group item to connect.")
        getExecutorService()
            .submit {
                interactor.getAppPreferenceInterface().setFutureSelectCity(
                    city.getId()
                )
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
        interactor.getCompositeDisposable()
            .add(Completable.fromAction { interactor.deleteFavourite(favourite) }
                .andThen(interactor.getFavourites())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<List<Favourite>>() {
                    override fun onError(e: Throwable) {
                        logger.debug("Failed to add location to favourites.")
                        detailView.showToast("Error occurred." + e.localizedMessage)
                    }

                    override fun onSuccess(favourites: List<Favourite>) {
                        logger.debug("Removed from favourites.")
                        detailView.showToast("Removed from favourites")
                        detailViewAdapter?.setFavourites(favourites)
                    }
                }))
    }

    private fun addToFav(cityId: Int) {
        val favourite = Favourite()
        favourite.id = cityId
        interactor.getCompositeDisposable().add(interactor.addToFavourites(favourite)
            .flatMap { interactor.getFavourites() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object : DisposableSingleObserver<List<Favourite>>() {
                override fun onError(e: Throwable) {
                    logger.debug("Failed to add location to favourites.")
                    detailView.showToast("Error occurred.")
                }

                override fun onSuccess(favourites: List<Favourite>) {
                    logger.debug("Added to favourites.")
                    detailView.showToast("Added to favourites")
                    detailViewAdapter?.setFavourites(favourites)
                }
            })
        )
    }

    private fun setBackground(regionId: Int) {
        interactor.getCompositeDisposable().add(
            interactor.getRegionAndCity(regionId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<RegionAndCities>() {
                    override fun onError(e: Throwable) {}
                    override fun onSuccess(cities: RegionAndCities) {
                        detailView.setCountryFlagBackground(
                            FlagIconResource.getFlag(cities.region.countryCode)
                        )
                        detailView.setTitle(cities.region.name)
                        detailView.setCount("" + cities.cities.size)
                    }
                })
        )
    }

    private fun setFavouriteStates() {
        interactor.getCompositeDisposable().add(
            interactor.getFavoriteServerList()
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<List<ServerNodeListOverLoaded>>() {
                    override fun onError(e: Throwable) {}
                    override fun onSuccess(serverNodeListOverLoaded: List<ServerNodeListOverLoaded>) {
                        detailViewAdapter?.addFav(serverNodeListOverLoaded)
                    }
                })
        )
    }

    private val latencyAtomic = AtomicBoolean(true)
    override suspend fun observeLatencyChange() {
        interactor.getLatencyRepository().latencyEvent.collectLatest {
            if (latencyAtomic.getAndSet(false)) return@collectLatest
            when (it.second) {
                LatencyRepository.LatencyType.Servers -> updateLatency()
                else -> {}
            }
        }
    }

    private fun updateLatency() {
        interactor.getCompositeDisposable().add(
            interactor.getAllPings().subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<List<PingTime>>() {
                    override fun onError(e: Throwable) {}
                    override fun onSuccess(pings: List<PingTime>) {
                        detailViewAdapter?.setPings(pings)
                    }
                })
        )
    }
}