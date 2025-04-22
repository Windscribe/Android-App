/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.dao;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.windscribe.vpn.serverlist.entity.Favourite;

import java.util.List;

import io.reactivex.Single;
import kotlinx.coroutines.flow.Flow;

@Dao
public interface FavouriteDao {

    @Insert(onConflict = REPLACE)
    Single<Long> addToFavourites(Favourite favourite);

    @Delete
    void delete(Favourite favourite);

    @Query("Select * from Favourite")
    Single<List<Favourite>> getFavourites();

    @Query("Select * from Favourite")
    Flow<List<Favourite>> getFavouritesAsFlow();

    @Query("Delete from Favourite where favourite_id = :ID")
    void deleteFavourite(int ID);
}
