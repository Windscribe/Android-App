/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.model

import com.windscribe.vpn.api.response.PortMapResponse.PortMap
import com.windscribe.vpn.backend.utils.ProtocolConfig

class ProtocolConfigAndPortMap(val protocolConfig: ProtocolConfig, val portMap: PortMap? = null)
