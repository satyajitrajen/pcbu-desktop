package com.jarvisauth.app.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingQrParserTest {
    @Test
    fun parsesCurrentQr() {
        val json = """{"serverId":"Ab12CdEf","port":43297,"method":"UDP","encKey":"secret-key"}"""
        val parsed = PairingQrParser.parse(json)!!
        assertEquals("Ab12CdEf", parsed.serverId)
        assertEquals(43297, parsed.port)
        assertEquals("UDP", parsed.method)
        assertEquals("secret-key", parsed.encKey)
        assertEquals("", parsed.ip)
    }

    @Test
    fun parsesLegacyQr() {
        val json = """{"ip":"192.168.1.20","port":43295,"method":"TCP","encKey":"legacy-key"}"""
        val parsed = PairingQrParser.parse(json)!!
        assertEquals("192.168.1.20", parsed.ip)
        assertEquals(43295, parsed.port)
        assertEquals("", parsed.serverId)
    }

    @Test
    fun parsesDashedBase32Code() {
        val json = """{"serverId":"sid","port":43297,"method":"BLUETOOTH","encKey":"k"}"""
        val code = Base32.withSeparators(Base32.encode(json.toByteArray(Charsets.UTF_8)))
        val parsed = PairingQrParser.parse(code)!!
        assertEquals("sid", parsed.serverId)
        assertEquals("BLUETOOTH", parsed.method)
    }

    @Test
    fun rejectsMissingEncKeyAndCloudFlag() {
        assertNull(PairingQrParser.parse("""{"serverId":"s","port":43297,"method":"UDP"}"""))
        assertTrue(PairingQrParser.isCloudMethod("CLOUD_TCP"))
        assertTrue(PairingQrParser.isCloudMethod("CLOUD_BT"))
        assertFalse(PairingQrParser.isCloudMethod("UDP"))
    }
}
