// File: src/main/kotlin/com/kasir/models/ProdukTable.kt
package com.kasir.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

/**
 * Table definition for Produk matching actual DB schema
 */
object ProdukTable : UUIDTable(name = "produk") {
    val namaProduk  = varchar("nama_produk", 255)
    val kodeProduk  = varchar("kode_produk", 100).uniqueIndex()
    val hargaModal  = double("harga_modal")
    val hargaJual1  = double("harga_jual_1")
    val hargaJual2  = double("harga_jual_2").nullable()
    val hargaJual3  = double("harga_jual_3").nullable()
    val stok        = integer("stok")
    val kategoriId  = uuid("kategori_id").references(KategoriProdukTable.id, onDelete = ReferenceOption.CASCADE)
    val supplierId  = uuid("supplier_id").nullable()
}

