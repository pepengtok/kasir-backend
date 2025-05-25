package com.kasir.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.*
import com.kasir.models.EntitasUsahaTable
/**
 * Table definition for Pelanggan using UUID primary keys.
 */
object PelangganTable : UUIDTable(name = "pelanggan") {
    // Nama pelanggan (varchar, not null)
    val namaPelanggan = varchar("nama_pelanggan", 255)

    // Kontak/telepon pelanggan, optional
    val no_hp        = varchar("no_hp", 20).nullable()

    // Alamat pelanggan, optional text
    val alamat        = text("alamat").nullable()

    // Keterangan tambahan, optional text
    val keterangan    = text("keterangan").nullable()
    val entitasId = reference("entitas_id", EntitasUsahaTable)
    // Relasi ke pegawai sales (UUID), optional
    val salesId = optReference("sales_id", SalesTable, onDelete = ReferenceOption.SET_NULL)

    // --- TAMBAH: Kolom Latitude dan Longitude ---
    val latitude = double("latitude").nullable()
    val longitude = double("longitude").nullable()
}