// File: src/main/kotlin/com/kasir/routes/ProdukRoutes.kt
package com.kasir.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import com.kasir.models.ProdukRequest
import com.kasir.models.ProdukResponse
import com.kasir.models.ProdukTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Route.produkRoutes() {
    authenticate("auth-jwt") {
        route("/produk") {
            // GET all produk
            get {
                val produkList = transaction {
                    ProdukTable.selectAll().map { row ->
                        ProdukResponse(
                            id = row[ProdukTable.id].value.toString(),
                            namaProduk = row[ProdukTable.namaProduk],
                            kodeProduk = row[ProdukTable.kodeProduk],
                            hargaModal = row[ProdukTable.hargaModal],
                            hargaJual1 = row[ProdukTable.hargaJual1],
                            hargaJual2 = row[ProdukTable.hargaJual2],
                            hargaJual3 = row[ProdukTable.hargaJual3],
                            stok = row[ProdukTable.stok],
                            supplierId = row[ProdukTable.supplierId]?.toString(),
                            kategoriId = row[ProdukTable.kategoriId].toString()
                        )
                    }
                }
                call.respond(HttpStatusCode.OK, produkList)
            }

            // GET produk by ID
            get("/{id}") {
                val idParam = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing ID"))
                val uuid = runCatching { UUID.fromString(idParam) }.getOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid UUID format"))
                val produk = transaction {
                    ProdukTable.select { ProdukTable.id eq uuid }
                        .map { row ->
                            ProdukResponse(
                                id = row[ProdukTable.id].value.toString(),
                                namaProduk = row[ProdukTable.namaProduk],
                                kodeProduk = row[ProdukTable.kodeProduk],
                                hargaModal = row[ProdukTable.hargaModal],
                                hargaJual1 = row[ProdukTable.hargaJual1],
                                hargaJual2 = row[ProdukTable.hargaJual2],
                                hargaJual3 = row[ProdukTable.hargaJual3],
                                stok = row[ProdukTable.stok],
                                supplierId = row[ProdukTable.supplierId]?.toString(),
                                kategoriId = row[ProdukTable.kategoriId].toString()
                            )
                        }
                        .singleOrNull()
                }
                produk?.let { call.respond(HttpStatusCode.OK, it) }
                    ?: call.respond(HttpStatusCode.NotFound, mapOf("error" to "Produk tidak ditemukan"))
            }

            // POST create produk baru
            post {
                val req = call.receive<ProdukRequest>()
                if (req.namaProduk.isBlank() || req.kodeProduk.isBlank() || req.hargaModal <= 0.0 ||
                    req.hargaJual1 <= 0.0 || req.stok < 0 || req.kategoriId.isBlank()
                ) {
                    return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Field tidak valid atau kosong"))
                }
                val exists = transaction { ProdukTable.select { ProdukTable.kodeProduk eq req.kodeProduk }.any() }
                if (exists) return@post call.respond(HttpStatusCode.Conflict, mapOf("error" to "Kode produk sudah ada"))

                val supUuid = req.supplierId?.let { UUID.fromString(it) }
                val katUuid = UUID.fromString(req.kategoriId)

                // Insert and retrieve EntityID<UUID>, then extract .value
                val newIdEnt = transaction {
                    ProdukTable.insert { it ->
                        it[ProdukTable.namaProduk] = req.namaProduk
                        it[ProdukTable.kodeProduk] = req.kodeProduk
                        it[ProdukTable.hargaModal] = req.hargaModal
                        it[ProdukTable.hargaJual1] = req.hargaJual1
                        it[ProdukTable.hargaJual2] = req.hargaJual2
                        it[ProdukTable.hargaJual3] = req.hargaJual3
                        it[ProdukTable.stok] = req.stok
                        it[ProdukTable.supplierId] = supUuid
                        it[ProdukTable.kategoriId] = katUuid
                    } get ProdukTable.id
                }
                val newId: UUID = newIdEnt.value

                val response = ProdukResponse(
                    id = newId.toString(),
                    namaProduk = req.namaProduk,
                    kodeProduk = req.kodeProduk,
                    hargaModal = req.hargaModal,
                    hargaJual1 = req.hargaJual1,
                    hargaJual2 = req.hargaJual2,
                    hargaJual3 = req.hargaJual3,
                    stok = req.stok,
                    supplierId = req.supplierId,
                    kategoriId = req.kategoriId
                )
                call.respond(HttpStatusCode.Created, response)
            }

            // PUT update produk
            put("/{id}") {
                val idParam = call.parameters["id"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing ID"))
                val uuid = runCatching { UUID.fromString(idParam) }.getOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid UUID format"))
                val req = call.receive<ProdukRequest>()
                val supUuid = req.supplierId?.let { UUID.fromString(it) }
                val katUuid = UUID.fromString(req.kategoriId)

                val updatedCount = transaction {
                    ProdukTable.update({ ProdukTable.id eq uuid }) { it ->
                        it[ProdukTable.namaProduk] = req.namaProduk
                        it[ProdukTable.kodeProduk] = req.kodeProduk
                        it[ProdukTable.hargaModal] = req.hargaModal
                        it[ProdukTable.hargaJual1] = req.hargaJual1
                        it[ProdukTable.hargaJual2] = req.hargaJual2
                        it[ProdukTable.hargaJual3] = req.hargaJual3
                        it[ProdukTable.stok] = req.stok
                        it[ProdukTable.supplierId] = supUuid
                        it[ProdukTable.kategoriId] = katUuid
                    }
                }
                if (updatedCount == 0) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Produk tidak ditemukan"))
                else call.respond(HttpStatusCode.OK)
            }

            // DELETE produk
            delete("/{id}") {
                val idParam = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing ID"))
                val uuid = runCatching { UUID.fromString(idParam) }.getOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid UUID format"))
                val deletedCount = transaction { ProdukTable.deleteWhere { ProdukTable.id eq uuid } }
                if (deletedCount == 0) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Produk tidak ditemukan"))
                else call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}