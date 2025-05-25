package com.kasir.dto

import kotlinx.serialization.Serializable

/**
 * DTO untuk mencatat transaksi kas (masuk/keluar).
 *
 * @param kasId      UUID akun kas yang digunakan
 * @param tanggal    ISO-8601 string timestamp transaksi
 * @param jumlah     Nilai uang yang masuk atau keluar
 * @param tipe       "MASUK" atau "KELUAR"
 * @param keterangan Deskripsi transaksi
 */
@Serializable
data class KasRecordRequestDto(
    val entitasId: String, // ✅ tambahan
    val kasId: String,
    val tanggal: String,
    val jumlah: Double,
    val tipe: String,
    val keterangan: String? = null
)
@Serializable
data class KasTransferRequestDto(
    val entitasId: String, // ✅ tambahan
    val kasAsalId: String,
    val kasTujuanId: String,
    val tanggal: String,
    val jumlah: Double
)

    @Serializable
data class KasTransfer(
    val id: String,
    val entitasId: String, // ✅ cukup String UUID untuk DTO
    val kasAsalId: String,
    val kasTujuanId: String,
    val tanggal: String,
    val jumlah: Double
)
@Serializable
data class KasTransaksi(
    val id: String,
    val kasId: String,
    val tanggal: String,
    val keterangan: String,
    val jumlah: Double,
    val tipe: String, // "MASUK" atau "KELUAR"
    val entitasId: String
)

@Serializable
data class Kas(
    val id: String? = null,
    val namaKas: String,
    val saldoAkhir: Double,
    val jenis: String,              // Tambahan
    val subJenis: String? = null ,  // Tambahan
    val entitasId: String
)