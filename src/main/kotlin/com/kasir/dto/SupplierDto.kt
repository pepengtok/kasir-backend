package com.kasir.dto

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
    val namaSales: String? = null,
    val entitasId: String
)

/**
 * DTO untuk response Supplier.
 */
@Serializable
data class SupplierResponse( // Nama DTO respons lengkap jika dipakai langsung
    val id: String,
    val namaSupplier: String,
    val no_hp: String?,
    val alamat: String?,
    val rekeningBank: String?,
    val namaSales: String?,
    val entitasId: String
)
// DTO untuk Supplier (pastikan ini di package yang sama atau diimpor)
@Serializable
data class SupplierDto(
    val id: String,
    val nama_supplier: String
)
