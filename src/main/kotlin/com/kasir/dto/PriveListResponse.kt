// src/main/kotlin/com/kasir/dto/PriveListResponse.kt
package com.kasir.dto

import kotlinx.serialization.Serializable



@Serializable
data class PriveListResponse(
    val kasOptions: List<Kas>,
    val privs: List<PriveResponse>,
    val entitasId: String // ✅ tambahan
)


@Serializable
data class PriveRequest(
    val kasId: String,
    val tanggal: String,
    val nominal: Double,
    val keterangan: String,
    val entitasId: String
)

@Serializable
data class PriveResponse(
    val id: String,
    val kasId: String,
    val tanggal: String,
    val nominal: Double,
    val keterangan: String,
    val entitasId: String
)