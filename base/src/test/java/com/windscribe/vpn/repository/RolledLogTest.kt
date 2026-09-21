package com.windscribe.vpn.repository

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * The rolled half of a bug report. It must never be the reason an upload fails, so every unreadable
 * shape has to come back empty rather than throw.
 */
class RolledLogTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun zipped(
        name: String,
        content: String,
    ): File =
        folder.newFile(name).also { file ->
            ZipOutputStream(file.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("applog.txt"))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }

    @Test
    fun `a rolled log is returned in full`() {
        val body = "{\"tm\":\"1\",\"msg\":\"one\"}\n{\"tm\":\"2\",\"msg\":\"two\"}\n"
        assertEquals(body, readRolledLog(zipped("rolled.zip", body)))
    }

    @Test
    fun `a missing trailing newline is added so the live log starts on its own line`() {
        assertEquals("no newline\n", readRolledLog(zipped("noeol.zip", "no newline")))
    }

    @Test
    fun `nothing rolled yet is not an error`() {
        assertEquals("", readRolledLog(File(folder.root, "absent.zip")))
    }

    @Test
    fun `a corrupt archive costs the history, not the upload`() {
        val corrupt = folder.newFile("corrupt.zip").apply { writeText("not a zip at all") }
        assertEquals("", readRolledLog(corrupt))
    }

    @Test
    fun `the viewer gets the rolled lines with no blank one at the join`() {
        val body = "{\"tm\":\"1\",\"msg\":\"one\"}\n{\"tm\":\"2\",\"msg\":\"two\"}\n"
        assertEquals(
            listOf("{\"tm\":\"1\",\"msg\":\"one\"}", "{\"tm\":\"2\",\"msg\":\"two\"}"),
            rolledLogLines(zipped("lines.zip", body)),
        )
    }

    @Test
    fun `nothing rolled yet leaves the viewer with the live log alone`() {
        assertEquals(emptyList<String>(), rolledLogLines(File(folder.root, "none.zip")))
    }

    @Test
    fun `an empty archive is not an error`() {
        val empty =
            folder.newFile("empty.zip").also { file ->
                ZipOutputStream(file.outputStream()).use { }
            }
        assertEquals("", readRolledLog(empty))
    }
}
