package com.kasir.models

import java.util.UUID

data class OrderanSalesDetail(
    val id: UUID,
    val orderanId: UUID,
    val produkId: UUID,
    val hargaJual: Double,
    val jumlah: Int,
    val subtotal: Double
)
