package com.windscribe.vpn.api.response

import com.google.gson.annotations.SerializedName

data class UnblockWgResponse(
    @SerializedName("params")
    val params: List<UnblockWgParams>
)

data class UnblockWgParams(
    val title: String,
    val countries: List<String>,
    @SerializedName("Jc")
    val jc: Int?,
    @SerializedName("Jmin")
    val jMin: Int?,
    @SerializedName("Jmax")
    val jMax: Int?,
    @SerializedName("S1")
    val s1: Int?,
    @SerializedName("S2")
    val s2: Int?,
    @SerializedName("S3")
    val s3: Int?,
    @SerializedName("S4")
    val s4: Int?,
    @SerializedName("H1")
    val h1: String?,
    @SerializedName("H2")
    val h2: String?,
    @SerializedName("H3")
    val h3: String?,
    @SerializedName("H4")
    val h4: String?,
    @SerializedName("I1")
    val i1: String?,
    @SerializedName("I2")
    val i2: String?,
    @SerializedName("I3")
    val i3: String?,
    @SerializedName("I4")
    val i4: String?,
    @SerializedName("I5")
    val i5: String?
)
