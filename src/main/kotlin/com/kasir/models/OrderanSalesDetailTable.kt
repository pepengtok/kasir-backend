// File: src/main/kotlin/com/kasir/models/OrderanSalesDetailTable.kt
package com.kasir.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import com.kasir.models.EntitasUsahaTable

/**
 * Tabel detail setiap baris produk di orderan.
 * ID otomatis dibuat oleh UUIDTable.
 */
object OrderanSalesDetailTable : UUIDTable("orderan_detail") {
    /** FK ke header orderan */
    val orderanId  = reference(
        "orderan_id",
        OrderanSalesTable.id,
        onDelete = ReferenceOption.CASCADE
    )
    /** FK ke tabel produk, nullable jika produk manual */
    val produkId   = reference(
        "produk_id",
        ProdukTable.id,
        onDelete = ReferenceOption.SET_NULL
    ).nullable()
    /** Nama produk (digunakan saat produk tidak ada di tabel Produk) */
    val namaProduk = varchar("nama_produk", length = 255)
    /** Harga jual saat orderan dibuat */
    val hargaJual  = double("harga_jual")
    /** Jumlah unit yang dipesan */
    val jumlah     = integer("jumlah")
    /** Subtotal = hargaJual * jumlah */
    val subtotal   = double("subtotal")
    val entitasId = reference("entitas_id", EntitasUsahaTable)
    // `id` sudah otomatis didefinisikan oleh UUIDTable
}
