// File: src/main/kotlin/com/kasir/models/HutangSupplier.kt
package com.kasir.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID
import com.kasir.models.EntitasUsahaTable

@Serializable
data class HutangSupplier(
    @Contextual val id: UUID,
    val supplierId: String,
    @Contextual val tanggal: LocalDateTime,
    val totalHutang: Double,
    val sisaHutang: Double,
    @Contextual val tanggalJatuhTempo: LocalDateTime,
    val status: String,
    val entitasId: String
)
