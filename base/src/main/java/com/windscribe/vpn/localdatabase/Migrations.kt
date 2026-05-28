/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.windscribe.vpn.Windscribe
import org.slf4j.LoggerFactory

object Migrations {
    private val logger = LoggerFactory.getLogger("migration")

    val migration_26_27: Migration =
        object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE City ADD COLUMN ovpn_x509 Text")
                invalidateData()
                logger.debug("Migrated db from version:26 to version:27")
            }
        }

    val migration_27_28: Migration =
        object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE StaticRegion ADD COLUMN ovpnX509 Text")
                invalidateData()
                logger.debug("Migrated db from version:27 to version:28")
            }
        }

    val migration_29_31: Migration =
        object : Migration(29, 31) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE City ADD COLUMN link_speed Text")
                db.execSQL("ALTER TABLE City ADD COLUMN health INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS WindNotification (`id` INTEGER NOT NULL, `date` INTEGER NOT NULL, `message` TEXT NOT NULL, `perm_free` INTEGER NOT NULL, `perm_pro` INTEGER NOT NULL, `title` TEXT NOT NULL, `popup` INTEGER NOT NULL, `isRead` INTEGER NOT NULL, `pcpID` TEXT, `promoCode` TEXT, `type` TEXT, `label` TEXT, PRIMARY KEY(`id`))",
                )
                invalidateData()
                logger.debug("Migrated db from version:29 to version:31")
            }
        }

    val migration_33_34: Migration =
        object : Migration(33, 34) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE PingTime ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE PingTime ADD COLUMN ip Text")
                invalidateData()
                logger.debug("Migrated db from version:33 to version:34")
            }
        }

    val migration_34_35: Migration =
        object : Migration(34, 35) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE StaticRegion ADD COLUMN status INTEGER")
                invalidateData()
                logger.debug("Migrated db from version:34 to version:35")
            }
        }

    val migration_35_36: Migration =
        object : Migration(35, 36) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Favourite ADD COLUMN pinned_ip TEXT")
                db.execSQL("ALTER TABLE Favourite ADD COLUMN pinned_node_ip TEXT")
                invalidateData()
                logger.debug("Migrated db from version:35 to version:36")
            }
        }

    val migration_36_37: Migration =
        object : Migration(36, 37) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE StaticRegion ADD COLUMN gps TEXT")
                invalidateData()
                logger.debug("Migrated db from version:36 to version:37")
            }
        }

    val migration_37_38: Migration =
        object : Migration(37, 38) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `UnBlockWgParam` " +
                        "(`title` TEXT NOT NULL DEFAULT '', " +
                        "`countries` TEXT NOT NULL DEFAULT '', " +
                        "`Jc` INTEGER NOT NULL DEFAULT 0, " +
                        "`Jmin` INTEGER NOT NULL DEFAULT 0, " +
                        "`Jmax` INTEGER NOT NULL DEFAULT 0, " +
                        "`S1` INTEGER NOT NULL DEFAULT 0, " +
                        "`S2` INTEGER NOT NULL DEFAULT 0, " +
                        "`S3` INTEGER NOT NULL DEFAULT 0, " +
                        "`S4` INTEGER NOT NULL DEFAULT 0, " +
                        "`H1` TEXT NOT NULL DEFAULT '', " +
                        "`H2` TEXT NOT NULL DEFAULT '', " +
                        "`H3` TEXT NOT NULL DEFAULT '', " +
                        "`H4` TEXT NOT NULL DEFAULT '', " +
                        "`I1` TEXT NOT NULL DEFAULT '', " +
                        "`I2` TEXT NOT NULL DEFAULT '', " +
                        "`I3` TEXT NOT NULL DEFAULT '', " +
                        "`I4` TEXT NOT NULL DEFAULT '', " +
                        "`I5` TEXT NOT NULL DEFAULT '', " +
                        "PRIMARY KEY(`title`))",
                )
                invalidateData()
                logger.debug("Migrated db from version:37 to version:38")
            }
        }

    val migration_38_39: Migration =
        object : Migration(38, 39) {
            // Long lines are unbreakable SQL column lists.
            @Suppress("ktlint:standard:max-line-length")
            override fun migrate(db: SupportSQLiteDatabase) {
                // V2 Federated Server List: Complete migration in single step

                // 1. Create Server table
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `Server` (" +
                        "`server_id` INTEGER NOT NULL, " +
                        "`hostname` TEXT NOT NULL, " +
                        "`ip` TEXT NOT NULL, " +
                        "`ip2` TEXT NOT NULL, " +
                        "`ip3` TEXT NOT NULL, " +
                        "`datacenter_id` INTEGER NOT NULL, " +
                        "`weight` INTEGER NOT NULL, " +
                        "`health` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`server_id`))",
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_Server_server_id` ON `Server` (`server_id`)")

                // 2. Migrate Region table - remove unused fields, add new fields, NO premium_only
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `Region_new` (" +
                        "`primaryKey` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`region_id` INTEGER NOT NULL, " +
                        "`name` TEXT, " +
                        "`country_code` TEXT, " +
                        "`short_name` TEXT, " +
                        "`sort_order` INTEGER NOT NULL, " +
                        "`continent` TEXT)",
                )

                db.execSQL(
                    "INSERT INTO `Region_new` " +
                        "(`primaryKey`, `region_id`, `name`, `country_code`, `short_name`, `sort_order`, `continent`) " +
                        "SELECT `primaryKey`, `region_id`, `name`, `country_code`, `short_name`, 0, '' " +
                        "FROM `Region`",
                )

                db.execSQL("DROP TABLE `Region`")
                db.execSQL("ALTER TABLE `Region_new` RENAME TO `Region`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_Region_region_id` ON `Region` (`region_id`)")

                // 3. Migrate City table - add new fields for V2
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `City_new` (" +
                        "`primaryKey` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`city_id` INTEGER NOT NULL, " +
                        "`region_id` INTEGER NOT NULL, " +
                        "`city` TEXT, " +
                        "`nick` TEXT, " +
                        "`gps` TEXT, " +
                        "`tz` TEXT, " +
                        "`iata` TEXT, " +
                        "`status` INTEGER NOT NULL, " +
                        "`p2p` INTEGER NOT NULL, " +
                        "`pro` INTEGER NOT NULL, " +
                        "`wg_pubkey` TEXT, " +
                        "`wg_endpoint` TEXT, " +
                        "`ovpn_x509` TEXT, " +
                        "`link_speed` INTEGER NOT NULL)",
                )

                db.execSQL(
                    "INSERT INTO `City_new` " +
                        "(`primaryKey`, `city_id`, `region_id`, `city`, `nick`, `gps`, `tz`, `iata`, `status`, `p2p`, `pro`, `wg_pubkey`, `wg_endpoint`, `ovpn_x509`, `link_speed`) " +
                        "SELECT `primaryKey`, `city_id`, `region_id`, `city`, `nick`, `gps`, `tz`, '', 1, 0, 0, `wg_pubkey`, '', `ovpn_x509`, " +
                        "CASE WHEN `link_speed` IS NULL OR `link_speed` = '' THEN 100 ELSE CAST(`link_speed` AS INTEGER) END " +
                        "FROM `City`",
                )

                db.execSQL("DROP TABLE `City`")
                db.execSQL("ALTER TABLE `City_new` RENAME TO `City`")

                // 4. Rename Region table to Location
                db.execSQL("ALTER TABLE `Region` RENAME TO `Location`")

                // 5. Rename City table to Datacenter
                db.execSQL("ALTER TABLE `City` RENAME TO `Datacenter`")

                invalidateData()
                logger.debug(
                    "Migrated db from version:38 to version:39 - V2 Complete (Server, Location (renamed from Region), Datacenter (renamed from City))",
                )
            }
        }

    val migration_39_40: Migration =
        object : Migration(39, 40) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Server ADD COLUMN ipv6 INTEGER NOT NULL DEFAULT 0")
                invalidateData()
                logger.debug("Migrated db from version:39 to version:40")
            }
        }

    val migration_40_41: Migration =
        object : Migration(40, 41) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop and recreate UnBlockWgParam with id as primary key
                // Data will be refreshed from API on next fetch
                db.execSQL("DROP TABLE IF EXISTS UnBlockWgParam")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `UnBlockWgParam` " +
                        "(`id` TEXT NOT NULL DEFAULT '', " +
                        "`title` TEXT NOT NULL DEFAULT '', " +
                        "`countries` TEXT NOT NULL DEFAULT '', " +
                        "`Jc` INTEGER NOT NULL DEFAULT 0, " +
                        "`Jmin` INTEGER NOT NULL DEFAULT 0, " +
                        "`Jmax` INTEGER NOT NULL DEFAULT 0, " +
                        "`S1` INTEGER NOT NULL DEFAULT 0, " +
                        "`S2` INTEGER NOT NULL DEFAULT 0, " +
                        "`S3` INTEGER NOT NULL DEFAULT 0, " +
                        "`S4` INTEGER NOT NULL DEFAULT 0, " +
                        "`H1` TEXT NOT NULL DEFAULT '', " +
                        "`H2` TEXT NOT NULL DEFAULT '', " +
                        "`H3` TEXT NOT NULL DEFAULT '', " +
                        "`H4` TEXT NOT NULL DEFAULT '', " +
                        "`I1` TEXT NOT NULL DEFAULT '', " +
                        "`I2` TEXT NOT NULL DEFAULT '', " +
                        "`I3` TEXT NOT NULL DEFAULT '', " +
                        "`I4` TEXT NOT NULL DEFAULT '', " +
                        "`I5` TEXT NOT NULL DEFAULT '', " +
                        "PRIMARY KEY(`id`))",
                )
                logger.debug("Migrated db from version:40 to version:41 - UnBlockWgParam primary key changed to id")
            }
        }

    private fun invalidateData() {
        Windscribe.appContext.preference.migrationRequired = true
    }
}
