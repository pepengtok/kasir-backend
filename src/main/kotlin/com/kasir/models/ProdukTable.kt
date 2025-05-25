// src/main/kotlin/com/kasir/models/ProdukTable.kt
package com.kasir.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Column
import com.kasir.models.EntitasUsahaTable

object ProdukTable : UUIDTable(name = "produk") {
    val namaProduk: Column<String> = varchar("nama_produk", 255)
    val kodeProduk: Column<String> = varchar("kode_produk", 100).uniqueIndex()
    val entitasId = reference("entitas_id", EntitasUsahaTable)
    val hargaModal: Column<Double> = double("harga_modal")
    val hargaJual1: Column<Double> = double("harga_jual_1")
    val hargaJual2: Column<Double?> = double("harga_jual_2").nullable()
    val hargaJual3: Column<Double?> = double("harga_jual_3").nullable()

    val stok: Column<Double> = double("stok").default(0.0)

    val satuan: Column<String> = varchar("satuan", 10).default("LUSIN")

    val supplierId = optReference(
        name = "supplier_id",
        foreign = SupplierTable,
        onDelete = ReferenceOption.SET_NULL
    )

    val kategoriId = reference(
        name = "kategori_id",
        refColumn = KategoriProdukTable.id,
        onDelete = ReferenceOption.CASCADE
    )

    // --- TAMBAHAN BARU (Pastikan ini ada di file Anda) ---
    val beratGram: Column<Double?> = double("berat_gram").nullable()
    val panjangCm: Column<Double?> = double("panjang_cm").nullable()
    val lebarCm: Column<Double?> = double("lebar_cm").nullable()
    val tinggiCm: Column<Double?> = double("tinggi_cm").nullable()
    // --- AKHIR TAMBAHAN BARU ---
}