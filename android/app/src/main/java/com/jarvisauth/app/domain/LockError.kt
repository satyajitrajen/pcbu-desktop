package com.jarvisauth.app.domain

sealed class LockError {
    data object NoAddress : LockError()

    data object Unreachable : LockError()

    data object OverlayUnreachable : LockError()

    data object TimeSkew : LockError()

    data object DataError : LockError()

    data object NotPaired : LockError()

    data object ProtocolError : LockError()

    data object AppError : LockError()

    val userMessage: String
        get() =
            when (this) {
                NoAddress ->
                    "This PC hasn’t been seen on this Wi-Fi or Tailscale yet. " +
                        "Unlock once while both are on the same network with Tailscale open."
                Unreachable ->
                    "Couldn’t reach the PC. Confirm JarvisAuth is running and you’re on the same Wi-Fi."
                OverlayUnreachable ->
                    "Couldn’t reach the PC over Tailscale. " +
                        "Confirm Tailscale is on and JarvisAuth is running."
                TimeSkew -> "Clocks don’t match. Set both to the same time."
                DataError -> "Couldn’t read the lock reply."
                NotPaired -> "This PC is not paired."
                ProtocolError -> "This phone and PC are on different versions."
                AppError -> "The PC could not complete that."
            }

    companion object {
        fun fromWire(code: String): LockError =
            when (code) {
                "TIME_ERROR" -> TimeSkew
                "DATA_ERROR" -> DataError
                "NOT_PAIRED" -> NotPaired
                "PROTOCOL_ERROR" -> ProtocolError
                "APP_ERROR" -> AppError
                else -> DataError
            }
    }
}

sealed class LockResult {
    data object Ok : LockResult()

    data class Err(
        val error: LockError,
    ) : LockResult()
}

fun interface LockClient {
    suspend fun lock(
        host: String,
        port: Int,
        pairing: Pairing,
        action: String,
    ): LockResult
}
