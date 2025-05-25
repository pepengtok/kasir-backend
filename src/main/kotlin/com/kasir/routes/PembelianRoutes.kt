package com.kasir.routes

import com.kasir.routes.helpers.getEntitasIdFromJwt
import com.kasir.dto.PembelianDetailResponseDto
import com.kasir.dto.PembelianRequestDto
import com.kasir.dto.PembelianResponseDto
import com.kasir.models.*
import com.kasir.service.KasService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

fun Route.pembelianRoutes() {
    authenticate("jwt-auth") {
        route("/pembelian") {

            // 1) GET all pembelian (header saja, atau dengan detail jika ada filter produk_id)
            get {
                val entitasIdFromJwt = try {
                    getEntitasIdFromJwt()
                } catch (e: IllegalArgumentException) {
                    return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
                }
                val produkIdFilter = call.request.queryParameters["produk_id"]

                val list = transaction {
                    // Tahap 1: Mulai dengan PembelianTable, lakukan join pertama.
                    val initialJoin: Join = PembelianTable
                        .join(SupplierTable, JoinType.INNER, additionalConstraint = { PembelianTable.supplierId eq SupplierTable.id })

                    var currentJoinedChain = initialJoin

                    if (produkIdFilter != null) {
                        val produkUUID = runCatching { UUID.fromString(produkIdFilter) }.getOrNull()
                            ?: throw IllegalArgumentException("Invalid produk_id format")

                        currentJoinedChain = currentJoinedChain
                            .join(PembelianDetailTable, JoinType.INNER, additionalConstraint = { PembelianDetailTable.pembelianId eq PembelianTable.id })
                            .join(ProdukTable, JoinType.INNER, additionalConstraint = { PembelianDetailTable.produkId eq ProdukTable.id })
                    }

                    var query = currentJoinedChain.selectAll().andWhere { PembelianTable.entitasId eq entitasIdFromJwt.value }

                    if (produkIdFilter != null) {
                        val produkUUID = runCatching { UUID.fromString(produkIdFilter) }.getOrNull()
                            ?: throw IllegalArgumentException("Invalid produk_id format")
                        query = query.andWhere { PembelianDetailTable.produkId eq EntityID(produkUUID, ProdukTable) }
                    }

                    query.orderBy(PembelianTable.tanggal to SortOrder.DESC, PembelianTable.noFaktur to SortOrder.DESC)
                        .map { row ->
                            val detailsList = if (produkIdFilter != null) {
                                PembelianDetailTable
                                    .join(ProdukTable, JoinType.INNER, additionalConstraint = { PembelianDetailTable.produkId eq ProdukTable.id })
                                    .selectAll()
                                    .andWhere {
                                        (PembelianDetailTable.pembelianId eq row[PembelianTable.id]) and
                                                (PembelianDetailTable.produkId eq EntityID(UUID.fromString(produkIdFilter), ProdukTable))
                                    }
                                    .map { dt ->
                                        PembelianDetailResponseDto(
                                            id = dt[PembelianDetailTable.id].value.toString(),
                                            produkId = dt.getOrNull(PembelianDetailTable.produkId)?.value?.toString().orEmpty(),
                                            produkName = dt[ProdukTable.namaProduk],
                                            hargaModal = dt[PembelianDetailTable.hargaModal],
                                            jumlah = dt[PembelianDetailTable.jumlah].toInt(),
                                            subtotal = dt[PembelianDetailTable.subtotal]
                                        )
                                    }
                            } else {
                                emptyList()
                            }

                            PembelianResponseDto(
                                id = row[PembelianTable.id].value.toString(),
                                noFaktur = row[PembelianTable.noFaktur],
                                tanggal = row[PembelianTable.tanggal].toString(),
                                supplierId = row[PembelianTable.supplierId].value.toString(),
                                supplierName = row[SupplierTable.namaSupplier],
                                total = row[PembelianTable.total],
                                metodePembayaran = row[PembelianTable.metodePembayaran],
                                status = row[PembelianTable.status],
                                kasId = row[PembelianTable.kasId]?.value?.toString(),
                                notaUrl = row[PembelianTable.notaUrl],
                                detail = detailsList,
                                entitasId = row[PembelianTable.entitasId].value.toString()
                            )
                        }
                }
                call.respond(HttpStatusCode.OK, list)
            }


            // 3) GET satu pembelian + detail
            get("/{id}") {
                val entitasIdFromJwt = try {
                    getEntitasIdFromJwt()
                } catch (e: IllegalArgumentException) {
                    return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
                }

                val idParam = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing ID"))
                val pembelianUUID = runCatching { UUID.fromString(idParam) }
                    .getOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID format"))

                val headerRow = transaction {
                    val joinedHeaderTable = PembelianTable.join(SupplierTable, JoinType.INNER, additionalConstraint = { PembelianTable.supplierId eq SupplierTable.id })
                    joinedHeaderTable
                        .selectAll()
                        .andWhere {
                            (PembelianTable.id eq EntityID(pembelianUUID, PembelianTable)) and
                                    (PembelianTable.entitasId eq entitasIdFromJwt.value)
                        }
                        .singleOrNull()
                } ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Pembelian not found"))

                val details = transaction {
                    val joinedDetailTable = PembelianDetailTable.join(ProdukTable, JoinType.INNER, additionalConstraint = { PembelianDetailTable.produkId eq ProdukTable.id })
                    joinedDetailTable
                        .selectAll()
                        .andWhere { PembelianDetailTable.pembelianId eq EntityID(pembelianUUID, PembelianTable) }
                        .map { dt ->
                            PembelianDetailResponseDto(
                                id = dt[PembelianDetailTable.id].value.toString(),
                                produkId = dt[PembelianDetailTable.produkId].value.toString(),
                                produkName = dt[ProdukTable.namaProduk],
                                hargaModal = dt[PembelianDetailTable.hargaModal],
                                jumlah = dt[PembelianDetailTable.jumlah].toInt(),
                                subtotal = dt[PembelianDetailTable.subtotal]
                            )
                        }
                }

                call.respond(
                    HttpStatusCode.OK,
                    PembelianResponseDto(
                        id = headerRow[PembelianTable.id].value.toString(),
                        noFaktur = headerRow[PembelianTable.noFaktur],
                        tanggal = headerRow[PembelianTable.tanggal].toString(),
                        supplierId = headerRow[PembelianTable.supplierId].value.toString(),
                        supplierName = headerRow[SupplierTable.namaSupplier],
                        total = headerRow[PembelianTable.total],
                        metodePembayaran = headerRow[PembelianTable.metodePembayaran],
                        status = headerRow[PembelianTable.status],
                        kasId = headerRow[PembelianTable.kasId]?.value?.toString(),
                        notaUrl = headerRow[PembelianTable.notaUrl],
                        detail = details,
                        entitasId = headerRow[PembelianTable.entitasId].value.toString()
                    )
                )
            }

            // 4) POST create pembelian + detail + stok + kas/hutang
            post {
                val entitasIdFromJwt = try {
                    getEntitasIdFromJwt()
                } catch (e: IllegalArgumentException) {
                    return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
                }
                val req = call.receive<PembelianRequestDto>()
                val now = LocalDateTime.now()
                val noFaktur = generateFakturUrut(entitasIdFromJwt.value)
                val totalAmount = req.detail.sumOf { it.hargaModal * it.jumlah }

                val newId = transaction {
                    val id = PembelianTable.insertAndGetId { r ->
                        r[entitasId] = entitasIdFromJwt.value
                        r[PembelianTable.noFaktur] = noFaktur
                        r[tanggal] = now
                        r[supplierId] = EntityID(UUID.fromString(req.supplierId), SupplierTable)
                        r[total] = totalAmount
                        r[metodePembayaran] = req.metodePembayaran
                        r[status] = if (req.metodePembayaran.equals("KREDIT", true)) "BELUM_LUNAS" else "LUNAS"
                        req.kasId?.let { r[kasId] = EntityID(UUID.fromString(it), KasTable) }
                        req.jatuhTempo?.let { r[jatuhTempo] = LocalDate.parse(it).atStartOfDay() }
                        r[notaUrl] = "/nota/pembelian/$id"
                    }.value


                    req.detail.forEach { item ->
                        val produkRow = ProdukTable.select { ProdukTable.id eq EntityID(UUID.fromString(item.produkId), ProdukTable) }.singleOrNull()
                            ?: throw IllegalArgumentException("Produk dengan ID ${item.produkId} tidak ditemukan.")

                        PembelianDetailTable.insert { d ->
                            d[pembelianId] = EntityID(id, PembelianTable)
                            d[produkId] = EntityID(UUID.fromString(item.produkId), ProdukTable)
                            d[hargaModal] = item.hargaModal
                            d[jumlah] = item.jumlah.toDouble() // KOREKSI: item.jumlah (Int) ke Double
                            d[subtotal] = item.hargaModal * item.jumlah
                            d[entitasId] = entitasIdFromJwt.value
                        }
                        ProdukTable.update({ ProdukTable.id eq EntityID(UUID.fromString(item.produkId), ProdukTable) }) {
                            val currentStok = ProdukTable.select { ProdukTable.id eq EntityID(UUID.fromString(item.produkId), ProdukTable) }.single()[stok]
                            it[stok] = currentStok.plus(item.jumlah.toDouble())
                        }
                    }

                    if (req.metodePembayaran.equals("TUNAI", true)) {
                        KasService.record(
                            entitasId = entitasIdFromJwt.value,
                            kasId = UUID.fromString(req.kasId!!),
                            tanggal = now,
                            jumlah = totalAmount,
                            tipe = "KELUAR",
                            keterangan = "Pembelian tunai #$noFaktur" // noFaktur di-scope di sini
                        )
                    }

                    if (req.metodePembayaran.equals("KREDIT", true)) {
                        HutangSupplierTable.insert { ht ->
                            ht[entitasId] = entitasIdFromJwt.value
                            ht[pembelianId] = EntityID(id, PembelianTable)
                            ht[supplierId] = EntityID(UUID.fromString(req.supplierId), SupplierTable)
                            ht[totalHutang] = totalAmount
                            ht[sisaHutang] = totalAmount
                            ht[tanggal] = now
                            req.jatuhTempo?.let { ht[tanggalJatuhTempo] = LocalDate.parse(it).atStartOfDay() }
                            ht[status] = "BELUM_LUNAS"
                            ht[fotoNotaUrl] = "/nota/pembelian/$id"
                        }
                    }
                    id
                }

                call.respond(HttpStatusCode.Created, mapOf("id" to newId.toString()))
            }

            // 5) PUT update pembelian (header saja + detail)
            put("/{id}") {
                val entitasIdFromJwt = try {
                    getEntitasIdFromJwt()
                } catch (e: IllegalArgumentException) {
                    return@put call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
                }
                val id = runCatching { UUID.fromString(call.parameters["id"]!!) }
                    .getOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
                val req = call.receive<PembelianRequestDto>()
                val now = LocalDateTime.now()

                val updatedCount = transaction {
                    // Ambil oldPembelian di sini untuk mendapatkan noFaktur
                    val oldPembelian = PembelianTable.select { PembelianTable.id eq id }.singleOrNull()
                        ?: throw NotFoundException("Pembelian with ID $id not found") // Tambahkan error jika tidak ditemukan

                    val oldNoFaktur = oldPembelian[PembelianTable.noFaktur] // Ambil noFaktur dari oldPembelian

                    val oldDetails = PembelianDetailTable
                        .select { PembelianDetailTable.pembelianId eq EntityID(id, PembelianTable) }
                        .map { row -> row[PembelianDetailTable.produkId].value to row[PembelianDetailTable.jumlah] }

                    oldDetails.forEach { (produkId, jumlah) ->
                        ProdukTable.update({ ProdukTable.id eq EntityID(produkId, ProdukTable) }) {
                            val currentStok = ProdukTable.select { ProdukTable.id eq EntityID(produkId, ProdukTable) }.single()[stok]
                            it[stok] = currentStok.minus(jumlah)
                        }
                    }

                    PembelianDetailTable.deleteWhere { pembelianId eq EntityID(id, PembelianTable) }

                    val headerUpdated = PembelianTable.update({
                        (PembelianTable.id eq EntityID(id, PembelianTable)) and
                                (PembelianTable.entitasId eq entitasIdFromJwt)
                    }) { r ->
                        r[metodePembayaran] = req.metodePembayaran
                        r[status] = if (req.metodePembayaran.equals("KREDIT", true)) "BELUM_LUNAS" else "LUNAS"
                        req.jatuhTempo?.let { r[jatuhTempo] = LocalDate.parse(it).atStartOfDay() }
                        r[total] = req.detail.sumOf { it.hargaModal * it.jumlah }
                    }

                    req.detail.forEach { item ->
                        val produkRow = ProdukTable.select { ProdukTable.id eq EntityID(UUID.fromString(item.produkId), ProdukTable) }.singleOrNull()
                            ?: throw IllegalArgumentException("Produk dengan ID ${item.produkId} tidak ditemukan.")

                        PembelianDetailTable.insert { d ->
                            d[pembelianId] = EntityID(id, PembelianTable)
                            d[produkId] = EntityID(UUID.fromString(item.produkId), ProdukTable)
                            d[hargaModal] = item.hargaModal
                            d[jumlah] = item.jumlah.toDouble()
                            d[subtotal] = item.hargaModal * item.jumlah
                            d[entitasId] = entitasIdFromJwt.value
                        }
                        ProdukTable.update({ ProdukTable.id eq EntityID(UUID.fromString(item.produkId), ProdukTable) }) {
                            val currentStok = ProdukTable.select { ProdukTable.id eq EntityID(UUID.fromString(item.produkId), ProdukTable) }.single()[stok]
                            it[stok] = currentStok.plus(item.jumlah.toDouble())
                        }
                    }

                    val oldMetodePembayaran = oldPembelian[PembelianTable.metodePembayaran]
                    val oldTotal = oldPembelian[PembelianTable.total] ?: 0.0
                    val newTotal = req.detail.sumOf { it.hargaModal * it.jumlah }

                    if (oldMetodePembayaran != req.metodePembayaran) {
                        if (oldMetodePembayaran.equals("KREDIT", true)) {
                            HutangSupplierTable.deleteWhere { pembelianId eq EntityID(id, PembelianTable) }
                        }
                        if (req.metodePembayaran.equals("KREDIT", true)) {
                            HutangSupplierTable.insert { ht ->
                                ht[entitasId] = entitasIdFromJwt.value
                                ht[pembelianId] = EntityID(id, PembelianTable)
                                ht[supplierId] = EntityID(UUID.fromString(req.supplierId), SupplierTable)
                                ht[totalHutang] = newTotal
                                ht[sisaHutang] = newTotal
                                ht[tanggal] = now
                                req.jatuhTempo?.let { ht[tanggalJatuhTempo] = LocalDate.parse(it).atStartOfDay() }
                                ht[status] = "BELUM_LUNAS"
                                ht[fotoNotaUrl] = oldPembelian[PembelianTable.notaUrl]
                            }
                        } else if (req.metodePembayaran.equals("TUNAI", true)) {
                            KasService.record(
                                entitasId = entitasIdFromJwt.value,
                                kasId = UUID.fromString(req.kasId!!),
                                tanggal = now,
                                jumlah = newTotal,
                                tipe = "KELUAR",
                                keterangan = "Koreksi Pembelian #${oldNoFaktur} (berubah menjadi Tunai)" // KOREKSI: Menggunakan oldNoFaktur
                            )
                        }
                    } else if (oldMetodePembayaran.equals("KREDIT", true) && req.metodePembayaran.equals("KREDIT", true)) {
                        if (oldTotal != newTotal) {
                            HutangSupplierTable.update({ HutangSupplierTable.pembelianId eq EntityID(id, PembelianTable) }) {
                                it[totalHutang] = newTotal
                                it[sisaHutang] = newTotal
                                req.jatuhTempo?.let { jt -> it[tanggalJatuhTempo] = LocalDate.parse(jt).atStartOfDay() }
                            }
                        }
                    }
                    headerUpdated
                }
                val httpStatusCode = if (updatedCount == 0) HttpStatusCode.NotFound else HttpStatusCode.OK
                val responseBody = if (updatedCount == 0) mapOf("error" to "Pembelian tidak ditemukan atau tidak memiliki akses") else mapOf("message" to "Pembelian berhasil diperbarui")
                call.respond(httpStatusCode, responseBody)
            }

            // 6) DELETE pembelian + detail + hutang
            delete("/{id}") {
                val entitasIdFromJwt = try {
                    getEntitasIdFromJwt()
                } catch (e: IllegalArgumentException) {
                    return@delete call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
                }
                val id = runCatching { UUID.fromString(call.parameters["id"]!!) }
                    .getOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
                val deletedCount = transaction {
                    val oldDetails = PembelianDetailTable
                        .select { PembelianDetailTable.pembelianId eq EntityID(id, PembelianTable) }
                        .map { it[PembelianDetailTable.produkId].value to it[PembelianDetailTable.jumlah] }

                    oldDetails.forEach { (produkId, jumlah) ->
                        ProdukTable.update({ ProdukTable.id eq EntityID(produkId, ProdukTable) }) {
                            val currentStok = ProdukTable.select { ProdukTable.id eq EntityID(produkId, ProdukTable) }.single()[stok]
                            it[stok] = currentStok.minus(jumlah.toDouble())
                        }
                    }

                    PembelianDetailTable.deleteWhere { pembelianId eq EntityID(id, PembelianTable) }
                    HutangSupplierTable.deleteWhere { pembelianId eq EntityID(id, PembelianTable) }
                    PembelianTable.deleteWhere {
                        (PembelianTable.id eq EntityID(id, PembelianTable)) and
                                (entitasId eq entitasIdFromJwt)
                    }
                }
                val httpStatusCode = if (deletedCount == 0) HttpStatusCode.NotFound else HttpStatusCode.NoContent
                val responseBody = if (deletedCount == 0) mapOf("error" to "Pembelian tidak ditemukan atau tidak memiliki akses") else null
                if (responseBody != null) {
                    call.respond(httpStatusCode, responseBody)
                } else {
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}

// Helper generate noFaktur per-entitas
private fun generateFakturUrut(entitasId: UUID): String = transaction {
    val today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    val prefix = "PB-$today"
    val last = PembelianTable
        .select {
            (PembelianTable.noFaktur like "$prefix%") and
                    (PembelianTable.entitasId eq EntityID(entitasId, EntitasUsahaTable))
        }
        .orderBy(PembelianTable.noFaktur to SortOrder.DESC)
        .limit(1)
        .firstOrNull()
        ?.get(PembelianTable.noFaktur)
    val next = last?.substringAfterLast("-")?.toIntOrNull()?.plus(1) ?: 1
    "$prefix-${"%04d".format(next)}"
}