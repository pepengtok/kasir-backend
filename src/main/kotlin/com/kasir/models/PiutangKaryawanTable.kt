// File: src/main/kotlin/com/kasir/models/PiutangKaryawanTable.kt
package com.kasir.models

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID
import java.time.LocalDate
import org.jetbrains.exposed.sql.javatime.date
import com.kasir.models.EntitasUsahaTable

object PiutangKaryawanTable : UUIDTable(name = "piutang_karyawan") {
    /** FK ke KaryawanTable.id */
    val karyawanId: Column<EntityID<UUID>> = reference("karyawan_id", KaryawanTable)
    /** Total piutang */
    val totalPiutang: Column<Double> = double("total_piutang")
    /** Sisa */
    val sisaPiutang: Column<Double> = double("sisa_piutang")
    /** Tanggal pencatatan piutang */
    val tanggal: Column<LocalDate> = date("tanggal")
    /** Tanggal jatuh tempo */
    val tanggalJatuhTempo: Column<LocalDateTime> = datetime("tanggal_jatuh_tempo")
    /** Status piutang */
    val status: Column<String> = varchar("status", length = 20)
    val entitasId = reference("entitas_id", EntitasUsahaTable)
}