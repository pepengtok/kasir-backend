// File: src/main/kotlin/com/kasir/models/HutangBankTable.kt
package com.kasir.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import com.kasir.models.EntitasUsahaTable

object HutangBankTable : UUIDTable("hutang_bank") {
    val bankName          = varchar("bank_name", 100)
    val tanggal           = datetime("tanggal")
    val totalHutang       = double("total_hutang")
    val sisaHutang        = double("sisa_hutang")
    val tanggalJatuhTempo = datetime("tanggal_jatuh_tempo")
    val status            = varchar("status", length = 15).default("BELUM_LUNAS")
    val fotoNotaUrl = varchar("foto_nota_url", 255).nullable()
    val entitasId = reference("entitas_id", EntitasUsahaTable)

}
