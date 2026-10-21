package com.realtimechat.core.network

import kotlinx.coroutines.flow.Flow

interface AuthRemoteDataSource {
    val authStateFlow: Flow<String?>
    suspend fun signIn(email: String, token: String): Result<String>
    suspend fun signOut()
    suspend fun getCurrentUserId(): String?
}
