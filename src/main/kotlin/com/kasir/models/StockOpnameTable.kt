package com.kasir.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime // ✅ Tambahkan ini

object StockOpnameTable : Table("stock_opname") {
    val id = uuid("id").autoGenerate().uniqueIndex()
    val tanggalOpname = datetime("tanggal_opname") // ✅ sekarang tidak error lagi
    val produkId = uuid("produk_id")
    val stokSistem = integer("stok_sistem")
    val stokFisik = integer("stok_fisik")
    val selisih = integer("selisih")

    override val primaryKey = PrimaryKey(id)
}
