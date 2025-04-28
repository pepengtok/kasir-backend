package com.kasir.models

import java.util.UUID
import java.time.LocalDateTime

data class KomisiSales(
    val id: UUID,
    val salesId: UUID,
    val penjualanId: UUID,
    val komisiPersen: Double,
    val nominalKomisi: Double,
    val status: String,
    val tanggalKomisi: LocalDateTime
)
