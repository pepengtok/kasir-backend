package com.kasir.models

import org.jetbrains.exposed.sql.Table
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object KomisiSalesTable : Table("komisi_sales") {
    val id = uuid("id").autoGenerate().uniqueIndex()
    val salesId = uuid("sales_id")
    val penjualanId = uuid("penjualan_id")
    val komisiPersen = double("komisi_persen")
    val nominalKomisi = double("nominal_komisi")
    val status = varchar("status", 20) // "PENDING" atau "DIBAYAR"
    val tanggalKomisi = datetime("tanggal_komisi")

    override val primaryKey = PrimaryKey(id)
}
