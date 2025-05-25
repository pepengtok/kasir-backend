package com.kasir.routes

import com.kasir.dto.IdResponse
import com.kasir.models.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID
import com.kasir.models.EntitasUsahaTable // ✅ tambahan

@Serializable
data class BiayaNonOpRequest(
    val tanggal: String,
    val nominal: Double,
    val keterangan: String,
    val kasId: String,
    val entitasId: String // ✅ tambahan
)

@Serializable
data class BiayaNonOpResponse(
    val id: String,
    val tanggal: String,
    val nominal: Double,
    val keterangan: String,
    val kasId: String,
    val saldoAfter: Double
)

@Serializable
data class BiayaOpRequest(
    val tanggal: String,
    val nominal: Double,
    val kategori: String,
    val keterangan: String,
    val kasId: String,
    val entitasId: String // ✅ tambahan
)

@Serializable
data class BiayaOpResponse(
    val id: String,
    val tanggal: String,
    val nominal: Double,
    val kategori: String,
    val keterangan: String,
    val kasId: String,
    val saldoAfter: Double
)

fun Route.biayaRoutes() {
    authenticate("jwt-auth") {

        // --- Biaya Non-Operasional ---
        route("/biaya/non-operasional") {
            get {
                val entitasId = call.request.queryParameters["entitas_id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "entitas_id wajib diisi"))

                val list = transaction {
                    BiayaNonOperasionalTable
                        .select { BiayaNonOperasionalTable.entitasId eq UUID.fromString(entitasId) }
                        .orderBy(BiayaNonOperasionalTable.tanggal to SortOrder.ASC)
                        .map { row ->
                            BiayaNonOpResponse(
                                id         = row[BiayaNonOperasionalTable.id].value.toString(),
                                tanggal    = row[BiayaNonOperasionalTable.tanggal].toString(),
                                nominal    = row[BiayaNonOperasionalTable.nominal],
                                keterangan = row[BiayaNonOperasionalTable.keterangan],
                                kasId      = row[BiayaNonOperasionalTable.kasId].value.toString(),
                                saldoAfter = row[BiayaNonOperasionalTable.saldoAfter]
                            )
                        }
                }
                call.respond(HttpStatusCode.OK, list)
            }

            post {
                val req = call.receive<BiayaNonOpRequest>()
                val newId = transaction {
                    val kasUUID = UUID.fromString(req.kasId)
                    val entitasUUID = UUID.fromString(req.entitasId)
                    val current = KasTable
                        .select { KasTable.id eq EntityID(kasUUID, KasTable) }
                        .single()[KasTable.saldoAkhir]
                    val updatedSaldo = current - req.nominal

                    val id = BiayaNonOperasionalTable.insertAndGetId { row ->
                        row[tanggal]    = LocalDateTime.parse(req.tanggal)
                        row[nominal]    = req.nominal
                        row[keterangan] = req.keterangan
                        row[kasId]      = EntityID(kasUUID, KasTable)
                        row[saldoAfter] = updatedSaldo
                        row[entitasId]  = EntityID(entitasUUID, EntitasUsahaTable) // ✅
                    }

                    KasTransaksiTable.insert { tr ->
                        tr[kasId]      = EntityID(kasUUID, KasTable)
                        tr[tanggal]    = LocalDateTime.now()
                        tr[jumlah]     = req.nominal
                        tr[keterangan] = "Biaya non-op entry ${id.value}"
                        tr[tipe]       = "KELUAR"
                        tr[entitasId]  = EntityID(entitasUUID, EntitasUsahaTable) // ✅
                    }

                    KasTable.update({ KasTable.id eq EntityID(kasUUID, KasTable) }) {
                        it[saldoAkhir] = updatedSaldo
                    }

                    id.value
                }

                call.respond(HttpStatusCode.Created, IdResponse(newId.toString()))
            }
        }

        // --- Biaya Operasional ---
        route("/biaya/operasional") {
            get {
                val entitasId = call.request.queryParameters["entitas_id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "entitas_id wajib diisi"))

                val list = transaction {
                    BiayaOperasionalTable
                        .select { BiayaOperasionalTable.entitasId eq UUID.fromString(entitasId) }
                        .orderBy(BiayaOperasionalTable.tanggal to SortOrder.ASC)
                        .map { row ->
                            BiayaOpResponse(
                                id         = row[BiayaOperasionalTable.id].value.toString(),
                                tanggal    = row[BiayaOperasionalTable.tanggal].toString(),
                                nominal    = row[BiayaOperasionalTable.nominal],
                                kategori   = row[BiayaOperasionalTable.kategori],
                                keterangan = row[BiayaOperasionalTable.keterangan],
                                kasId      = row[BiayaOperasionalTable.kasId].value.toString(),
                                saldoAfter = row[BiayaOperasionalTable.saldoAfter]
                            )
                        }
                }
                call.respond(HttpStatusCode.OK, list)
            }

            post {
                val req = call.receive<BiayaOpRequest>()
                val newId = transaction {
                    val kasUUID = UUID.fromString(req.kasId)
                    val entitasUUID = UUID.fromString(req.entitasId)
                    val current = KasTable
                        .select { KasTable.id eq EntityID(kasUUID, KasTable) }
                        .single()[KasTable.saldoAkhir]
                    val updatedSaldo = current - req.nominal

                    val id = BiayaOperasionalTable.insertAndGetId { row ->
                        row[tanggal]    = LocalDateTime.parse(req.tanggal)
                        row[nominal]    = req.nominal
                        row[kategori]   = req.kategori
                        row[keterangan] = req.keterangan
                        row[kasId]      = EntityID(kasUUID, KasTable)
                        row[saldoAfter] = updatedSaldo
                        row[entitasId]  = EntityID(entitasUUID, EntitasUsahaTable) // ✅
                    }

                    KasTransaksiTable.insert { tr ->
                        tr[kasId]      = EntityID(kasUUID, KasTable)
                        tr[tanggal]    = LocalDateTime.now()
                        tr[jumlah]     = req.nominal
                        tr[keterangan] = "Biaya op entry ${id.value}"
                        tr[tipe]       = "KELUAR"
                        tr[entitasId]  = EntityID(entitasUUID, EntitasUsahaTable) // ✅
                    }

                    KasTable.update({ KasTable.id eq EntityID(kasUUID, KasTable) }) {
                        it[saldoAkhir] = updatedSaldo
                    }

                    id.value
                }

                call.respond(HttpStatusCode.Created, IdResponse(newId.toString()))
            }
        }
    }
}
