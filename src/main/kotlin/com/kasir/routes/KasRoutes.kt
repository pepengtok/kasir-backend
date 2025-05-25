package com.kasir.routes

import com.kasir.dto.KasRecordRequestDto
import com.kasir.models.*
import com.kasir.models.KasTable
import com.kasir.dto.*
import com.kasir.models.KasTransaksiTable
import com.kasir.models.KasTransferTable
import com.kasir.service.KasService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID
import com.kasir.dto.KasTransferRequestDto
import com.kasir.dto.IdResponse
import com.kasir.models.EntitasUsahaTable

fun Route.kasRoutes() {
    authenticate("jwt-auth") {

        // === KAS ===
        route("/kas") {
            // GET list kas
            get {
                val typeParam   = call.request.queryParameters["type"]?.lowercase()
                val entitasId   = call.request.queryParameters["entitas_id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "entitas_id wajib diisi")

                val list = transaction {
                    val baseQuery = KasTable.select { KasTable.entitasId eq UUID.fromString(entitasId) }

                    val query = when (typeParam) {
                        "besar" -> baseQuery.andWhere { KasTable.jenis eq "BESAR" }
                        "kecil" -> baseQuery.andWhere { KasTable.jenis eq "KECIL" }
                        "bank"  -> baseQuery.andWhere { KasTable.jenis eq "BANK" }
                        else    -> baseQuery
                    }

                    query.map { row ->
                        Kas(
                            id         = row[KasTable.id].value.toString(),
                            namaKas    = row[KasTable.namaKas],
                            saldoAkhir = row[KasTable.saldoAkhir],
                            jenis      = row[KasTable.jenis],
                            subJenis   = row[KasTable.subJenis],
                            entitasId  = row[KasTable.entitasId].value.toString()
                        )
                    }
                }
                call.respond(HttpStatusCode.OK, list)
            }

            // POST buat akun kas
            post {
                val req = call.receive<Kas>()
                val entitasId = req.entitasId // pastikan ada di DTO Kas

                val newId = transaction {
                    KasTable.insertAndGetId { row ->
                        row[namaKas]    = req.namaKas
                        row[saldoAkhir] = req.saldoAkhir
                        row[jenis]      = req.jenis
                        row[subJenis]   = req.subJenis
                        row[KasTable.entitasId] = EntityID(UUID.fromString(entitasId), EntitasUsahaTable) // ✅ penting
                    }
                }.value

                call.respond(HttpStatusCode.Created, IdResponse(newId.toString()))
            }


            // GET by ID
            get("{id}") {
                val idParam = call.parameters["id"] ?: return@get call.respondText(
                    "Missing id", status = HttpStatusCode.BadRequest
                )
                val item = transaction {
                    KasTable.select { KasTable.id eq EntityID(UUID.fromString(idParam), KasTable) }
                        .map { row ->
                            Kas(
                                id         = row[KasTable.id].value.toString(),
                                namaKas    = row[KasTable.namaKas],
                                saldoAkhir = row[KasTable.saldoAkhir],
                                jenis      = row[KasTable.jenis],
                                subJenis   = row[KasTable.subJenis],
                                entitasId  = row[KasTable.entitasId].value.toString()
                            )
                        }
                        .singleOrNull()
                }
                item?.let { call.respond(HttpStatusCode.OK, it) }
                    ?: call.respondText("Not found", status = HttpStatusCode.NotFound)
            }

            // PUT update
            put("{id}") {
                val idParam = call.parameters["id"] ?: return@put call.respondText(
                    "Missing id", status = HttpStatusCode.BadRequest
                )
                val req = call.receive<Kas>()
                val updated = transaction {
                    KasTable.update({ KasTable.id eq EntityID(UUID.fromString(idParam), KasTable) }) { row ->
                        row[namaKas]    = req.namaKas
                        row[saldoAkhir] = req.saldoAkhir
                        row[jenis]      = req.jenis
                        row[subJenis]   = req.subJenis
                        // Tidak ubah entitasId saat update
                    }
                }
                call.respond(HttpStatusCode.OK, mapOf("updated" to (updated > 0)))
            }
            // DELETE
            delete("{id}") {
                val idParam = call.parameters["id"] ?: return@delete call.respondText(
                    "Missing id", status = HttpStatusCode.BadRequest
                )
                val deleted = transaction {
                    KasTable.deleteWhere { KasTable.id eq EntityID(UUID.fromString(idParam), KasTable) }
                }
                call.respond(HttpStatusCode.OK, mapOf("deleted" to (deleted > 0)))
            }
        }

        // === KAS TRANSAKSI ===
        route("/kas-transaksi") {
            // List transaksi kas dengan filter optional
            get {
                val kasIdParam   = call.request.queryParameters["kasId"]?.let(UUID::fromString)
                val fromParam    = call.request.queryParameters["from"]?.let(LocalDateTime::parse)
                val toParam      = call.request.queryParameters["to"]?.let(LocalDateTime::parse)
                val entitasParam = call.request.queryParameters["entitas_id"]?.let(UUID::fromString)

                val list = transaction {
                    var query = KasTransaksiTable.selectAll()

                    kasIdParam?.let { query = query.andWhere { KasTransaksiTable.kasId eq EntityID(it, KasTable) } }
                    fromParam?.let { query = query.andWhere { KasTransaksiTable.tanggal greaterEq it } }
                    toParam?.let   { query = query.andWhere { KasTransaksiTable.tanggal lessEq it } }
                    entitasParam?.let { query = query.andWhere { KasTransaksiTable.entitasId eq it } } // ✅

                    query.orderBy(KasTransaksiTable.tanggal to SortOrder.ASC)
                        .map { row ->
                            KasTransaksi(
                                id         = row[KasTransaksiTable.id].value.toString(),
                                kasId      = row[KasTransaksiTable.kasId].value.toString(),
                                tanggal    = row[KasTransaksiTable.tanggal].toString(),
                                keterangan = row[KasTransaksiTable.keterangan],
                                jumlah     = row[KasTransaksiTable.jumlah],
                                tipe       = row[KasTransaksiTable.tipe],
                                entitasId  = row[KasTransaksiTable.entitasId].value.toString() // ✅ fix
                            )
                        }
                }

                call.respond(HttpStatusCode.OK, list)
            }

            // Catat transaksi kas (masuk/keluar)
            post {
                val req = call.receive<KasRecordRequestDto>()
                KasService.record(
                    entitasId  = UUID.fromString(req.entitasId), // ✅ wajib
                    kasId      = UUID.fromString(req.kasId),
                    tanggal    = LocalDateTime.parse(req.tanggal),
                    jumlah     = req.jumlah,
                    tipe       = req.tipe,
                    keterangan = req.keterangan ?: ""
                )
                call.respond(HttpStatusCode.Created, mapOf("message" to "Kas transaksi tercatat"))
            }
        }

        // === KAS TRANSFER ===
        route("/kas-transfer") {
            // List
            get {
                val fromParam    = call.request.queryParameters["from"]?.let(LocalDateTime::parse)
                val toParam      = call.request.queryParameters["to"]?.let(LocalDateTime::parse)
                val entitasParam = call.request.queryParameters["entitas_id"]?.let(UUID::fromString)

                val list = transaction {
                    var query = KasTransferTable.selectAll()
                    fromParam?.let { query = query.andWhere { KasTransferTable.tanggal greaterEq it } }
                    toParam?.let   { query = query.andWhere { KasTransferTable.tanggal lessEq it } }
                    entitasParam?.let { query = query.andWhere { KasTransferTable.entitasId eq it } } // ✅

                    query.orderBy(KasTransferTable.tanggal to SortOrder.ASC)
                        .map { row ->
                            KasTransfer(
                                id          = row[KasTransferTable.id].value.toString(),
                                kasAsalId   = row[KasTransferTable.kasAsalId].value.toString(),
                                kasTujuanId = row[KasTransferTable.kasTujuanId].value.toString(),
                                tanggal     = row[KasTransferTable.tanggal].toString(),
                                jumlah      = row[KasTransferTable.jumlah],
                                entitasId   = row[KasTransferTable.entitasId].value.toString() // ✅
                            )
                        }
                }
                call.respond(HttpStatusCode.OK, list)
            }

            // Transfer saldo antar kas
            post {
                val req = call.receive<KasTransferRequestDto>()
                val newId = transaction {
                    KasTransferTable.insertAndGetId { row ->
                        row[kasAsalId]   = EntityID(UUID.fromString(req.kasAsalId), KasTable)
                        row[kasTujuanId] = EntityID(UUID.fromString(req.kasTujuanId), KasTable)
                        row[tanggal]     = LocalDateTime.parse(req.tanggal)
                        row[jumlah]      = req.jumlah
                        row[entitasId]   = EntityID(UUID.fromString(req.entitasId), EntitasUsahaTable) // ✅
                    }
                }.value

                transaction {
                    val asalId   = UUID.fromString(req.kasAsalId)
                    val tujuanId = UUID.fromString(req.kasTujuanId)


                    val asalSaldo = KasTable.select { KasTable.id eq EntityID(asalId, KasTable) }
                        .single()[KasTable.saldoAkhir]
                    KasTable.update({ KasTable.id eq EntityID(asalId, KasTable) }) {
                        it[saldoAkhir] = asalSaldo - req.jumlah
                    }

                    // update saldo Kas Tujuan
                    val tujuanSaldo = KasTable.select { KasTable.id eq EntityID(tujuanId, KasTable) }
                        .single()[KasTable.saldoAkhir]
                    KasTable.update({ KasTable.id eq EntityID(tujuanId, KasTable) }) {
                        it[saldoAkhir] = tujuanSaldo + req.jumlah
                    }
                }

                call.respond(HttpStatusCode.Created, mapOf("id" to newId.toString()))
            }
        }
    }
}
