package com.jarvisauth.app.domain

sealed class UnlockDialResult {
    data object Unreachable : UnlockDialResult()

    data object ProtocolFail : UnlockDialResult()

    data class Finished(
        val decision: UnlockDecision,
    ) : UnlockDialResult()
}

fun interface UnlockDialer {
    suspend fun run(
        host: String,
        port: Int,
        pairing: Pairing,
        onLearnIp: suspend (String) -> Unit,
        decide: suspend (UnlockPrompt) -> UnlockDecision,
    ): UnlockDialResult
}

sealed class UnlockPcError {
    data object NoAddress : UnlockPcError()

    data object NotAsking : UnlockPcError()

    data object NotPaired : UnlockPcError()

    val userMessage: String
        get() =
            when (this) {
                NoAddress -> LockError.NoAddress.userMessage
                NotAsking -> "PC isn’t asking to unlock."
                NotPaired -> "This PC is not paired."
            }
}

sealed class UnlockPcResult {
    data object Resumed : UnlockPcResult()

    data object Unlocked : UnlockPcResult()

    data object Cancelled : UnlockPcResult()

    data class Err(
        val error: UnlockPcError,
    ) : UnlockPcResult()
}

class UnlockPc(
    private val store: PairingStore,
    private val session: UnlockSession,
    private val dialer: UnlockDialer,
    private val appLog: AppLog,
) {
    suspend operator fun invoke(
        deviceId: String,
        onShowPlate: () -> Unit,
    ): UnlockPcResult {
        val pairing =
            store.snapshot().pairings.find { it.deviceId == deviceId }
                ?: return fail(UnlockPcError.NotPaired)
        if (session.promptFor(deviceId) != null) {
            appLog.append("unlock pc resume ${pairing.deviceId.take(8)}")
            onShowPlate()
            return UnlockPcResult.Resumed
        }
        val hops = lockHops(pairing)
        if (hops.isEmpty()) {
            return fail(UnlockPcError.NoAddress)
        }
        val port =
            pairing.unlockServerPort.takeIf { it in 1..65535 } ?: 43296
        for (hop in hops) {
            appLog.append("unlock pc ${pairing.deviceId.take(8)} path=${hop.path}")
            val result =
                try {
                    dialer.run(
                        hop.host,
                        port,
                        pairing,
                        { ip -> store.rememberIpv4(pairing.deviceId, ip) },
                    ) { prompt ->
                        session.awaitDecision(prompt, onShowPlate)
                    }
                } catch (_: Exception) {
                    UnlockDialResult.Unreachable
                }
            when (result) {
                UnlockDialResult.Unreachable -> continue
                UnlockDialResult.ProtocolFail -> {
                    appLog.append("unlock not asking ${pairing.deviceId.take(8)} path=${hop.path}")
                    return UnlockPcResult.Err(UnlockPcError.NotAsking)
                }
                is UnlockDialResult.Finished -> {
                    return when (result.decision) {
                        UnlockDecision.Confirm -> {
                            appLog.append(
                                "unlock ok ${pairing.deviceId.take(8)} path=${hop.path}",
                            )
                            UnlockPcResult.Unlocked
                        }
                        UnlockDecision.Cancel -> {
                            appLog.append("unlock cancel ${pairing.deviceId.take(8)}")
                            UnlockPcResult.Cancelled
                        }
                    }
                }
            }
        }
        appLog.append("unlock not asking ${pairing.deviceId.take(8)}")
        return UnlockPcResult.Err(UnlockPcError.NotAsking)
    }

    private suspend fun fail(error: UnlockPcError): UnlockPcResult {
        appLog.append("unlock pc failed ${error::class.simpleName}")
        return UnlockPcResult.Err(error)
    }
}
