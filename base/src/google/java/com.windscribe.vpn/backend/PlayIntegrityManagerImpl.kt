package com.windscribe.vpn.backend

import android.util.Base64
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.windscribe.vpn.BuildConfig
import com.windscribe.vpn.Windscribe.Companion.appContext
import kotlinx.coroutines.tasks.await
import org.slf4j.LoggerFactory
import java.security.SecureRandom

/**
 * Google Play flavor implementation of PlayIntegrityManager.
 * Uses Play Integrity API to generate attestation tokens.
 */
class PlayIntegrityManagerImpl : PlayIntegrityManager {
    private val logger = LoggerFactory.getLogger("play-integrity")
    private val integrityManager = IntegrityManagerFactory.create(appContext)

    private val cloudProjectNumber = BuildConfig.CLOUD_PROJECT_NUMBER.takeIf { it.isNotBlank() }

    override suspend fun requestIntegrityToken(): String? {
        if (cloudProjectNumber == null) {
            logger.warn("Cloud project number not configured, skipping integrity token request")
            return null
        }

        return try {
            logger.debug("Requesting Play Integrity token...")
            // Create nonce (optional but recommended for better security)
            val nonce = generateNonce()

            val integrityTokenResponse =
                integrityManager
                    .requestIntegrityToken(
                        IntegrityTokenRequest
                            .builder()
                            .setCloudProjectNumber(cloudProjectNumber.toLong())
                            .setNonce(nonce)
                            .build(),
                    ).await()

            val token = integrityTokenResponse.token()
            logger.debug("Successfully obtained Play Integrity token")
            token
        } catch (e: Exception) {
            logger.error("Failed to request integrity token: ${e.message}", e)
            null
        }
    }

    override fun isAvailable(): Boolean = cloudProjectNumber != null

    /**
     * Generate a cryptographically secure nonce for the integrity token request.
     * Uses SecureRandom to generate 32 bytes of entropy, ensuring unpredictability
     * and preventing replay attacks. Server-side should validate this nonce.
     */
    private fun generateNonce(): String {
        val nonceBytes = ByteArray(32) // 256-bit nonce
        SecureRandom().nextBytes(nonceBytes)
        return Base64.encodeToString(nonceBytes, Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
