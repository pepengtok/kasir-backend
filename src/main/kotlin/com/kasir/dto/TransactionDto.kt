// File: src/main/kotlin/com/kasir/models/TransantionDTO
package com.kasir.dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID


@Serializable
data class TransactionDto(
    val id: String,
    val tanggal: String,
    val tipe: String,
    val jumlah: Double,
    val keterangan: String?
)