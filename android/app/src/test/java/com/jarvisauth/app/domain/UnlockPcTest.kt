package com.jarvisauth.app.domain

import com.jarvisauth.app.protocol.PairingMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnlockPcTest {
    private val pairing =
        Pairing(
            deviceId = "pc-1",
            encryptionKey = "enc-key",
            passwordKey = "pw-key",
            pairingMethod = PairingMethod.UDP,
            pcName = "WORKSTATION",
            pcOs = "Windows",
            userName = "satya",
            unlockServerPort = 43296,
            pairedAtEpochMs = 1L,
            lastIpv4 = "192.168.1.20",
            lastTailscaleIpv4 = "100.64.1.9",
        )

    @Test
    fun inFlightResumesWithoutDial() =
        runBlocking {
            val session = UnlockSession()
            val prompt =
                UnlockPrompt("pc-1", "WORKSTATION", "satya", "Windows login", "tok")
            var dialed = false
            val waiter = launch { session.awaitDecision(prompt) }
            yield()
            val unlockPc =
                UnlockPc(
                    store = UnlockStore(listOf(pairing)),
                    session = session,
                    dialer = { _, _, _, _, _ ->
                        dialed = true
                        UnlockDialResult.Unreachable
                    },
                    appLog = AppLog(),
                )
            var plate = false
            val result = unlockPc("pc-1") { plate = true }
            assertEquals(UnlockPcResult.Resumed, result)
            assertTrue(plate)
            assertFalse(dialed)
            session.complete(UnlockDecision.Cancel)
            waiter.join()
        }

    @Test
    fun notAskingDoesNotCallDecide() =
        runBlocking {
            val hosts = mutableListOf<String>()
            val unlockPc =
                UnlockPc(
                    store = UnlockStore(listOf(pairing)),
                    session = UnlockSession(),
                    dialer = { host, port, _, _, _ ->
                        hosts += host
                        assertEquals(43296, port)
                        UnlockDialResult.Unreachable
                    },
                    appLog = AppLog(),
                )
            val result = unlockPc("pc-1") {} as UnlockPcResult.Err
            assertEquals(UnlockPcError.NotAsking, result.error)
            assertEquals(listOf("192.168.1.20", "100.64.1.9"), hosts)
        }

    @Test
    fun lanThenOverlayOnUnreachable() =
        runBlocking {
            val hosts = mutableListOf<String>()
            val unlockPc =
                UnlockPc(
                    store = UnlockStore(listOf(pairing)),
                    session = UnlockSession(),
                    dialer = { host, _, _, _, _ ->
                        hosts += host
                        if (host == "192.168.1.20") {
                            UnlockDialResult.Unreachable
                        } else {
                            UnlockDialResult.Finished(UnlockDecision.Confirm)
                        }
                    },
                    appLog = AppLog(),
                )
            assertEquals(UnlockPcResult.Unlocked, unlockPc("pc-1") {})
            assertEquals(listOf("192.168.1.20", "100.64.1.9"), hosts)
        }

    @Test
    fun missingAddressDoesNotDial() =
        runBlocking {
            var dialed = false
            val unlockPc =
                UnlockPc(
                    store = UnlockStore(listOf(pairing.copy(lastIpv4 = "", lastTailscaleIpv4 = ""))),
                    session = UnlockSession(),
                    dialer = { _, _, _, _, _ ->
                        dialed = true
                        UnlockDialResult.Unreachable
                    },
                    appLog = AppLog(),
                )
            val result = unlockPc("pc-1") {} as UnlockPcResult.Err
            assertEquals(UnlockPcError.NoAddress, result.error)
            assertFalse(dialed)
        }
}

private class UnlockStore(
    initial: List<Pairing>,
) : PairingStore {
    private val items = MutableStateFlow(initial)

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
        items.value =
            items.value.map {
                if (it.deviceId != deviceId) it else it.rememberAddress(ip) ?: it
            }
    }
}
