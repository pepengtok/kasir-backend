// File: src/main/kotlin/com/kasir/models/Penjualan.kt
package com.kasir.models

import java.util.UUID
import java.time.LocalDateTime

/**
 * Domain model untuk Penjualan (header).
 */
data class Penjualan(
    val id: UUID,
    val tanggal: LocalDateTime,
    val pelangganId: UUID?,
    val salesId: UUID?,
    val total: Double,
    val hargaModal: Double,
    val entitasId: String,
    val metodePembayaran: String,
    val status: String,
    val kasId: UUID?
)
