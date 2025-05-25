package com.kasir.models

import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import com.kasir.models.EntitasUsahaTable

object KaryawanTable : UUIDTable("karyawan") {
    val nama = varchar("nama", 100)
    val bulanTerdaftar = varchar("bulan_terdaftar", 20)
    val createdAt = datetime("created_at")
    val entitasId = reference("entitas_id", EntitasUsahaTable) // ✅ Tambahkan baris ini
}