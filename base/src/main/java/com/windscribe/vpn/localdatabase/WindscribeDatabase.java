/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.windscribe.vpn.localdatabase.tables.NetworkInfo;
import com.windscribe.vpn.localdatabase.tables.PingTestResults;
import com.windscribe.vpn.localdatabase.tables.PopupNotificationTable;
import com.windscribe.vpn.localdatabase.tables.ServerStatusUpdateTable;
import com.windscribe.vpn.localdatabase.tables.UserStatusTable;
import com.windscribe.vpn.localdatabase.tables.WindNotification;
import com.windscribe.vpn.serverlist.dao.CityAndRegionDao;
import com.windscribe.vpn.serverlist.dao.CityDao;
import com.windscribe.vpn.serverlist.dao.CityDetailDao;
import com.windscribe.vpn.serverlist.dao.ConfigFileDao;
import com.windscribe.vpn.serverlist.dao.FavouriteDao;
import com.windscribe.vpn.serverlist.dao.PingTimeDao;
import com.windscribe.vpn.serverlist.dao.RegionAndCitiesDao;
import com.windscribe.vpn.serverlist.dao.RegionDao;
import com.windscribe.vpn.serverlist.dao.StaticRegionDao;
import com.windscribe.vpn.serverlist.entity.City;
import com.windscribe.vpn.serverlist.entity.ConfigFile;
import com.windscribe.vpn.serverlist.entity.Favourite;
import com.windscribe.vpn.serverlist.entity.PingTime;
import com.windscribe.vpn.serverlist.entity.Region;
import com.windscribe.vpn.serverlist.entity.StaticRegion;

import javax.inject.Singleton;

@Database(entities = {PingTestResults.class, UserStatusTable.class, ServerStatusUpdateTable.class,
        PopupNotificationTable.class, Region.class,
        City.class, Favourite.class, PingTime.class, StaticRegion.class, NetworkInfo.class, ConfigFile.class,
        WindNotification.class}, version = 35)
@Singleton
public abstract class WindscribeDatabase extends RoomDatabase {


    public abstract CityAndRegionDao cityAndRegionDao();

    public abstract CityDao cityDao();

    public abstract CityDetailDao cityDetailDao();

    public abstract ConfigFileDao configFileDao();

    public abstract FavouriteDao favouriteDao();

    public abstract NetworkInfoDao networkInfoDao();

    public abstract PingTestDao pingTestDao();

    public abstract PingTimeDao pingTimeDao();

    public abstract PopupNotificationDao popupNotificationDao();

    public abstract RegionAndCitiesDao regionAndCitiesDao();

    public abstract RegionDao regionDao();

    public abstract ServerStatusDao serverStatusDao();

    public abstract StaticRegionDao staticRegionDao();

    public abstract UserStatusDao userStatusDao();

    public abstract WindNotificationDao windNotificationDao();
}
