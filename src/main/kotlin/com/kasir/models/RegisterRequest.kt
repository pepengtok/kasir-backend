package com.kasir.models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val nama: String,
    val email: String,
    val password: String,
    val role: String
)
