/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.help

interface HelpView {
    fun goToSendTicket()
    fun openInBrowser(url: String)
    fun setActivityTitle(title: String)
    fun showProgress(inProgress: Boolean, success: Boolean)
    fun showToast(message: String)
    fun setSendTicketVisibility(visible: Boolean)
}