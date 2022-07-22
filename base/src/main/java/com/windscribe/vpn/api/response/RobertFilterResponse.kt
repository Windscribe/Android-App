package com.windscribe.vpn.api.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class RobertFilterResponse(@SerializedName("filters")
                                @Expose
                                val filters: List<RobertFilter>)

data class RobertFilter(@SerializedName("title")
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