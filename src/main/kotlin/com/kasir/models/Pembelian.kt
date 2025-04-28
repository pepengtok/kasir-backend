package com.kasir.models

import java.time.LocalDateTime
import java.util.UUID

data class Pembelian(
    val id: UUID,
    val supplierId: UUID,
    val total: Double,
    val status: String,
    val metodePembayaran: String,
    val tanggal: LocalDateTime,
    val jatuhTempo: LocalDateTime?
)