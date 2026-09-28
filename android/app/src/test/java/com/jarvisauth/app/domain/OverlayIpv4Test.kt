package com.jarvisauth.app.domain

import com.jarvisauth.app.protocol.PairingMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayIpv4Test {
    private val pairing =
        Pairing(
            deviceId = "pc-1",
            encryptionKey = "enc",
            passwordKey = "pw",
            pairingMethod = PairingMethod.UDP,
            pcName = "PC",
            pcOs = "Windows",
            userName = "satya",
            unlockServerPort = 43296,
            pairedAtEpochMs = 1L,
            lastIpv4 = "192.168.1.7",
        )

    @Test
    fun tailscaleCgnatIsOverlay() {
        assertTrue(OverlayIpv4.isOverlay("100.64.0.1"))
        assertTrue(OverlayIpv4.isOverlay("100.127.255.254"))
        assertFalse(OverlayIpv4.isOverlay("100.63.0.1"))
        assertFalse(OverlayIpv4.isOverlay("100.128.0.1"))
        assertFalse(OverlayIpv4.isOverlay("192.168.1.7"))
        assertFalse(OverlayIpv4.isOverlay("10.0.0.2"))
    }

    @Test
    fun rememberOverlayDoesNotClobberLan() {
        val next = pairing.rememberAddress("100.64.1.9")!!
        assertEquals("192.168.1.7", next.lastIpv4)
        assertEquals("100.64.1.9", next.lastTailscaleIpv4)
    }

    @Test
    fun rememberLanDoesNotClobberOverlay() {
        val withTs = pairing.copy(lastTailscaleIpv4 = "100.64.1.9")
        val next = withTs.rememberAddress("10.0.0.8")!!
        assertEquals("10.0.0.8", next.lastIpv4)
        assertEquals("100.64.1.9", next.lastTailscaleIpv4)
    }

    @Test
    fun lanClassifierExcludesOverlay() {
        assertTrue(OverlayIpv4.isLan("192.168.1.7"))
        assertTrue(OverlayIpv4.isLan("10.0.0.2"))
        assertTrue(OverlayIpv4.isLan("172.16.0.1"))
        assertTrue(OverlayIpv4.isLan("172.31.255.1"))
        assertFalse(OverlayIpv4.isLan("172.15.0.1"))
        assertFalse(OverlayIpv4.isLan("100.64.1.9"))
        assertFalse(OverlayIpv4.isLan("8.8.8.8"))
    }

    @Test
    fun localPicksOverlayFromInjectedAddresses() {
        val addrs = sequenceOf("192.168.1.7", "100.64.8.2", "10.0.0.4")
        assertEquals("100.64.8.2", OverlayIpv4.local(addrs))
        assertEquals("192.168.1.7", OverlayIpv4.localLan(addrs))
        assertNull(OverlayIpv4.local(sequenceOf("10.0.0.1")))
    }

    @Test
    fun invalidAddressIsIgnored() {
        assertNull(pairing.rememberAddress("not-an-ip"))
        assertNull(pairing.rememberAddress("100.64.1"))
    }
}
