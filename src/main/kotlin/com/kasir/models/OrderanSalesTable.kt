// File: src/main/kotlin/com/kasir/models/OrderanSalesTable.kt
package com.kasir.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime
import com.kasir.models.EntitasUsahaTable
/**
 * Tabel header pre-order / orderan
 * ID otomatis dibuat oleh UUIDTable
 */
object OrderanSalesTable : UUIDTable("orderan") {
    // FK ke SalesTable
    val salesId          = reference(
        "sales_id",
        SalesTable.id,
        onDelete = ReferenceOption.CASCADE
    )
    // FK ke PelangganTable, nullable
    val pelangganId      = reference(
        "pelanggan_id",
        PelangganTable.id,
        onDelete = ReferenceOption.CASCADE
    ).nullable()
    val total            = double("total")
    val entitasId = reference("entitas_id", EntitasUsahaTable)
    val metodePembayaran = varchar("metode_pembayaran", 20)
    val tempoHari        = integer("tempo_hari").nullable()
    val status           = varchar("status", 20)
    val tanggalOrder     = datetime("tanggal_order")
    // `id` sudah otomatis tersedia dari UUIDTable
}
