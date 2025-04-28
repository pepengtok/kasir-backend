// File: src/main/kotlin/com/kasir/routes/SupplierRoutes.kt
package com.kasir.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import com.kasir.models.SupplierRequest
import com.kasir.models.SupplierResponse
import com.kasir.models.SupplierTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

/**
 * CRUD routes untuk Supplier.
 * Semua route berada di bawah authenticate("auth-jwt").
 */
fun Route.supplierRoutes() {
    authenticate("auth-jwt") {
        route("/supplier") {

            // GET /supplier -> daftar semua supplier
            get {
                val list = transaction {
                    SupplierTable.selectAll().map { row ->
                        SupplierResponse(
                            id           = row[SupplierTable.id].value.toString(),
                            namaSupplier = row[SupplierTable.namaSupplier],
                            no_hp      = row[SupplierTable.no_hp],
                            alamat       = row[SupplierTable.alamat],
                            rekeningBank = row[SupplierTable.rekeningBank],
                            namaSales    = row[SupplierTable.namaSales]
                        )
                    }
                }
                call.respond(HttpStatusCode.OK, list)
            }

            // POST /supplier -> tambah supplier baru
            post {
                val req = call.receive<SupplierRequest>()
                val newId = transaction {
                    SupplierTable.insert { it ->
                        it[SupplierTable.namaSupplier] = req.namaSupplier
                        it[SupplierTable.no_hp]       = req.no_hp
                        it[SupplierTable.alamat]       = req.alamat
                        it[SupplierTable.rekeningBank] = req.rekeningBank
                        it[SupplierTable.namaSales]    = req.namaSales
                    } get SupplierTable.id
                }
                call.respond(HttpStatusCode.Created, SupplierResponse(
                    id           = newId.value.toString(),
                    namaSupplier = req.namaSupplier,
                    no_hp       = req.no_hp,
                    alamat       = req.alamat,
                    rekeningBank = req.rekeningBank,
                    namaSales    = req.namaSales
                ))
            }

            // PUT /supplier/{id} -> update supplier
            put("/{id}") {
                val idParam = call.parameters["id"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing ID"))
                val uuid = runCatching { UUID.fromString(idParam) }.getOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid UUID format"))
                val req = call.receive<SupplierRequest>()
                val count = transaction {
                    SupplierTable.update({ SupplierTable.id eq uuid }) { it ->
                        it[SupplierTable.namaSupplier] = req.namaSupplier
                        it[SupplierTable.no_hp]       = req.no_hp
                        it[SupplierTable.alamat]       = req.alamat
                        it[SupplierTable.rekeningBank] = req.rekeningBank
                        it[SupplierTable.namaSales]    = req.namaSales
                    }
                }
                if (count == 0) call.respond(HttpStatusCode.NotFound)
                else call.respond(HttpStatusCode.OK)
            }

            // DELETE /supplier/{id} -> hapus supplier
            delete("/{id}") {
                val idParam = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing ID"))
                val uuid = runCatching { UUID.fromString(idParam) }.getOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid UUID format"))
                val deleted = transaction { SupplierTable.deleteWhere { SupplierTable.id eq uuid } }
                if (deleted == 0) call.respond(HttpStatusCode.NotFound)
                else call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
