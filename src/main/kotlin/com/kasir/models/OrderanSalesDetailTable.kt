package com.kasir.models

import org.jetbrains.exposed.sql.Table

object OrderanSalesDetailTable : Table("orderan_sales_detail") {
    val id = uuid("id").autoGenerate().uniqueIndex()
    val orderanId = uuid("orderan_id")
    val produkId = uuid("produk_id")
    val hargaJual = double("harga_jual")
    val jumlah = integer("jumlah")
    val subtotal = double("subtotal")

    override val primaryKey = PrimaryKey(id)
}
