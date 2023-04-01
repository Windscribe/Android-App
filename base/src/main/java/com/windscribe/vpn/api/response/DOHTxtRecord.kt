package com.windscribe.vpn.api.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class DOHTxtRecord(
    @SerializedName("Answer") @Expose val answer: List<TxtAnswer>
)

data class TxtAnswer(
    @SerializedName("name") @Expose val name: String,
    @SerializedName("type") @Expose val type: Int,
    @SerializedName("TTL") @Expose val ttl: Int,
    @SerializedName("data") @Expose val data: String
) : java.io.Serializable