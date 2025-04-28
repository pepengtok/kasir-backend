// File: src/main/kotlin/com/kasir/models/ProdukDto.kt
package com.kasir.models

import kotlinx.serialization.Serializable

@Serializable
data class ProdukRequest(
    val namaProduk: String,
    val kodeProduk: String,
    val hargaModal: Double,
    val hargaJual1: Double,
    val hargaJual2: Double?,
    val hargaJual3: Double?,
    val stok: Int,
    val supplierId: String?,
    val kategoriId: String
)

@Serializable
data class ProdukResponse(
    val id: String,
    val namaProduk: String,
    val kodeProduk: String,
    val hargaModal: Double,
    val hargaJual1: Double,
    val hargaJual2: Double?,
    val hargaJual3: Double?,
    val stok: Int,
    val supplierId: String?,
    val kategoriId: String
)
