/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.serverlist.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.windscribe.vpn.serverlist.entity.DatacenterAndLocation

@Dao
interface DatacenterAndLocationDao {
    @Transaction
    @Query("Select * from Datacenter where city_id=:cityId limit 1")
    fun getDatacenterAndLocation(cityId: Int): DatacenterAndLocation?

    @Query("Select region_id From Datacenter where city_id=:cityId limit 1")
    suspend fun getLocationIdFromDatacenterAsync(cityId: Int): Int
}
