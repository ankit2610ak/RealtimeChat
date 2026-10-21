package com.realtimechat.core.crypto

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.AEADBadTagException

class CryptoEngineTest {

    private val cryptoEngine = CryptoEngine()

    @Test
    fun testSymmetricEncryptionDecryptionRoundtrip() = runTest {
        val secretKey = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        val originalMessage = "Hello, secure E2EE chat!"

        val encryptedPayload = cryptoEngine.encrypt(originalMessage, secretKey)
        assertNotNull(encryptedPayload.payload)

        val decryptedMessage = cryptoEngine.decrypt(encryptedPayload, secretKey)
        assertEquals(originalMessage, decryptedMessage)
    }

    @Test
    fun testEcdhKeyExchangeAndRoundtrip() = runTest {
        val aliceKeyPair = cryptoEngine.generateEcdhKeyPair()
        val bobKeyPair = cryptoEngine.generateEcdhKeyPair()

        val aliceSharedKey = cryptoEngine.deriveSharedSecret(aliceKeyPair.private, bobKeyPair.public)
        val bobSharedKey = cryptoEngine.deriveSharedSecret(bobKeyPair.private, aliceKeyPair.public)

        org.junit.Assert.assertArrayEquals(aliceSharedKey, bobSharedKey)

        val message = "Secret message from Alice to Bob via ECDH"
        val encrypted = cryptoEngine.encrypt(message, aliceSharedKey)
        val decrypted = cryptoEngine.decrypt(encrypted, bobSharedKey)

        assertEquals(message, decrypted)
    }

    @Test
    fun testGroupKeyDerivationAndRoundtrip() = runTest {
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val passphrase = "super-secret-group-passphrase".toCharArray()

        val groupKey1 = cryptoEngine.deriveGroupKey(passphrase, salt)
        val groupKey2 = cryptoEngine.deriveGroupKey(passphrase, salt)

        org.junit.Assert.assertArrayEquals(groupKey1, groupKey2)

        val chatMessage = "Hello group members!"
        val encrypted = cryptoEngine.encrypt(chatMessage, groupKey1)
        val decrypted = cryptoEngine.decrypt(encrypted, groupKey2)

        assertEquals(chatMessage, decrypted)
    }

    @Test(expected = AEADBadTagException::class)
    fun testDecryptionFailsWhenCiphertextTampered() = runTest {
        val secretKey = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        val original = "Secret Message"
        val encrypted = cryptoEngine.encrypt(original, secretKey)

        // Tamper with payload
        val tamperedPayload = EncryptedPayload(payload = encrypted.payload.dropLast(1) + "A")

        cryptoEngine.decrypt(tamperedPayload, secretKey)
    }
}
