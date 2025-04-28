package com.kasir.models

import java.time.LocalDateTime
import java.util.UUID

data class StockOpname(
    val id: UUID,
    val tanggalOpname: LocalDateTime,
    val produkId: UUID,
    val stokSistem: Int,
    val stokFisik: Int,
    val selisih: Int
)
