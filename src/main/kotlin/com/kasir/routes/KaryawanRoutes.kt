// === KaryawanRoutes.kt ===
package com.kasir.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.kasir.models.* // Pastikan ini mengimpor KaryawanTable, EntitasUsahaTable
import com.kasir.dto.* // Pastikan ini mengimpor KaryawanRequest, KaryawanResponse
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.dao.id.EntityID // ✅ PENTING: Tambahkan ini
import java.time.LocalDateTime
import java.util.*

fun Route.karyawanRoutes() {
    authenticate("jwt-auth") { // Asumsi rute karyawan juga perlu otentikasi
        route("/karyawan") {

            get {
                val principal = call.principal<JWTPrincipal>()!!
                val entitasUUID = UUID.fromString(principal.payload.getClaim("entitasId").asString())

                val karyawanList = transaction {
                    KaryawanTable
                        .select { KaryawanTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable) }
                        .map { row ->
                            KaryawanResponse(
                                id = row[KaryawanTable.id].value.toString(),
                                nama = row[KaryawanTable.nama],
                                bulanTerdaftar = row[KaryawanTable.bulanTerdaftar],
                                createdAt = row[KaryawanTable.createdAt].toString(),
                                entitasId = row[KaryawanTable.entitasId].value.toString()
                            )
                        }
                }
                call.respond(karyawanList)
            }

            post {
                val principal = call.principal<JWTPrincipal>()!!
                val entitasUUID = UUID.fromString(principal.payload.getClaim("entitasId").asString())
                val request = call.receive<KaryawanRequest>()

                val newId = transaction {
                    KaryawanTable.insertAndGetId {
                        it[nama] = request.nama
                        it[bulanTerdaftar] = request.bulanTerdaftar
                        it[createdAt] = LocalDateTime.now()
                        it[entitasId] = EntityID(entitasUUID, EntitasUsahaTable)
                    }.value // Ambil nilai UUID dari EntityID
                }
                call.respond(HttpStatusCode.Created, mapOf("id" to newId.toString()))
            }

            put("/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val entitasUUID = UUID.fromString(principal.payload.getClaim("entitasId").asString())
                val id = call.parameters["id"]?.let(UUID::fromString)
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing or invalid ID")

                // ✅ Anda perlu menerima request DTO DI SINI, sebelum blok transaction
                val request = call.receive<KaryawanRequest>()

                val updatedCount = transaction {
                    KaryawanTable.update({ (KaryawanTable.id eq id) and (KaryawanTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable)) }) {
                        it[nama] = request.nama // 'request' sekarang sudah bisa diakses
                        it[bulanTerdaftar] = request.bulanTerdaftar // 'request' sekarang sudah bisa diakses
                    }
                }
                if (updatedCount > 0) {
                    call.respond(HttpStatusCode.OK, mapOf("success" to true))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Karyawan tidak ditemukan atau tidak memiliki akses"))
                }
            }

            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val entitasUUID = UUID.fromString(principal.payload.getClaim("entitasId").asString())
                val uuid = call.parameters["id"]?.let(UUID::fromString)
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing or invalid id"))

                val deletedRowCount = transaction { // ✅ Tangkap hasil delete
                    KaryawanTable.deleteWhere { (KaryawanTable.id eq uuid) and (KaryawanTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable)) }
                }
                if (deletedRowCount > 0) {
                    call.respond(HttpStatusCode.NoContent) // ✅ Pindahkan keluar
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Karyawan tidak ditemukan atau tidak memiliki akses")) // ✅ Pindahkan keluar
                }
            }
        }
    }
}