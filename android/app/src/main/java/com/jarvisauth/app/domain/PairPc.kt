package com.jarvisauth.app.domain

import com.jarvisauth.app.protocol.CryptResult
import com.jarvisauth.app.protocol.PacketPairInit
import com.jarvisauth.app.protocol.PairingMethod
import com.jarvisauth.app.protocol.PairingQrParser
import com.jarvisauth.app.protocol.SystemEpochClock

class PairPc(
    private val store: PairingStore,
    private val discovery: PairingDiscovery,
    private val client: PairingClient,
    private val deviceName: String,
    private val nowMs: () -> Long = { SystemEpochClock.nowMs() },
    private val localOverlayIpv4: () -> String? = { OverlayIpv4.local() },
    private val localLanIpv4: () -> String? = { OverlayIpv4.localLan() },
) {
    suspend operator fun invoke(payload: String): PairResult {
        val qr = PairingQrParser.parse(payload) ?: return PairResult.Err(PairError.InvalidPayload)
        if (PairingQrParser.isCloudMethod(qr.method)) {
            return PairResult.Err(PairError.CloudUnsupported)
        }
        val local = store.ensureLocalDevice(deviceName)
        val host =
            if (qr.ip.isNotBlank()) {
                DiscoveredHost(qr.ip, qr.port)
            } else {
                discovery.find(qr.serverId, qr.port, qr.encKey)
                    ?: return PairResult.Err(PairError.DiscoveryTimeout)
            }
        val overlay = localOverlayIpv4()?.takeIf { OverlayIpv4.isOverlay(it) }.orEmpty()
        val lan = localLanIpv4()?.takeIf { OverlayIpv4.isLan(it) }.orEmpty()
        val init =
            PacketPairInit(
                protoVersion = "4.0.0",
                deviceUUID = local.uuid,
                deviceName = local.displayName,
                ipAddress = lan,
                tcpPort = local.tcpPort,
                udpPort = local.udpPort,
                udpManualPort = local.udpManualPort,
                cloudToken = "",
                tsIP = overlay,
            )
        val response =
            try {
                client.pair(host.ip, host.port, qr.encKey, init)
            } catch (_: PairDecryptException) {
                return PairResult.Err(PairError.DecryptFailed)
            } catch (_: PairTimeSkewException) {
                return PairResult.Err(PairError.TimeSkew)
            } catch (_: Exception) {
                return PairResult.Err(PairError.ConnectionFailed)
            }
        if (response.errMsg.isNotBlank()) {
            return PairResult.Err(mapDesktopErrMsg(response.errMsg))
        }
        val data = response.data
        val method = PairingMethod.fromWire(data.pairingMethod)
        if (method == null || PairingQrParser.isCloudMethod(data.pairingMethod)) {
            return PairResult.Err(PairError.CloudUnsupported)
        }
        if (data.deviceId.isBlank() || data.passwordKey.isBlank()) {
            return PairResult.Err(PairError.DecryptFailed)
        }
        val pairing =
            Pairing(
                deviceId = data.deviceId,
                encryptionKey = qr.encKey,
                passwordKey = data.passwordKey,
                pairingMethod = method,
                pcName = data.deviceName.ifBlank { "PC" },
                pcOs = data.deviceOS,
                userName = data.userName,
                unlockServerPort = data.unlockServerPort,
                macAddresses = data.macAddresses,
                pairedAtEpochMs = nowMs(),
            ).let { it.rememberAddress(host.ip) ?: it }
        store.upsert(pairing)
        return PairResult.Ok(pairing)
    }
}

class PairDecryptException : Exception()

class PairTimeSkewException : Exception()

fun cryptFailure(result: CryptResult): Exception =
    when (result) {
        CryptResult.InvalidTimestamp -> PairTimeSkewException()
        CryptResult.OtherError -> PairDecryptException()
        is CryptResult.Ok -> IllegalStateException("not a failure")
    }
