// File: src/main/kotlin/com/kasir/models/PenjualanDetailTable.kt
package com.kasir.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import com.kasir.models.EntitasUsahaTable // ✅ Pastikan ini diimpor

object PenjualanDetailTable : UUIDTable("penjualan_detail") {
    val penjualanId = reference(
        name   = "penjualan_id",
        foreign= PenjualanTable,
        onDelete = ReferenceOption.CASCADE
    )

    val produkId = reference(
        name    = "produk_id",
        foreign = ProdukTable,
        onDelete= ReferenceOption.SET_NULL
    ).nullable()
    val hargaModal  = double("harga_modal")
    val hargaJual    = double("harga_jual")
    val jumlah = integer("jumlah")
    val subtotal     = double("subtotal")
    val potensiLaba  = double("potensi_laba").default(0.0)
    val satuan: Column<String>  = text("satuan")
    val entitasId = reference("entitas_id", EntitasUsahaTable) // ✅ Tambahkan baris ini
}