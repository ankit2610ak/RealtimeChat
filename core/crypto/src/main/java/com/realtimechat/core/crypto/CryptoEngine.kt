package com.realtimechat.core.crypto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedPayload(
    val ciphertext: String, // Base64 encoded
    val iv: String          // Base64 encoded initialization vector
)

data class EcdhKeyPair(
    val publicKey: String,  // Base64 encoded
    val privateKey: String  // Base64 encoded
)

class CryptoEngine {

    companion object {
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BIT = 128
        private const val IV_LENGTH_BYTE = 12
        private const val KEY_LENGTH_BIT = 256
        private const val PBKDF2_ITERATIONS = 600_000
    }

    /**
     * Encrypts plaintext message payload using AES-256-GCM with a symmetric shared key on Default dispatcher.
     */
    suspend fun encrypt(plaintext: String, secretKey: ByteArray): EncryptedPayload = withContext(Dispatchers.Default) {
        val iv = ByteArray(IV_LENGTH_BYTE).apply {
            SecureRandom().nextBytes(this)
        }
        val cipher = Cipher.getInstance(AES_TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(secretKey, "AES"), GCMParameterSpec(GCM_TAG_LENGTH_BIT, iv))
        }
        val ciphertextBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        EncryptedPayload(
            ciphertext = Base64.getEncoder().encodeToString(ciphertextBytes),
            iv = Base64.getEncoder().encodeToString(iv)
        )
    }

    /**
     * Decrypts ciphertext message payload using AES-256-GCM with a symmetric shared key on Default dispatcher.
     */
    suspend fun decrypt(payload: EncryptedPayload, secretKey: ByteArray): String = withContext(Dispatchers.Default) {
        val iv = Base64.getDecoder().decode(payload.iv)
        val ciphertextBytes = Base64.getDecoder().decode(payload.ciphertext)
        val cipher = Cipher.getInstance(AES_TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(secretKey, "AES"), GCMParameterSpec(GCM_TAG_LENGTH_BIT, iv))
        }
        val plaintextBytes = cipher.doFinal(ciphertextBytes)
        String(plaintextBytes, Charsets.UTF_8)
    }

    /**
     * Generates an Elliptic Curve Diffie-Hellman (ECDH) key pair for 1:1 secure end-to-end encryption.
     */
    fun generateEcdhKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        keyPairGenerator.initialize(ECGenParameterSpec("secp256r1"), SecureRandom())
        return keyPairGenerator.generateKeyPair()
    }

    /**
     * Derives a shared symmetric AES-256 key from local private key and remote public key using ECDH on Default dispatcher.
     */
    suspend fun deriveSharedSecret(privateKey: java.security.PrivateKey, publicKey: java.security.PublicKey): ByteArray = withContext(Dispatchers.Default) {
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(privateKey)
        keyAgreement.doPhase(publicKey, true)
        val sharedSecret = keyAgreement.generateSecret()
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        digest.digest(sharedSecret)
    }

    /**
     * Derives a symmetric group chat key from a base passphrase / group secret and salt using PBKDF2 (600,000 iterations) on Default dispatcher.
     */
    suspend fun deriveGroupKey(passphrase: CharArray, salt: ByteArray): ByteArray = withContext(Dispatchers.Default) {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BIT)
        val tmp = factory.generateSecret(spec)
        tmp.encoded
    }
}
