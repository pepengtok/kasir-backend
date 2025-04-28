package com.kasir.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import com.kasir.models.PelangganRequest
import com.kasir.models.PelangganResponse
import com.kasir.models.PelangganTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

/**
 * CRUD routes untuk Pelanggan.
 */
fun Route.pelangganRoutes() {
    authenticate("auth-jwt") {
        route("/pelanggan") {

            // GET /pelanggan -> daftar semua pelanggan
            get {
                val list = transaction {
                    PelangganTable.selectAll().map { row ->
                        PelangganResponse(
                            id            = row[PelangganTable.id].value.toString(),
                            namaPelanggan = row[PelangganTable.namaPelanggan],
                            kontak        = row[PelangganTable.kontak],
                            alamat        = row[PelangganTable.alamat],
                            keterangan    = row[PelangganTable.keterangan],
                            salesId       = row[PelangganTable.salesId]?.toString()
                        )
                    }
                }
                call.respond(HttpStatusCode.OK, list)
            }

            // POST /pelanggan -> tambah pelanggan baru
            post {
                val req = call.receive<PelangganRequest>()
                val newId = transaction {
                    PelangganTable.insert { it ->
                        it[PelangganTable.namaPelanggan] = req.namaPelanggan
                        it[PelangganTable.kontak]        = req.kontak
                        it[PelangganTable.alamat]        = req.alamat
                        it[PelangganTable.keterangan]    = req.keterangan
                        it[PelangganTable.salesId]       = req.salesId?.let(UUID::fromString)
                    } get PelangganTable.id
                }
                call.respond(HttpStatusCode.Created, PelangganResponse(
                    id            = newId.value.toString(),
                    namaPelanggan = req.namaPelanggan,
                    kontak        = req.kontak,
                    alamat        = req.alamat,
                    keterangan    = req.keterangan,
                    salesId       = req.salesId
                ))
            }

            // PUT /pelanggan/{id} -> update pelanggan
            put("/{id}") {
                val idParam = call.parameters["id"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing ID"))
                val uuid = runCatching { UUID.fromString(idParam) }.getOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid UUID format"))
                val req = call.receive<PelangganRequest>()
                val updated = transaction {
                    PelangganTable.update({ PelangganTable.id eq uuid }) { it ->
                        it[PelangganTable.namaPelanggan] = req.namaPelanggan
                        it[PelangganTable.kontak]        = req.kontak
                        it[PelangganTable.alamat]        = req.alamat
                        it[PelangganTable.keterangan]    = req.keterangan
                        it[PelangganTable.salesId]       = req.salesId?.let(UUID::fromString)
                    }
                }
                if (updated == 0) call.respond(HttpStatusCode.NotFound)
                else call.respond(HttpStatusCode.OK)
            }

            // DELETE /pelanggan/{id} -> hapus pelanggan
            delete("/{id}") {
                val idParam = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing ID"))
                val uuid = runCatching { UUID.fromString(idParam) }.getOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid UUID format"))
                val deleted = transaction { PelangganTable.deleteWhere { PelangganTable.id eq uuid } }
                if (deleted == 0) call.respond(HttpStatusCode.NotFound)
                else call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
