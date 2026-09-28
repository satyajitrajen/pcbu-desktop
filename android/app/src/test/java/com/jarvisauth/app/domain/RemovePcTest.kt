package com.jarvisauth.app.domain

import com.jarvisauth.app.protocol.AesGcmPacket
import com.jarvisauth.app.protocol.CryptResult
import com.jarvisauth.app.protocol.EpochClock
import com.jarvisauth.app.protocol.Hex
import com.jarvisauth.app.protocol.PacketUnlockRequest
import com.jarvisauth.app.protocol.PacketUnlockRequestData
import com.jarvisauth.app.protocol.PairingMethod
import com.jarvisauth.app.protocol.ProtocolJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RemovePcTest {
    @Test
    fun removedPcIsNotPairedOnUnlockRequest() =
        runBlocking {
            val store = MemoryPairingStore()
            val kept =
                Pairing(
                    deviceId = "pc-keep",
                    encryptionKey = "enc-keep",
                    passwordKey = "pw-keep",
                    pairingMethod = PairingMethod.UDP,
                    pcName = "KEEP",
                    pcOs = "Windows",
                    userName = "satya",
                    unlockServerPort = 43296,
                    pairedAtEpochMs = 1L,
                )
            val gone =
                kept.copy(
                    deviceId = "pc-gone",
                    encryptionKey = "enc-gone",
                    passwordKey = "pw-gone",
                    pcName = "GONE",
                )
            store.upsert(kept)
            store.upsert(gone)
            store.remove("pc-gone")
            val pairings = store.snapshot().pairings.associateBy { it.deviceId }
            val clock = EpochClock { 1_700_000_000_000L }
            val inner =
                ProtocolJson.encode(
                    PacketUnlockRequestData("satya", "Windows-Login", "tok"),
                ).toByteArray()
            val blob = (AesGcmPacket.encrypt(inner, "enc-gone", clock) as CryptResult.Ok).data
            val payload =
                ProtocolJson.encode(
                    PacketUnlockRequest("3.0.0", "pc-gone", Hex.encode(blob)),
                ).toByteArray()
            val parsed = UnlockHandshake.parseRequest(payload, pairings, clock)
            assertEquals(UnlockError.NotPaired, (parsed as UnlockHandshake.Parse.Fail).error)
            assertEquals(setOf("pc-keep"), pairings.keys)
        }
}

private class MemoryPairingStore : PairingStore {
    private val items = MutableStateFlow<List<Pairing>>(emptyList())

    override fun observe(): Flow<List<Pairing>> = items

    override suspend fun reload() = Unit

    override suspend fun snapshot(): PairingsSnapshot = PairingsSnapshot(pairings = items.value)

    override suspend fun ensureLocalDevice(displayName: String) =
        LocalDevice("u", displayName, 43310, 43311, 43312)

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
        val parsed = Ipv4.parse(ip) ?: return
        items.value =
            items.value.map { if (it.deviceId == deviceId) it.copy(lastIpv4 = parsed) else it }
    }
}
