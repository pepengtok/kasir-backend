package com.kasir.models

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID
import com.kasir.models.EntitasUsahaTable

object PembelianTable : UUIDTable(name = "pembelian") {
    /** Foreign key ke SupplierTable.id */
    val supplierId: Column<EntityID<UUID>> = reference("supplier_id", SupplierTable)
    /** Total amount */
    val total: Column<Double> = double("total")
    /** Status LUNAS/BELUM_LUNAS */
    val status: Column<String> = varchar("status", length = 50)
    /** Metode pembayaran TUNAI/KREDIT */
    val metodePembayaran: Column<String> = varchar("metode_pembayaran", length = 50)
    /** Waktu transaksi */
    val tanggal: Column<LocalDateTime> = datetime("tanggal")
    /** Jatuh tempo (nullable) */
    val jatuhTempo: Column<LocalDateTime?> = datetime("jatuh_tempo").nullable()
    /** FK ke KasTable.id */
    val kasId: Column<EntityID<UUID>?>         = reference("kas_id", KasTable).nullable()
    /** Nota URL (nullable) */
    val notaUrl: Column<String?> = varchar("nota_url", length = 255).nullable()
    val entitasId = reference("entitas_id", EntitasUsahaTable)
    val fotoNota = varchar("foto_nota", 255).nullable()
    val noFaktur      = varchar("no_faktur", 50)
}