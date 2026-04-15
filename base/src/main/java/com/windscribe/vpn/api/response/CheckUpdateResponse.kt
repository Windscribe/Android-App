/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Keep
data class CheckUpdateResponse(
    @SerializedName("update_needed_flag")
    @Expose
    val updateNeededFlag: Int = 0,

    @SerializedName("latest_version")
    @Expose
    val latestVersion: String? = null,

    @SerializedName("latest_build")
    @Expose
    val latestBuild: Int? = null,

    @SerializedName("supported")
    @Expose
    val supported: Int = 1,

    @SerializedName("update_url")
    @Expose
    val updateUrl: String? = null
) {
    val isUpdateAvailable: Boolean
        get() = updateNeededFlag == 1
}
