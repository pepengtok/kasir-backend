package com.kasir.models

import kotlinx.serialization.Serializable

/**
 * Represents a product category for API responses and requests.
 * The ID is a String of a UUID for easy JSON serialization.
 */
@Serializable
data class KategoriProduk(
    val id: String,
    val namaKategori: String
)