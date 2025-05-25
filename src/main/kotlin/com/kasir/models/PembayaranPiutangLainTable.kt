package com.kasir.models

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID
import com.kasir.models.EntitasUsahaTable

object PembayaranPiutangLainTable : UUIDTable(name = "pembayaran_piutang_lain") {
    /** FK ke PiutangLainTable.id */
    val piutangId: Column<EntityID<UUID>> = reference("piutang_id", PiutangLainTable)
    /** Tanggal bayar */
    val tanggalBayar: Column<LocalDateTime> = datetime("tanggal_bayar")
    /** Jumlah bayar */
    val jumlahBayar: Column<Double> = double("jumlah_bayar")
    /** FK ke KasTable.id */
    val kasId: Column<EntityID<UUID>> = reference("kas_id", KasTable)
    /** Keterangan */
    val keterangan: Column<String?> = varchar("keterangan", length = 255).nullable()
    val entitasId = reference("entitas_id", EntitasUsahaTable)
}