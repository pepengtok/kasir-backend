package com.kasir.dto

import kotlinx.serialization.Serializable




// Pastikan DTO hanya menggunakan String



@Serializable
data class StokFilterRequestDto(
    val tanggalMulai: String? = null,
    val tanggalAkhir: String? = null,
    val entitasId: String
)