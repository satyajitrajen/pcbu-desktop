package com.jarvisauth.app.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AesGcmPacketTest {
    private val password = "test-enc-key"
    private val payload = "{\"hello\":\"jarvis\"}".toByteArray(Charsets.UTF_8)
    private val iv = ByteArray(16) { it.toByte() }
    private val salt = ByteArray(16) { (it + 16).toByte() }
    private val timestampMs = 1_700_000_000_000L
    private val clock = EpochClock { timestampMs }

    // Port of CryptUtils.cpp: IV16 + salt16 + AES-256-GCM + PBKDF2-HMAC-SHA256 65535.
    private val goldenBlobHex =
        "000102030405060708090A0B0C0D0E0F101112131415161718191A1B1C1D1E1F" +
            "45D66F53F1DEDA1DC3080693CCC2C72020DD4557EDBCCD499E6DCEE92DE85871F36227126DBF49EAAA36"

    @Test
    fun ivLengthIsSixteenNotTwelve() {
        assertEquals(16, AesGcmPacket.IV_SIZE)
        val result = AesGcmPacket.encrypt(payload, password, clock, iv, salt)
        val blob = (result as CryptResult.Ok).data
        assertEquals(16, blob.copyOfRange(0, 16).size)
        assertTrue(blob.size > 16 + 16 + 16)
    }

    @Test
    fun rejectsTwelveByteIv() {
        val twelve = ByteArray(12) { it.toByte() }
        val result = AesGcmPacket.encrypt(payload, password, clock, twelve, salt)
        assertEquals(CryptResult.OtherError, result)
    }

    @Test
    fun decryptsKnownVectorFromDesktopAlgorithm() {
        val blob = Hex.decode(goldenBlobHex)!!
        val result = AesGcmPacket.decrypt(blob, password, clock)
        val data = (result as CryptResult.Ok).data
        assertArrayEquals(payload, data)
    }

    @Test
    fun encryptMatchesKnownVector() {
        val result = AesGcmPacket.encrypt(payload, password, clock, iv, salt)
        val blob = (result as CryptResult.Ok).data
        assertEquals(goldenBlobHex, Hex.encode(blob))
    }

    @Test
    fun roundTrip() {
        val encrypted = AesGcmPacket.encrypt(payload, password, clock) as CryptResult.Ok
        val decrypted = AesGcmPacket.decrypt(encrypted.data, password, clock) as CryptResult.Ok
        assertArrayEquals(payload, decrypted.data)
    }

    @Test
    fun rejectsStaleTimestamp() {
        val encrypted = AesGcmPacket.encrypt(payload, password, clock, iv, salt) as CryptResult.Ok
        val later = EpochClock { timestampMs + AesGcmPacket.TIMESTAMP_TIMEOUT_MS + 1 }
        assertEquals(CryptResult.InvalidTimestamp, AesGcmPacket.decrypt(encrypted.data, password, later))
    }

    @Test
    fun rejectsOversizePayload() {
        val huge = ByteArray(AesGcmPacket.MAX_PAYLOAD + 1)
        assertEquals(CryptResult.OtherError, AesGcmPacket.encrypt(huge, password, clock))
    }

    @Test
    fun rejectsOversizeCiphertext() {
        val huge = ByteArray(AesGcmPacket.MAX_PAYLOAD + 1)
        assertEquals(CryptResult.OtherError, AesGcmPacket.decrypt(huge, password, clock))
    }

    @Test
    fun pbkdf2Uses65535Iterations() {
        assertEquals(65535, AesGcmPacket.ITERATIONS)
    }
}
