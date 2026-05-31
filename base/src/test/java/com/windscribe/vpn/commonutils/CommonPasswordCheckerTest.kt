package com.windscribe.vpn.commonutils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommonPasswordCheckerTest {
    @Test
    fun `flags well known weak passwords`() {
        listOf(
            "password",
            "123456",
            "qwerty",
            "iloveyou",
            "monkey",
            "welcome",
            "passw0rd",
        ).forEach {
            assertTrue("Expected '$it' to be flagged as common", CommonPasswordChecker.isAMatch(it))
        }
    }

    @Test
    fun `input is lowercased before matching`() {
        // The checker lowercases the input but compares against the raw list.
        // Any list entry that's already lowercase will match regardless of input casing.
        assertTrue(CommonPasswordChecker.isAMatch("Password"))
        assertTrue(CommonPasswordChecker.isAMatch("PASSWORD"))
        assertTrue(CommonPasswordChecker.isAMatch("PaSsWoRd"))
        assertTrue(CommonPasswordChecker.isAMatch("MONKEY"))
    }

    @Test
    fun `mixed-case list entries are unreachable — documented quirk`() {
        // The list contains "BvtTest123" verbatim (mixed case). Since the checker
        // always lowercases the input, no possible input can produce the exact
        // string "BvtTest123" to match against — the entry is effectively dead.
        // Lock in that quirk so a future refactor of the list (or a switch to
        // case-insensitive comparison) surfaces here.
        assertFalse(CommonPasswordChecker.isAMatch("bvttest123"))
        assertFalse(CommonPasswordChecker.isAMatch("BvtTest123"))
        assertFalse(CommonPasswordChecker.isAMatch("BVTTEST123"))
    }

    @Test
    fun `does not flag strong passwords`() {
        listOf(
            "n8H!q2Lp\$wxR7v",
            "correct-horse-battery-staple",
            "windscribe-rocks-2026",
            "T#3Quick&Brown-Fox",
            "ZxC!asd789-PoIuYt",
        ).forEach {
            assertFalse("Expected '$it' NOT to be flagged", CommonPasswordChecker.isAMatch(it))
        }
    }

    @Test
    fun `does not flag near-miss variants`() {
        // Common-password match is exact (after lowercasing) — variations should pass.
        assertFalse(CommonPasswordChecker.isAMatch("password!"))
        assertFalse(CommonPasswordChecker.isAMatch("password "))
        assertFalse(CommonPasswordChecker.isAMatch(" password"))
        assertFalse(CommonPasswordChecker.isAMatch("password2"))
    }

    @Test
    fun `empty string is not flagged`() {
        assertFalse(CommonPasswordChecker.isAMatch(""))
    }
}
