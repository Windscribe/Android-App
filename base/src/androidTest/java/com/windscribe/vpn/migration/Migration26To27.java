/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.migration;

import static com.windscribe.vpn.localdatabase.Migrations.migration_26_27;
import static org.junit.Assert.*;

import android.content.ContentValues;
import androidx.room.OnConflictStrategy;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.platform.app.InstrumentationRegistry;
import com.windscribe.vpn.Windscribe;
import com.windscribe.vpn.localdatabase.WindscribeDatabase;
import com.windscribe.vpn.serverlist.dao.CityDao;
import com.windscribe.vpn.serverlist.entity.City;
import java.io.IOException;
import org.junit.*;

public class Migration26To27 {

    @Rule
    public MigrationTestHelper testHelper =
            new MigrationTestHelper(
                    InstrumentationRegistry.getInstrumentation(),
                    WindscribeDatabase.class.getCanonicalName(),
                    new FrameworkSQLiteOpenHelperFactory());

    private CityDao mCityDao;

    @Before
    public void init() {
        mCityDao = Windscribe.getAppContext().getWindscribeDatabase().cityDao();
    }

    @Test
    public void addOVPNX509Column() throws IOException {
        final String TEST_DB_NAME = "wind_db";
        SupportSQLiteDatabase db = testHelper.createDatabase(TEST_DB_NAME, 26);
        int cityID = addCity(db);
        db.close();
        testHelper.runMigrationsAndValidate(TEST_DB_NAME, 27, true, migration_26_27);
        City city = mCityDao.getCityByID(cityID).blockingGet();
        assertNull(city.getOvpnX509());
        assertEquals(cityID, city.getId());
    }

    private int addCity(SupportSQLiteDatabase db) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("region_id", 4);
        contentValues.put("city_id", 180);
        contentValues.put("city", "Los Angeles");
        contentValues.put("nick", "Pac");
        contentValues.put("pro", 1);
        contentValues.put("gps", "34.05,-118.24");
        contentValues.put("tz", "America Los_Angeles");
        contentValues.put("wg_pubkey", "-----------");
        contentValues.put("ping_ip", "-----------");
        db.insert("City", OnConflictStrategy.REPLACE, contentValues);
        return 180;
    }
}
