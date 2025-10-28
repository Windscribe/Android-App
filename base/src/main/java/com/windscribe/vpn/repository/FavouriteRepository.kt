package com.windscribe.vpn.repository

import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.serverlist.entity.City
import com.windscribe.vpn.serverlist.entity.Favourite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavouriteWithCity(val favourite: Favourite, val city: City)

class FavouriteRepository @Inject constructor(
    private val scope: CoroutineScope, private val localDbInterface: LocalDbInterface
) {
    private val _favourites = MutableSharedFlow<List<FavouriteWithCity>>(replay = 1)
    val favourites: SharedFlow<List<FavouriteWithCity>> = _favourites

    init {
        load()
    }

    fun load() {
        scope.launch {
            localDbInterface.getFavourites().collect { favourites ->
                val favouriteCityList = favourites.mapNotNull { fav ->
                    try {
                        val city = localDbInterface.getCityByIDAsync(fav.id)
                        FavouriteWithCity(fav, city)
                    } catch (e: Exception) {
                        null
                    }
                }
                _favourites.emit(favouriteCityList)
            }
        }
    }

    fun remove(id: Int) {
        scope.launch {
            localDbInterface.deleteFavourite(id)
        }
    }
}