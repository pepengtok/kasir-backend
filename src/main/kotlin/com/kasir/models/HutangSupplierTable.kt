package com.kasir.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object HutangSupplierTable : Table("hutang_supplier") {
    val id = uuid("id").autoGenerate().uniqueIndex()
    val supplierId = uuid("supplier_id")
    val pembelianId = uuid("pembelian_id").nullable() // relasi ke pembelian (opsional)
    val totalHutang = double("total_hutang")
    val sisaHutang = double("sisa_hutang")
    val tanggal = datetime("tanggal") // ⬅️ tambahkan ini
    override val primaryKey = PrimaryKey(id)
}
