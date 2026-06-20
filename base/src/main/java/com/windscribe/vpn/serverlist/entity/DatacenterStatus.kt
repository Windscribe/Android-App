/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.serverlist.entity

/**
 * Represents the availability status of a datacenter/location.
 */
enum class DatacenterStatus {
    /**
     * Location is available for connection.
     * Has active servers and is operational.
     */
    Available,

    /**
     * Location requires Pro subscription.
     * Either marked as pro-only or has no servers but is enabled.
     */
    Pro,

    /**
     * Location is under maintenance.
     * Status is set to maintenance (2) and has no active servers.
     */
    UnderMaintenance,
}

/**
 * Helper object to determine datacenter status based on various conditions.
 */
object DatacenterStatusHelper {
    fun getStatus(
        datacenter: Datacenter,
        serverCount: Int,
        isPro: Boolean,
        hasAlcAccess: Boolean = false,
    ): DatacenterStatus {
        val hasAtLeastOneServer = serverCount > 0
        val isEnabled = datacenter.status == 1
        val hasAccess = isPro || hasAlcAccess
        val isPremiumLocation = datacenter.pro == 1

        return when {
            // Enabled only when the location is active (status == 1) and has at least one server in inventory.
            isEnabled && hasAtLeastOneServer -> DatacenterStatus.Available
            // User without access (no Pro/ALC) on a premium location: prompt upgrade.
            !hasAccess && isPremiumLocation -> DatacenterStatus.Pro
            // Everything else disabled: down/empty location, or access user with no servers.
            else -> DatacenterStatus.UnderMaintenance
        }
    }

    /**
     * Checks if a datacenter is available for connection.
     *
     * @param datacenter The datacenter to check
     * @param serverCount Number of servers available
     * @param isPro Whether the user has Pro subscription
     * @return true if available, false otherwise
     */
    fun isAvailable(
        datacenter: Datacenter,
        serverCount: Int,
        isPro: Boolean,
        hasAlcAccess: Boolean = false,
    ): Boolean = getStatus(datacenter, serverCount, isPro, hasAlcAccess) == DatacenterStatus.Available

    /**
     * Checks if a datacenter requires Pro subscription.
     *
     * @param datacenter The datacenter to check
     * @param serverCount Number of servers available
     * @param isPro Whether the user has Pro subscription
     * @return true if Pro required, false otherwise
     */
    fun requiresPro(
        datacenter: Datacenter,
        serverCount: Int,
        isPro: Boolean,
        hasAlcAccess: Boolean = false,
    ): Boolean = getStatus(datacenter, serverCount, isPro, hasAlcAccess) == DatacenterStatus.Pro

    /**
     * Checks if a datacenter is under maintenance.
     *
     * @param datacenter The datacenter to check
     * @param serverCount Number of servers available
     * @param isPro Whether the user has Pro subscription
     * @return true if under maintenance, false otherwise
     */
    fun isUnderMaintenance(
        datacenter: Datacenter,
        serverCount: Int,
        isPro: Boolean,
        hasAlcAccess: Boolean = false,
    ): Boolean = getStatus(datacenter, serverCount, isPro, hasAlcAccess) == DatacenterStatus.UnderMaintenance
}
