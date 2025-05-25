package com.kasir.routes

import com.kasir.dto.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import com.kasir.models.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.util.pipeline.PipelineContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.dao.id.EntityID
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun Route.pelangganRoutes() {
    authenticate("jwt-auth") {
        route("/pelanggan") {

            // Helper function untuk mendapatkan entitasId dari JWT
            fun PipelineContext<Unit, ApplicationCall>.getEntitasIdFromJwt(): EntityID<UUID> {
                val principal = call.principal<JWTPrincipal>()!!
                val entitasUUID = UUID.fromString(principal.payload.getClaim("entitasId").asString())
                return EntityID(entitasUUID, EntitasUsahaTable)
            }

            // GET semua pelanggan yang dimiliki oleh entitas dari JWT
            get {
                val entitasIdFromJwt = getEntitasIdFromJwt()

                val list = transaction {
                    PelangganTable
                        .select { PelangganTable.entitasId eq entitasIdFromJwt }
                        .map { row ->
                            PelangganResponse(
                                id = row[PelangganTable.id].value.toString(),
                                namaPelanggan = row[PelangganTable.namaPelanggan],
                                no_hp = row[PelangganTable.no_hp],
                                alamat = row[PelangganTable.alamat],
                                keterangan = row[PelangganTable.keterangan],
                                salesId = row[PelangganTable.salesId]?.value?.toString(),
                                entitasId = row[PelangganTable.entitasId].value.toString(),
                                latitude = row[PelangganTable.latitude],
                                longitude = row[PelangganTable.longitude]
                            )
                        }
                }
                call.respond(HttpStatusCode.OK, list)
            }

            // GET pelanggan berdasarkan ID
            get("/{id}") {
                val entitasIdFromJwt = getEntitasIdFromJwt()
                val pelangganUUID = call.parameters["id"]?.let(UUID::fromString)
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID pelanggan tidak valid"))

                val pelangganResponse = transaction {
                    PelangganTable
                        .select { (PelangganTable.id eq pelangganUUID) and (PelangganTable.entitasId eq entitasIdFromJwt) }
                        .singleOrNull()
                        ?.let { row ->
                            PelangganResponse(
                                id = row[PelangganTable.id].value.toString(),
                                namaPelanggan = row[PelangganTable.namaPelanggan],
                                no_hp = row[PelangganTable.no_hp],
                                alamat = row[PelangganTable.alamat],
                                keterangan = row[PelangganTable.keterangan],
                                salesId = row[PelangganTable.salesId]?.value?.toString(),
                                entitasId = row[PelangganTable.entitasId].value.toString(),
                                latitude = row[PelangganTable.latitude],
                                longitude = row[PelangganTable.longitude]
                            )
                        }
                }

                if (pelangganResponse == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Pelanggan tidak ditemukan atau tidak memiliki akses"))
                } else {
                    call.respond(HttpStatusCode.OK, pelangganResponse)
                }
            }


            // POST Tambah pelanggan baru
            post {
                val entitasIdFromJwt = getEntitasIdFromJwt()

                val req = call.receive<PelangganRequest>()

                val newId = transaction {
                    PelangganTable.insert {
                        it[namaPelanggan] = req.namaPelanggan
                        it[no_hp]         = req.no_hp
                        it[alamat]        = req.alamat
                        it[keterangan]    = req.keterangan
                        it[salesId]       = req.salesId?.let(UUID::fromString)
                        it[PelangganTable.entitasId] = entitasIdFromJwt
                        it[latitude]      = req.latitude
                        it[longitude]     = req.longitude
                    } get PelangganTable.id
                }

                call.respond(HttpStatusCode.Created, PelangganResponse(
                    id = newId.value.toString(),
                    namaPelanggan = req.namaPelanggan,
                    no_hp = req.no_hp,
                    alamat = req.alamat,
                    keterangan = req.keterangan,
                    salesId = req.salesId,
                    entitasId = entitasIdFromJwt.value.toString(),
                    latitude = req.latitude,
                    longitude = req.longitude
                ))
            }

            // PUT Update pelanggan
            put("/{id}") {
                val entitasIdFromJwt = getEntitasIdFromJwt()

                val uuid = call.parameters["id"]?.let(UUID::fromString)
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID pelanggan tidak valid"))
                val req = call.receive<PelangganRequest>()

                val updatedCount = transaction {
                    PelangganTable.update({ (PelangganTable.id eq uuid) and (PelangganTable.entitasId eq entitasIdFromJwt) }) {
                        it[namaPelanggan] = req.namaPelanggan
                        it[no_hp] = req.no_hp
                        it[alamat] = req.alamat
                        it[keterangan] = req.keterangan
                        it[salesId] = req.salesId?.let(UUID::fromString)
                        it[latitude] = req.latitude
                        it[longitude] = req.longitude
                    }
                }
                if (updatedCount == 0) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Pelanggan tidak ditemukan atau tidak memiliki akses"))
                else call.respond(HttpStatusCode.OK, mapOf("message" to "Pelanggan berhasil diperbarui"))
            }

            // DELETE Hapus pelanggan
            delete("/{id}") {
                val entitasIdFromJwt = getEntitasIdFromJwt()

                val uuid = call.parameters["id"]?.let(UUID::fromString)
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID pelanggan tidak valid"))

                val deletedRowCount = transaction {
                    PelangganTable.deleteWhere { (PelangganTable.id eq uuid) and (PelangganTable.entitasId eq entitasIdFromJwt) }
                }
                if (deletedRowCount == 0) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Pelanggan tidak ditemukan atau tidak memiliki akses"))
                else call.respond(HttpStatusCode.NoContent)
            }

            // GET Riwayat Pembelian Pelanggan
            get("/{id}/pembelian") {
                val entitasIdFromJwt = getEntitasIdFromJwt()

                val pelangganId = call.parameters["id"]?.let(UUID::fromString)
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID pelanggan tidak valid"))

                val tipe = call.request.queryParameters["tipe"]?.uppercase()
                val filter = call.request.queryParameters["filter"]?.lowercase()
                val statusQueryParam = call.request.queryParameters["status"]?.uppercase()

                val since: LocalDateTime? = when (filter) {
                    "harian"  -> LocalDate.now().atStartOfDay()
                    "bulanan" -> LocalDate.now().withDayOfMonth(1).atStartOfDay()
                    "tahunan" -> LocalDate.now().withDayOfYear(1).atStartOfDay()
                    else -> null
                }

                val history = transaction {
                    var query = PenjualanTable.select {
                        (PenjualanTable.pelangganId eq pelangganId) and (PenjualanTable.entitasId eq entitasIdFromJwt)
                    }

                    tipe?.let    { query = query.andWhere { PenjualanTable.metodePembayaran eq it } }
                    // ✅ PERBAIKAN: Gunakan .greaterEq() sebagai fungsi biasa
                    since?.let   { query = query.andWhere { PenjualanTable.tanggal.greaterEq(since.toLocalDate()) } } // Mengonversi LocalDateTime ke LocalDate
                    statusQueryParam?.let  { query = query.andWhere { PenjualanTable.status eq it } }

                    query.map { row ->
                        PelangganPembelianResponse(
                            penjualanId = row[PenjualanTable.id].value.toString(),
                            tanggal = row[PenjualanTable.tanggal].toString(),
                            total = row[PenjualanTable.total],
                            metode = row[PenjualanTable.metodePembayaran],
                            status = row[PenjualanTable.status],
                            jatuhTempo = row[PenjualanTable.jatuhTempo]?.toString(),
                            noNota = row[PenjualanTable.noNota],
                            notaUrl = row[PenjualanTable.notaUrl]
                        )
                    }
                }

                call.respond(HttpStatusCode.OK, history)
            }
        }
    }
}