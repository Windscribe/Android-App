/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.serverlist.entity

/**
 * State representing the server map grouped by datacenter ID.
 * Used for health calculations and server availability checks.
 */
sealed class ServerMapState {
    object Loading : ServerMapState()
    data class Success(val data: Map<Int, List<Server>>) : ServerMapState()
    data class Error(val message: String) : ServerMapState()
}
