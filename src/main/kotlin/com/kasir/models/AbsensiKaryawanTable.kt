package com.kasir.models

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import com.kasir.models.EntitasUsahaTable

object AbsensiKaryawanTable : UUIDTable(name = "absensi_karyawan") {
    // FK ke KaryawanTable.id → Column<EntityID<UUID>>
    val karyawanId: Column<EntityID<UUID>> = reference("karyawan_id", KaryawanTable)

    // Tanggal absen
    val tanggal: Column<LocalDate> = date("tanggal")
    val entitasId = reference("entitas_id", EntitasUsahaTable)

    // Status absen (misal "HADIR", "IZIN", dll)
    val statusAbsen: Column<String> = varchar("status_absen", length = 20)

    // Keterangan opsional
    val keterangan: Column<String?> = text("keterangan").nullable()

    // Waktu pencatatan
    val createdAt: Column<LocalDateTime> = datetime("created_at")
}
