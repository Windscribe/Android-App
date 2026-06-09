package com.windscribe.vpn.backend

/**
 * Interface for managing Play Integrity API token generation.
 * Provides device attestation tokens for API security.
 */
interface PlayIntegrityManager {
    /**
     * Request a Play Integrity token.
     * @return The integrity token string, or null if token generation fails or is not available
     */
    suspend fun requestIntegrityToken(): String?

    /**
     * Check if Play Integrity API is available on this device
     * @return true if available, false otherwise
     */
    fun isAvailable(): Boolean
}
