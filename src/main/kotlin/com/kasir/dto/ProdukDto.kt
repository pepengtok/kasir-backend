package com.kasir.dto // Asumsi package Anda adalah dto

import kotlinx.serialization.Serializable

@Serializable
data class ProdukRequestDto(
    val namaProduk: String,
    val kodeProduk: String,
    val hargaModal: Double,
    val hargaJual1: Double,
    val hargaJual2: Double? = null,
    val hargaJual3: Double? = null,
    val stok: Double,
    val satuan: String,
    val supplierId: String? = null,
    val kategoriId: String,
    val berat_gram: Double? = null,
    val panjang_cm: Double? = null,
    val lebar_cm: Double? = null,
    val tinggi_cm: Double? = null
)

@Serializable
data class ProdukResponseDto(
    val id: String,
    val namaProduk: String,
    val kodeProduk: String,
    val hargaModal: Double,
    val hargaJual1: Double,
    val hargaJual2: Double?,
    val hargaJual3: Double?,
    val stok: Double,
    val satuan: String,
    val supplier: SupplierDto?, // Objek SupplierDto
    val kategori: KategoriProdukDto, // Ini sekarang non-nullable karena kategoriId di ProdukTable adalah non-nullable reference
    val entitas_id: String,
    val berat_gram: Double? = null,
    val panjang_cm: Double? = null,
    val lebar_cm: Double? = null,
    val tinggi_cm: Double? = null
)

@Serializable
data class Produk(
    val id: String,
    val namaProduk: String,
    val kodeProduk: String,
    val hargaModal: Double,
    val hargaJual1: Double,
    val hargaJual2: Double?,
    val hargaJual3: Double?,
    val stok: Double,
    val satuan: String,       // Field baru
    val supplierId: String?,
    val kategoriId: String,
    val entitasId: String
)

data class ProdukRiwayatResponse(
    val pembelianId: String,
    val tanggal: String,
    val jumlah: Int,
    val hargaModal: Double,
    val subtotal: Double,
    val metode: String,
    val status: String,
    val jatuhTempo: String?,
    val supplierId: String,
    val supplierNama: String
)