/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Keep
class PortMapResponse {
    @SerializedName("portmap")
    @Expose
    val portmap: List<PortMap>? = null

    @SerializedName("suggested")
    @Expose
    val suggested: Suggested? = null

    val isProtocolSuggested: Boolean
        get() = suggested != null

    override fun toString(): String =
        "PortMapResponse{" +
            ", portmap=" + portmap +
            ", suggested=" + suggested +
            '}'

    @Keep
    class PortMap {
        @SerializedName("legacy_ports")
        @Expose
        val legacyPorts: List<String>? = null

        @SerializedName("ports")
        @Expose
        val ports: List<String>? = null

        @SerializedName("heading")
        @Expose
        val heading: String? = null

        @SerializedName("protocol")
        @Expose
        val protocol: String? = null

        @SerializedName("use")
        @Expose
        val use: String? = null

        override fun toString(): String =
            "PortMap{" +
                "protocol='" + protocol + '\'' +
                ", heading='" + heading + '\'' +
                ", use='" + use + '\'' +
                ", ports=" + ports +
                ", legacyPorts=" + legacyPorts +
                '}'
    }

    @Keep
    class Suggested {
        @SerializedName("port")
        @Expose
        val port: Int = 0

        @SerializedName("protocol")
        @Expose
        val protocol: String? = null

        override fun toString(): String =
            "Suggested{" +
                "protocol='" + protocol + '\'' +
                ", port='" + port + '\'' +
                '}'
    }
}
