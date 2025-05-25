package com.kasir.models

import java.util.UUID
import java.time.LocalDateTime

data class PembayaranHutangSupplier(
    val id: UUID,
    val hutangId: UUID,
    val tanggalBayar: LocalDateTime,
    val jumlahBayar: Double,
    val kasId: UUID,
    val keterangan: String?,
    val entitasId: String
)
