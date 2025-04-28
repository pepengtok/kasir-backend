package com.kasir.models

import java.util.UUID
import java.time.LocalDateTime

data class PiutangPelanggan(
    val id: UUID,
    val penjualanId: UUID,
    val pelangganId: UUID,
    val totalPiutang: Double,
    val sisaPiutang: Double,
    val tanggalJatuhTempo: LocalDateTime
)
