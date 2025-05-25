package com.kasir.dto

import kotlinx.serialization.Serializable

@Serializable
data class RiwayatKasDto(
    val id: String,
    val tanggal: String,
    val kasId: String,
    val jumlah: Double,
    val keterangan: String?,
    val tipe: String,
    val entitasId: String
)

@Serializable
data class RiwayatKasResponseDto(
    val data: List<RiwayatKasDto>,
    val entitasId: String
)

@Serializable
data class RiwayatKasEntryDto(
    val id: String,
    val tanggal: String,
    val kasId: String,
    val jumlah: Double,
    val keterangan: String,
    val tipe: String,
    val entitasId: String
)
