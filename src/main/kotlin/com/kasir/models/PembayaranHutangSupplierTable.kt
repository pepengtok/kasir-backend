package com.kasir.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object PembayaranHutangSupplierTable : Table("pembayaran_hutang_supplier") {
    val id = uuid("id").autoGenerate().uniqueIndex()
    val hutangId = uuid("hutang_id")
    val tanggalBayar = datetime("tanggal_bayar")
    val jumlahBayar = double("jumlah_bayar")
    val kasId = uuid("kas_id")
    val keterangan = text("keterangan").nullable()

    override val primaryKey = PrimaryKey(id)
}
