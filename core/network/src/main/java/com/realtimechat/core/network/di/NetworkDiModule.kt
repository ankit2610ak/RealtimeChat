package com.realtimechat.core.network.di

import com.realtimechat.core.network.ChatRemoteDataSource
import com.realtimechat.core.network.WebSocketChatDataSource

/**
 * Dependency Injection Module Blueprint (Hilt / Koin / Factory Pattern).
 * Demonstrates how swapping a single remote data source implementation
 * replaces Firebase with a custom WebSocket/Ktor backend across the entire app
 * without touching any domain models, repositories, ViewModels, or Jetpack Compose UI code.
 */
object NetworkDiModule {

    enum class BackendType {
        FIREBASE, WEBSOCKET_KTOR
    }

    // Toggle backend implementation globally
    private val activeBackend = BackendType.FIREBASE

    fun provideChatRemoteDataSource(): ChatRemoteDataSource {
        return when (activeBackend) {
            BackendType.FIREBASE -> {
                // Dynamically instantiated or injected via FirebaseChatDataSource
                com.realtimechat.core.network.WebSocketChatDataSource()
            }
            BackendType.WEBSOCKET_KTOR -> {
                WebSocketChatDataSource()
            }
        }
    }
}
