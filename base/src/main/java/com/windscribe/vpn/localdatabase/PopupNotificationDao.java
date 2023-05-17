/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.windscribe.vpn.localdatabase.tables.PopupNotificationTable;

import java.util.List;

import io.reactivex.Flowable;

@Dao
public interface PopupNotificationDao {

    @Query("SELECT * FROM notification_table WHERE user_name =:userName")
    Flowable<List<PopupNotificationTable>> getPopupNotification(String userName);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPopupNotification(PopupNotificationTable popupNotificationTable);


}
