package com.jarvisauth.app.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProtocolJsonTest {
    @Test
    fun preservesPcbuFieldNames() {
        val json =
            """{"deviceId":"abc","pcbuIP":"192.168.1.10","pcbuPort":43296,"isManual":false}"""
        val packet = ProtocolJson.decode<PacketUdpBroadcast>(json)!!
        assertEquals("192.168.1.10", packet.pcbuIP)
        assertEquals(43296, packet.pcbuPort)
        assertEquals(json.replace(" ", ""), ProtocolJson.encode(packet).replace(" ", ""))
    }

    @Test
    fun udpBroadcastIgnoresUnknownKeysAndParsesTsIP() {
        val json =
            """{"deviceId":"abc","pcbuIP":"192.168.1.10","pcbuPort":43296,"isManual":false,"tsIP":"100.64.1.2","extra":1}"""
        val packet = ProtocolJson.decode<PacketUdpBroadcast>(json)!!
        assertEquals("192.168.1.10", packet.pcbuIP)
        assertEquals("100.64.1.2", packet.tsIP)
    }

    @Test
    fun emptyTsIPIsOmittedOnEncode() {
        val json = ProtocolJson.encode(PacketUdpBroadcast(deviceId = "abc", pcbuIP = "10.0.0.1", pcbuPort = 1, isManual = false))
        assertEquals(false, json.contains("tsIP"))
    }

    @Test
    fun pairInitRoundTrip() {
        val init =
            PacketPairInit(
                protoVersion = "4.0.0",
                deviceUUID = "uuid-1",
                deviceName = "Pixel",
                ipAddress = "10.0.0.2",
                tcpPort = 43310,
                udpPort = 43311,
                udpManualPort = 43312,
                cloudToken = "",
            )
        val parsed = ProtocolJson.decode<PacketPairInit>(ProtocolJson.encode(init))!!
        assertEquals(init, parsed)
    }

    @Test
    fun pairingMethodWireNames() {
        assertEquals(PairingMethod.UDP, PairingMethod.fromWire("UDP"))
        assertEquals(PairingMethod.TCP, PairingMethod.fromWire("TCP"))
        assertEquals(PairingMethod.BLUETOOTH, PairingMethod.fromWire("BLUETOOTH"))
        assertEquals(PairingMethod.MANUAL_UDP, PairingMethod.fromWire("MANUAL_UDP"))
        assertNull(PairingMethod.fromWire("CLOUD_TCP"))
    }

    @Test
    fun unlockResponseKeepsEmptyErrorOnSuccess() {
        val json = ProtocolJson.encode(PacketUnlockResponse(error = "", encData = "AABB"))
        val parsed = ProtocolJson.decode<PacketUnlockResponse>(json)!!
        assertEquals("", parsed.error)
        assertEquals("AABB", parsed.encData)
        assertEquals("", parsed.tsIP)
    }
}
