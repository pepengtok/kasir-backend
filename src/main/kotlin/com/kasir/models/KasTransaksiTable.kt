package com.kasir.models

import org.jetbrains.exposed.sql.Table
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object KasTransaksiTable : Table("kas_transaksi") {
    val id = uuid("id").autoGenerate().uniqueIndex()
    val kasId = uuid("kas_id")
    val tanggal = datetime("tanggal")
    val keterangan = text("keterangan")
    val jumlah = double("jumlah")
    val tipe = varchar("tipe", 10) // "MASUK" atau "KELUAR"

    override val primaryKey = PrimaryKey(id)
}
