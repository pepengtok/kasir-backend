// src/main/kotlin/com/kasir/dto/ModalDto.kt
package com.kasir.dto

import kotlinx.serialization.Serializable

@Serializable
data class ModalListResponse(
    val kasOptions: List<Kas>,
    val modals: List<ModalResponse>,
    val entitasId: String // ✅
)

@Serializable
data class ModalRequest(
    val kasId: String,
    val tanggal: String,
    val nominal: Double,
    val keterangan: String,
    val entitasId: String
)

@Serializable
data class ModalResponse(
    val id: String,
    val kasId: String,
    val tanggal: String,
    val nominal: Double,
    val keterangan: String,
    val entitasId: String
)