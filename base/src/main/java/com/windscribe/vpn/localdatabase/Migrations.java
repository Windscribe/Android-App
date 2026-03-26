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
                    + "`H1` TEXT NOT NULL DEFAULT '', "
                    + "`H2` TEXT NOT NULL DEFAULT '', "
                    + "`H3` TEXT NOT NULL DEFAULT '', "
                    + "`H4` TEXT NOT NULL DEFAULT '', "
                    + "`I1` TEXT NOT NULL DEFAULT '', "
                    + "`I2` TEXT NOT NULL DEFAULT '', "
                    + "`I3` TEXT NOT NULL DEFAULT '', "
                    + "`I4` TEXT NOT NULL DEFAULT '', "
                    + "`I5` TEXT NOT NULL DEFAULT '', "
                    + "PRIMARY KEY(`title`))");
            invalidateData();
            logger.debug("Migrated database from version:37 to version:38");
        }
    };

    public static final Migration migration_38_39 = new Migration(38, 39) {
        @Override
        public void migrate(@NonNull final SupportSQLiteDatabase database) {
            // V2 Federated Server List: Complete migration in single step

            // 1. Create Server table
            database.execSQL("CREATE TABLE IF NOT EXISTS `Server` ("
                    + "`server_id` INTEGER NOT NULL, "
                    + "`hostname` TEXT NOT NULL, "
                    + "`ip` TEXT NOT NULL, "
                    + "`ip2` TEXT NOT NULL, "
                    + "`ip3` TEXT NOT NULL, "
                    + "`datacenter_id` INTEGER NOT NULL, "
                    + "`weight` INTEGER NOT NULL, "
                    + "`health` INTEGER NOT NULL, "
                    + "PRIMARY KEY(`server_id`))");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_Server_server_id` ON `Server` (`server_id`)");

            // 2. Migrate Region table - remove unused fields, add new fields, NO premium_only
            database.execSQL("CREATE TABLE IF NOT EXISTS `Region_new` ("
                    + "`primaryKey` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`region_id` INTEGER NOT NULL, "
                    + "`name` TEXT, "
                    + "`country_code` TEXT, "
                    + "`short_name` TEXT, "
                    + "`sort_order` INTEGER NOT NULL, "
                    + "`continent` TEXT)");

            database.execSQL("INSERT INTO `Region_new` "
                    + "(`primaryKey`, `region_id`, `name`, `country_code`, `short_name`, `sort_order`, `continent`) "
                    + "SELECT `primaryKey`, `region_id`, `name`, `country_code`, `short_name`, 0, '' "
                    + "FROM `Region`");

            database.execSQL("DROP TABLE `Region`");
            database.execSQL("ALTER TABLE `Region_new` RENAME TO `Region`");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_Region_region_id` ON `Region` (`region_id`)");

            // 3. Migrate City table - add new fields for V2
            database.execSQL("CREATE TABLE IF NOT EXISTS `City_new` ("
                    + "`primaryKey` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`city_id` INTEGER NOT NULL, "
                    + "`region_id` INTEGER NOT NULL, "
                    + "`city` TEXT, "
                    + "`nick` TEXT, "
                    + "`gps` TEXT, "
                    + "`tz` TEXT, "
                    + "`iata` TEXT, "
                    + "`status` INTEGER NOT NULL, "
                    + "`p2p` INTEGER NOT NULL, "
                    + "`pro` INTEGER NOT NULL, "
                    + "`wg_pubkey` TEXT, "
                    + "`wg_endpoint` TEXT, "
                    + "`ovpn_x509` TEXT, "
                    + "`link_speed` INTEGER NOT NULL)");

            database.execSQL("INSERT INTO `City_new` "
                    + "(`primaryKey`, `city_id`, `region_id`, `city`, `nick`, `gps`, `tz`, `iata`, `status`, `p2p`, `pro`, `wg_pubkey`, `wg_endpoint`, `ovpn_x509`, `link_speed`) "
                    + "SELECT `primaryKey`, `city_id`, `region_id`, `city`, `nick`, `gps`, `tz`, '', 1, 0, 0, `wg_pubkey`, '', `ovpn_x509`, "
                    + "CASE WHEN `link_speed` IS NULL OR `link_speed` = '' THEN 100 ELSE CAST(`link_speed` AS INTEGER) END "
                    + "FROM `City`");

            database.execSQL("DROP TABLE `City`");
            database.execSQL("ALTER TABLE `City_new` RENAME TO `City`");

            // 4. Rename Region table to Location
            database.execSQL("ALTER TABLE `Region` RENAME TO `Location`");

            // 5. Rename City table to Datacenter
            database.execSQL("ALTER TABLE `City` RENAME TO `Datacenter`");

            invalidateData();
            logger.debug("Migrated database from version:38 to version:39 - V2 Complete (Server, Location (renamed from Region), Datacenter (renamed from City))");
        }
    };

    public static final Migration migration_39_40 = new Migration(39, 40) {
        @Override
        public void migrate(@NonNull final SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE Server"
                    + " ADD COLUMN ipv6 INTEGER NOT NULL DEFAULT 0");
            invalidateData();
            logger.debug("Migrated database from version:39 to version:40");
        }
    };

    public static final Migration migration_40_41 = new Migration(40, 41) {
        @Override
        public void migrate(@NonNull final SupportSQLiteDatabase database) {
            // Drop and recreate UnBlockWgParam with id as primary key
            // Data will be refreshed from API on next fetch
            database.execSQL("DROP TABLE IF EXISTS UnBlockWgParam");
            database.execSQL("CREATE TABLE IF NOT EXISTS `UnBlockWgParam` "
                    + "(`id` TEXT NOT NULL DEFAULT '', "
                    + "`title` TEXT NOT NULL DEFAULT '', "
                    + "`countries` TEXT NOT NULL DEFAULT '', "
                    + "`Jc` INTEGER NOT NULL DEFAULT 0, "
                    + "`Jmin` INTEGER NOT NULL DEFAULT 0, "
                    + "`Jmax` INTEGER NOT NULL DEFAULT 0, "
                    + "`S1` INTEGER NOT NULL DEFAULT 0, "
                    + "`S2` INTEGER NOT NULL DEFAULT 0, "
                    + "`S3` INTEGER NOT NULL DEFAULT 0, "
                    + "`S4` INTEGER NOT NULL DEFAULT 0, "
                    + "`H1` TEXT NOT NULL DEFAULT '', "
                    + "`H2` TEXT NOT NULL DEFAULT '', "
                    + "`H3` TEXT NOT NULL DEFAULT '', "
                    + "`H4` TEXT NOT NULL DEFAULT '', "
                    + "`I1` TEXT NOT NULL DEFAULT '', "
                    + "`I2` TEXT NOT NULL DEFAULT '', "
                    + "`I3` TEXT NOT NULL DEFAULT '', "
                    + "`I4` TEXT NOT NULL DEFAULT '', "
                    + "`I5` TEXT NOT NULL DEFAULT '', "
                    + "PRIMARY KEY(`id`))");
            logger.debug("Migrated database from version:40 to version:41 - UnBlockWgParam primary key changed to id");
        }
    };

    private static void invalidateData() {
        Windscribe.getAppContext().getPreference().setMigrationRequired(true);
    }
}
