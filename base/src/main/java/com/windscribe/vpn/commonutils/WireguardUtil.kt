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
        val prefixLength: Int,
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
        val prefixLength =
            parts[1].toIntOrNull()
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
        val publicKeyBytes =
            try {
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
    fun generateIPFromHash(
        cidrBlock: CIDRBlock,
        hashValue: Int,
    ): Int =
        (cidrBlock.networkAddress and cidrBlock.networkMask) or
            (hashValue and cidrBlock.hostMask)

    /**
     * Generates a WireGuard LAN IP address from a public key and CIDR block.
     * This is the main entry point that combines all the steps:
     * 1. Parse CIDR notation
     * 2. Hash the public key
     * 3. Generate IP within the CIDR range
     *
     * @param publicKeyBase64 Base64-encoded WireGuard public key
     * @param cidr CIDR notation string (e.g., "100.64.0.0/10")
     * @return IP address string within the specified CIDR range with /32 suffix
     * @throws IllegalArgumentException if inputs are invalid
     */
    fun generateWireguardIP(
        publicKeyBase64: String,
        cidr: String,
    ): String {
        val cidrBlock = parseCIDR(cidr)
        val hashValue = hashPublicKey(publicKeyBase64)
        val ipInt = generateIPFromHash(cidrBlock, hashValue)
        return "${ipIntToString(ipInt)}/32"
    }

    /**
     * Determines the IPv6 LAN IP for a WireGuard connection within the configured
     * CIDR prefix (defaults to fd54:0004::/64).
     *
     * The allocation is deterministic based on the WireGuard public key:
     * - SHA-256 hash of the public key is computed
     * - First 8 bytes of the hash are used as the interface identifier (last 64 bits)
     * - This provides 2^64 possible unique /128 addresses within the /64 prefix
     * - Same public key always gets the same IPv6 address
     * - Different public keys have negligible collision probability
     *
     * Returns a single IPv6 address (without /128 suffix) that represents a /128 allocation.
     *
     * @param publicKeyBase64 Base64-encoded WireGuard public key
     * @param cidr IPv6 CIDR notation string (e.g., "fd54:0004::/64")
     * @return IPv6 address string (without /128 suffix)
     * @throws IllegalArgumentException if inputs are invalid or prefix length > 64
     */
    fun generateWireguardIPv6(
        publicKeyBase64: String,
        cidr: String,
    ): String {
        // Parse the IPv6 CIDR prefix
        val parts = cidr.split("/")
        require(parts.size == 2) { "Invalid IPv6 CIDR format: $cidr" }

        val prefixAddr = parts[0]
        val prefixLength =
            parts[1].toIntOrNull()
                ?: throw IllegalArgumentException("Invalid prefix length in IPv6 CIDR: $cidr")

        require(prefixLength in 0..128) {
            "Invalid IPv6 prefix length: $prefixLength (must be 0-128)"
        }

        // Validate prefix length (must be /64 or larger to support unique /128 allocations)
        require(prefixLength <= 64) {
            "WireGuard LAN prefix $cidr must be /64 or larger (smaller prefix length) to support unique /128 allocations"
        }

        // Decode the base64 public key
        val publicKeyBytes =
            try {
                Base64.getDecoder().decode(publicKeyBase64)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "Failed to decode base64 public key: $publicKeyBase64",
                    e,
                )
            }

        // Generate SHA-256 hash of the decoded public key
        val hashBytes = MessageDigest.getInstance("SHA-256").digest(publicKeyBytes)

        // Extract first 8 bytes for the interface identifier
        val interfaceId = hashBytes.copyOfRange(0, 8)

        // Parse the prefix address to a 16-byte binary representation
        val prefixBytes =
            try {
                java.net.InetAddress
                    .getByName(prefixAddr)
                    .address
            } catch (e: Exception) {
                throw IllegalArgumentException("Failed to parse IPv6 address: $prefixAddr", e)
            }
        require(prefixBytes.size == 16) {
            "Invalid IPv6 address: $prefixAddr (must be an IPv6 address)"
        }
        // Set the interface identifier (last 64 bits / bytes 8-15) from the hash
        val ipBytes = prefixBytes.copyOf()
        for (i in 0 until 8) {
            ipBytes[8 + i] = interfaceId[i]
        }
        // Convert back to a human-readable IPv6 address
        val ipv6Address =
            try {
                java.net.InetAddress.getByAddress(ipBytes)
            } catch (e: Exception) {
                throw IllegalArgumentException("Failed to construct IPv6 address from public key", e)
            }
        return ipv6Address.hostAddress
            ?: throw IllegalArgumentException("Failed to format IPv6 address")
    }
}
