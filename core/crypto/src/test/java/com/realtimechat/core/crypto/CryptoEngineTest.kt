package com.realtimechat.core.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.security.SecureRandom

class CryptoEngineTest {

    private val cryptoEngine = CryptoEngine()

    @Test
    fun testSymmetricEncryptionDecryptionRoundtrip() {
        val secretKey = ByteArray(32).apply { SecureRandom().nextBytes(this) }
        val originalMessage = "Hello, secure E2EE chat!"

        val encryptedPayload = cryptoEngine.encrypt(originalMessage, secretKey)
        assertNotNull(encryptedPayload.ciphertext)
        assertNotNull(encryptedPayload.iv)

        val decryptedMessage = cryptoEngine.decrypt(encryptedPayload, secretKey)
        assertEquals(originalMessage, decryptedMessage)
    }

    @Test
    fun testEcdhKeyExchangeAndRoundtrip() {
        // Alice generates key pair
        val aliceKeyPair = cryptoEngine.generateEcdhKeyPair()
        // Bob generates key pair
        val bobKeyPair = cryptoEngine.generateEcdhKeyPair()

        // Alice derives shared secret with Bob's public key
        val aliceSharedKey = cryptoEngine.deriveSharedSecret(aliceKeyPair.private, bobKeyPair.public)
        // Bob derives shared secret with Alice's public key
        val bobSharedKey = cryptoEngine.deriveSharedSecret(bobKeyPair.private, aliceKeyPair.public)

        // Both shared keys must be identical
        org.junit.Assert.assertArrayEquals(aliceSharedKey, bobSharedKey)

        // Encrypt message from Alice to Bob
        val message = "Secret message from Alice to Bob via ECDH"
        val encrypted = cryptoEngine.encrypt(message, aliceSharedKey)
        val decrypted = cryptoEngine.decrypt(encrypted, bobSharedKey)

        assertEquals(message, decrypted)
    }

    @Test
    fun testGroupKeyDerivationAndRoundtrip() {
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
}
