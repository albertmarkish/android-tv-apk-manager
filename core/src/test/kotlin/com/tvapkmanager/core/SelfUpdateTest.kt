package com.tvapkmanager.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SelfUpdateTest {

    @Test
    fun newerPatchMinorAndMajorCount() {
        assertTrue(isNewerVersion("0.2.1", "0.2.0"))
        assertTrue(isNewerVersion("0.3.0", "0.2.9"))
        assertTrue(isNewerVersion("1.0.0", "0.9.9"))
    }

    @Test
    fun comparesNumbersNotText() {
        assertTrue(isNewerVersion("0.10.0", "0.9.0"))
        assertFalse(isNewerVersion("0.9.0", "0.10.0"))
    }

    @Test
    fun sameOrOlderIsNotAnUpdate() {
        assertFalse(isNewerVersion("0.2.0", "0.2.0"))
        assertFalse(isNewerVersion("0.2", "0.2.0"))
        assertFalse(isNewerVersion("0.1.9", "0.2.0"))
    }

    @Test
    fun acceptsTagsAndSuffixes() {
        assertTrue(isNewerVersion("v0.3.0", "0.2.1"))
        assertTrue(isNewerVersion("0.3.0", "0.2.1-debug"))
        assertEquals("0.3.0", versionFromTag("v0.3.0"))
    }

    @Test
    fun unreadableReleaseIsNeverAnUpdate() {
        assertFalse(isNewerVersion("latest", "0.2.0"))
        assertTrue(isNewerVersion("0.2.0", "unknown"))
    }
}
