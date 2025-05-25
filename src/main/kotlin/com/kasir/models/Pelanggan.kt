package com.kasir.models

import java.util.UUID

data class Pelanggan(
    val id: UUID,
    val namaPelanggan: String,
    val no_hp: String?,
    val alamat: String?,
    val keterangan: String?,
    val entitasId: String
)
