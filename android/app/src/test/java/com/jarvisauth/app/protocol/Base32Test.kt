package com.jarvisauth.app.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Base32Test {
    @Test
    fun roundTripMatchesDesktopUnpaddedAlphabet() {
        val payload = """{"serverId":"Ab12CdEf","port":43297,"method":"UDP","encKey":"k"}"""
        val encoded = Base32.encode(payload.toByteArray(Charsets.UTF_8))
        val dashed = Base32.withSeparators(encoded)
        assertEquals(payload, Base32.decode(dashed)!!.toString(Charsets.UTF_8))
        assertEquals(payload, Base32.decode(encoded)!!.toString(Charsets.UTF_8))
    }

    @Test
    fun rejectsInvalidAlphabet() {
        assertNull(Base32.decode("ABC1"))
    }
}
