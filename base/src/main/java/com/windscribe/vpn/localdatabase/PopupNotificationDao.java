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
import kotlinx.coroutines.flow.Flow;

@Dao
public interface PopupNotificationDao {

    @Query("SELECT * FROM notification_table WHERE user_name =:userName")
    Flowable<List<PopupNotificationTable>> getPopupNotification(String userName);

    @Query("SELECT * FROM notification_table WHERE user_name =:userName")
    Flow<List<PopupNotificationTable>> getPopupNotificationAsFlow(String userName);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPopupNotification(PopupNotificationTable popupNotificationTable);


}
