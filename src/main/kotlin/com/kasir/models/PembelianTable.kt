// PembelianTable.kt
package com.kasir.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.util.UUID

object PembelianTable : Table("pembelian") {
    val id = uuid("id").autoGenerate().uniqueIndex()
    val supplierId = uuid("supplier_id")
    val total = double("total")
    val status = varchar("status", 50) // LUNAS / BELUM_LUNAS
    val metodePembayaran = varchar("metode_pembayaran", 50) // TUNAI / KREDIT
    val tanggal = datetime("tanggal")
    val jatuhTempo = datetime("jatuh_tempo").nullable()

    override val primaryKey = PrimaryKey(id)
}