package com.kasir.models

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID
import com.kasir.models.EntitasUsahaTable
/**
 * Tabel untuk menyimpan komisi sales per penjualan
 */
object KomisiSalesTable : UUIDTable(name = "komisi_sales") {
    /** FK ke SalesTable.id */
    val salesId: Column<EntityID<UUID>> = reference(
        name = "sales_id",
        foreign = SalesTable,
        onDelete = ReferenceOption.CASCADE
    )

    /** FK ke PenjualanTable.id */
    val penjualanId: Column<EntityID<UUID>> = reference(
        name = "penjualan_id",
        foreign = PenjualanTable,
        onDelete = ReferenceOption.CASCADE
    )

    /** Persentase komisi */
    val komisiPersen: Column<Double> = double("komisi_persen")

    /** Nominal komisi */
    val nominalKomisi: Column<Double> = double("nominal_komisi")
    val entitasId = reference("entitas_id", EntitasUsahaTable)
    /** Status komisi: PENDING atau PAID */
    val status: Column<String> = varchar("status", length = 20).default("PENDING")

    /** Tanggal komisi dicatat */
    val tanggalKomisi: Column<LocalDateTime> = datetime("tanggal_komisi")

}
