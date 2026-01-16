package com.windscribe.vpn.commonutils

import java.security.MessageDigest
import java.util.Base64

/**
 * Utility object for WireGuard IP address generation and CIDR operations.
 * Provides functions to generate deterministic IP addresses within a CIDR range
 * based on hashing public keys.
 */
object WireguardUtil {

    /**
     * Represents a parsed CIDR notation with network address and prefix length.
     *
     * @property networkAddress The base network address as a 32-bit integer
     * @property prefixLength The number of network bits (0-32)
     */
    data class CIDRBlock(
        val networkAddress: Int,
        val prefixLength: Int
    ) {
        val hostBits: Int get() = 32 - prefixLength
        val hostMask: Int get() = if (hostBits == 32) -1 else (1 shl hostBits) - 1
        val networkMask: Int get() = hostMask.inv()
    }

    /**
     * Parses CIDR notation string into a CIDRBlock.
     *
     * @param cidr CIDR notation string (e.g., "100.64.0.0/10")
     * @return CIDRBlock containing network address and prefix length
     * @throws IllegalArgumentException if CIDR format is invalid
     */
    fun parseCIDR(cidr: String): CIDRBlock {
        val parts = cidr.split("/")
        require(parts.size == 2) { "Invalid CIDR format: $cidr (expected format: x.x.x.x/n)" }

        val baseIp = parts[0]
        val prefixLength = parts[1].toIntOrNull()
            ?: throw IllegalArgumentException("Invalid prefix length in CIDR: $cidr")

        require(prefixLength in 0..32) {
            "Invalid prefix length: $prefixLength (must be 0-32)"
        }

        val networkAddress = ipStringToInt(baseIp)
        return CIDRBlock(networkAddress, prefixLength)
    }

    /**
     * Converts an IP address string to a 32-bit integer.
     *
     * @param ip IP address string (e.g., "100.64.0.0")
     * @return 32-bit integer representation of the IP address
     * @throws IllegalArgumentException if IP format is invalid
     */
    fun ipStringToInt(ip: String): Int {
        val parts = ip.split(".")
        require(parts.size == 4) { "Invalid IP address format: $ip (expected 4 octets)" }

        return try {
            val octets = parts.map { it.toInt() }
            octets.forEach { octet ->
                require(octet in 0..255) {
                    "Invalid IP address octet value: $octet (must be 0-255) in $ip"
                }
            }
            ((octets[0] and 0xFF) shl 24) or
            ((octets[1] and 0xFF) shl 16) or
            ((octets[2] and 0xFF) shl 8) or
            (octets[3] and 0xFF)
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid IP address octets: $ip", e)
        }
    }

    /**
     * Converts a 32-bit integer to an IP address string.
     *
     * @param ip 32-bit integer representation of IP address
     * @return IP address string (e.g., "100.64.0.0")
     */
    fun ipIntToString(ip: Int): String {
        val octet1 = (ip shr 24) and 0xFF
        val octet2 = (ip shr 16) and 0xFF
        val octet3 = (ip shr 8) and 0xFF
        val octet4 = ip and 0xFF
        return "$octet1.$octet2.$octet3.$octet4"
    }

    /**
     * Hashes a Base64-encoded WireGuard public key using SHA-256.
     *
     * @param publicKeyBase64 Base64-encoded WireGuard public key
     * @return 32-bit hash value derived from first 4 bytes of SHA-256 digest
     * @throws IllegalArgumentException if public key is not valid Base64
     */
    fun hashPublicKey(publicKeyBase64: String): Int {
        val publicKeyBytes = try {
            Base64.getDecoder().decode(publicKeyBase64)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid Base64 public key: $publicKeyBase64", e)
        }

        val digest = MessageDigest.getInstance("SHA-256").digest(publicKeyBytes)
        return ((digest[0].toInt() and 0xFF) shl 24) or
               ((digest[1].toInt() and 0xFF) shl 16) or
               ((digest[2].toInt() and 0xFF) shl 8) or
               (digest[3].toInt() and 0xFF)
    }

    /**
     * Generates an IP address within a CIDR block based on a hash value.
     * The network portion comes from the CIDR block, and the host portion
     * comes from the hash value.
     *
     * @param cidrBlock The CIDR block to generate IP within
     * @param hashValue Hash value to use for host portion
     * @return IP address as a 32-bit integer
     */
    fun generateIPFromHash(cidrBlock: CIDRBlock, hashValue: Int): Int {
        return (cidrBlock.networkAddress and cidrBlock.networkMask) or
               (hashValue and cidrBlock.hostMask)
    }

    /**
     * Generates a WireGuard LAN IP address from a public key and CIDR block.
     * This is the main entry point that combines all the steps:
     * 1. Parse CIDR notation
     * 2. Hash the public key
     * 3. Generate IP within the CIDR range
     *
     * @param publicKeyBase64 Base64-encoded WireGuard public key
     * @param cidr CIDR notation string (e.g., "100.64.0.0/10")
     * @return IP address string within the specified CIDR range
     * @throws IllegalArgumentException if inputs are invalid
     */
    fun generateWireguardIP(publicKeyBase64: String, cidr: String): String {
        val cidrBlock = parseCIDR(cidr)
        val hashValue = hashPublicKey(publicKeyBase64)
        val ipInt = generateIPFromHash(cidrBlock, hashValue)
        return ipIntToString(ipInt)
    }
}
