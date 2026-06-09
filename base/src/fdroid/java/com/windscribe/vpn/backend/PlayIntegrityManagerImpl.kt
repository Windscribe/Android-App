package com.windscribe.vpn.backend

import org.slf4j.LoggerFactory

/**
 * F-Droid flavor stub implementation of PlayIntegrityManager.
 * Play Integrity API is not available on F-Droid builds.
 */
class PlayIntegrityManagerImpl : PlayIntegrityManager {
    private val logger = LoggerFactory.getLogger("play-integrity")

    init {
        logger.debug("Play Integrity not available in F-Droid build")
    }

    override suspend fun requestIntegrityToken(): String? {
        logger.debug("Play Integrity token requested but not available in F-Droid build")
        return null
    }

    override fun isAvailable(): Boolean = false
}
