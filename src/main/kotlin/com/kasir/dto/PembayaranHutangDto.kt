// src/main/kotlin/com/kasir/dto/PembayaranHutangDto.kt
package com.kasir.dto

import kotlinx.serialization.Serializable

/**
 * DTO untuk request pembayaran hutang (supplier, bank, atau lain-lain)
 */
@Serializable
data class PembayaranHutangRequest(
    val kasId: String,
    val jumlahBayar: Double,
    val tanggalBayar: String,
    val keterangan: String? = null,
    val entitasId: String // ✅ Tambahan
)

/**
 * DTO untuk merepresentasikan record pembayaran hutang saat dikembalikan ke client
 */
@Serializable
data class PembayaranRecord(
    val id: String,
    val tanggalBayar: String,
    val jumlahBayar: Double,
    val kasId: String,
    val keterangan: String? = null,
    val entitasId: String // ✅ Tambahan
)

/**
 * DTO untuk entry laporan pembayaran hutang
 * @param tanggal  Tanggal pembayaran (ISO-8601 string)
 * @param supplier Nama supplier
 * @param jumlah   Nominal bayar
 * @param keterangan Keterangan pembayaran
 */
@Serializable
data class PembayaranHutangEntryDto(
    val tanggal: String,
    val supplier: String,
    val jumlah: Double,
    val keterangan: String?,
    val entitasId: String
)

