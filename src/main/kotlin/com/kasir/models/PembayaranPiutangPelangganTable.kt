package com.kasir.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object PembayaranPiutangPelangganTable : Table("pembayaran_piutang_pelanggan") {
    val id = uuid("id").autoGenerate().uniqueIndex()
    val piutangId = uuid("piutang_id")
    val tanggalBayar = datetime("tanggal_bayar")
    val jumlahBayar = double("jumlah_bayar")
    val kasId = uuid("kas_id")
    val keterangan = text("keterangan").nullable()

    override val primaryKey = PrimaryKey(id)
}
