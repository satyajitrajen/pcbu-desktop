package com.jarvisauth.app.domain

sealed class UnlockError {
    data object Cancel : UnlockError()

    data object TimeSkew : UnlockError()

    data object DataError : UnlockError()

    data object NotPaired : UnlockError()

    data object ProtocolError : UnlockError()

    data object AppError : UnlockError()

    val wireCode: String
        get() =
            when (this) {
                Cancel -> "CANCEL"
                TimeSkew -> "TIME_ERROR"
                DataError -> "DATA_ERROR"
                NotPaired -> "NOT_PAIRED"
                ProtocolError -> "PROTOCOL_ERROR"
                AppError -> "APP_ERROR"
            }

    val userMessage: String
        get() =
            when (this) {
                Cancel -> "Cancelled."
                TimeSkew -> "Clocks don’t match. Set both to the same time."
                DataError -> "Couldn’t read the unlock request."
                NotPaired -> "This PC is not paired."
                ProtocolError -> "This phone and PC are on different versions."
                AppError -> "Unlock failed on this phone."
            }
}

sealed class UnlockDecision {
    data object Confirm : UnlockDecision()

    data object Cancel : UnlockDecision()
}

data class UnlockPrompt(
    val deviceId: String,
    val pcName: String,
    val user: String,
    val program: String,
    val unlockToken: String,
)

fun formatUnlockProgram(program: String): String =
    when (program) {
        "Windows-Login" -> "Windows login"
        else -> program.replace('-', ' ')
    }
