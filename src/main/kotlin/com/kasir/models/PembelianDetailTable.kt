package com.kasir.models

import org.jetbrains.exposed.sql.Table
import java.util.UUID

object PembelianDetailTable : Table("pembelian_detail") {
    val id = uuid("id").autoGenerate().uniqueIndex()
    val pembelianId = uuid("pembelian_id")
    val produkId = uuid("produk_id")
    val hargaBeli = double("harga_beli")
    val jumlah = integer("jumlah")
    val subtotal = double("subtotal")

    override val primaryKey = PrimaryKey(id)
}