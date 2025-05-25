package com.kasir.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import com.kasir.models.EntitasUsahaTable

object KasTable : UUIDTable(name = "kas") {

    /** Nama akun kas, contoh: "Kas Besar", "Kas Kecil", "Kas Bank BCA" */
    val namaKas: Column<String> = varchar("nama_kas", 255)

    /** Saldo akhir akun kas */
    val saldoAkhir: Column<Double> = double("saldo_akhir").default(0.0)

    /** Jenis kas: BESAR, KECIL, BANK */
    val jenis: Column<String> = varchar("jenis", 50).default("BANK")

    val entitasId = reference("entitas_id", EntitasUsahaTable)

    /** Sub jenis: contoh sub-bank seperti "BCA DILA", "MANDIRI FAIR", dll */
    val subJenis: Column<String?> = varchar("sub_jenis", 100).nullable()
}
