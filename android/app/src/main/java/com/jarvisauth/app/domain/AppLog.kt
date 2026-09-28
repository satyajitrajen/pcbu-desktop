package com.jarvisauth.app.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AppLog(
    private val maxLines: Int = 500,
    private val logcat: ((String) -> Unit)? = null,
) {
    private val mutex = Mutex()
    private val ring = ArrayDeque<String>(maxLines + 1)
    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    @Volatile
    var debugEnabled: Boolean = false

    suspend fun append(message: String) {
        val redacted = redact(message)
        mutex.withLock {
            ring.addLast(redacted)
            while (ring.size > maxLines) ring.removeFirst()
            _lines.value = ring.toList()
        }
        if (debugEnabled) {
            logcat?.invoke(redacted)
        }
    }

    suspend fun clear() {
        mutex.withLock {
            ring.clear()
            _lines.value = emptyList()
        }
    }

    companion object {
        private val SENSITIVE =
            Regex("(?i)(passwordKey|encKey|encData|encryptionKey)\\s*[:=]\\s*[^\\s,;\"']+")

        fun redact(raw: String): String = SENSITIVE.replace(raw) { "${it.groupValues[1]}=[redacted]" }
    }
}
