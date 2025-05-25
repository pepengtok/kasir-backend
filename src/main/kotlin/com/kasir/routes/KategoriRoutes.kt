// src/main/kotlin/com/kasir/routes/KategoriRoutes.kt
package com.kasir.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import com.kasir.models.* // Untuk KategoriProdukTable, EntitasUsahaTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.dao.id.EntityID
import java.util.UUID
import com.kasir.models.EntitasUsahaTable
import io.ktor.server.auth.principal
import io.ktor.server.auth.jwt.JWTPrincipal
import com.kasir.dto.KategoriProdukDto // Import DTO yang benar
import com.kasir.dto.KategoriProdukRequestDto // Import DTO yang benar

fun Route.kategoriRoutes() {
    authenticate("jwt-admin") { // Asumsi hanya admin yang bisa kelola kategori
        route("/kategori") {
            // GET all kategori (filtered by entitasId dari JWT)
            get {
                val principal = call.principal<JWTPrincipal>()!!
                val entitasUUID = UUID.fromString(principal.payload.getClaim("entitasId").asString())

                val list = transaction {
                    KategoriProdukTable
                        .select { KategoriProdukTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable) }
                        .map { row ->
                            KategoriProdukDto(
                                id = row[KategoriProdukTable.id].value.toString(),
                                namaKategori = row[KategoriProdukTable.namaKategori],
                                entitasId = row[KategoriProdukTable.entitasId].value.toString() // ✅ PERBAIKAN DI SINI
                            )
                        }
                }
                call.respond(HttpStatusCode.OK, list)
            }

            // POST (Create) kategori
            post {
                val principal = call.principal<JWTPrincipal>()!!
                val entitasUUID = UUID.fromString(principal.payload.getClaim("entitasId").asString())
                val req = call.receive<KategoriProdukRequestDto>()

                val newKategoriId = transaction {
                    KategoriProdukTable.insertAndGetId { id ->
                        id[KategoriProdukTable.namaKategori] = req.nama
                        id[KategoriProdukTable.entitasId] = EntityID(entitasUUID, EntitasUsahaTable)
                    }.value
                }
                // Kembalikan DTO yang sesuai dengan yang diharapkan frontend
                call.respond(HttpStatusCode.Created, KategoriProdukDto(newKategoriId.toString(), req.nama, entitasId = entitasUUID.toString())) // ✅ PERBAIKAN DI SINI
            }

            // ... (PUT dan DELETE jika ada)
        }
    }
}