package com.kasir.models

import java.util.UUID
import java.time.LocalDateTime

data class PembayaranPiutangPelanggan(
    val id: UUID,
    val piutangId: UUID,
    val tanggalBayar: LocalDateTime,
    val jumlahBayar: Double,
    val kasId: UUID,
    val keterangan: String?
)
