package com.kasir.dto

import kotlinx.serialization.Serializable

@Serializable
data class PenjualanRequestDto(
    val pelangganId: String?,
    val salesId: String?,
    val tanggal: String,
    val metodePembayaran: String,
    val status: String,
    val total: Double,
    val noNota: String?,
    val kasId: String?,
    val jatuhTempo: String?,
    val entitasId: String,
    val items: List<PenjualanDetailRequestDto> = emptyList()
)

@Serializable
data class PenjualanDetailRequestDto(
    val produkId: String,
    val satuan: String,
    val jumlah: Double,
    val hargaJual: Double,
    val subtotal: Double
)

@Serializable
data class PenjualanResponseDto(
    val id: String,
    val pelangganId: String?, // ID pelanggan
    val namaPelanggan: String?, // ✅ Nama pelanggan (untuk display)
    val salesId: String?,     // ID sales
    val namaSales: String?,   // ✅ Nama sales (untuk display)
    val tanggal: String,
    val metodePembayaran: String,
    val status: String,
    val total: Double,
    val noNota: String,       // <-- DI SINI TETAP NON-NULLABLE UNTUK RESPONSE
    val notaUrl: String?,     // ✅ Tambahkan: URL Nota
    val kasId: String?,
    val namaKas: String?,     // ✅ Nama kas (untuk display)
    val jatuhTempo: String?,
    val entitasId: String,
    val createdAt: String,
    val detail: List<PenjualanDetailResponseDto> = emptyList() // Waktu pembuatan
)

@Serializable
data class PenjualanDetailResponseDto(
    val id: String,
    val produkId: String,
    val namaProduk: String, // ✅ Nama produk (untuk display)
    val satuan: String,
    val jumlah: Int,
    val hargaJual: Double,
    val hargaModal: Double, // ✅ Harga modal produk saat penjualan
    val subtotal: Double,
    val potensiLaba: Double // ✅ Potensi laba per item
)

@Serializable
data class PenjualanResponseFull(
    val id: String,
    val noNota: String,
    val tanggal: String,
    val pelangganId: String?,
    val total: Double,
    val metodePembayaran: String,
    val status: String,
    val kasId: String?,
    val detail: List<PenjualanDetailResponseDto>,
    val entitasId: String // ✅ Tambahan
)

/**
 * DTO untuk update data penjualan.
 * Hanya mencakup field yang bisa diperbarui.
 */
@Serializable
data class PenjualanUpdateRequest(
    val total: Double,
    val metodePembayaran: String,
    val status: String,
    val kasId: String? = null,
    val jatuhTempo: String? = null
)

// Di PenjualanDto.kt
@Serializable
data class PenjualanResponseWithDetailDto(
    val penjualan: PenjualanResponseDto,
    val detail: List<PenjualanDetailResponseDto>
)