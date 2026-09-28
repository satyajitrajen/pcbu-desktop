package com.jarvisauth.app.domain

internal data class LockHop(
    val host: String,
    val path: String,
)

class LockPc(
    private val store: PairingStore,
    private val client: LockClient,
    private val appLog: AppLog,
    private val port: Int = LockHandshake.PORT,
) {
    suspend operator fun invoke(
        deviceId: String,
        action: String = "",
    ): LockResult {
        val pairing =
            store.snapshot().pairings.find { it.deviceId == deviceId }
                ?: return fail("lock", LockError.NotPaired)
        val hops = lockHops(pairing)
        if (hops.isEmpty()) {
            return fail(logVerb(action), LockError.NoAddress)
        }
        val powerAction = if (action == "shutdown" || action == "restart") action else ""
        val verb = logVerb(powerAction)
        val actionTag = if (powerAction.isEmpty()) "" else " action=$powerAction"
        var lastUnreachable: LockError = LockError.Unreachable
        for (hop in hops) {
            appLog.append("$verb ${pairing.deviceId.take(8)} path=${hop.path}$actionTag")
            val result =
                try {
                    client.lock(hop.host, port, pairing, powerAction)
                } catch (_: Exception) {
                    LockResult.Err(LockError.Unreachable)
                }
            when (result) {
                LockResult.Ok -> {
                    appLog.append("$verb ok ${pairing.deviceId.take(8)} path=${hop.path}$actionTag")
                    return result
                }
                is LockResult.Err -> {
                    if (result.error != LockError.Unreachable) {
                        appLog.append(
                            "$verb failed ${result.error::class.simpleName} path=${hop.path}$actionTag",
                        )
                        return result
                    }
                    lastUnreachable =
                        if (hop.path == "tailscale") {
                            LockError.OverlayUnreachable
                        } else {
                            LockError.Unreachable
                        }
                }
            }
        }
        return fail(verb, lastUnreachable)
    }

    private suspend fun fail(
        verb: String,
        error: LockError,
    ): LockResult {
        appLog.append("$verb failed ${error::class.simpleName}")
        return LockResult.Err(error)
    }
}

private fun logVerb(action: String): String =
    when (action) {
        "shutdown" -> "shutdown"
        "restart" -> "restart"
        else -> "lock"
    }

internal fun lockHops(pairing: Pairing): List<LockHop> {
    val hops = mutableListOf<LockHop>()
    Ipv4.parse(pairing.lastIpv4)?.let { ip ->
        if (!OverlayIpv4.isOverlay(ip)) hops += LockHop(ip, "lan")
    }
    Ipv4.parse(pairing.lastTailscaleIpv4)?.let { ip ->
        if (OverlayIpv4.isOverlay(ip) && hops.none { it.host == ip }) {
            hops += LockHop(ip, "tailscale")
        }
    }
    return hops
}
