package com.kasir.models

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID
import com.kasir.models.EntitasUsahaTable

object KasTransaksiTable : UUIDTable(name = "kas_transaksi") {
    /** FK ke KasTable.id */
    val kasId: Column<EntityID<UUID>> = reference("kas_id", KasTable)

    /** Waktu transaksi */
    val tanggal: Column<LocalDateTime> = datetime("tanggal")

    val entitasId = reference("entitas_id", EntitasUsahaTable)
    /** Keterangan transaksi */
    val keterangan: Column<String> = text("keterangan")

    /** Jumlah (debit/kredit) */
    val jumlah: Column<Double> = double("jumlah")

    /** Tipe: "MASUK" atau "KELUAR" */
    val tipe: Column<String> = varchar("tipe", 10)
}