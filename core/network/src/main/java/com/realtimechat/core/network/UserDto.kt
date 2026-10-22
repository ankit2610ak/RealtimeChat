package com.realtimechat.core.network

data class UserDto(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val publicKey: String = "",
    val createdAt: Long = 0L
)
