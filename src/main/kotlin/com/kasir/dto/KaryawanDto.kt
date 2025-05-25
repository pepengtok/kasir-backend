package com.kasir.dto

import kotlinx.serialization.Serializable

@Serializable
data class KaryawanRequest(
    val nama: String,
    val bulanTerdaftar: String,
    val entitasId: String
)

@Serializable
data class KaryawanResponse(
    val id: String,
    val nama: String,
    val bulanTerdaftar: String,
    val createdAt: String,
    val entitasId: String
)

@Serializable
data class AbsensiKaryawan(
    val id: String,
    val karyawanId: String,
    val tanggal: String,
    val statusAbsen: String,
    val keterangan: String? = null,
    val createdAt: String,
    val entitasId: String
)

@Serializable
data class AbsensiKaryawanRequest(
    val karyawanId: String,
    val tanggal: String,
    val statusAbsen: String,
    val keterangan: String? = null,
    val entitasId: String
)