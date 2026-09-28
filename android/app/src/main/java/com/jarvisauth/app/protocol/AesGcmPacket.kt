package com.jarvisauth.app.protocol

sealed class CryptResult {
    data class Ok(
        val data: ByteArray,
    ) : CryptResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Ok) return false
            return data.contentEquals(other.data)
        }

        override fun hashCode(): Int = data.contentHashCode()
    }

    data object InvalidTimestamp : CryptResult()

    data object OtherError : CryptResult()
}

object AesGcmPacket {
    const val IV_SIZE = 16
    const val SALT_SIZE = 16
    const val TAG_SIZE = 16
    const val ITERATIONS = 65535
    const val KEY_BITS = 256
    const val MAX_PAYLOAD = 2048
    const val TIMESTAMP_TIMEOUT_MS = 120_000L

    fun encrypt(
        payload: ByteArray,
        password: String,
        clock: EpochClock = SystemEpochClock,
        iv: ByteArray = randomBytes(IV_SIZE),
        salt: ByteArray = randomBytes(SALT_SIZE),
    ): CryptResult {
        if (payload.size > MAX_PAYLOAD) return CryptResult.OtherError
        if (iv.size != IV_SIZE || salt.size != SALT_SIZE) return CryptResult.OtherError
        val plain = ByteArray(8 + payload.size)
        writeU64Be(plain, 0, clock.nowMs())
        payload.copyInto(plain, 8)
        val body =
            encryptRaw(plain, password, iv, salt) ?: return CryptResult.OtherError
        return CryptResult.Ok(body)
    }

    fun decrypt(
        blob: ByteArray,
        password: String,
        clock: EpochClock = SystemEpochClock,
    ): CryptResult {
        if (blob.size > MAX_PAYLOAD) return CryptResult.OtherError
        if (blob.size < IV_SIZE + SALT_SIZE + TAG_SIZE) return CryptResult.OtherError
        val iv = blob.copyOfRange(0, IV_SIZE)
        val salt = blob.copyOfRange(IV_SIZE, IV_SIZE + SALT_SIZE)
        val ctAndTag = blob.copyOfRange(IV_SIZE + SALT_SIZE, blob.size)
        val plain =
            decryptRaw(ctAndTag, password, iv, salt) ?: return CryptResult.OtherError
        if (plain.size <= 8) return CryptResult.OtherError
        val timestamp = readU64Be(plain, 0)
        val diff = clock.nowMs() - timestamp
        if (diff < -TIMESTAMP_TIMEOUT_MS || diff > TIMESTAMP_TIMEOUT_MS) {
            return CryptResult.InvalidTimestamp
        }
        return CryptResult.Ok(plain.copyOfRange(8, plain.size))
    }

    private fun encryptRaw(
        plain: ByteArray,
        password: String,
        iv: ByteArray,
        salt: ByteArray,
    ): ByteArray? {
        return try {
            val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                javax.crypto.Cipher.ENCRYPT_MODE,
                secretKey(password, salt),
                javax.crypto.spec.GCMParameterSpec(TAG_SIZE * 8, iv),
            )
            val ctAndTag = cipher.doFinal(plain)
            ByteArray(IV_SIZE + SALT_SIZE + ctAndTag.size).also { out ->
                iv.copyInto(out, 0)
                salt.copyInto(out, IV_SIZE)
                ctAndTag.copyInto(out, IV_SIZE + SALT_SIZE)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun decryptRaw(
        ctAndTag: ByteArray,
        password: String,
        iv: ByteArray,
        salt: ByteArray,
    ): ByteArray? {
        if (iv.size != IV_SIZE) return null
        return try {
            val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                javax.crypto.Cipher.DECRYPT_MODE,
                secretKey(password, salt),
                javax.crypto.spec.GCMParameterSpec(TAG_SIZE * 8, iv),
            )
            cipher.doFinal(ctAndTag)
        } catch (_: Exception) {
            null
        }
    }

    private fun secretKey(
        password: String,
        salt: ByteArray,
    ): javax.crypto.SecretKey {
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec =
            javax.crypto.spec.PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATIONS,
                KEY_BITS,
            )
        val encoded = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return javax.crypto.spec.SecretKeySpec(encoded, "AES")
    }

    private fun randomBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        java.security.SecureRandom().nextBytes(bytes)
        return bytes
    }

    private fun writeU64Be(
        dst: ByteArray,
        offset: Int,
        value: Long,
    ) {
        var v = value
        for (i in 7 downTo 0) {
            dst[offset + i] = (v and 0xFF).toByte()
            v = v ushr 8
        }
    }

    private fun readU64Be(
        src: ByteArray,
        offset: Int,
    ): Long {
        var v = 0L
        for (i in 0 until 8) {
            v = (v shl 8) or (src[offset + i].toLong() and 0xFF)
        }
        return v
    }
}
