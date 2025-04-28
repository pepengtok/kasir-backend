// File: src/main/kotlin/com/kasir/models/SupplierDto.kt
package com.kasir.models

import kotlinx.serialization.Serializable

/**
 * DTO untuk operasi POST/PUT pada Supplier.
 */
@Serializable
data class SupplierRequest(
    val namaSupplier: String,
    val no_hp: String? = null,
    val alamat: String? = null,
    val rekeningBank: String? = null,
    val namaSales: String? = null
)

/**
 * DTO untuk response Supplier.
 */
@Serializable
data class SupplierResponse(
    val id: String,
    val namaSupplier: String,
    val no_hp: String?,
    val alamat: String?,
    val rekeningBank: String?,
    val namaSales: String?
)
