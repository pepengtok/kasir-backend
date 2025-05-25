// File: src/main/kotlin/com/kasir/models/PembayaranPiutangKaryawanTable.kt
package com.kasir.models

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import com.kasir.models.EntitasUsahaTable
import java.util.UUID

object PembayaranPiutangKaryawanTable : UUIDTable(name = "pembayaran_piutang_karyawan") {
    /** FK ke PiutangKaryawanTable.id */
    val piutangId: Column<EntityID<UUID>> = reference("piutang_id", PiutangKaryawanTable)
    /** Tanggal bayar */
    val tanggalBayar: Column<LocalDateTime> = datetime("tanggal_bayar")
    /** Jumlah bayar */
    val jumlahBayar: Column<Double> = double("jumlah_bayar")
    /** FK ke KasTable.id */
    val kasId: Column<EntityID<UUID>> = reference("kas_id", KasTable)
    /** Keterangan */
    val keterangan: Column<String> = varchar("keterangan", length = 255)
    val entitasId = reference("entitas_id", EntitasUsahaTable)
}
