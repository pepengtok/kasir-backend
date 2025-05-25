// File: src/main/kotlin/com/kasir/models/HutangLainTable.kt
package com.kasir.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import com.kasir.models.EntitasUsahaTable

object HutangLainTable : UUIDTable("hutang_lain") {
    val nama = varchar("nama", 255)
    val tanggal = datetime("tanggal")
    val totalHutang = double("total_hutang")
    val sisaHutang = double("sisa_hutang")
    val tanggalJatuhTempo = datetime("tanggal_jatuh_tempo")
    val status = varchar("status", length = 15).default("BELUM_LUNAS")
    val fotoNotaUrl = varchar("foto_nota_url", 255).nullable()
    val keterangan        = text("keterangan").clientDefault { "" }
    val entitasId = reference("entitas_id", EntitasUsahaTable)
// UUIDTable auto‐define id }
}