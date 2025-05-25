// File: src/main/kotlin/com/kasir/models/PembayaranHutangLainTable.kt
package com.kasir.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import com.kasir.models.EntitasUsahaTable
object PembayaranHutangLainTable : UUIDTable("pembayaran_hutang_lain") {
    val hutangLainId   = reference("hutang_lain_id", HutangLainTable.id, onDelete = ReferenceOption.CASCADE)
    val jumlahBayar    = double("jumlah_bayar")
    val kasId          = reference("kas_id", KasTable.id)
    val tanggalBayar   = datetime("tanggal_bayar")
    val keterangan     = text("keterangan").nullable()
    val entitasId = reference("entitas_id", EntitasUsahaTable)
}
