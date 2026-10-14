/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.vpn.repository

import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Three places have to agree on where the log lives: the appender's `file`, its rolled
 * `fileNamePattern`, and the two constants `LogRepository` reads when it uploads. Nothing at
 * runtime notices when they drift — the upload just reads a file nobody is writing.
 */
class LogPathsAgreeTest {
    private fun logbackXml(): String {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            val candidate = File(dir, "base/src/main/assets/logback.xml")
            if (candidate.isFile) return candidate.readText()
            val here = File(dir, "src/main/assets/logback.xml")
            if (here.isFile) return here.readText()
            dir = dir.parentFile
        }
        throw AssertionError("logback.xml not found from ${File("").absolutePath}")
    }

    @Test
    fun `neither path is pinned to our own package or user`() {
        val xml = logbackXml()
        assertTrue(
            "a hard-coded sandbox path breaks partner builds of windscribe-core and secondary users",
            !xml.contains("/data/user/"),
        )
        assertEquals(
            "both the live file and the rolled pattern must come from the host app",
            2,
            Regex("\\$\\{DATA_DIR}").findAll(xml).count(),
        )
    }

    @Test
    fun `the appender writes the file LogRepository uploads`() {
        val file = Regex("<file>\\$\\{DATA_DIR}(.*?)</file>").find(logbackXml())?.groupValues?.get(1)
        assertEquals(PreferencesKeyConstants.DEBUG_LOG_FILE_NAME, file)
    }

    @Test
    fun `the rolled name matches the one LogRepository looks for`() {
        val pattern =
            Regex("<fileNamePattern>\\$\\{DATA_DIR}(.*?)</fileNamePattern>")
                .find(logbackXml())
                ?.groupValues
                ?.get(1)
        // The pattern carries logback's %i where the constant carries the only index maxIndex allows.
        assertEquals(PreferencesKeyConstants.DEBUG_LOG_ROLLED_FILE_NAME, pattern?.replace("%i", "1"))
    }
}
