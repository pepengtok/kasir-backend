package com.kasir.models

import java.util.UUID

data class PembelianDetail(
    val id: UUID,
    val pembelianId: UUID,
    val produkId: UUID,
    val hargaBeli: Double,
    val jumlah: Int,
    val subtotal: Double
)