/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.listeners

import com.windscribe.vpn.backend.utils.ProtocolConfig

interface ProtocolClickListener {

    fun onProtocolSelected(protocolConfig: ProtocolConfig?)
}
