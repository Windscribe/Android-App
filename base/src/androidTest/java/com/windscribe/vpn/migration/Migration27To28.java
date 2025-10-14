/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.migration;

import static com.windscribe.vpn.localdatabase.Migrations.migration_27_28;
import static org.junit.Assert.*;

import android.content.ContentValues;
import androidx.room.OnConflictStrategy;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.platform.app.InstrumentationRegistry;
import com.windscribe.vpn.Windscribe;
import com.windscribe.vpn.localdatabase.WindscribeDatabase;
import com.windscribe.vpn.serverlist.dao.StaticRegionDao;
import com.windscribe.vpn.serverlist.entity.StaticRegion;
import java.io.IOException;
import org.junit.*;

public class Migration27To28 {

    @Rule
    public MigrationTestHelper testHelper =
            new MigrationTestHelper(
                    InstrumentationRegistry.getInstrumentation(),
                    WindscribeDatabase.class.getCanonicalName(),
                    new FrameworkSQLiteOpenHelperFactory());

    private StaticRegionDao mStaticRegionDao;

    @Before
    public void init() {
        mStaticRegionDao = Windscribe.getAppContext().getWindscribeDatabase().staticRegionDao();
    }


    private int addRegion(SupportSQLiteDatabase db) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("id", 127);
        contentValues.put("staticIp", "----------");
        contentValues.put("type", "dc");
        contentValues.put("name", "Canada East");
        contentValues.put("countryCode", "CA");
        contentValues.put("shortName", "CA");
        contentValues.put("cityName", "Toronto");
        contentValues.put("serverId", 223);
        contentValues.put("wgIp", "----------");
        contentValues.put("wgPubKey", "----------");
        db.insert("StaticRegion", OnConflictStrategy.REPLACE, contentValues);
        return 127;
    }
}
