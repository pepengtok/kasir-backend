package com.kasir.models

import org.jetbrains.exposed.sql.Table
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object KasTransferTable : Table("kas_transfer") {
    val id = uuid("id").autoGenerate().uniqueIndex()
    val kasAsalId = uuid("kas_asal_id")
    val kasTujuanId = uuid("kas_tujuan_id")
    val tanggal = datetime("tanggal")
    val jumlah = double("jumlah")

    override val primaryKey = PrimaryKey(id)
}
