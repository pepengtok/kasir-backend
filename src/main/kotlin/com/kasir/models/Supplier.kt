// File: src/main/kotlin/com/kasir/models/SupplierEntity.kt
package com.kasir.models

import java.util.UUID

/**
 * Domain model untuk tabel supplier.
 * Dipisahkan dari DTO (SupplierRequest / SupplierResponse).
 */
data class SupplierEntity(
    val id: UUID,
    val namaSupplier: String,
    val no_hp: String?        = null,
    val alamat: String?        = null,
    val rekeningBank: String?  = null,
    val namaSales: String?     = null,
    val entitasId: String
)
