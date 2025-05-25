package com.kasir.routes


import com.kasir.dto.StockOpname
import com.kasir.dto.StockOpnameResponse
import com.kasir.models.EntitasUsahaTable
import com.kasir.models.ProdukTable
import com.kasir.models.StockOpnameTable
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*


fun Route.stockOpnameRoutes() {
    authenticate("jwt-auth") {
        route("/stock-opname") {

            // GET all opname untuk entitas aktif
            get {
                val jwt = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val entitasUUID = runCatching {
                    UUID.fromString(jwt.payload.getClaim("entitasId").asString())
                }.getOrElse {
                    return@get call.respond(HttpStatusCode.BadRequest, "Invalid entitasId in token")
                }

                val list = transaction {
                    (StockOpnameTable innerJoin ProdukTable)
                        .select { StockOpnameTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable) }
                        .map { row ->
                            StockOpnameResponse(
                                id            = row[StockOpnameTable.id].value.toString(),
                                tanggalOpname = row[StockOpnameTable.tanggalOpname].toString(),
                                produkId      = row[StockOpnameTable.produkId].value.toString(),
                                produkName    = row[ProdukTable.namaProduk],
                                stokSistem    = row[StockOpnameTable.stokSistem],
                                stokFisik     = row[StockOpnameTable.stokFisik],
                                selisih       = row[StockOpnameTable.selisih],
                                keterangan    = row[StockOpnameTable.keterangan]
                            )
                        }
                }
                call.respond(HttpStatusCode.OK, list)
            }

            // POST create opname baru
            post {
                val jwt = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val entitasUUID = runCatching {
                    UUID.fromString(jwt.payload.getClaim("entitasId").asString())
                }.getOrElse {
                    return@post call.respond(HttpStatusCode.BadRequest, "Invalid entitasId in token")
                }
                val req = call.receive<StockOpname>()

                val createdId = transaction {
                    // Insert opname dengan entitasId
                    val id = StockOpnameTable.insertAndGetId { r ->
                        r[StockOpnameTable.entitasId]     = EntityID(entitasUUID, EntitasUsahaTable)
                        r[StockOpnameTable.tanggalOpname] = LocalDateTime.parse(req.tanggalOpname)
                        r[StockOpnameTable.produkId]      = EntityID(UUID.fromString(req.produkId), ProdukTable)
                        r[StockOpnameTable.stokSistem]    = req.stokSistem
                        r[StockOpnameTable.stokFisik]     = req.stokFisik
                        r[StockOpnameTable.selisih]       = req.selisih
                        r[StockOpnameTable.keterangan]    = req.keterangan
                    }
                    // Update stok produk, hanya jika milik entitas
                    ProdukTable.update({
                        (ProdukTable.id eq EntityID(UUID.fromString(req.produkId), ProdukTable)) and
                                (ProdukTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable))
                    }) {
                        it[ProdukTable.stok] = req.stokFisik
                    }
                    id.value
                }
                // Ambil record hasil insert untuk response
                val response = transaction {
                    (StockOpnameTable innerJoin ProdukTable)
                        .select {
                            (StockOpnameTable.id eq EntityID(createdId, StockOpnameTable)) and
                                    (StockOpnameTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable))
                        }
                        .first().let { row ->
                            StockOpnameResponse(
                                id            = row[StockOpnameTable.id].value.toString(),
                                tanggalOpname = row[StockOpnameTable.tanggalOpname].toString(),
                                produkId      = row[StockOpnameTable.produkId].value.toString(),
                                produkName    = row[ProdukTable.namaProduk],
                                stokSistem    = row[StockOpnameTable.stokSistem],
                                stokFisik     = row[StockOpnameTable.stokFisik],
                                selisih       = row[StockOpnameTable.selisih],
                                keterangan    = row[StockOpnameTable.keterangan]
                            )
                        }
                }
                call.respond(HttpStatusCode.Created, response)
            }

            // PUT update opname
            put("/{id}") {
                val jwt = call.principal<JWTPrincipal>()
                    ?: return@put call.respond(HttpStatusCode.Unauthorized)
                val entitasUUID = runCatching {
                    UUID.fromString(jwt.payload.getClaim("entitasId").asString())
                }.getOrElse {
                    return@put call.respond(HttpStatusCode.BadRequest, "Invalid entitasId in token")
                }
                val idParam = call.parameters["id"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing ID")
                val opnameUUID = runCatching { UUID.fromString(idParam) }.getOrNull()
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid UUID")
                val req = call.receive<StockOpname>()


                transaction {
                    // Update record opname
                    StockOpnameTable.update({
                        (StockOpnameTable.id eq EntityID(opnameUUID, StockOpnameTable)) and
                                (StockOpnameTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable))
                    }) { r ->
                        r[StockOpnameTable.tanggalOpname] = LocalDateTime.parse(req.tanggalOpname)
                        r[StockOpnameTable.produkId]      = EntityID(UUID.fromString(req.produkId), ProdukTable)
                        r[StockOpnameTable.stokSistem]    = req.stokSistem
                        r[StockOpnameTable.stokFisik]     = req.stokFisik
                        r[StockOpnameTable.selisih]       = req.selisih
                        r[StockOpnameTable.keterangan]    = req.keterangan
                    }
                    // Update stok produk
                    ProdukTable.update({
                        (ProdukTable.id eq EntityID(UUID.fromString(req.produkId), ProdukTable)) and
                                (ProdukTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable))
                    }) {
                        it[ProdukTable.stok] = req.stokFisik
                    }
                }

                call.respond(HttpStatusCode.OK)
            }


            // DELETE opname
            delete("/{id}") {
                val jwt = call.principal<JWTPrincipal>()
                    ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                val entitasUUID = runCatching {
                    UUID.fromString(jwt.payload.getClaim("entitasId").asString())
                }.getOrElse {
                    return@delete call.respond(HttpStatusCode.BadRequest, "Invalid entitasId in token")
                }
                val idParam = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing ID")
                val opnameUUID = runCatching { UUID.fromString(idParam) }.getOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid UUID")

                val deleted = transaction {
                    StockOpnameTable.deleteWhere {
                        (StockOpnameTable.id eq EntityID(opnameUUID, StockOpnameTable)) and
                                (StockOpnameTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable))
                    }
                }
                if (deleted > 0) call.respond(HttpStatusCode.NoContent)
                else             call.respond(HttpStatusCode.NotFound, "Opname not found")
            }
        }
    }
}