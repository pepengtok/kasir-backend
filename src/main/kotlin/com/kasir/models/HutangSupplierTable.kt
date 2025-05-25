package com.kasir.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.ReferenceOption
import com.kasir.models.EntitasUsahaTable

object HutangSupplierTable : UUIDTable("hutang_supplier") {
    // Foreign key to Pembelian (cascade on delete)
    // FK ke Pembelian (cascade delete)
    val pembelianId       = reference("pembelian_id", PembelianTable.id, onDelete = ReferenceOption.CASCADE)

    // Foreign key to Supplier (cascade on delete)
    val supplierId        = reference("supplier_id", SupplierTable.id, onDelete = ReferenceOption.CASCADE)
    val fotoNotaUrl = varchar("foto_nota_url", 255).nullable()

    // Date of the debt entry
    val tanggal           = datetime("tanggal")

    // Total amount of debt
    val totalHutang       = double("total_hutang")
    val entitasId = reference("entitas_id", EntitasUsahaTable)
    // Remaining debt balance
    val sisaHutang        = double("sisa_hutang")

    // Due date for payment
    val tanggalJatuhTempo = datetime("tanggal_jatuh_tempo")

    // Status: BELUM_LUNAS, LUNAS
    val status            = varchar("status", 15).default("BELUM_LUNAS")
}
// UUIDTable sudah auto‐define `val id` & primary key