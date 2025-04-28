package com.kasir.models

import java.util.UUID
import java.time.LocalDateTime

data class HutangSupplier(
    val id: UUID,
    val pembelianId: UUID,
    val supplierId: UUID,
    val totalHutang: Double,
    val sisaHutang: Double,
    val tanggalJatuhTempo: LocalDateTime
)
