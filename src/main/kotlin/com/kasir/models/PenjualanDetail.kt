// File: src/main/kotlin/com/kasir/models/PenjualanDetail.kt
package com.kasir.models

import java.util.UUID

/**
 * Domain model untuk detail Penjualan.
 */
data class PenjualanDetail(
    val id: UUID,
    val penjualanId: UUID,
    val produkId: UUID?,
    val hargaJual: Double,
    val hargaModal: Double,

    val jumlah: Int,
    val subtotal: Double,
    val potensiLaba: Double // ✅ Tambahan field
)
