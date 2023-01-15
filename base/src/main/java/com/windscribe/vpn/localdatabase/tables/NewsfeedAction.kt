/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase.tables

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class NewsfeedAction(
    @SerializedName("pcpid")
    @Expose
    var pcpID: String,

    @SerializedName("promo_code")
    @Expose
    var promoCode: String,

    @SerializedName("type")
    @Expose
    var type: String,

    @SerializedName("label")
    @Expose
    var label: String
)
