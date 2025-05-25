package com.kasir.models

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID
import com.kasir.models.EntitasUsahaTable

object PriveTable : UUIDTable(name = "prive") {
    /** FK ke KasTable.id */
    val kasId: Column<EntityID<UUID>> = reference("sumber_input", KasTable)
    /** Tanggal pencatatan prive */
    val tanggal: Column<LocalDateTime> = datetime("tanggal")
    /** Nominal prive */
    val nominal: Column<Double> = double("nominal")
    /** Keterangan detail prive */
    val keterangan: Column<String> = varchar("keterangan", length = 255)
    val entitasId = reference("entitas_id", EntitasUsahaTable)
}