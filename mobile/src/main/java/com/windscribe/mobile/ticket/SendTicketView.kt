/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.ticket

interface SendTicketView {
    fun addTextChangeListener()
    fun setActivityTitle(title: String)
    fun setEmail(email: String)
    fun setErrorLayout(message: String)
    fun setProgressView(show: Boolean)
    fun setQueryTypeSpinner()
    fun setSendButtonState(enabled: Boolean)
    fun setSuccessLayout(message: String)
}