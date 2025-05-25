// src/main/kotlin/com/kasir/routes/KomisiRoutes.kt
package com.kasir.routes

import com.kasir.dto.BayarKomisiRequestDto
import com.kasir.dto.BatchBayarKomisiRequest
import com.kasir.dto.BatchBayarKomisiResponse
import com.kasir.dto.KomisiEntryDto
import com.kasir.dto.KomisiResponseDto
import com.kasir.models.*
import com.kasir.service.KasService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

// --- IMPOR EKSPILISIT UNTUK Exposed ---
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.between
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.JoinType // Penting: Pastikan ini terimpor
// import org.jetbrains.exposed.sql.ColumnSet // Ini mungkin tidak lagi diperlukan, bisa dihapus jika unused warning
// import org.jetbrains.exposed.sql.select // Ini mungkin tidak lagi diperlukan, bisa dihapus jika unused warning

fun Route.komisiRoutes() {
    authenticate("jwt-auth") {
        route("/komisi") {
            get {
                val filter = call.request.queryParameters["filter"]?.lowercase() ?: "all"
                val entitasIdParam = call.request.queryParameters["entitas_id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "entitas_id wajib diisi")
                val entitasId = EntityID(UUID.fromString(entitasIdParam), EntitasUsahaTable)

                val list = transaction {
                    val ks = KomisiSalesTable
                    val pj = PenjualanTable
                    val sales = SalesTable

                    // PERBAIKAN: Sintaks Join berantai dengan lambda kondisi langsung (tanpa 'on=')
                    val joinedQuery = ks.join(pj, JoinType.INNER) { ks.penjualanId eq pj.id }
                        .join(sales, JoinType.INNER) { ks.salesId eq sales.id }

                    joinedQuery.slice(
                        ks.columns + listOf(pj.id, pj.notaUrl, sales.nama)
                    )
                        .select {
                            (ks.entitasId eq entitasId) and
                                    when (filter) {
                                        "ready" -> (ks.status eq "PENDING") and (pj.status eq "LUNAS")
                                        "paid"  -> ks.status eq "DIBAYAR"
                                        else    -> Op.TRUE
                                    }
                        }
                        .orderBy(ks.tanggalKomisi to SortOrder.DESC)
                        .map { row ->
                            KomisiEntryDto(
                                id            = row[ks.id].value.toString(),
                                tanggalKomisi = row[ks.tanggalKomisi].toString(),
                                salesId       = row[ks.salesId].value.toString(),
                                namaSales     = row[sales.nama],
                                penjualanId   = row[pj.id].value.toString(),
                                komisiPersen  = row[ks.komisiPersen],
                                nominalKomisi = row[ks.nominalKomisi],
                                status        = row[ks.status],
                                canPay        = row[ks.status] == "PENDING" && row[pj.status] == "LUNAS",
                                notaId        = row[pj.id].value.toString(),
                                notaUrl       = row[pj.notaUrl],
                                entitasId     = row[ks.entitasId].value.toString()
                            )
                        }
                }

                call.respond(HttpStatusCode.OK, KomisiResponseDto(list))
            }

            // 2) Bayar satu komisi
            // POST /komisi/{id}/bayar (Tidak ada join yang bermasalah di sini)
            post("/{id}/bayar") {
                val komisiId = UUID.fromString(call.parameters["id"]!!)
                val req = call.receive<BayarKomisiRequestDto>()

                val result = transaction {
                    val row = KomisiSalesTable
                        .select { KomisiSalesTable.id eq EntityID(komisiId, KomisiSalesTable) }
                        .singleOrNull() ?: return@transaction -1

                    if (row[KomisiSalesTable.status] != "PENDING") return@transaction -2

                    val saleStatus = PenjualanTable
                        .select { PenjualanTable.id eq row[KomisiSalesTable.penjualanId].value }
                        .single()[PenjualanTable.status]
                    if (saleStatus != "LUNAS") return@transaction -3

                    KasService.record(
                        entitasId  = row[KomisiSalesTable.entitasId].value,
                        kasId      = UUID.fromString(req.kasId),
                        tanggal    = LocalDateTime.parse(req.tanggalBayar),
                        jumlah     = row[KomisiSalesTable.nominalKomisi],
                        tipe       = "KELUAR",
                        keterangan = "Bayar komisi sales ${row[KomisiSalesTable.salesId].value}"
                    )

                    // e) update status komisi
                    KomisiSalesTable.update({ KomisiSalesTable.id eq EntityID(komisiId, KomisiSalesTable) }) {
                        it[status] = "DIBAYAR"
                    }

                    1
                }


                when (result) {
                    -1  -> call.respond(HttpStatusCode.NotFound, mapOf("error" to "Komisi tidak ditemukan"))
                    -2  -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Komisi sudah dibayar"))
                    -3  -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Penjualan belum lunas"))
                    1   -> call.respond(HttpStatusCode.OK, mapOf("message" to "Komisi berhasil dibayar"))
                    else-> call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Unknown error"))
                }
            }
// 3) Bayar batch komisi
            post("/bayar-batch") {
                val req = call.receive<BatchBayarKomisiRequest>()
                val ids = req.komisiIds.map(UUID::fromString)
                var paidCount = 0
                var totalAmt = 0.0

                transaction {
                    val ks = KomisiSalesTable
                    val pj = PenjualanTable
                    val entitasIdInBatch = EntityID(UUID.fromString(req.entitasId), EntitasUsahaTable)

                    // 1) filter komisi siap bayar dan sesuai entitas
                    // PERBAIKAN: Gunakan join() eksplisit dengan lambda kondisi langsung
                    val readyList = ks.join(pj, JoinType.INNER) { ks.penjualanId eq pj.id }
                        .slice(ks.id, ks.nominalKomisi)
                        .select {
                            (ks.id inList ids) and
                                    (ks.status eq "PENDING") and
                                    (pj.status eq "LUNAS") and
                                    (ks.entitasId eq entitasIdInBatch)
                        }
                        .toList()

                    if (readyList.isEmpty()) return@transaction

                    paidCount = readyList.size
                    totalAmt  = readyList.sumOf { it[ks.nominalKomisi] }

                    // 2) Catat kas keluar
                    KasService.record(
                        entitasId  = UUID.fromString(req.entitasId),
                        kasId      = UUID.fromString(req.kasId),
                        tanggal    = LocalDateTime.now(),
                        jumlah     = totalAmt,
                        tipe       = "KELUAR",
                        keterangan = "Pembayaran batch komisi: ${readyList.joinToString(",") { it[ks.id].value.toString() }}"
                    )

                    // 3) Update status komisi ke DIBAYAR
                    val readyIds = readyList.map { it[ks.id].value }
                    ks.update({ ks.id inList readyIds }) {
                        it[status] = "DIBAYAR"
                    }
                }

                call.respond(HttpStatusCode.OK, BatchBayarKomisiResponse(paidCount, totalAmt))
            }
        }
    }
}