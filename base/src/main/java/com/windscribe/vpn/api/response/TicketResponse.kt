/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Keep
class TicketResponse {
    @SerializedName("status")
    @Expose
    val status: String? = null

    @SerializedName("ticket_id")
    @Expose
    val ticketId: String? = null
}
