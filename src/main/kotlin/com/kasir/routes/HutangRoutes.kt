// src/main/kotlin/com/kasir/routes/HutangRoutes.kt
package com.kasir.routes

import com.kasir.dto.HutangSupplierDto
import com.kasir.dto.HutangBankEntryDto
import com.kasir.dto.HutangLainEntryDto
import com.kasir.dto.PembayaranHutangRequest
import com.kasir.dto.PembayaranRecord
import com.kasir.models.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID
import com.kasir.service.KasService

fun Route.hutangRoutes() {
    authenticate("jwt-auth") {
        route("/hutang") {

            // 1) Hutang Supplier
            route("/supplier") {
                // List
                get {
                    val status = call.request.queryParameters["status"]?.uppercase() ?: "BELUM_LUNAS"
                    val entitasId = call.request.queryParameters["entitas_id"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "entitas_id wajib diisi"))

                    val supplierFilter = call.request.queryParameters["supplierId"]?.let(UUID::fromString)

                    val list = transaction {
                        var q = HutangSupplierTable
                            .join(SupplierTable, JoinType.INNER, HutangSupplierTable.supplierId, SupplierTable.id)
                            .slice(
                                HutangSupplierTable.id,
                                HutangSupplierTable.supplierId,
                                SupplierTable.namaSupplier,
                                HutangSupplierTable.tanggal,
                                HutangSupplierTable.totalHutang,
                                HutangSupplierTable.sisaHutang,
                                HutangSupplierTable.tanggalJatuhTempo,
                                HutangSupplierTable.status,
                                HutangSupplierTable.fotoNotaUrl
                            )
                            .select {
                                (HutangSupplierTable.status eq status) and
                                        (HutangSupplierTable.entitasId eq UUID.fromString(entitasId))
                            }

                        supplierFilter?.let { sid ->
                            q = q.andWhere { HutangSupplierTable.supplierId eq sid }
                        }

                        q.orderBy(HutangSupplierTable.tanggalJatuhTempo to SortOrder.ASC)
                            .map { row ->
                                HutangSupplierDto(
                                    id          = row[HutangSupplierTable.id].value.toString(),
                                    partnerId   = row[HutangSupplierTable.supplierId].value.toString(),
                                    partnerName = row[SupplierTable.namaSupplier],
                                    tanggal     = row[HutangSupplierTable.tanggal].toString(),
                                    totalHutang = row[HutangSupplierTable.totalHutang],
                                    sisaHutang  = row[HutangSupplierTable.sisaHutang],
                                    jatuhTempo  = row[HutangSupplierTable.tanggalJatuhTempo].toString(),
                                    status      = row[HutangSupplierTable.status],
                                    fotoNotaUrl = row[HutangSupplierTable.fotoNotaUrl],
                                    entitasId   = row[HutangSupplierTable.entitasId].value.toString() // ✅ tambahan penting
                                )
                            }
                    }

                    call.respond(HttpStatusCode.OK, list)
                }

                // Bayar sebagian
                post("/{id}/bayar") {
                    val id        = UUID.fromString(call.parameters["id"]!!)
                    val req       = call.receive<PembayaranHutangRequest>()
                    val bayarTime = LocalDateTime.parse(req.tanggalBayar)

                    val result = transaction {
                        val hutangRow = HutangSupplierTable
                            .select { HutangSupplierTable.id eq id }
                            .single()

                        val current = hutangRow[HutangSupplierTable.sisaHutang]
                        if (req.jumlahBayar > current) return@transaction "OVERPAY"

                        PembayaranHutangSupplierTable.insert { p ->
                            p[hutangSupplierId] = EntityID(id, HutangSupplierTable)
                            p[jumlahBayar]      = req.jumlahBayar
                            p[kasId]            = EntityID(UUID.fromString(req.kasId), KasTable)
                            p[tanggalBayar]     = bayarTime
                            req.keterangan?.let { k -> p[keterangan] = k }
                        }

                        val newSisa = current - req.jumlahBayar
                        HutangSupplierTable.update({ HutangSupplierTable.id eq id }) { h ->
                            h[sisaHutang] = newSisa
                            h[status]     = if (newSisa <= 0.0) "LUNAS" else hutangRow[HutangSupplierTable.status]
                        }

                        val supplierName = SupplierTable
                            .select { SupplierTable.id eq hutangRow[HutangSupplierTable.supplierId] }
                            .single()[SupplierTable.namaSupplier]

                        KasService.record(
                            entitasId  = hutangRow[HutangSupplierTable.entitasId].value, // ✅ ditambahkan
                            kasId      = UUID.fromString(req.kasId),
                            tanggal    = bayarTime,
                            jumlah     = req.jumlahBayar,
                            tipe       = "KELUAR",
                            keterangan = "Bayar hutang supplier: $supplierName"
                        )

                        "OK"
                    }

                    when (result) {
                        "OK"      -> call.respond(HttpStatusCode.Created)
                        "OVERPAY" -> call.respond(HttpStatusCode.BadRequest, "Jumlah bayar melebihi sisa hutang")
                    }
                }

                // History pembayaran
                get("/{id}/history") {
                    val id = UUID.fromString(call.parameters["id"]!!)
                    val history = transaction {
                        PembayaranHutangSupplierTable
                            .select { PembayaranHutangSupplierTable.hutangSupplierId eq EntityID(id, HutangSupplierTable) }
                            .orderBy(PembayaranHutangSupplierTable.tanggalBayar to SortOrder.ASC)
                            .map { row ->
                                PembayaranRecord(
                                    id           = row[PembayaranHutangSupplierTable.id].value.toString(),
                                    tanggalBayar = row[PembayaranHutangSupplierTable.tanggalBayar].toString(),
                                    jumlahBayar  = row[PembayaranHutangSupplierTable.jumlahBayar],
                                    kasId        = row[PembayaranHutangSupplierTable.kasId].value.toString(),
                                    keterangan   = row[PembayaranHutangSupplierTable.keterangan] ?: "",
                                    entitasId    = row[PembayaranHutangSupplierTable.entitasId].value.toString() // ✅ Tambahkan ini
                                )
                            }
                    }
                    call.respond(HttpStatusCode.OK, history)
                }
            }

            // 2) Hutang Bank
            route("/bank") {
                // List
                get {
                    val status = call.request.queryParameters["status"]?.uppercase() ?: "BELUM_LUNAS"
                    val entitasId = call.request.queryParameters["entitas_id"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "entitas_id wajib diisi")

                    val list = transaction {
                        HutangBankTable
                            .select {
                                (HutangBankTable.status eq status) and
                                        (HutangBankTable.entitasId eq UUID.fromString(entitasId))
                            }
                            .orderBy(HutangBankTable.tanggalJatuhTempo to SortOrder.ASC)
                            .map { row ->
                                HutangBankEntryDto(
                                    bank              = row[HutangBankTable.bankName],
                                    tanggalJatuhTempo = row[HutangBankTable.tanggalJatuhTempo].toString(),
                                    totalHutang       = row[HutangBankTable.totalHutang],
                                    sisaHutang        = row[HutangBankTable.sisaHutang],
                                    status            = row[HutangBankTable.status],
                                    entitasId         = row[HutangBankTable.entitasId].value.toString() // ✅
                                )
                            }
                    }
                    call.respond(HttpStatusCode.OK, list)
                }

                // Update data hutang bank
                put("/{id}") {
                    val id  = UUID.fromString(call.parameters["id"]!!)
                    val req = call.receive<HutangBankEntryDto>()
                    transaction {
                        HutangBankTable.update({ HutangBankTable.id eq id }) {
                            it[bankName]          = req.bank
                            it[tanggalJatuhTempo] = LocalDateTime.parse(req.tanggalJatuhTempo)
                            it[totalHutang]       = req.totalHutang
                            it[sisaHutang]        = req.sisaHutang
                            it[status]            = req.status.uppercase()
                        }
                    }
                    call.respond(HttpStatusCode.OK)
                }

                // Bayar sebagian
                post("/{id}/bayar") {
                    val id        = UUID.fromString(call.parameters["id"]!!)
                    val req       = call.receive<PembayaranHutangRequest>()
                    val bayarTime = LocalDateTime.parse(req.tanggalBayar)

                    val result = transaction {
                        val row     = HutangBankTable.select { HutangBankTable.id eq id }.single()
                        val current = row[HutangBankTable.sisaHutang]
                        if (req.jumlahBayar > current) return@transaction "OVERPAY"

                        // simpan bayar
                        PembayaranHutangBankTable.insert {
                            it[hutangBankId] = EntityID(id, HutangBankTable)
                            it[jumlahBayar]  = req.jumlahBayar
                            it[kasId]        = EntityID(UUID.fromString(req.kasId), KasTable)
                            it[tanggalBayar] = bayarTime
                            req.keterangan?.let { k -> it[keterangan] = k }
                            it[entitasId]    = row[HutangBankTable.entitasId] // ✅ jika ada di tabel
                        }

                        val newSisa = current - req.jumlahBayar
                        HutangBankTable.update({ HutangBankTable.id eq id }) {
                            it[sisaHutang] = newSisa
                            it[status]     = if (newSisa <= 0.0) "LUNAS" else row[HutangBankTable.status]
                        }

                        // catat kas keluar
                        KasService.record(
                            entitasId  = row[HutangBankTable.entitasId].value, // ✅ tambahan
                            kasId      = UUID.fromString(req.kasId),
                            tanggal    = bayarTime,
                            jumlah     = req.jumlahBayar,
                            tipe       = "KELUAR",
                            keterangan = "Bayar hutang bank #$id"
                        )

                        "OK"
                    }
                    when (result) {
                        "OK"      -> call.respond(HttpStatusCode.Created)
                        "OVERPAY" -> call.respond(HttpStatusCode.BadRequest, "Jumlah bayar melebihi sisa hutang")
                    }
                }

                // History bayar bank
                get("/{id}/history") {
                    val id = UUID.fromString(call.parameters["id"]!!)
                    val history = transaction {
                        PembayaranHutangBankTable
                            .select { PembayaranHutangBankTable.hutangBankId eq EntityID(id, HutangBankTable) }
                            .orderBy(PembayaranHutangBankTable.tanggalBayar to SortOrder.ASC)
                            .map { row ->
                                PembayaranRecord(
                                    id           = row[PembayaranHutangBankTable.id].value.toString(),
                                    tanggalBayar = row[PembayaranHutangBankTable.tanggalBayar].toString(),
                                    jumlahBayar  = row[PembayaranHutangBankTable.jumlahBayar],
                                    kasId        = row[PembayaranHutangBankTable.kasId].value.toString(),
                                    keterangan   = row[PembayaranHutangBankTable.keterangan] ?: "",
                                    entitasId    = row[PembayaranHutangBankTable.entitasId].value.toString() // ✅
                                )
                            }
                    }
                    call.respond(HttpStatusCode.OK, history)
                }
            }

            // 3) Hutang Lain-lain
            route("/lain") {
                // List
                get {
                    val status = call.request.queryParameters["status"]?.uppercase() ?: "BELUM_LUNAS"
                    val entitasId = call.request.queryParameters["entitas_id"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "entitas_id wajib diisi")

                    val list = transaction {
                        HutangLainTable
                            .select {
                                (HutangLainTable.status eq status) and
                                        (HutangLainTable.entitasId eq UUID.fromString(entitasId))
                            }
                            .orderBy(HutangLainTable.tanggalJatuhTempo to SortOrder.ASC)
                            .map { row ->
                                HutangLainEntryDto(
                                    keterangan        = row[HutangLainTable.nama],
                                    tanggalJatuhTempo = row[HutangLainTable.tanggalJatuhTempo].toString(),
                                    totalHutang       = row[HutangLainTable.totalHutang],
                                    sisaHutang        = row[HutangLainTable.sisaHutang],
                                    status            = row[HutangLainTable.status],
                                    entitasId         = row[HutangLainTable.entitasId].value.toString() // ✅
                                )
                            }
                    }
                    call.respond(HttpStatusCode.OK, list)
                }

                // Update data
                put("/{id}") {
                    val id  = UUID.fromString(call.parameters["id"]!!)
                    val req = call.receive<HutangLainEntryDto>()
                    transaction {
                        HutangLainTable.update({ HutangLainTable.id eq id }) { row ->
                            row[keterangan]         = req.keterangan.orEmpty()
                            row[tanggal]            = LocalDateTime.parse(req.tanggalJatuhTempo)
                            row[totalHutang]        = req.totalHutang
                            row[sisaHutang]         = req.sisaHutang
                            row[tanggalJatuhTempo]  = LocalDateTime.parse(req.tanggalJatuhTempo)
                            row[status]             = req.status.uppercase()
                        }
                    }
                    call.respond(HttpStatusCode.OK)
                }

                // Bayar sebagian
                post("/{id}/bayar") {
                    val id        = UUID.fromString(call.parameters["id"]!!)
                    val req       = call.receive<PembayaranHutangRequest>()
                    val bayarTime = LocalDateTime.parse(req.tanggalBayar)

                    val result = transaction {
                        val row     = HutangLainTable.select { HutangLainTable.id eq id }.single()
                        val current = row[HutangLainTable.sisaHutang]
                        if (req.jumlahBayar > current) return@transaction "OVERPAY"

                        PembayaranHutangLainTable.insert {
                            it[hutangLainId] = EntityID(id, HutangLainTable)
                            it[jumlahBayar]  = req.jumlahBayar
                            it[kasId]        = EntityID(UUID.fromString(req.kasId), KasTable)
                            it[tanggalBayar] = bayarTime
                            req.keterangan?.let { k -> it[keterangan] = k }
                            it[entitasId]    = row[HutangLainTable.entitasId] // ✅
                        }

                        val newSisa = current - req.jumlahBayar
                        HutangLainTable.update({ HutangLainTable.id eq id }) {
                            it[sisaHutang] = newSisa
                            it[status]     = if (newSisa <= 0.0) "LUNAS" else row[HutangLainTable.status]
                        }

                        KasService.record(
                            entitasId  = row[HutangLainTable.entitasId].value, // ✅
                            kasId      = UUID.fromString(req.kasId),
                            tanggal    = bayarTime,
                            jumlah     = req.jumlahBayar,
                            tipe       = "KELUAR",
                            keterangan = "Bayar hutang lain: ${row[HutangLainTable.nama]}"
                        )

                        "OK"
                    }
                    when (result) {
                        "OK"      -> call.respond(HttpStatusCode.Created)
                        "OVERPAY" -> call.respond(HttpStatusCode.BadRequest, "Jumlah bayar melebihi sisa hutang")
                    }
                }

                // History bayar lain
                get("/{id}/history") {
                    val id = UUID.fromString(call.parameters["id"]!!)
                    val history = transaction {
                        PembayaranHutangLainTable
                            .select { PembayaranHutangLainTable.hutangLainId eq EntityID(id, HutangLainTable) }
                            .orderBy(PembayaranHutangLainTable.tanggalBayar to SortOrder.ASC)
                            .map { row ->
                                PembayaranRecord(
                                    id           = row[PembayaranHutangLainTable.id].value.toString(),
                                    tanggalBayar = row[PembayaranHutangLainTable.tanggalBayar].toString(),
                                    jumlahBayar  = row[PembayaranHutangLainTable.jumlahBayar],
                                    kasId        = row[PembayaranHutangLainTable.kasId].value.toString(),
                                    keterangan   = row[PembayaranHutangLainTable.keterangan] ?: "",
                                    entitasId    = row[PembayaranHutangLainTable.entitasId].value.toString() // ✅
                                )
                            }
                    }
                    call.respond(HttpStatusCode.OK, history)
                }
            }
        }
}
}