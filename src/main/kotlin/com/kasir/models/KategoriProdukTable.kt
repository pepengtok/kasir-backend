package com.kasir.models

import org.jetbrains.exposed.sql.Table
import java.util.UUID

object KategoriProdukTable : Table("kategori_produk") {
    val id = uuid("id").autoGenerate().uniqueIndex()
    val namaKategori = varchar("nama", 255) // ✅ Ganti jadi namaKategori
    override val primaryKey = PrimaryKey(id)
}
