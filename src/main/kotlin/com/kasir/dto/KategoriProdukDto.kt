package com.kasir.dto // Pindahkan ke package dto untuk DTO

import kotlinx.serialization.Serializable

/**
 * DTO untuk kategori produk (sebagai response atau bagian dari response lain)
 */
@Serializable
data class KategoriProdukDto(
    val id: String,
    val namaKategori: String,
    val entitasId: String // ✅ tambahan: entitas pemilik kategori
)

/**
 * DTO untuk request menambah kategori
 */
@Serializable
data class KategoriProdukRequestDto(
    val nama: String,
    val entitasId: String // ✅ tambahan: entitas asal request
)

/**
 * Wrapper untuk list kategori
 */
@Serializable
data class KategoriListDto(val items: List<KategoriProdukDto>) // Mengubah nama