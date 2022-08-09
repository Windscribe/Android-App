/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.ticket

import android.content.Context
import com.windscribe.vpn.api.response.QueryType

interface SendTicketPresenter {
    fun init()
    fun onInputChanged(email: String, subject: String, message: String)
    fun onQueryTypeSelected(queryType: QueryType)
    fun onSendTicketClicked(email: String, subject: String, message: String)
    fun setTheme(context: Context)
}