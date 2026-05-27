/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase

import androidx.room.Database
import androidx.room.RoomDatabase
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.localdatabase.tables.PingTestResults
import com.windscribe.vpn.localdatabase.tables.PopupNotificationTable
import com.windscribe.vpn.localdatabase.tables.ServerStatusUpdateTable
import com.windscribe.vpn.localdatabase.tables.UnBlockWgParam
import com.windscribe.vpn.localdatabase.tables.UserStatusTable
import com.windscribe.vpn.localdatabase.tables.WindNotification
import com.windscribe.vpn.serverlist.dao.ConfigFileDao
import com.windscribe.vpn.serverlist.dao.DatacenterAndLocationDao
import com.windscribe.vpn.serverlist.dao.DatacenterDao
import com.windscribe.vpn.serverlist.dao.FavouriteDao
import com.windscribe.vpn.serverlist.dao.LocationAndDatacentersDao
import com.windscribe.vpn.serverlist.dao.LocationDao
import com.windscribe.vpn.serverlist.dao.PingTimeDao
import com.windscribe.vpn.serverlist.dao.ServerDao
import com.windscribe.vpn.serverlist.dao.StaticRegionDao
import com.windscribe.vpn.serverlist.entity.ConfigFile
import com.windscribe.vpn.serverlist.entity.Datacenter
import com.windscribe.vpn.serverlist.entity.Favourite
import com.windscribe.vpn.serverlist.entity.Location
import com.windscribe.vpn.serverlist.entity.PingTime
import com.windscribe.vpn.serverlist.entity.Server
import com.windscribe.vpn.serverlist.entity.StaticRegion
import javax.inject.Singleton

@Database(
    entities = [PingTestResults::class, UserStatusTable::class, ServerStatusUpdateTable::class,
        PopupNotificationTable::class, Location::class,
        Datacenter::class, Server::class, Favourite::class, PingTime::class, StaticRegion::class, NetworkInfo::class, ConfigFile::class,
        WindNotification::class, UnBlockWgParam::class], version = 41, exportSchema = true
)
@Singleton
abstract class WindscribeDatabase : RoomDatabase() {

    abstract fun datacenterAndLocationDao(): DatacenterAndLocationDao

    abstract fun cityDao(): DatacenterDao

    abstract fun configFileDao(): ConfigFileDao

    abstract fun favouriteDao(): FavouriteDao

    abstract fun networkInfoDao(): NetworkInfoDao

    abstract fun pingTimeDao(): PingTimeDao

    abstract fun popupNotificationDao(): PopupNotificationDao

    abstract fun locationAndDatacentersDao(): LocationAndDatacentersDao

    abstract fun locationDao(): LocationDao

    abstract fun serverDao(): ServerDao

    abstract fun serverStatusDao(): ServerStatusDao

    abstract fun staticRegionDao(): StaticRegionDao

    abstract fun userStatusDao(): UserStatusDao

    abstract fun windNotificationDao(): WindNotificationDao

    abstract fun unblockWgDao(): UnblockWgDao
}
