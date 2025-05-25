package com.kasir.models

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.*
import java.util.UUID

object PembelianDetailTable : UUIDTable(name = "pembelian_detail") {
    /** FK ke PembelianTable.id */
    val pembelianId: Column<EntityID<UUID>> = reference("pembelian_id", PembelianTable)
    /** FK ke ProdukTable.id */
    val produkId: Column<EntityID<UUID>> = reference("produk_id", ProdukTable)
    /** Harga beli per unit */
    val hargaModal: Column<Double> = double("harga_modal")
    /** Jumlah unit */
    val jumlah       = double("jumlah")
    /** Subtotal = hargaModal * jumlah */
    val subtotal: Column<Double> = double("subtotal")
    val entitasId = reference("entitas_id", EntitasUsahaTable) // <-- Tambahkan baris ini
}