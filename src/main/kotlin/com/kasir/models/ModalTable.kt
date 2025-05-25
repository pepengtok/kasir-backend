package com.kasir.models

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID
import com.kasir.models.EntitasUsahaTable

object ModalTable : UUIDTable(name = "modal") {
    /** FK ke KasTable.id */
    val kasId: Column<EntityID<UUID>> = reference("tujuan_input", KasTable)
    /** Tanggal pencatatan modal */
    val tanggal: Column<LocalDateTime> = datetime("tanggal")
    /** Nominal modal */
    val nominal: Column<Double> = double("nominal")
    /** Keterangan detail modal */
    val keterangan: Column<String> = varchar("keterangan", length = 255)
    val entitasId = reference("entitas_id", EntitasUsahaTable)
}