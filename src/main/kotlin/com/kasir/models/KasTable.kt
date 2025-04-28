package com.kasir.models

import org.jetbrains.exposed.sql.Table
import java.util.UUID

object KasTable : Table("kas") {
    val id = uuid("id").autoGenerate().uniqueIndex()
    val namaKas = varchar("nama_kas", 255)
    val saldoAkhir = double("saldo_akhir").default(0.0)

    override val primaryKey = PrimaryKey(id)
}
