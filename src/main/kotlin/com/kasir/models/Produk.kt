package com.kasir.models

import java.util.UUID

data class Produk(
    val id: UUID,
    val namaProduk: String,
    val kodeProduk: String,
    val hargaModal: Double,
    val hargaJual1: Double,
    val hargaJual2: Double,
    val hargaJual3: Double,
    val stok: Int,
    val supplierId: String?,
    val kategoriId: UUID      // 🔥 Ini WAJIB ADA, tanpa nullable
)
