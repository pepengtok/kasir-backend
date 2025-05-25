// File: src/main/kotlin/com/kasir/models/PembayaranHutangBankTable.kt
package com.kasir.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import com.kasir.models.EntitasUsahaTable

object
PembayaranHutangBankTable : UUIDTable("pembayaran_hutang_bank") {
    val hutangBankId   = reference("hutang_bank_id", HutangBankTable.id, onDelete = ReferenceOption.CASCADE)
    val jumlahBayar    = double("jumlah_bayar")
    val kasId          = reference("kas_id", KasTable.id)
    val tanggalBayar   = datetime("tanggal_bayar")
    val keterangan     = text("keterangan").nullable()
    val entitasId = reference("entitas_id", EntitasUsahaTable)
    // UUIDTable auto‐define id
}