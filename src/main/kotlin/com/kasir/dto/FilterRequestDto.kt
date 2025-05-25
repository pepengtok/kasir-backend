package com.kasir.dto

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate
/**
 * DTO untuk menerima filter tanggal pada laporan.
 * @param tipe   Jenis filter: HARIAN, BULANAN, TAHUNAN
 * @param tanggal Tanggal dalam format ISO ("yyyy-MM-dd"), optional untuk tipe lain
 */
@Serializable
data class FilterRequestDto(
    val tipe: String,
    val tanggal: String? = null,
    val entitasId: String
)

