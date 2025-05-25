// src/main/kotlin/com/kasir/routes/PiutangRoutes.kt
package com.kasir.routes

import com.kasir.dto.*
import com.kasir.models.*
import com.kasir.service.KasService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import com.kasir.routes.helpers.getEntitasIdFromJwt // ✅ Import helper function dari lokasi barunya

fun Route.piutangRoutes() {
    authenticate("jwt-auth") {
        route("/piutang") {

            route("/pelanggan") {
                // List summary piutang pelanggan
                get {
                    val entitasIdFromJwt = getEntitasIdFromJwt()
                    val statusParam    = call.request.queryParameters["status"]?.uppercase()
                    val salesParam     = call.request.queryParameters["salesId"]?.let(UUID::fromString)
                    val pelangganParam = call.request.queryParameters["pelangganId"]?.let(UUID::fromString)
                    val jatuhTempoParam= call.request.queryParameters["jatuh_tempo"]?.let(LocalDate::parse) // ✅ Perbaikan: Parse sebagai LocalDate

                    val list = transaction {
                        var q = PiutangPelangganTable
                            .join(PenjualanTable, JoinType.INNER,
                                PiutangPelangganTable.penjualanId, PenjualanTable.id)
                            .join(PelangganTable, JoinType.INNER,
                                PenjualanTable.pelangganId, PelangganTable.id)
                            .slice(
                                PiutangPelangganTable.id,
                                PiutangPelangganTable.penjualanId,
                                PelangganTable.namaPelanggan,
                                PiutangPelangganTable.totalPiutang,
                                PiutangPelangganTable.sisaPiutang,
                                PiutangPelangganTable.tanggalJatuhTempo,
                                PiutangPelangganTable.status,
                                PiutangPelangganTable.fotoNotaUrl,
                                PiutangPelangganTable.entitasId,
                                PenjualanTable.noNota
                            )
                            .select { PiutangPelangganTable.entitasId eq entitasIdFromJwt }

                        statusParam?.let    { q = q.andWhere { PiutangPelangganTable.status eq it } }
                        salesParam?.let     { q = q.andWhere { PenjualanTable.salesId eq EntityID(it, SalesTable) } }
                        pelangganParam?.let { q = q.andWhere { PenjualanTable.pelangganId eq EntityID(it, PelangganTable) } }
                        // ✅ Perbaikan: Gunakan lessEq sebagai fungsi dan bandingkan LocalDate
                        jatuhTempoParam?.let{ q = q.andWhere { PiutangPelangganTable.tanggalJatuhTempo.lessEq(it) } }

                        q.orderBy(PiutangPelangganTable.tanggalJatuhTempo to SortOrder.ASC)
                            .map { row ->
                                PiutangPelangganSummaryDto(
                                    id           = row[PiutangPelangganTable.id].value.toString(),
                                    noNota       = row[PenjualanTable.noNota],
                                    nama         = row[PelangganTable.namaPelanggan],
                                    totalPiutang = row[PiutangPelangganTable.totalPiutang],
                                    sisaPiutang  = row[PiutangPelangganTable.sisaPiutang],
                                    jatuhTempo   = row[PiutangPelangganTable.tanggalJatuhTempo].toString(),
                                    status       = row[PiutangPelangganTable.status],
                                    fotoNotaUrl  = row[PiutangPelangganTable.fotoNotaUrl],
                                    entitasId    = row[PiutangPelangganTable.entitasId].value.toString()
                                )
                            }
                    }
                    call.respond(HttpStatusCode.OK, list)
                }

                // History pembayaran piutang
                get("/{id}/history") {
                    val entitasUUID = getEntitasIdFromJwt().value
                    val piutangId = UUID.fromString(call.parameters["id"]!!)

                    val history = transaction {
                        PembayaranPiutangPelangganTable
                            .select {
                                (PembayaranPiutangPelangganTable.piutangId eq EntityID(piutangId, PiutangPelangganTable)) and
                                        (PembayaranPiutangPelangganTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable))
                            }
                            .orderBy(PembayaranPiutangPelangganTable.tanggalBayar to SortOrder.ASC)
                            .map { row ->
                                PiutangPelangganHistoryDto(
                                    id           = row[PembayaranPiutangPelangganTable.id].value.toString(),
                                    tanggalBayar = row[PembayaranPiutangPelangganTable.tanggalBayar].toString(),
                                    jumlahBayar  = row[PembayaranPiutangPelangganTable.jumlahBayar],
                                    kasId        = row[PembayaranPiutangPelangganTable.kasId]?.value, // Gunakan ?.value untuk mendapatkan UUID?
                                    keterangan   = row[PembayaranPiutangPelangganTable.keterangan]
                                )
                            }
                    }
                    call.respond(HttpStatusCode.OK, history)
                }

                // Bayar piutang pelanggan
                post("/{id}/bayar") {
                    val entitasUUID = getEntitasIdFromJwt().value
                    val piutangId = UUID.fromString(call.parameters["id"]!!)
                    val req = call.receive<PiutangPelangganBayarRequestDto>()
                    val bayarTime = LocalDateTime.parse(req.tanggalBayar)

                    transaction {
                        // 1) Insert pembayaran
                        PembayaranPiutangPelangganTable.insert {
                            it[PembayaranPiutangPelangganTable.entitasId]         = EntityID(entitasUUID, EntitasUsahaTable)
                            it[PembayaranPiutangPelangganTable.piutangId]          = EntityID(piutangId, PiutangPelangganTable)
                            it[PembayaranPiutangPelangganTable.tanggalBayar]       = bayarTime
                            it[PembayaranPiutangPelangganTable.jumlahBayar]        = req.jumlahBayar
                            it[PembayaranPiutangPelangganTable.kasId]              = EntityID(UUID.fromString(req.kasId), KasTable)
                            it[PembayaranPiutangPelangganTable.keterangan]         = req.keterangan ?: "Pembayaran piutang pelanggan"
                        }

                        // 2) Update sisa & status piutang
                        val piutangRow = PiutangPelangganTable
                            .select {
                                (PiutangPelangganTable.id eq EntityID(piutangId, PiutangPelangganTable)) and
                                        (PiutangPelangganTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable))
                            }
                            .single()
                        val oldSisa     = piutangRow[PiutangPelangganTable.sisaPiutang]
                        val newSisa     = oldSisa - req.jumlahBayar
                        val penjualanId = piutangRow[PiutangPelangganTable.penjualanId].value

                        PiutangPelangganTable.update({
                            (PiutangPelangganTable.id eq EntityID(piutangId, PiutangPelangganTable)) and
                                    (PiutangPelangganTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable))
                        }) {
                            it[PiutangPelangganTable.sisaPiutang] = newSisa
                            it[PiutangPelangganTable.status]      = if (newSisa <= 0.0) "LUNAS" else piutangRow[PiutangPelangganTable.status]
                        }

                        // 3) Jika lunas, update status penjualan
                        if (newSisa <= 0.0) {
                            PenjualanTable.update({
                                (PenjualanTable.id eq EntityID(penjualanId, PenjualanTable)) and
                                        (PenjualanTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable))
                            }) {
                                it[PenjualanTable.status] = "LUNAS"
                            }
                        }

                        // 4) Catat arus kas masuk
                        KasService.record(
                            entitasId  = entitasUUID,
                            kasId      = UUID.fromString(req.kasId),
                            tanggal    = bayarTime,
                            jumlah     = req.jumlahBayar,
                            tipe       = "MASUK",
                            keterangan = "Pelunasan piutang penjualan #$penjualanId"
                        )
                    }
                    call.respond(HttpStatusCode.Created)
                }

                // Update record piutang pelanggan
                put("/{id}") {
                    val entitasUUID = getEntitasIdFromJwt().value
                    val piutangId = runCatching { UUID.fromString(call.parameters["id"]!!) }
                        .getOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                    val req = call.receive<PiutangPelangganUpdateRequestDto>()

                    transaction {
                        PiutangPelangganTable.update({
                            (PiutangPelangganTable.id eq EntityID(piutangId, PiutangPelangganTable)) and
                                    (PiutangPelangganTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable))
                        }) { r ->
                            r[PiutangPelangganTable.totalPiutang]      = req.totalPiutang
                            r[PiutangPelangganTable.sisaPiutang]       = req.sisaPiutang
                            r[PiutangPelangganTable.status]            = req.status
                            // ✅ Perbaikan: Parse sebagai LocalDate
                            req.jatuhTempo?.let { r[PiutangPelangganTable.tanggalJatuhTempo] = LocalDate.parse(it) }
                            req.fotoNotaUrl?.let { r[PiutangPelangganTable.fotoNotaUrl]   = it }
                        }
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }


            // ==== Piutang Karyawan ====
            route("/karyawan") {
                // List piutang karyawan
                get {
                    val entitasUUID = getEntitasIdFromJwt().value
                    val statusParam = call.request.queryParameters["status"]?.uppercase() ?: "BELUM_LUNAS"

                    val list = transaction {
                        PiutangKaryawanTable
                            .join(KaryawanTable, JoinType.INNER, PiutangKaryawanTable.karyawanId, KaryawanTable.id)
                            .slice(
                                PiutangKaryawanTable.id,
                                PiutangKaryawanTable.karyawanId,
                                KaryawanTable.nama,
                                PiutangKaryawanTable.tanggal,
                                PiutangKaryawanTable.totalPiutang,
                                PiutangKaryawanTable.sisaPiutang,
                                PiutangKaryawanTable.tanggalJatuhTempo,
                                PiutangKaryawanTable.status,
                                PiutangKaryawanTable.entitasId
                            )
                            .select {
                                (PiutangKaryawanTable.status eq statusParam) and
                                        (PiutangKaryawanTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable))
                            }
                            .orderBy(PiutangKaryawanTable.tanggalJatuhTempo to SortOrder.ASC)
                            .map { row ->
                                PiutangKaryawanSummary(
                                    id                = row[PiutangKaryawanTable.id].value.toString(),
                                    karyawanId        = row[PiutangKaryawanTable.karyawanId].value.toString(),
                                    nama              = row[KaryawanTable.nama],
                                    tanggal           = row[PiutangKaryawanTable.tanggal].toString(),
                                    totalPiutang      = row[PiutangKaryawanTable.totalPiutang],
                                    sisaPiutang       = row[PiutangKaryawanTable.sisaPiutang],
                                    tanggalJatuhTempo = row[PiutangKaryawanTable.tanggalJatuhTempo].toString(),
                                    status            = row[PiutangKaryawanTable.status],
                                    entitasId         = row[PiutangKaryawanTable.entitasId].value.toString()
                                )
                            }
                    }
                    call.respond(HttpStatusCode.OK, list)
                }


                // Bayar
                post("/{id}/bayar") {
                    val entitasUUID = getEntitasIdFromJwt().value
                    val piutangId = UUID.fromString(call.parameters["id"]!!)
                    val req = call.receive<PiutangKaryawanBayarRequest>()
                    val bayarTime = LocalDateTime.parse(req.tanggalBayar)

                    transaction {
                        PembayaranPiutangKaryawanTable.insert {
                            it[PembayaranPiutangKaryawanTable.entitasId]         = EntityID(entitasUUID, EntitasUsahaTable)
                            it[PembayaranPiutangKaryawanTable.piutangId]         = EntityID(piutangId, PiutangKaryawanTable)
                            it[PembayaranPiutangKaryawanTable.tanggalBayar]      = bayarTime
                            it[PembayaranPiutangKaryawanTable.jumlahBayar]       = req.jumlahBayar
                            it[PembayaranPiutangKaryawanTable.kasId]             = EntityID(UUID.fromString(req.kasId), KasTable)
                            it[PembayaranPiutangKaryawanTable.keterangan]        = req.keterangan ?: ""
                        }

                        val row = PiutangKaryawanTable
                            .select {
                                (PiutangKaryawanTable.id eq EntityID(piutangId, PiutangKaryawanTable)) and
                                        (PiutangKaryawanTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable))
                            }
                            .single()
                        val newSisa = row[PiutangKaryawanTable.sisaPiutang] - req.jumlahBayar
                        PiutangKaryawanTable.update({
                            (PiutangKaryawanTable.id eq EntityID(piutangId, PiutangKaryawanTable)) and
                                    (PiutangKaryawanTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable))
                        }) {
                            it[PiutangKaryawanTable.sisaPiutang] = newSisa
                            it[PiutangKaryawanTable.status]      = if (newSisa <= 0.0) "LUNAS" else row[PiutangKaryawanTable.status]
                        }

                        KasService.record(
                            entitasId  = entitasUUID,
                            kasId      = UUID.fromString(req.kasId),
                            tanggal    = bayarTime,
                            jumlah     = req.jumlahBayar,
                            tipe       = "MASUK",
                            keterangan = "Pelunasan piutang karyawan #$piutangId"
                        )
                    }

                    call.respond(HttpStatusCode.Created)
                }

                // History
                get("/{id}/history") {
                    val entitasUUID = getEntitasIdFromJwt().value
                    val piutangId = UUID.fromString(call.parameters["id"]!!)

                    val history = transaction {
                        PembayaranPiutangKaryawanTable
                            .select {
                                (PembayaranPiutangKaryawanTable.piutangId eq EntityID(piutangId, PiutangKaryawanTable)) and
                                        (PembayaranPiutangKaryawanTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable))
                            }
                            .orderBy(PembayaranPiutangKaryawanTable.tanggalBayar to SortOrder.ASC)
                            .map { row ->
                                PiutangKaryawanHistory(
                                    id           = row[PembayaranPiutangKaryawanTable.id].value.toString(),
                                    tanggalBayar = row[PembayaranPiutangKaryawanTable.tanggalBayar].toString(),
                                    jumlahBayar  = row[PembayaranPiutangKaryawanTable.jumlahBayar],
                                    kasId        = row[PembayaranPiutangKaryawanTable.kasId].value.toString(),
                                    keterangan   = row[PembayaranPiutangKaryawanTable.keterangan]
                                )
                            }
                    }
                    call.respond(HttpStatusCode.OK, history)
                }
            }

            // ==== Piutang Lain-lain ====
            route("/lain") {
                // List piutang lain-lain
                get {
                    val entitasUUID = getEntitasIdFromJwt().value
                    val statusParam = call.request.queryParameters["status"]?.uppercase() ?: "BELUM_LUNAS"

                    val list = transaction {
                        PiutangLainTable
                            .slice(
                                PiutangLainTable.id,
                                PiutangLainTable.keterangan,
                                PiutangLainTable.totalPiutang,
                                PiutangLainTable.sisaPiutang,
                                PiutangLainTable.tanggalJatuhTempo,
                                PiutangLainTable.status,
                                PiutangLainTable.entitasId
                            )
                            .select {
                                (PiutangLainTable.status eq statusParam) and
                                        (PiutangLainTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable))
                            }
                            .orderBy(PiutangLainTable.tanggalJatuhTempo to SortOrder.ASC)
                            .map { row ->
                                PiutangLainSummaryDto(
                                    id           = row[PiutangLainTable.id].value.toString(),
                                    keterangan   = row[PiutangLainTable.keterangan],
                                    totalPiutang = row[PiutangLainTable.totalPiutang],
                                    sisaPiutang  = row[PiutangLainTable.sisaPiutang],
                                    jatuhTempo   = row[PiutangLainTable.tanggalJatuhTempo].toString(),
                                    status       = row[PiutangLainTable.status],
                                    entitasId    = row[PiutangLainTable.entitasId].value.toString()
                                )
                            }
                    }
                    call.respond(HttpStatusCode.OK, list)
                }


                // Bayar
                post("/{id}/bayar") {
                    val entitasUUID = getEntitasIdFromJwt().value
                    val piutangId = UUID.fromString(call.parameters["id"]!!)
                    val req = call.receive<PiutangLainBayarRequestDto>()
                    val bayarTime = LocalDateTime.parse(req.tanggalBayar)

                    transaction {
                        PembayaranPiutangLainTable.insert {
                            it[PembayaranPiutangLainTable.entitasId]         = EntityID(entitasUUID, EntitasUsahaTable)
                            it[PembayaranPiutangLainTable.piutangId]          = EntityID(piutangId, PiutangLainTable)
                            it[PembayaranPiutangLainTable.tanggalBayar]       = bayarTime
                            it[PembayaranPiutangLainTable.jumlahBayar]        = req.jumlahBayar
                            it[PembayaranPiutangLainTable.kasId]              = EntityID(UUID.fromString(req.kasId), KasTable)
                            it[PembayaranPiutangLainTable.keterangan]         = req.keterangan ?: ""
                        }

                        val row = PiutangLainTable
                            .select {
                                (PiutangLainTable.id eq EntityID(piutangId, PiutangLainTable)) and
                                        (PiutangLainTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable))
                            }
                            .single()
                        val newSisa = row[PiutangLainTable.sisaPiutang] - req.jumlahBayar
                        PiutangLainTable.update({
                            (PiutangLainTable.id eq EntityID(piutangId, PiutangLainTable)) and
                                    (PiutangLainTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable))
                        }) {
                            it[PiutangLainTable.sisaPiutang] = newSisa
                            it[PiutangLainTable.status]      = if (newSisa <= 0.0) "LUNAS" else row[PiutangLainTable.status]
                        }

                        KasService.record(
                            entitasId  = entitasUUID,
                            kasId      = UUID.fromString(req.kasId),
                            tanggal    = bayarTime,
                            jumlah     = req.jumlahBayar,
                            tipe       = "MASUK",
                            keterangan = "Pelunasan piutang lain-lain #$piutangId"
                        )
                    }

                    call.respond(HttpStatusCode.Created)
                }

                // ==== History Bayar Piutang Lain ====
                get("/{id}/history") {
                    val entitasUUID = getEntitasIdFromJwt().value
                    val piutangId = UUID.fromString(call.parameters["id"]!!)

                    val history = transaction {
                        PembayaranPiutangLainTable
                            .select {
                                (PembayaranPiutangLainTable.piutangId eq EntityID(piutangId, PiutangLainTable)) and
                                        (PembayaranPiutangLainTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable))
                            }
                            .orderBy(PembayaranPiutangLainTable.tanggalBayar to SortOrder.ASC)
                            .map { row ->
                                PiutangLainHistoryDto(
                                    id           = row[PembayaranPiutangLainTable.id].value.toString(),
                                    tanggalBayar = row[PembayaranPiutangLainTable.tanggalBayar].toString(),
                                    jumlahBayar  = row[PembayaranPiutangLainTable.jumlahBayar],
                                    kasId        = row[PembayaranPiutangLainTable.kasId].value.toString(),
                                    keterangan   = row[PembayaranPiutangLainTable.keterangan]
                                )
                            }
                    }
                    call.respond(HttpStatusCode.OK, history)
                }
            }
        }
    }
}


