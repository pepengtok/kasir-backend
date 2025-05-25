package com.kasir.dto

import kotlinx.serialization.Serializable

// DTO untuk request saat menambah atau mengupdate data Pelanggan
@Serializable
data class PelangganRequest(
    val namaPelanggan: String,
    val no_hp: String? = null,
    val alamat: String? = null,
    val keterangan: String? = null,
    val salesId: String? = null,
    val entitasId: String, // entitas pemilik pelanggan, Wajib
    val latitude: Double? = null, // Tambahan untuk koordinat
    val longitude: Double? = null // Tambahan untuk koordinat
)

// DTO untuk response data Pelanggan (ketika mendapatkan detail atau daftar pelanggan)
@Serializable
data class PelangganResponse(
    val id: String,
    val namaPelanggan: String,
    val no_hp: String? = null,
    val alamat: String? = null,
    val keterangan: String? = null,
    val salesId: String? = null, // ID sales dalam bentuk String
    val entitasId: String, // entitas pemilik pelanggan
    val latitude: Double? = null,
    val longitude: Double? = null
)

// DTO yang sudah ada dari file PelangganDTO.kt Anda:
@Serializable
data class PelangganPembelianResponse(
    val penjualanId: String,
    val tanggal: String,
    val total: Double,
    val metode: String,
    val status: String,
    val jatuhTempo: String?,
    val noNota: String?, // ✅ Tambahkan ini
    val notaUrl: String? // ✅ Tambahkan ini (ini bisa jadi URL ke foto atau PDF)
)