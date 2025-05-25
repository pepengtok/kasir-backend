package com.kasir.routes

import com.kasir.dto.ReturPenjualanEntryDto
import com.kasir.dto.ReturPembelianEntryDto
import com.kasir.models.*
import com.kasir.service.KasService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import java.util.UUID

fun Route.returRoutes() {
    authenticate("jwt-auth") {
        route("/retur") {

            // ==== Retur Penjualan ====
            post("/penjualan") {
                // ambil entitasId dari token
                val jwt = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val entitasUUID = runCatching {
                    UUID.fromString(jwt.payload.getClaim("entitasId").asString())
                }.getOrElse {
                    return@post call.respond(HttpStatusCode.BadRequest, "Invalid entitasId in token")
                }

                val dto = call.receive<ReturPenjualanEntryDto>()
                transaction {
                    // 1) Insert Retur Penjualan
                    val returId = ReturPenjualanTable.insertAndGetId { row ->
                        row[ReturPenjualanTable.entitasId]   = EntityID(entitasUUID, EntitasUsahaTable)
                        row[ReturPenjualanTable.tanggalRetur] = LocalDateTime.parse(dto.tanggalRetur)
                        row[ReturPenjualanTable.jumlahRetur]  = dto.jumlahRetur
                        row[ReturPenjualanTable.keterangan]   = dto.keterangan
                        row[ReturPenjualanTable.pelangganId]  = EntityID(UUID.fromString(dto.pelanggan), PelangganTable)
                        row[ReturPenjualanTable.penjualanId]  = EntityID(UUID.fromString(dto.penjualanId), PenjualanTable)
                    }.value

                    // 2) Ambil penjualanId asli
                    val penjualanIdVal = ReturPenjualanTable
                        .select { ReturPenjualanTable.id eq returId }
                        .single()[ReturPenjualanTable.penjualanId].value

                    // 3) Update total & status penjualan
                    PenjualanTable
                        .select { PenjualanTable.id eq penjualanIdVal }
                        .singleOrNull()
                        ?.let { row ->
                            val newTotal = row[PenjualanTable.total] - dto.jumlahRetur
                            PenjualanTable.update({ PenjualanTable.id eq penjualanIdVal }) {
                                it[total] = newTotal
                                it[status] = if (newTotal <= 0.0) "VOID" else row[PenjualanTable.status]
                            }
                        }

                    // 4) Update sisa & status piutang pelanggan
                    PiutangPelangganTable
                        .select { PiutangPelangganTable.penjualanId eq penjualanIdVal }
                        .singleOrNull()
                        ?.let { row ->
                            val piutangId = row[PiutangPelangganTable.id].value
                            val newSisa = row[PiutangPelangganTable.sisaPiutang] - dto.jumlahRetur
                            PiutangPelangganTable.update({ PiutangPelangganTable.id eq piutangId }) {
                                it[sisaPiutang] = newSisa
                                it[status]      = if (newSisa <= 0.0) "LUNAS" else row[PiutangPelangganTable.status]
                            }
                        }

                    // 5) Catat reverse kas masuk
                    KasService.record(
                        entitasId  = entitasUUID,
                        kasId      = UUID.fromString(dto.kasId),
                        tanggal    = LocalDateTime.now(),
                        jumlah     = dto.jumlahRetur,
                        tipe       = "KELUAR",
                        keterangan = "Retur penjualan #${dto.penjualanId}"
                    )

                    // 6) Update komisi sales
                    KomisiSalesTable
                        .select { KomisiSalesTable.penjualanId eq penjualanIdVal }
                        .singleOrNull()
                        ?.let { row ->
                            val komisiId  = row[KomisiSalesTable.id].value
                            val persen    = row[KomisiSalesTable.komisiPersen]
                            val newKomisi = row[KomisiSalesTable.nominalKomisi] - (dto.jumlahRetur * persen / 100.0)
                            KomisiSalesTable.update({ KomisiSalesTable.id eq komisiId }) {
                                it[nominalKomisi] = newKomisi
                                it[status]        = if (newKomisi <= 0.0) "VOID" else row[KomisiSalesTable.status]
                            }
                        }

                    // 7) Kembalikan stok produk
                    PenjualanDetailTable
                        .select { PenjualanDetailTable.penjualanId eq penjualanIdVal }
                        .forEach { detailRow ->
                            detailRow[PenjualanDetailTable.produkId]?.let { prodUuid ->
                                val qty = detailRow[PenjualanDetailTable.jumlah]
                                val current = ProdukTable
                                    .select { ProdukTable.id eq prodUuid }
                                    .single()[ProdukTable.stok]
                                ProdukTable.update({ ProdukTable.id eq prodUuid }) {
                                    it[stok] = current + qty.toDouble()
                                }
                            }
                        }
                }

                call.respond(HttpStatusCode.Created, dto)
            }

            // ==== Retur Pembelian ====
            post("/pembelian") {
                // ambil entitasId dari token
                val jwt = call.principal<JWTPrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val entitasUUID = runCatching {
                    UUID.fromString(jwt.payload.getClaim("entitasId").asString())
                }.getOrElse {
                    return@post call.respond(HttpStatusCode.BadRequest, "Invalid entitasId in token")
                }

                val dto = call.receive<ReturPembelianEntryDto>()
                transaction {
                    // 1) Insert Retur Pembelian
                    val returId = ReturPembelianTable.insertAndGetId { row ->
                        row[ReturPembelianTable.entitasId]   = EntityID(entitasUUID, EntitasUsahaTable)
                        row[ReturPembelianTable.tanggalRetur] = LocalDateTime.parse(dto.tanggalRetur)
                        row[ReturPembelianTable.jumlahRetur]  = dto.jumlahRetur
                        row[ReturPembelianTable.keterangan]   = dto.keterangan
                        row[ReturPembelianTable.supplierId]   = EntityID(UUID.fromString(dto.supplier), SupplierTable)
                        row[ReturPembelianTable.pembelianId]  = EntityID(UUID.fromString(dto.pembelianId), PembelianTable)
                    }.value

                    // 2) Ambil pembelianId asli
                    val pembelianIdVal = ReturPembelianTable
                        .select { ReturPembelianTable.id eq returId }
                        .single()[ReturPembelianTable.pembelianId].value

                    // 3) Update total & status pembelian
                    PembelianTable
                        .select { PembelianTable.id eq pembelianIdVal }
                        .singleOrNull()
                        ?.let { row ->
                            val newTotal = row[PembelianTable.total] - dto.jumlahRetur
                            PembelianTable.update({ PembelianTable.id eq pembelianIdVal }) {
                                it[total]  = newTotal
                                it[status] = if (newTotal <= 0.0) "LUNAS" else row[PembelianTable.status]
                            }
                        }

                    // 4) Update hutang supplier
                    HutangSupplierTable
                        .select { HutangSupplierTable.pembelianId eq pembelianIdVal }
                        .singleOrNull()
                        ?.let { row ->
                            val hutangId = row[HutangSupplierTable.id].value
                            val newSisa  = row[HutangSupplierTable.sisaHutang] - dto.jumlahRetur
                            HutangSupplierTable.update({ HutangSupplierTable.id eq hutangId }) {
                                it[sisaHutang] = newSisa
                                it[status]     = if (newSisa <= 0.0) "LUNAS" else row[HutangSupplierTable.status]
                            }
                        }

                    // 5) Reverse kas keluar
                    KasService.record(
                        entitasId  = entitasUUID,
                        kasId      = UUID.fromString(dto.kasId),
                        tanggal    = LocalDateTime.now(),
                        jumlah     = dto.jumlahRetur,
                        tipe       = "MASUK",
                        keterangan = "Retur pembelian #${dto.pembelianId}"
                    )

                    // 6) Kembalikan stok barang
                    PembelianDetailTable
                        .select { PembelianDetailTable.pembelianId eq pembelianIdVal }
                        .forEach { detailRow ->
                            // langsung akses .value, tanpa safe-call
                            val prodUuid = detailRow[PembelianDetailTable.produkId].value
                            val qty      = detailRow[PembelianDetailTable.jumlah]
                            val current  = ProdukTable
                                .select { ProdukTable.id eq prodUuid }
                                .single()[ProdukTable.stok]
                            ProdukTable.update({ ProdukTable.id eq prodUuid }) {
                                it[stok] = current - qty.toDouble()
                            }
                        }

                }

                call.respond(HttpStatusCode.Created, dto)
            }
        }
    }
}
