// File: src/main/kotlin/com/kasir/models/PiutangPelangganTable.kt
package com.kasir.models

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime // <-- Pastikan ini diimpor
import java.time.LocalDate
import java.time.LocalDateTime // <-- Tambahkan import ini
import java.util.UUID
import org.jetbrains.exposed.sql.javatime.date

object PiutangPelangganTable : UUIDTable(name = "piutang_pelanggan") {
    val penjualanId: Column<EntityID<UUID>> = reference("penjualan_id", PenjualanTable)
    val pelangganId: Column<EntityID<UUID>> = reference("pelanggan_id", PelangganTable)
    val totalPiutang: Column<Double> = double("total_piutang")
    val sisaPiutang: Column<Double> = double("sisa_piutang")
    val tanggal: Column<LocalDate> = date("tanggal") // Biarkan ini LocalDate jika di DB juga hanya DATE
    val tanggalJatuhTempo: Column<LocalDateTime?> = datetime("tanggal_jatuh_tempo").nullable() // <-- UBAH KE LocalDateTime? dan datetime()
    val status: Column<String> = varchar("status", length = 20)
    val fotoNotaUrl: Column<String?> = varchar("foto_nota_url", 255).nullable()
    val entitasId = reference("entitas_id", EntitasUsahaTable)
}