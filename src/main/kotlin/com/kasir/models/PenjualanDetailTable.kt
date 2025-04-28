package com.kasir.models

import org.jetbrains.exposed.sql.Table

object PenjualanDetailTable : Table("penjualan_detail") {
    val id = uuid("id").autoGenerate().uniqueIndex()
    val penjualanId = uuid("penjualan_id")
    val produkId = uuid("produk_id")
    val hargaJual = double("harga_jual")
    val jumlah = integer("jumlah")
    val subtotal = double("subtotal")

    override val primaryKey = PrimaryKey(id)
}
