package com.kasir.dto

import kotlinx.serialization.Serializable

/**
 * DTO untuk ringkasan hutang lain
 * @param id               UUID hutang_lain
 * @param nama             Deskripsi hutang
 * @param tanggal          Tanggal pencatatan hutang
 * @param totalHutang      Total hutang awal
 * @param sisaHutang       Sisa hutang saat ini
 * @param tanggalJatuhTempo Tanggal jatuh tempo hutang
 * @param status           Status: BELUM_LUNAS atau LUNAS
 * @param fotoNotaUrl      URL foto nota (nullable)
 * @param keterangan       Keterangan tambahan
 */
@Serializable
data class HutangLainDto(
    val id: String,
    val nama: String,
    val tanggal: String,
    val totalHutang: Double,
    val sisaHutang: Double,
    val tanggalJatuhTempo: String,
    val status: String,
    val fotoNotaUrl: String? = null,
    val keterangan: String,
    val entitasId: String // ✅ tambahan
)

@Serializable
data class HutangLainEntryDto(
    val keterangan: String? = "",
    val tanggalJatuhTempo: String,
    val totalHutang: Double,
    val sisaHutang: Double,
    val status: String,
    val entitasId: String
)


@Serializable
data class HutangBankEntryDto(
    val bank: String,
    val tanggalJatuhTempo: String,
    val totalHutang: Double,
    val sisaHutang: Double,
    val status: String,
    val entitasId: String
)
