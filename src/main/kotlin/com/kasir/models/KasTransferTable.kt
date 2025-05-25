package com.kasir.models

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID
import com.kasir.models.EntitasUsahaTable

object KasTransferTable : UUIDTable(name = "kas_transfer") {
    /** FK ke akun asal */
    val kasAsalId: Column<EntityID<UUID>> = reference("kas_asal_id", KasTable)

    /** FK ke akun tujuan */
    val kasTujuanId: Column<EntityID<UUID>> = reference("kas_tujuan_id", KasTable)

    /** Waktu transfer */
    val tanggal: Column<LocalDateTime> = datetime("tanggal")
    val entitasId = reference("entitas_id", EntitasUsahaTable)
    /** Jumlah yang dipindah */
    val jumlah: Column<Double> = double("jumlah")
}