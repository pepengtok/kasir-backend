package com.kasir.models

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID
import com.kasir.models.EntitasUsahaTable

object StockOpnameTable : UUIDTable(name = "stock_opname") {
    /** Tanggal opname */
    val tanggalOpname: Column<LocalDateTime> = datetime("tanggal_opname")
    /** FK ke ProdukTable.id */
    val produkId: Column<EntityID<UUID>>      = reference("produk_id", ProdukTable)
    /** Stok sistem berdasarkan database */
    val stokSistem: Column<Double> = double("stok_sistem").default(0.0)
    /** Stok fisik hasil opname */
    val stokFisik: Column<Double> = double("stok_fisik").default(0.0)
    /** Selisih antara fisik dan sistem */
    val selisih: Column<Double> = double("selisih").default(0.0)
    val entitasId = reference("entitas_id", EntitasUsahaTable)
    /** Keterangan opsional */
    val keterangan: Column<String?>           = varchar("keterangan", length = 255).nullable()
}