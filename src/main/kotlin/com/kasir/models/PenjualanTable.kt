package com.kasir.models

import org.jetbrains.exposed.sql.Table
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object PenjualanTable : Table("penjualan") {
    val id = uuid("id").autoGenerate().uniqueIndex()
    val tanggal = datetime("tanggal")
    val pelangganId = uuid("pelanggan_id").nullable()
    val salesId = uuid("sales_id").nullable()
    val total = double("total")
    val metodePembayaran = varchar("metode_pembayaran", 20) // "KONTAN" atau "PIUTANG"
    val status = varchar("status", 20) // "LUNAS" atau "BELUM_LUNAS"
    val kasId = uuid("kas_id") // Kas tempat uang masuk

    override val primaryKey = PrimaryKey(id)
}
