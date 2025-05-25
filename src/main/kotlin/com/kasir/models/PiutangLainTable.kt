// File: src/main/kotlin/com/kasir/models/PiutangLainTable.kt
package com.kasir.models

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID
import com.kasir.models.EntitasUsahaTable

object PiutangLainTable : UUIDTable(name = "piutang_lain") {
    /** Deskripsi piutang lain */
    val keterangan: Column<String> = varchar("keterangan", length = 255)
    /** Total piutang */
    val totalPiutang: Column<Double> = double("total_piutang")
    /** Sisa */
    val sisaPiutang: Column<Double> = double("sisa_piutang")
    /** Tanggal jatuh tempo */
    val tanggalJatuhTempo: Column<LocalDateTime> = datetime("tanggal_jatuh_tempo")
    /** Status piutang */
    val status: Column<String> = varchar("status", length = 20)
    val entitasId = reference("entitas_id", EntitasUsahaTable)

}