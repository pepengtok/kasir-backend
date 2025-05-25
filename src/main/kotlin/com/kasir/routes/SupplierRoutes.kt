// File: src/main/kotlin/com/kasir/routes/SupplierRoutes.kt
package com.kasir.routes

import com.kasir.dto.SupplierRequest
import com.kasir.dto.SupplierResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.kasir.models.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import java.time.YearMonth // Tambahkan import ini untuk YearMonth

@Serializable
data class SupplierPembelianResponse(
    val pembelianId: String,
    val tanggal: String,
    val total: Double,
    val metode: String, // Ini seharusnya metode_pembayaran
    val status: String,
    val notaUrl: String?
)


fun Route.supplierRoutes() {
    authenticate("jwt-auth") {
        route("/supplier") {
            // helper ambil entitasId dari JWT
            fun currentEntitas(call: ApplicationCall): UUID {
                val jwt = call.principal<JWTPrincipal>()
                    ?: throw IllegalArgumentException("Unauthorized")
                return UUID.fromString(jwt.payload.getClaim("entitasId").asString())
            }

            // GET /supplier -> daftar semua supplier milik entitas
            get {
                val entitasUUID = currentEntitas(call)
                val list = transaction {
                    SupplierTable
                        .select { SupplierTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable) }
                        .map { row ->
                            SupplierResponse(
                                id = row[SupplierTable.id].value.toString(),
                                namaSupplier = row[SupplierTable.namaSupplier],
                                no_hp = row[SupplierTable.no_hp],
                                alamat = row[SupplierTable.alamat],
                                rekeningBank = row[SupplierTable.rekeningBank],
                                namaSales = row[SupplierTable.namaSales],
                                entitasId = row[SupplierTable.entitasId].value.toString()
                            )
                        }
                }
                call.respond(HttpStatusCode.OK, list)
            }

            // POST /supplier -> tambah supplier baru
            post {
                val entitasUUID = currentEntitas(call)
                val req = call.receive<SupplierRequest>()
                val newId = transaction {
                    SupplierTable.insertAndGetId { r ->
                        r[SupplierTable.namaSupplier] = req.namaSupplier
                        r[SupplierTable.no_hp]        = req.no_hp
                        r[SupplierTable.alamat]       = req.alamat
                        r[SupplierTable.rekeningBank] = req.rekeningBank
                        r[SupplierTable.namaSales]    = req.namaSales
                        r[SupplierTable.entitasId]    = EntityID(entitasUUID, EntitasUsahaTable)
                    }.value
                }
                call.respond(HttpStatusCode.Created, SupplierResponse(
                    id = newId.toString(),
                    namaSupplier = req.namaSupplier,
                    no_hp = req.no_hp,
                    alamat = req.alamat,
                    rekeningBank = req.rekeningBank,
                    namaSales = req.namaSales,
                    entitasId = entitasUUID.toString()
                )
                )
            }


            // PUT /supplier/{id} -> update supplier
            put("/{id}") {
                val entitasUUID = currentEntitas(call)
                val uuid = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing or invalid ID"))
                val req = call.receive<SupplierRequest>()

                val count = transaction {
                    SupplierTable.update({
                        (SupplierTable.id eq uuid) and
                                (SupplierTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable))
                    }) { r ->
                        r[SupplierTable.namaSupplier] = req.namaSupplier
                        r[SupplierTable.no_hp]        = req.no_hp
                        r[SupplierTable.alamat]       = req.alamat
                        r[SupplierTable.rekeningBank] = req.rekeningBank
                        r[SupplierTable.namaSales]    = req.namaSales
                    }
                }
                if (count == 0) call.respond(HttpStatusCode.NotFound)
                else            call.respond(HttpStatusCode.OK)
            }

            // DELETE /supplier/{id} -> hapus supplier
            delete("/{id}") {
                val entitasUUID = currentEntitas(call)
                val uuid = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing or invalid ID"))

                val deleted = transaction {
                    SupplierTable.deleteWhere {
                        (SupplierTable.id eq uuid) and
                                (SupplierTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable))
                    }
                }
                if (deleted == 0) call.respond(HttpStatusCode.NotFound)
                else               call.respond(HttpStatusCode.NoContent)
            }

            // GET /supplier/{id}/pembelian -> riwayat pembelian supplier
            route("/{id}/pembelian") {
                get {
                    val entitasUUID = currentEntitas(call)
                    val sidParam = call.parameters["id"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing Supplier ID"))
                    val supplierId = runCatching { UUID.fromString(sidParam) }.getOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid UUID"))

                    // --- Perbaikan filter tanggal dan status ---
                    val monthFilterParam = call.request.queryParameters["month"]?.toIntOrNull() // Parameter month dari frontend
                    val yearFilterParam = call.request.queryParameters["year"]?.toIntOrNull() // Parameter year dari frontend
                    val statusFilterParam = call.request.queryParameters["status"]?.uppercase() // Parameter status dari frontend
                    // Asumsi: 'tipe' query parameter tidak digunakan untuk tanggal filtering di backend,
                    //         tapi untuk metode_pembayaran seperti di Pembelian.jsx.
                    //         Jika digunakan untuk tanggal, maka sesuaikan nama variabel di backend.

                    val purchases = transaction {
                        var query = PembelianTable
                            .select {
                                (PembelianTable.supplierId eq EntityID(supplierId, SupplierTable)) and
                                        (PembelianTable.entitasId   eq EntityID(entitasUUID, EntitasUsahaTable))
                            }
                            .orderBy(PembelianTable.tanggal to SortOrder.DESC) // Urutkan berdasarkan tanggal

                        // Filter berdasarkan bulan dan tahun
                        if (monthFilterParam != null && yearFilterParam != null) {
                            val yearMonth = YearMonth.of(yearFilterParam, monthFilterParam)
                            val startOfMonth = yearMonth.atDay(1).atStartOfDay()
                            val endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59)
                            query = query.andWhere { PembelianTable.tanggal greaterEq startOfMonth }
                            query = query.andWhere { PembelianTable.tanggal lessEq endOfMonth }
                        } else if (yearFilterParam != null) { // Hanya filter tahun
                            val startOfYear = LocalDate.of(yearFilterParam, 1, 1).atStartOfDay()
                            val endOfYear = LocalDate.of(yearFilterParam, 12, 31).atTime(23, 59, 59)
                            query = query.andWhere { PembelianTable.tanggal greaterEq startOfYear }
                            query = query.andWhere { PembelianTable.tanggal lessEq endOfYear }
                        }

                        // Filter berdasarkan status (LUNAS/BELUM_LUNAS)
                        statusFilterParam?.let { status ->
                            query = query.andWhere { PembelianTable.status eq status }
                        }

                        query.map { row ->
                            SupplierPembelianResponse(
                                pembelianId = row[PembelianTable.id].value.toString(),
                                tanggal     = row[PembelianTable.tanggal].toString(),
                                total       = row[PembelianTable.total],
                                metode      = row[PembelianTable.metodePembayaran], // Ini adalah metode_pembayaran
                                status      = row[PembelianTable.status],
                                notaUrl     = row[PembelianTable.notaUrl]
                            )
                        }
                    }

                    call.respond(HttpStatusCode.OK, purchases)
                }
            }
        }
    }
}

// Helper generate noFaktur per-entitas (tetap sama)
private fun generateFakturUrut(entitasId: UUID): String = transaction {
    val today  = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    val prefix = "PB-$today"
    val last   = PembelianTable
        .select  {
            (PembelianTable.noFaktur like "$prefix%") and
                    (PembelianTable.entitasId eq EntityID(entitasId, EntitasUsahaTable))
        }
        .orderBy(PembelianTable.noFaktur to SortOrder.DESC)
        .limit(1)
        .firstOrNull()
        ?.get(PembelianTable.noFaktur)
    val next   = last?.substringAfterLast("-")?.toIntOrNull()?.plus(1) ?: 1
    "$prefix-${"%04d".format(next)}"
}

// Tidak perlu getUUIDParamOrRespond jika tidak dipakai
// private suspend fun ApplicationCall.getUUIDParamOrRespond(param: String, respondWith: suspend () -> Unit): UUID? {
//     val idParam = parameters[param] ?: return null.also { respondWith() }
//     return runCatching { UUID.fromString(idParam) }.getOrNull() ?: run {
//         respondWith()
//         null
//     }
// }