// src/main/kotlin/com/kasir/models/User.kt
package com.kasir.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class User (
    val id: String,
    val nama: String,
    val email: String,
    val role: String
)
