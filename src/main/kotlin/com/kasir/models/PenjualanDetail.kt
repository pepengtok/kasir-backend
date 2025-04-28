package com.kasir.models

import java.util.UUID

data class PenjualanDetail(
    val id: UUID,
    val penjualanId: UUID,
    val produkId: UUID,
    val hargaJual: Double,
    val jumlah: Int,
    val subtotal: Double
)
