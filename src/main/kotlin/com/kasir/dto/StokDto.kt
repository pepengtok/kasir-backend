package com.kasir.dto
import kotlinx.serialization.Serializable

/**
 * DTO untuk ringkasan stok barang
 * @param namaProduk  Nama produk
 * @param stok        Jumlah stok saat ini
 * @param hargaModal  Harga modal per satuan
 * @param totalModal  Total nilai modal (stok * hargaModal)
 */
@Serializable
data class StokBarangEntryDto(
    val id: String,
    val nama_produk: String,
    val stok_masuk: Double? = null,
    val stok_keluar: Double? = null,
    val stok_terakhir: Double,
    val harga_modal: Double,
    val total_modal: Double
)

/** Request DTO untuk Stock Opname */
@Serializable
data class StockOpname(
    val tanggalOpname: String,
    val produkId: String,
    val stokSistem: Double,
    val stokFisik: Double,
    val selisih: Double,
    val entitasId: String,
    val keterangan: String? = null
)

@Serializable
data class StockOpnameResponse(
    val id: String,
    val tanggalOpname: String,
    val produkId: String,
    val produkName: String,
    val stokSistem: Double,
    val stokFisik: Double,
    val selisih: Double,
    val keterangan: String? = null
)