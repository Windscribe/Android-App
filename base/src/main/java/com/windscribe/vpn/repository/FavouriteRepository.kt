package com.windscribe.vpn.repository

import com.windscribe.vpn.commonutils.Ext.toResult
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.serverlist.entity.City
import com.windscribe.vpn.serverlist.entity.Favourite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class FavouriteRepository @Inject constructor(
    private val scope: CoroutineScope, private val localDbInterface: LocalDbInterface
) {
    private val _favourites = MutableSharedFlow<List<City>>(replay = 1)
    val favourites: SharedFlow<List<City>> = _favourites

    init {
        load()
    }

    private fun load() {
        scope.launch {
            localDbInterface.getFavourites().collect { favourites ->
                val favouriteCityList = favourites.mapNotNull { fav ->
                    try {
                        localDbInterface.getCityByIDAsync(fav.id)
                    } catch (e: Exception) {
                        null
                    }
                }
                _favourites.emit(favouriteCityList)
            }
        }
    }

    suspend fun add(city: City): Result<Long> {
        return localDbInterface.addToFavourites(Favourite(city.id)).toResult()
    }

    fun remove(id: Int) {
        scope.launch {
            localDbInterface.deleteFavourite(id)
        }
    }
}