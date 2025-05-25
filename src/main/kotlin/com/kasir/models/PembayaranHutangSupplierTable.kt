// File: src/main/kotlin/com/kasir/models/PembayaranHutangSupplierTable.kt
package com.kasir.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import com.kasir.models.EntitasUsahaTable
import org.jetbrains.exposed.sql.javatime.datetime

object PembayaranHutangSupplierTable : UUIDTable("pembayaran_hutang_supplier") {
    val hutangSupplierId = reference("hutang_supplier_id", HutangSupplierTable.id, onDelete = ReferenceOption.CASCADE)
    val jumlahBayar      = double("jumlah_bayar")
    val kasId            = reference("kas_id", KasTable.id)
    val tanggalBayar     = datetime("tanggal_bayar")
    val keterangan     = text("keterangan").nullable()
    val entitasId = reference("entitas_id", EntitasUsahaTable)
    // UUIDTable auto‐define id
}
