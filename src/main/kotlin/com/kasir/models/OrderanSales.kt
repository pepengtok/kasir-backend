package com.kasir.models

import java.util.UUID
import java.time.LocalDateTime

data class OrderanSales(
    val id: UUID,
    val salesId: UUID,
    val pelangganId: UUID?,
    val total: Double,
    val status: String,
    val tanggalOrder: LocalDateTime
)
