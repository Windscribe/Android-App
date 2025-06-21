package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Keep
data class RobertFilterResponse(
    @SerializedName("filters")
    @Expose
    val filters: List<RobertFilter>
)

@Keep
data class RobertFilter(
    @SerializedName("title")
    @Expose
    val title: String,
    @SerializedName("description")
    @Expose
    val description: String,
    @SerializedName("id")
    @Expose
    val id: String,
    @SerializedName("status")
    @Expose
    var status: Int
)