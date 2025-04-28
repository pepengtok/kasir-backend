package com.kasir.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.*

/**
 * Table definition for Pelanggan using UUID primary keys.
 */
object PelangganTable : UUIDTable(name = "pelanggan") {
    // Nama pelanggan (varchar, not null)
    val namaPelanggan = varchar("nama_pelanggan", 255)

    // Kontak/telepon pelanggan, optional
    val kontak        = varchar("kontak", 20).nullable()

    // Alamat pelanggan, optional text
    val alamat        = text("alamat").nullable()

    // Keterangan tambahan, optional text
    val keterangan    = text("keterangan").nullable()

    // Relasi ke pegawai sales (UUID), optional
    val salesId       = uuid("sales_id").nullable()
        .references(SalesTable.id, onDelete = ReferenceOption.SET_NULL)
}
