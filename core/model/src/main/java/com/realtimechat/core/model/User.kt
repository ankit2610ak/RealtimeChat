package com.realtimechat.core.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val uid: String,
    val displayName: String,
    val email: String,
    val publicKey: String,
    val createdAt: Long
)
