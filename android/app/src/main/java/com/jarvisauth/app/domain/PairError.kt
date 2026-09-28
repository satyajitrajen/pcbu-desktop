package com.jarvisauth.app.domain

sealed class PairError {
    data object InvalidPayload : PairError()

    data object CloudUnsupported : PairError()

    data object ProtocolMismatch : PairError()

    data object TimeSkew : PairError()

    data object DecryptFailed : PairError()

    data object DiscoveryTimeout : PairError()

    data object ConnectionFailed : PairError()

    data class Desktop(
        val remoteMessage: String,
    ) : PairError()
}

fun PairError.userMessage(): String =
    when (this) {
        PairError.InvalidPayload -> "That pairing code isn’t valid."
        PairError.CloudUnsupported -> "Cloud pairing is not supported."
        PairError.ProtocolMismatch -> "This phone and PC are on different versions."
        PairError.TimeSkew -> "Clocks don’t match. Set both to the same time."
        PairError.DecryptFailed -> "Couldn’t read the pairing data. Scan again."
        PairError.DiscoveryTimeout -> "Couldn’t find the PC on the network. Confirm pairing is open and you’re on the same Wi-Fi."
        PairError.ConnectionFailed -> "Couldn’t reach the PC. Confirm pairing is still open."
        is PairError.Desktop -> remoteMessage.ifBlank { "The PC refused pairing." }
    }

fun mapDesktopErrMsg(message: String): PairError {
    val m = message.lowercase()
    val protocolHints = listOf("incompatible", "version", "protokoll", "versão", "versao", "版本")
    if (protocolHints.any { it in m }) return PairError.ProtocolMismatch
    val timeHints = listOf("clock", "time", "uhr", "hora", "时钟", "时间")
    if (timeHints.any { it in m }) return PairError.TimeSkew
    return PairError.Desktop(message)
}
