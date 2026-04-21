/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Server inventory delta response returned inline with GET /Session
 * when inv_rev parameter is supplied
 */
@Keep
data class ServerInventory(
    @SerializedName("action")
    @Expose
    val action: String,  // "delta" or "hold"

    @SerializedName("enabled")
    @Expose
    val enabled: List<ServerData>?,

    @SerializedName("disabled")
    @Expose
    val disabled: List<DisabledServer>?,

    @SerializedName("revision")
    @Expose
    val revision: Long,

    @SerializedName("amneziawg_config_id")
    @Expose
    val amneziaWgConfigId: String? = null
)

@Keep
data class DisabledServer(
    @SerializedName("id")
    @Expose
    val id: Int
)