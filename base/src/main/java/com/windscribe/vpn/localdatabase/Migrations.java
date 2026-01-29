/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.windscribe.vpn.Windscribe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Migrations {

    private static final Logger logger = LoggerFactory.getLogger("migration");

    public static final Migration migration_26_27 = new Migration(26, 27) {
        @Override
        public void migrate(@NonNull final SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE City"
                    + " ADD COLUMN ovpn_x509 Text");
            invalidateData();
            logger.debug("Migrated database from version:26 to version:27");
        }
    };

    public static final Migration migration_27_28 = new Migration(27, 28) {
        @Override
        public void migrate(@NonNull final SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE StaticRegion"
                    + " ADD COLUMN ovpnX509 Text");
            invalidateData();
            logger.debug("Migrated database from version:27 to version:28");
        }
    };

    public static final Migration migration_29_31 = new Migration(29, 31) {
        @Override
        public void migrate(@NonNull final SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE City"
                    + " ADD COLUMN link_speed Text");
            database.execSQL("ALTER TABLE City"
                    + " ADD COLUMN health INTEGER NOT NULL DEFAULT 0");
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS WindNotification (`id` INTEGER NOT NULL, `date` INTEGER NOT NULL, `message` TEXT NOT NULL, `perm_free` INTEGER NOT NULL, `perm_pro` INTEGER NOT NULL, `title` TEXT NOT NULL, `popup` INTEGER NOT NULL, `isRead` INTEGER NOT NULL, `pcpID` TEXT, `promoCode` TEXT, `type` TEXT, `label` TEXT, PRIMARY KEY(`id`))");
            invalidateData();
            logger.debug("Migrated database from version:29 to version:31");
        }
    };

    public static final Migration migration_33_34 = new Migration(33, 34) {
        @Override
        public void migrate(@NonNull final SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE PingTime"
                    + " ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE PingTime"
                    + " ADD COLUMN ip Text");
            invalidateData();
            logger.debug("Migrated database from version:33 to version:34");
        }
    };

    public static final Migration migration_34_35 = new Migration(34, 35) {
        @Override
        public void migrate(@NonNull final SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE StaticRegion"
                    + " ADD COLUMN status INTEGER");
            invalidateData();
            logger.debug("Migrated database from version:34 to version:35");
        }
    };

    public static final Migration migration_35_36 = new Migration(35, 36) {
        @Override
        public void migrate(@NonNull final SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Favourite"
                    + " ADD COLUMN pinned_ip TEXT");
            database.execSQL("ALTER TABLE Favourite"
                    + " ADD COLUMN pinned_node_ip TEXT");
            invalidateData();
            logger.debug("Migrated database from version:35 to version:36");
        }
    };

    public static final Migration migration_36_37 = new Migration(36, 37) {
        @Override
        public void migrate(@NonNull final SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE StaticRegion"
                    + " ADD COLUMN gps TEXT");
            invalidateData();
            logger.debug("Migrated database from version:36 to version:37");
        }
    };
    public static final Migration migration_37_38 = new Migration(37, 38) {
        @Override
        public void migrate(@NonNull final SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `UnBlockWgParam` "
                    + "(`title` TEXT NOT NULL DEFAULT '', "
                    + "`countries` TEXT NOT NULL DEFAULT '', "
                    + "`Jc` INTEGER NOT NULL DEFAULT 0, "
                    + "`Jmin` INTEGER NOT NULL DEFAULT 0, "
                    + "`Jmax` INTEGER NOT NULL DEFAULT 0, "
                    + "`S1` INTEGER NOT NULL DEFAULT 0, "
                    + "`S2` INTEGER NOT NULL DEFAULT 0, "
                    + "`S3` INTEGER NOT NULL DEFAULT 0, "
                    + "`S4` INTEGER NOT NULL DEFAULT 0, "
                    + "`H1` INTEGER NOT NULL DEFAULT 0, "
                    + "`H2` INTEGER NOT NULL DEFAULT 0, "
                    + "`H3` INTEGER NOT NULL DEFAULT 0, "
                    + "`H4` INTEGER NOT NULL DEFAULT 0, "
                    + "`I1` TEXT NOT NULL DEFAULT '', "
                    + "`I2` TEXT NOT NULL DEFAULT '', "
                    + "`I3` TEXT NOT NULL DEFAULT '', "
                    + "`I4` TEXT NOT NULL DEFAULT '', "
                    + "PRIMARY KEY(`title`))");
            invalidateData();
            logger.debug("Migrated database from version:37 to version:38");
        }
    };

    private static void invalidateData() {
        Windscribe.getAppContext().getPreference().setMigrationRequired(true);
    }
}
