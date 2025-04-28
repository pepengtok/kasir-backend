package com.kasir.models

import java.util.UUID
import java.time.LocalDateTime

data class Penjualan(
    val id: UUID,
    val tanggal: LocalDateTime,
    val pelangganId: UUID?,
    val salesId: UUID?,
    val total: Double,
    val metodePembayaran: String,
    val status: String,
    val kasId: UUID
)
