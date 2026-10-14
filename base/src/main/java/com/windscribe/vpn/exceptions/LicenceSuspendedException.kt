/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.exceptions

/**
 * Partner licence suspended. Unlike [InvalidSessionException] this is recoverable, so the licence
 * code is kept and the session retried.
 */
class LicenceSuspendedException(
    message: String?,
) : Exception(message)
