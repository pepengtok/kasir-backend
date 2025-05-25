package com.kasir.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object SalesTable : UUIDTable("sales") {
    val nama = varchar("nama", 100)
    val komisiTunai = double("komisi_tunai")
    val komisiPiutang = double("komisi_piutang")
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() } // <-- TAMBAHKAN INI
    val entitasId = reference("entitas_id", EntitasUsahaTable) // ✅ Tambahkan ini
}