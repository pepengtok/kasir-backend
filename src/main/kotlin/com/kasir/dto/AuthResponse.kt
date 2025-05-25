package com.kasir.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserDto    // ← this now refers to the User in User.kt
)