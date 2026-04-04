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
    UnderMaintenance
}

/**
 * Helper object to determine datacenter status based on various conditions.
 */
object DatacenterStatusHelper {

    /**
     * Determines the status of a datacenter based on status field and server count.
     *
     * Logic:
     * - UnderMaintenance: status == 2 (maintenance) AND no servers available
     * - Pro: status == 1 (enabled) AND no servers available
     * - Available: Has servers
     *
     * @param datacenter The datacenter to check
     * @param serverCount Number of servers available in this datacenter
     * @return The determined status
     */
    fun getStatus(datacenter: Datacenter, serverCount: Int): DatacenterStatus {
        val hasServers = serverCount > 0

        return when {
            // Under maintenance: status 2 with no servers
            datacenter.status == 2 && !hasServers -> DatacenterStatus.UnderMaintenance

            // Pro required: enabled but no servers available
            datacenter.status == 1 && !hasServers -> DatacenterStatus.Pro

            // Available: has servers
            else -> DatacenterStatus.Available
        }
    }

    /**
     * Checks if a datacenter is available for connection.
     *
     * @param datacenter The datacenter to check
     * @param serverCount Number of servers available
     * @return true if available, false otherwise
     */
    fun isAvailable(datacenter: Datacenter, serverCount: Int): Boolean {
        return getStatus(datacenter, serverCount) == DatacenterStatus.Available
    }

    /**
     * Checks if a datacenter requires Pro subscription.
     *
     * @param datacenter The datacenter to check
     * @param serverCount Number of servers available
     * @return true if Pro required, false otherwise
     */
    fun requiresPro(datacenter: Datacenter, serverCount: Int): Boolean {
        return getStatus(datacenter, serverCount) == DatacenterStatus.Pro
    }

    /**
     * Checks if a datacenter is under maintenance.
     *
     * @param datacenter The datacenter to check
     * @param serverCount Number of servers available
     * @return true if under maintenance, false otherwise
     */
    fun isUnderMaintenance(datacenter: Datacenter, serverCount: Int): Boolean {
        return getStatus(datacenter, serverCount) == DatacenterStatus.UnderMaintenance
    }
}
