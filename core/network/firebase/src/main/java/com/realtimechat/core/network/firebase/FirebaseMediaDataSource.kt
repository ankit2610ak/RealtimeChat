package com.realtimechat.core.network.firebase

import com.google.firebase.storage.FirebaseStorage
import com.realtimechat.core.network.MediaRemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseMediaDataSource(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : MediaRemoteDataSource {

    override suspend fun uploadEncryptedMedia(path: String, encryptedBytes: ByteArray): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val ref = storage.reference.child(path)
                ref.putBytes(encryptedBytes).await()
                val downloadUrl = ref.downloadUrl.await()
                downloadUrl.toString()
            }
        }

    override suspend fun downloadEncryptedMedia(url: String): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            runCatching {
                val ref = storage.getReferenceFromUrl(url)
                val maxDownloadSizeBytes: Long = 10 * 1024 * 1024 // 10MB
                ref.getBytes(maxDownloadSizeBytes).await()
            }
        }
}
