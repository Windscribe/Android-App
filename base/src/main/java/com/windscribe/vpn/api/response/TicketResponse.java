/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class TicketResponse {

    @SerializedName("status")
    @Expose
    private String status;

    @SerializedName("ticket_id")
    @Expose
    private String ticketId;

    public String getTicketId() {
        return ticketId;
    }
}
