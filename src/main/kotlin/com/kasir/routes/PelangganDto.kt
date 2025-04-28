package com.kasir.models

import kotlinx.serialization.Serializable

/**
 * DTO untuk operasi POST/PUT pada Pelanggan.
 */
@Serializable
data class PelangganRequest(
    val namaPelanggan: String,
    val kontak: String?     = null,
    val alamat: String?     = null,
    val keterangan: String? = null,
    val salesId: String?    = null
)

/**
 * DTO untuk response Pelanggan.
 */
@Serializable
data class PelangganResponse(
    val id: String,
    val namaPelanggan: String,
    val kontak: String?,
    val alamat: String?,
    val keterangan: String?,
    val salesId: String?
)
