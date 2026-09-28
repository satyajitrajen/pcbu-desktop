package com.jarvisauth.app.domain

import com.jarvisauth.app.protocol.PacketPairInit
import com.jarvisauth.app.protocol.PacketPairResponse
import com.jarvisauth.app.protocol.PacketPairResponseData
import com.jarvisauth.app.protocol.PairingMethod
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PairPcTest {
    private val qr = """{"serverId":"sid1","port":43297,"method":"UDP","encKey":"enc-key"}"""

    @Test
    fun persistsOnSuccess() =
        runBlocking {
            val store = FakePairingStore()
            val pairPc =
                PairPc(
                    store = store,
                    discovery = { _, _, _ -> DiscoveredHost("10.0.0.5", 43295) },
                    client = { _, _, _, _ -> successResponse() },
                    deviceName = "Pixel",
                    nowMs = { 1000L },
                    localOverlayIpv4 = { null },
                    localLanIpv4 = { null },
                )
            val result = pairPc(qr) as PairResult.Ok
            assertEquals("pc-1", result.pairing.deviceId)
            assertEquals("enc-key", result.pairing.encryptionKey)
            assertEquals("pw-key", result.pairing.passwordKey)
            assertEquals(PairingMethod.UDP, result.pairing.pairingMethod)
            assertEquals("10.0.0.5", result.pairing.lastIpv4)
            assertEquals("", result.pairing.lastTailscaleIpv4)
            assertEquals(1, store.items.value.size)
        }

    @Test
    fun pairInitSendsPhoneOverlayAndLanWithoutClobberingPcLan() =
        runBlocking {
            val store = FakePairingStore()
            var captured: PacketPairInit? = null
            val pairPc =
                PairPc(
                    store = store,
                    discovery = { _, _, _ -> DiscoveredHost("10.0.0.5", 43295) },
                    client = { _, _, _, init ->
                        captured = init
                        successResponse()
                    },
                    deviceName = "Pixel",
                    nowMs = { 1000L },
                    localOverlayIpv4 = { "100.64.2.3" },
                    localLanIpv4 = { "10.8.0.9" },
                )
            val result = pairPc(qr) as PairResult.Ok
            assertEquals("100.64.2.3", captured!!.tsIP)
            assertEquals("10.8.0.9", captured!!.ipAddress)
            assertEquals("10.0.0.5", result.pairing.lastIpv4)
            assertEquals("", result.pairing.lastTailscaleIpv4)
        }

    @Test
    fun cancelDoesNotPersist() =
        runBlocking {
            val store = FakePairingStore()
            val pairPc =
                PairPc(
                    store = store,
                    discovery = { _, _, _ -> DiscoveredHost("10.0.0.5", 43295) },
                    client = { _, _, _, _ ->
                        delay(5_000)
                        successResponse()
                    },
                    deviceName = "Pixel",
                )
            val job = launch { pairPc(qr) }
            delay(30)
            job.cancel()
            job.join()
            assertTrue(store.items.value.isEmpty())
        }

    @Test
    fun rejectsCloudAndMapsDesktopErrors() =
        runBlocking {
            val store = FakePairingStore()
            val cloud =
                PairPc(store, { _, _, _ -> error("no") }, { _, _, _, _ -> error("no") }, "Pixel")
            assertEquals(
                PairError.CloudUnsupported,
                (cloud("""{"serverId":"s","port":1,"method":"CLOUD_TCP","encKey":"k"}""") as PairResult.Err).error,
            )
            val mismatch =
                PairPc(
                    store,
                    { _, _, _ -> DiscoveredHost("10.0.0.5", 43295) },
                    { _, _, _, _ ->
                        PacketPairResponse(
                            errMsg = "Your app's version is incompatible with the desktop app's version.",
                        )
                    },
                    "Pixel",
                )
            assertEquals(PairError.ProtocolMismatch, (mismatch(qr) as PairResult.Err).error)
            assertTrue(store.items.value.isEmpty())
        }

    @Test
    fun unknownPayloadIsInvalid() =
        runBlocking {
            val pairPc =
                PairPc(FakePairingStore(), { _, _, _ -> error("no") }, { _, _, _, _ -> error("no") }, "Pixel")
            assertEquals(PairError.InvalidPayload, (pairPc("not-a-code") as PairResult.Err).error)
        }

    private fun successResponse() =
        PacketPairResponse(
            errMsg = "",
            data =
                PacketPairResponseData(
                    pairingMethod = "UDP",
                    deviceId = "pc-1",
                    deviceName = "WORKSTATION",
                    deviceOS = "Windows",
                    unlockServerPort = 43296,
                    userName = "satya",
                    passwordKey = "pw-key",
                ),
        )
}

private class FakePairingStore : PairingStore {
    val items = MutableStateFlow<List<Pairing>>(emptyList())
    private var local =
        LocalDevice(
            uuid = "device-uuid",
            displayName = "Pixel",
            tcpPort = 43310,
            udpPort = 43311,
            udpManualPort = 43312,
        )

    override fun observe(): Flow<List<Pairing>> = items

    override suspend fun reload() = Unit

    override suspend fun snapshot(): PairingsSnapshot = PairingsSnapshot(local, items.value)

    override suspend fun ensureLocalDevice(displayName: String): LocalDevice {
        local = local.copy(displayName = displayName)
        return local
    }

    override suspend fun upsert(pairing: Pairing) {
        items.value = items.value.filterNot { it.deviceId == pairing.deviceId } + pairing
    }

    override suspend fun remove(deviceId: String) {
        items.value = items.value.filterNot { it.deviceId == deviceId }
    }

    override suspend fun rememberIpv4(
        deviceId: String,
        ip: String,
    ) {
        items.value =
            items.value.map {
                if (it.deviceId != deviceId) {
                    it
                } else {
                    it.rememberAddress(ip) ?: it
                }
            }
    }
}
