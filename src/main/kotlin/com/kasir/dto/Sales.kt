package com.kasir.dto

import kotlinx.serialization.Serializable

@Serializable
data class Sales(
    val id: String,
    val nama: String,
    val komisiTunai: Double,
    val komisiPiutang: Double,
    val createdAt: String
)

@Serializable
data class SalesRequest(
    val nama: String,
    val komisiTunai: Double,
    val komisiPiutang: Double
)
