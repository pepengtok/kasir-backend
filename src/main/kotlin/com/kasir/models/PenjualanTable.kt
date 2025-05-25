package com.kasir.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object PenjualanTable : UUIDTable("penjualan") {
    val pelangganId = optReference("pelanggan_id", PelangganTable, onDelete = ReferenceOption.SET_NULL)
    val salesId = optReference("sales_id", SalesTable, onDelete = ReferenceOption.SET_NULL)
    val tanggal = datetime("tanggal")
    val metodePembayaran = varchar("metode_pembayaran", 20)
    val status = varchar("status", 20) // LUNAS, BELUM_LUNAS
    val total = double("total")
    val noNota = varchar("no_nota", 50).uniqueIndex() // ✅ Tambahkan ini
    val notaUrl = varchar("nota_url", 255).nullable() // ✅ Tambahkan ini

    val kasId = optReference("kas_id", KasTable, onDelete = ReferenceOption.SET_NULL)
    val jatuhTempo = datetime("jatuh_tempo").nullable()
    val entitasId = reference("entitas_id", EntitasUsahaTable)
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
}
