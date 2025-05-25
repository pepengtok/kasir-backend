package com.kasir.dto

import kotlinx.serialization.Serializable

/**
 * DTO untuk request pembelian utama
 */
@Serializable
data class PembelianRequestDto(
    val supplierId: String,
    val metodePembayaran: String,
    val kasId: String?,
    val jatuhTempo: String? = null,
    val notaUrl: String? = null,
    val detail: List<PembelianDetailRequestDto>,
val entitasId: String // ✅ Tambahan
)


/**
 * DTO untuk setiap detail item pembelian
 */
@Serializable
data class PembelianDetailRequestDto(
    val produkId: String,
    val hargaModal: Double,
    val jumlah: Int
)

/**
 * DTO untuk response header pembelian (ringkasan)
 */
@Serializable
data class PembelianResponseDto(
    val id: String,
    val tanggal: String,
    val supplierId: String,
    val supplierName: String,
    val total: Double,
    val metodePembayaran: String,
    val status: String,
    val kasId: String?,
    val notaUrl: String?,
    val noFaktur: String?,
    val detail: List<PembelianDetailResponseDto>,
    val entitasId: String // ✅ Tambahan
)

/**
 * DTO untuk setiap detail response pembelian
 */
@Serializable
data class PembelianDetailResponseDto(
    val id: String,
    val produkId: String,
    val produkName: String,
    val hargaModal: Double,
    val jumlah: Int,
    val subtotal: Double
)

@Serializable
data class PembelianRequest(
    val supplierId: String,
    val kasId: String,
    val metodePembayaran: String,
    val jatuhTempo: String? = null,
    val notaUrl: String? = null,
    val detail: List<PembelianDetailRequest>,
    val entitasId: String // ✅ Tambahan
)

@Serializable
data class PembelianDetailRequest(
    val produkId: String,
    val hargaModal: Double,
    val jumlah: Int
)

@Serializable
data class PembelianResponse(
    val id: String,
    val tanggal: String,
    val supplierId: String,
    val supplierName: String,
    val total: Double,
    val metodePembayaran: String,
    val status: String,
    val kasId: String?,
    val notaUrl: String?,
    val detail: List<PembelianDetailResponse>,
    val entitasId: String // ✅ Tambahan
)

@Serializable
data class PembelianDetailResponse(
    val id: String,
    val produkId: String,
    val produkName: String,
    val hargaModal: Double,
    val jumlah: Int,
    val subtotal: Double
)