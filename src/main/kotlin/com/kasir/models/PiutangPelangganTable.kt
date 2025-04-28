package com.kasir.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object PiutangPelangganTable : Table("piutang_pelanggan") {
    val id = uuid("id").autoGenerate().uniqueIndex()
    val penjualanId = uuid("penjualan_id")
    val pelangganId = uuid("pelanggan_id")
    val totalPiutang = double("total_piutang")
    val sisaPiutang = double("sisa_piutang")
    val tanggalJatuhTempo = datetime("tanggal_jatuh_tempo")

    override val primaryKey = PrimaryKey(id)
}
