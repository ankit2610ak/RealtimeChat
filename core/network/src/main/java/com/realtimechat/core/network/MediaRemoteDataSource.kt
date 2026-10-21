package com.realtimechat.core.network

interface MediaRemoteDataSource {
    suspend fun uploadEncryptedMedia(path: String, encryptedBytes: ByteArray): Result<String>
    suspend fun downloadEncryptedMedia(url: String): Result<ByteArray>
}
