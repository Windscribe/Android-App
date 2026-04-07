package com.windscribe.vpn.commonutils

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

object HashUtils {

    /**
     * Generate SHA256 hash from InputStream (truncated to 128 bits)
     * @param inputStream The input stream to hash
     * @return Hex string of truncated SHA256 hash (32 hex chars) prefixed with "0x" = 34 chars total
     */
    fun sha256FromInputStream(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var read: Int

        inputStream.use { stream ->
            while (stream.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }

        val hashBytes = digest.digest()
        // Truncate to first 16 bytes (128 bits = 32 hex characters)
        return "0x" + hashBytes.take(16).joinToString("") { "%02x".format(it) }
    }
}
