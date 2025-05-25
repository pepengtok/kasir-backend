package com.kasir.models

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID
import com.kasir.models.EntitasUsahaTable

object BiayaNonOperasionalTable : UUIDTable("biaya_non_operasional") {
    val tanggal     = datetime("tanggal")
    val nominal     = double("nominal")
    val keterangan  = varchar("keterangan", 255)
    val entitasId = reference("entitas_id", EntitasUsahaTable)
    val kasId       = reference("kas_id", KasTable)
    val saldoAfter  = double("saldo_after").default(0.0)
}
