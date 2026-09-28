package com.jarvisauth.app.domain

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppLogTest {
    @Test
    fun redactsSecretFields() {
        val raw = """unlock ok passwordKey=super-secret encData=deadbeef encKey=qr"""
        val redacted = AppLog.redact(raw)
        assertFalse(redacted.contains("super-secret"))
        assertFalse(redacted.contains("deadbeef"))
        assertFalse(redacted.contains("qr"))
        assertEquals(true, redacted.contains("[redacted]"))
    }

    @Test
    fun ringDropsOldestAndKeepsSafeLines() =
        runBlocking {
            val log = AppLog(maxLines = 2)
            log.append("one")
            log.append("two")
            log.append("three")
            assertEquals(listOf("two", "three"), log.lines.value)
        }
}
