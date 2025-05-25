package com.kasir.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import com.kasir.models.EntitasUsahaTable

object KategoriProdukTable : UUIDTable(name = "kategori_produk") {
    /** Nama kategori produk */
    val namaKategori: Column<String> = varchar("nama_kategori", length = 255)
    val entitasId = reference("entitas_id", EntitasUsahaTable)
}