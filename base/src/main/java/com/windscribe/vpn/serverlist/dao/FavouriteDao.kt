/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import com.windscribe.vpn.serverlist.entity.Favourite
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteDao {

    @Insert(onConflict = REPLACE)
    suspend fun addToFavouritesAsync(favourite: Favourite): Long

    @Query("Select * from Favourite")
    suspend fun getFavouritesAsync(): List<Favourite>

    @get:Query("Select * from Favourite")
    val favouritesAsFlow: Flow<List<Favourite>>

    @Query("Delete from Favourite where favourite_id = :ID")
    fun deleteFavourite(ID: Int)
}