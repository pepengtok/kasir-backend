package com.kasir.dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * DTO untuk menampilkan detail lengkap orderan,
 * termasuk header + list detail.
 */
@Serializable
data class OrderanReview(
    val orderHeader: OrderanSales, // ✅ Ubah nama properti DTO
    val orderDetail: List<OrderanSalesDetail>, // ✅ Ubah nama properti DTO
    val entitasId: String
)

// DTO lainnya (tetap sama)
@Serializable
data class OrderanRequest(
    @Contextual val pelangganId: UUID, // Asumsi masih UUID berdasarkan file Anda
    val metodePembayaran: String,
    val tempoHari: Int? = null,
    val total: Double,
    val detail: List<OrderanDetailRequest>,
    val entitasId: String
)

@Serializable
data class OrderanDetailRequest(
    @Contextual  val produkId: UUID?, // Asumsi masih UUID?
    val namaProduk: String,
    val hargaJual: Double,
    val jumlah: Int,
    val subtotal: Double,
    val entitasId: String
)

@Serializable
data class OrderanSales(
    val id: String,
    @Contextual  val salesId: UUID, // Asumsi masih UUID
    @Contextual  val pelangganId: UUID?, // Asumsi masih UUID?
    val total: Double,
    val metodePembayaran: String,
    val tempoHari: Int?,
    val status: String,
    val tanggalOrder: String,
    val entitasId: String
)

@Serializable
data class OrderanSalesDetail(
    val id: String,
    @Contextual  val orderanId: UUID, // Asumsi masih UUID
    @Contextual  val produkId: UUID?, // Asumsi masih UUID?
    val namaProduk: String,
    val hargaJual: Double,
    val jumlah: Int,
    val subtotal: Double,
    val entitasId: String
)

@Serializable
data class KirimOrderanRequest(
    @Contextual    val orderanId: UUID, // Asumsi masih UUID
    @Contextual val kasId: UUID? = null
)

@Serializable
data class ApproveDetailRequest(
    val detailId: String,
    val produkId: String,
    val namaProduk: String,
    val hargaModal: Double,
    val hargaJual: Double,
    val qty: Int
)

@Serializable
data class EditOrderanDetailRequest(
    @Contextual   val produkId: UUID, // Asumsi masih UUID
    val namaProduk: String,
    val hargaJual: Double,
    val jumlah: Int,
    val subtotal: Double,
    val entitasId: String
)

// Catatan: Jika Anda menggunakan @Contextual, pastikan Anda juga mengimpornya.
// Jika tidak, sebaiknya Anda ubah tipe UUID menjadi String di semua DTO ini
// dan melakukan konversi UUID.fromString() / .toString() di Routes.
// Berdasarkan file OrderanDTO.kt Anda, Anda menggunakan @Contextual UUID.
// Saya akan pertahankan itu untuk saat ini.