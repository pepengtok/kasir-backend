package com.kasir.models

import java.util.UUID

data class Pelanggan(
    val id: UUID,
    val namaPelanggan: String,
    val kontak: String?,
    val alamat: String?,
    val keterangan: String?
)
