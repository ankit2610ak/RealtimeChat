package com.realtimechat.core.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
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
    val payload: String // Base64 encoded (IV + Ciphertext)
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
     * Prepends 12-byte IV to ciphertext bytes and encodes as Base64.
     */
    suspend fun encrypt(plaintext: String, secretKey: ByteArray): EncryptedPayload = withContext(Dispatchers.Default) {
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val iv = ByteArray(IV_LENGTH_BYTE).apply { SecureRandom().nextBytes(this) }
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(secretKey, "AES"), spec)

        val ciphertextBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = ByteBuffer.allocate(iv.size + ciphertextBytes.size).put(iv).put(ciphertextBytes).array()

        EncryptedPayload(
            payload = Base64.getEncoder().encodeToString(combined)
        )
    }

    /**
     * Decrypts ciphertext message payload (IV + Ciphertext) using AES-256-GCM with a symmetric shared key on Default dispatcher.
     */
    suspend fun decrypt(payload: EncryptedPayload, secretKey: ByteArray): String = withContext(Dispatchers.Default) {
        val combined = Base64.getDecoder().decode(payload.payload)
        require(combined.size >= IV_LENGTH_BYTE) { "Invalid payload: too short to contain IV" }

        val iv = combined.copyOfRange(0, IV_LENGTH_BYTE)
        val ciphertextBytes = combined.copyOfRange(IV_LENGTH_BYTE, combined.size)

        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(secretKey, "AES"), spec)

        val plaintextBytes = cipher.doFinal(ciphertextBytes)
        String(plaintextBytes, Charsets.UTF_8)
    }

    /**
     * Generates an Elliptic Curve Diffie-Hellman (ECDH) key pair for 1:1 secure end-to-end encryption.
     * Supports Android KeyStore hardware-backed identity key generation when running on device (API 31+).
     */
    fun generateEcdhKeyPair(alias: String? = null): KeyPair {
        return if (alias != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            try {
                val keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_EC,
                    "AndroidKeyStore"
                )
                keyPairGenerator.initialize(
                    KeyGenParameterSpec.Builder(
                        alias,
                        KeyProperties.PURPOSE_AGREE_KEY
                    )
                        .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                        .setUserAuthenticationRequired(false)
                        .build()
                )
                keyPairGenerator.generateKeyPair()
            } catch (_: Exception) {
                generateInMemoryEcdhKeyPair()
            }
        } else {
            generateInMemoryEcdhKeyPair()
        }
    }

    private fun generateInMemoryEcdhKeyPair(): KeyPair {
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
