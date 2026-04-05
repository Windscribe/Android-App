package com.windscribe.vpn.commonutils

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

object HashUtils {

    /**
     * Generate SHA256 hash from InputStream
     * @param inputStream The input stream to hash
     * @return Hex string of SHA256 hash prefixed with "0x"
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
        return "0x" + hashBytes.joinToString("") { "%02x".format(it) }
    }
}
