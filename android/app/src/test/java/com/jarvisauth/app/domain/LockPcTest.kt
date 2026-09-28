package com.jarvisauth.app.domain

import com.jarvisauth.app.protocol.PairingMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class LockPcTest {
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
        )

    @Test
    fun missingIpDoesNotCallClient() =
        runBlocking {
            val store = LockStore(listOf(pairing))
            var called = false
            val lockPc =
                LockPc(
                    store = store,
                    client = { _, _, _, _ ->
                        called = true
                        LockResult.Ok
                    },
                    appLog = AppLog(),
                )
            val result = lockPc("pc-1") as LockResult.Err
            assertEquals(LockError.NoAddress, result.error)
            assertEquals(false, called)
        }

    @Test
    fun withIpCallsClient() =
        runBlocking {
            val store = LockStore(listOf(pairing.copy(lastIpv4 = "192.168.1.20")))
            val lockPc =
                LockPc(
                    store = store,
                    client = { host, port, target, action ->
                        assertEquals("192.168.1.20", host)
                        assertEquals(43298, port)
                        assertEquals("pc-1", target.deviceId)
                        assertEquals("", action)
                        LockResult.Ok
                    },
                    appLog = AppLog(),
                )
            assertEquals(LockResult.Ok, lockPc("pc-1"))
        }

    @Test
    fun overlayOnlyDialsTailscaleHost() =
        runBlocking {
            val store =
                LockStore(
                    listOf(pairing.copy(lastTailscaleIpv4 = "100.64.1.9")),
                )
            val hosts = mutableListOf<String>()
            val lockPc =
                LockPc(
                    store = store,
                    client = { host, port, _, _ ->
                        hosts += host
                        assertEquals(43298, port)
                        LockResult.Ok
                    },
                    appLog = AppLog(),
                )
            assertEquals(LockResult.Ok, lockPc("pc-1"))
            assertEquals(listOf("100.64.1.9"), hosts)
        }

    @Test
    fun lanThenOverlayOnUnreachable() =
        runBlocking {
            val store =
                LockStore(
                    listOf(
                        pairing.copy(
                            lastIpv4 = "192.168.1.20",
                            lastTailscaleIpv4 = "100.64.1.9",
                        ),
                    ),
                )
            val hosts = mutableListOf<String>()
            val lockPc =
                LockPc(
                    store = store,
                    client = { host, _, _, _ ->
                        hosts += host
                        if (host == "192.168.1.20") {
                            LockResult.Err(LockError.Unreachable)
                        } else {
                            LockResult.Ok
                        }
                    },
                    appLog = AppLog(),
                )
            assertEquals(LockResult.Ok, lockPc("pc-1"))
            assertEquals(listOf("192.168.1.20", "100.64.1.9"), hosts)
        }

    @Test
    fun lanSuccessDoesNotDialOverlay() =
        runBlocking {
            val store =
                LockStore(
                    listOf(
                        pairing.copy(
                            lastIpv4 = "192.168.1.20",
                            lastTailscaleIpv4 = "100.64.1.9",
                        ),
                    ),
                )
            val hosts = mutableListOf<String>()
            val lockPc =
                LockPc(
                    store = store,
                    client = { host, _, _, _ ->
                        hosts += host
                        LockResult.Ok
                    },
                    appLog = AppLog(),
                )
            assertEquals(LockResult.Ok, lockPc("pc-1"))
            assertEquals(listOf("192.168.1.20"), hosts)
        }

    @Test
    fun overlayFailureIsOverlayUnreachable() =
        runBlocking {
            val store = LockStore(listOf(pairing.copy(lastTailscaleIpv4 = "100.64.1.9")))
            val lockPc =
                LockPc(
                    store = store,
                    client = { _, _, _, _ -> LockResult.Err(LockError.Unreachable) },
                    appLog = AppLog(),
                )
            val result = lockPc("pc-1") as LockResult.Err
            assertEquals(LockError.OverlayUnreachable, result.error)
        }

    @Test
    fun lanCryptoErrorDoesNotTryOverlay() =
        runBlocking {
            val store =
                LockStore(
                    listOf(
                        pairing.copy(
                            lastIpv4 = "192.168.1.20",
                            lastTailscaleIpv4 = "100.64.1.9",
                        ),
                    ),
                )
            val hosts = mutableListOf<String>()
            val lockPc =
                LockPc(
                    store = store,
                    client = { host, _, _, _ ->
                        hosts += host
                        LockResult.Err(LockError.DataError)
                    },
                    appLog = AppLog(),
                )
            val result = lockPc("pc-1") as LockResult.Err
            assertEquals(LockError.DataError, result.error)
            assertEquals(listOf("192.168.1.20"), hosts)
        }

    @Test
    fun shutdownPassesActionAndKeepsHops() =
        runBlocking {
            val store = LockStore(listOf(pairing.copy(lastIpv4 = "192.168.1.20")))
            val seen = mutableListOf<String>()
            val lockPc =
                LockPc(
                    store = store,
                    client = { _, _, _, action ->
                        seen += action
                        LockResult.Ok
                    },
                    appLog = AppLog(),
                )
            assertEquals(LockResult.Ok, lockPc("pc-1", "shutdown"))
            assertEquals(listOf("shutdown"), seen)
        }
}

private class LockStore(
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
                if (it.deviceId != deviceId) {
                    it
                } else {
                    it.rememberAddress(ip) ?: it
                }
            }
    }
}
