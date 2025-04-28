package com.kasir.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object OrderanSalesTable : Table("orderan_sales") {
    val id = uuid("id").autoGenerate().uniqueIndex()
    val salesId = uuid("sales_id")
    val pelangganId = uuid("pelanggan_id").nullable()
    val total = double("total")
    val status = varchar("status", 20) // "MENUNGGU", "DISETUJUI", "DITOLAK"
    val tanggalOrder = datetime("tanggal_order")

    override val primaryKey = PrimaryKey(id)
}
