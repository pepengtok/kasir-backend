package com.kasir.models

import java.util.UUID
import java.time.LocalDateTime

data class KasTransaksi(
    val id: UUID,
    val kasId: UUID,
    val tanggal: LocalDateTime,
    val keterangan: String,
    val jumlah: Double,
    val tipe: String // "MASUK" atau "KELUAR"
)
