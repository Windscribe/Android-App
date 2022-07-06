package com.windscribe.vpn.api

import com.google.common.hash.HashCode
import com.google.common.hash.Hashing
import com.windscribe.vpn.BuildConfig
import com.windscribe.vpn.constants.NetworkKeyConstants.BACKUP_HASH_CHAR_SET
import java.nio.charset.Charset
import java.util.Calendar

object HashDomainGenerator {
    /**
     * Creates hashed domains for backup api calls
     * @param urlHost url host
     * @return list of 3 backup hash domains.
     */
    fun create(urlHost: String): List<String> {
        val backupApiEndpointList = mutableListOf<String>()
        var backUpAPIEndPoint: String
        var backUpUrl: String
        var hashCode: HashCode
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        for (i in 1..3) {
            backUpAPIEndPoint = (
                    BuildConfig.BACKUP_API_ENDPOINT_STRING + i +
                            (calendar[Calendar.MONTH] + 1) + calendar[Calendar.YEAR]
                    )
            hashCode = Hashing.sha1()
                    .hashString(
                            backUpAPIEndPoint,
                            Charset.forName(BACKUP_HASH_CHAR_SET)
                    )
            backUpUrl = "$urlHost$hashCode.com"
            backupApiEndpointList.add(backUpUrl)
        }
        return backupApiEndpointList
    }
}
