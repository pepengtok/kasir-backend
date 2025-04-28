package com.kasir.models

import java.util.UUID
import java.time.LocalDateTime

data class KasTransfer(
    val id: UUID,
    val kasAsalId: UUID,
    val kasTujuanId: UUID,
    val tanggal: LocalDateTime,
    val jumlah: Double
)
