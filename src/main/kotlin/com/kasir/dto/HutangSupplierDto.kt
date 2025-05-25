package com.kasir.dto

import kotlinx.serialization.Serializable

/**
 * DTO untuk ringkasan hutang supplier
 * @param id              UUID hutang_supplier
 * @param partnerId       UUID supplier
 * @param partnerName     Nama supplier
 * @param tanggal         Tanggal pencatatan hutang
 * @param totalHutang     Total hutang awal
 * @param sisaHutang      Sisa hutang saat ini
 * @param jatuhTempo      Tanggal jatuh tempo hutang
 * @param status          Status hutang: BELUM_LUNAS atau LUNAS
 * @param fotoNotaUrl     URL foto nota (jika ada)
 */
@Serializable
data class HutangSupplierDto(
    val id: String,
    val partnerId: String,
    val partnerName: String,
    val tanggal: String,
    val totalHutang: Double,
    val sisaHutang: Double,
    val jatuhTempo: String,
    val status: String,
    val fotoNotaUrl: String? = null,
    val entitasId: String // ✅ tambahan
)

@Serializable
data class HutangSupplierEntryDto(
    val supplier: String,
    val tanggalJatuhTempo: String,
    val totalHutang: Double,
    val sisaHutang: Double,
    val status: String,
    val pembelianId: String? = null,
    val noFaktur: String? = null,
    val fotoNota: String? = null,
    val notaUrl: String? = null,
    val entitasId: String
)




