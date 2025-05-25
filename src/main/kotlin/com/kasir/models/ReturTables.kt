package com.kasir.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import com.kasir.models.EntitasUsahaTable

// Tabel untuk mencatat retur penjualan
object ReturPenjualanTable : UUIDTable("retur_penjualan") {
    val tanggalRetur: Column<LocalDateTime> = datetime("tanggal_retur")
    val jumlahRetur: Column<Double> = double("jumlah_retur")
    val keterangan: Column<String> = text("keterangan")
    val pelangganId = reference("pelanggan_id", PelangganTable)
    val penjualanId = reference("penjualan_id", PenjualanTable)
    val entitasId = reference("entitas_id", EntitasUsahaTable)
}

// Tabel untuk mencatat retur pembelian
object ReturPembelianTable : UUIDTable("retur_pembelian") {
    val tanggalRetur: Column<LocalDateTime> = datetime("tanggal_retur")
    val jumlahRetur: Column<Double> = double("jumlah_retur")
    val keterangan: Column<String> = text("keterangan")
    val supplierId = reference("supplier_id", SupplierTable)
    val pembelianId = reference("pembelian_id", PembelianTable)
    val entitasId = reference("entitas_id", EntitasUsahaTable)
}
