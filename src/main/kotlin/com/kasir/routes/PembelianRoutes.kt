package com.kasir.routes

import com.kasir.dto.PembelianDetailResponseDto
import com.kasir.dto.PembelianRequestDto
import com.kasir.dto.PembelianResponseDto
import com.kasir.models.*
import com.kasir.service.KasService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.util.pipeline.PipelineContext
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

// Helper function untuk mendapatkan entitasId dari JWT
fun PipelineContext<Unit, ApplicationCall>.getEntitasIdFromJwt(): EntityID<UUID> {
    val principal = call.principal<JWTPrincipal>()
        ?: throw IllegalArgumentException("JWT Principal not found") // Autentikasi seharusnya sudah menangani ini
    val entitasUUID = UUID.fromString(principal.payload.getClaim("entitasId").asString())
    return EntityID(entitasUUID, EntitasUsahaTable)
}

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
                val produkIdFilter = call.request.queryParameters["produk_id"] // Ambil filter produk_id

                val list = transaction {
                    var query = (PembelianTable innerJoin SupplierTable)
                        .select { PembelianTable.entitasId eq entitasIdFromJwt }

                    // Jika ada filter produk_id, tambahkan kondisi join dan filter ke query
                    if (produkIdFilter != null) {
                        val produkUUID = runCatching { UUID.fromString(produkIdFilter) }.getOrNull()
                            ?: throw IllegalArgumentException("Invalid produk_id format")

                        // Penting: Lakukan join ke tabel detail dan produk di sini
                        query = query.join(PembelianDetailTable, JoinType.INNER) { PembelianDetailTable.pembelianId eq PembelianTable.id }
                            .andWhere { PembelianDetailTable.produkId eq EntityID(produkUUID, ProdukTable) }
                            .join(ProdukTable, JoinType.INNER) { PembelianDetailTable.produkId eq ProdukTable.id }
                    }

                    query.orderBy(PembelianTable.tanggal to SortOrder.DESC, PembelianTable.noFaktur to SortOrder.DESC) // Tambah order by agar konsisten
                        .map { row ->
                            val detailsList = if (produkIdFilter != null) {
                                // Jika difilter per produk, ambil detail yang relevan dari pembelian ini
                                PembelianDetailTable
                                    .join(ProdukTable, JoinType.INNER) { PembelianDetailTable.produkId eq ProdukTable.id }
                                    .select {
                                        (PembelianDetailTable.pembelianId eq row[PembelianTable.id]) and
                                                (PembelianDetailTable.produkId eq EntityID(UUID.fromString(produkIdFilter), ProdukTable))
                                    }
                                    .map { dt ->
                                        PembelianDetailResponseDto(
                                            id         = dt[PembelianDetailTable.id].value.toString(),
                                            produkId   = dt[PembelianDetailTable.produkId].value.toString(),
                                            produkName = dt[ProdukTable.namaProduk],
                                            hargaModal = dt[PembelianDetailTable.hargaModal],
                                            jumlah     = dt[PembelianDetailTable.jumlah],
                                            subtotal   = dt[PembelianDetailTable.subtotal]
                                        )
                                    }
                            } else {
                                emptyList() // Jika tidak ada filter produk_id, detail tetap kosong untuk daftar umum
                            }

                            PembelianResponseDto(
                                id               = row[PembelianTable.id].value.toString(),
                                noFaktur         = row[PembelianTable.noFaktur],
                                tanggal          = row[PembelianTable.tanggal].toString(),
                                supplierId       = row[PembelianTable.supplierId].value.toString(),
                                supplierName     = row[SupplierTable.namaSupplier],
                                total            = row[PembelianTable.total],
                                metodePembayaran = row[PembelianTable.metodePembayaran],
                                status           = row[PembelianTable.status],
                                kasId            = row[PembelianTable.kasId]?.value?.toString(),
                                notaUrl          = row[PembelianTable.notaUrl],
                                detail           = detailsList,
                                entitasId        = row[PembelianTable.entitasId].value.toString()
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
                    .getOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid UUID"))

                val headerRow = transaction {
                    (PembelianTable innerJoin SupplierTable)
                        .select {
                            (PembelianTable.id eq pembelianUUID) and
                                    (PembelianTable.entitasId eq entitasIdFromJwt)
                        }
                        .singleOrNull()
                } ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Pembelian not found"))

                val details = transaction {
                    (PembelianDetailTable innerJoin ProdukTable)
                        .select { PembelianDetailTable.pembelianId eq pembelianUUID }
                        .map { dt ->
                            PembelianDetailResponseDto(
                                id         = dt[PembelianDetailTable.id].value.toString(),
                                produkId   = dt[PembelianDetailTable.produkId].value.toString(),
                                produkName = dt[ProdukTable.namaProduk],
                                hargaModal = dt[PembelianDetailTable.hargaModal],
                                jumlah     = dt[PembelianDetailTable.jumlah],
                                subtotal   = dt[PembelianDetailTable.subtotal]
                            )
                        }
                }

                call.respond(
                    HttpStatusCode.OK,
                    PembelianResponseDto(
                        id               = headerRow[PembelianTable.id].value.toString(),
                        noFaktur         = headerRow[PembelianTable.noFaktur],
                        tanggal          = headerRow[PembelianTable.tanggal].toString(),
                        supplierId       = headerRow[PembelianTable.supplierId].value.toString(),
                        supplierName     = headerRow[SupplierTable.namaSupplier],
                        total            = headerRow[PembelianTable.total],
                        metodePembayaran = headerRow[PembelianTable.metodePembayaran],
                        status           = headerRow[PembelianTable.status],
                        kasId            = headerRow[PembelianTable.kasId]?.value?.toString(),
                        notaUrl          = headerRow[PembelianTable.notaUrl],
                        detail           = details,
                        entitasId        = headerRow[PembelianTable.entitasId].value.toString()
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
                        r[PembelianTable.entitasId]        = entitasIdFromJwt
                        r[PembelianTable.noFaktur]         = noFaktur
                        r[PembelianTable.tanggal]          = now
                        r[PembelianTable.supplierId]       = EntityID(UUID.fromString(req.supplierId), SupplierTable)
                        r[PembelianTable.total]            = totalAmount
                        r[PembelianTable.metodePembayaran] = req.metodePembayaran
                        r[PembelianTable.status]           = if (req.metodePembayaran.equals("KREDIT", true)) "BELUM_LUNAS" else "LUNAS"
                        req.kasId?.let { r[PembelianTable.kasId] = EntityID(UUID.fromString(it), KasTable) }
                        req.jatuhTempo?.let { r[PembelianTable.jatuhTempo] = LocalDate.parse(it).atStartOfDay() }
                        r[PembelianTable.notaUrl]          = "/nota/pembelian/$id" // Menggunakan ID yang baru dibuat
                    }.value


                    // detail + update stok
                    req.detail.forEach { item ->
                        val produkRow = ProdukTable.select { ProdukTable.id eq EntityID(UUID.fromString(item.produkId), ProdukTable) }.singleOrNull()
                            ?: throw IllegalArgumentException("Produk dengan ID ${item.produkId} tidak ditemukan.")

                        PembelianDetailTable.insert { d ->
                            d[PembelianDetailTable.pembelianId] = EntityID(id, PembelianTable)
                            d[PembelianDetailTable.produkId]    = EntityID(UUID.fromString(item.produkId), ProdukTable)
                            d[PembelianDetailTable.hargaModal]  = item.hargaModal
                            d[PembelianDetailTable.jumlah]      = item.jumlah
                            d[PembelianDetailTable.subtotal]    = item.hargaModal * item.jumlah
                            d[PembelianDetailTable.entitasId]   = entitasIdFromJwt
                        }
                        // Tambahkan stok produk
                        ProdukTable.update({ ProdukTable.id eq EntityID(UUID.fromString(item.produkId), ProdukTable) }) {
                            it[ProdukTable.stok] = ProdukTable.stok.plus(item.jumlah.toDouble()) // Asumsi stok adalah Double
                        }
                    }

                    if (req.metodePembayaran.equals("TUNAI", true)) {
                        KasService.record(
                            entitasId  = entitasIdFromJwt.value,
                            kasId      = UUID.fromString(req.kasId!!),
                            tanggal    = now,
                            jumlah     = totalAmount,
                            tipe       = "KELUAR",
                            keterangan = "Pembelian tunai #$noFaktur"
                        )
                    }

                    // hutang untuk KREDIT
                    if (req.metodePembayaran.equals("KREDIT", true)) {
                        HutangSupplierTable.insert { ht ->
                            ht[HutangSupplierTable.entitasId]         = entitasIdFromJwt
                            ht[HutangSupplierTable.pembelianId]       = EntityID(id, PembelianTable)
                            ht[HutangSupplierTable.supplierId]        = EntityID(UUID.fromString(req.supplierId), SupplierTable)
                            ht[HutangSupplierTable.totalHutang]       = totalAmount
                            ht[HutangSupplierTable.sisaHutang]        = totalAmount
                            ht[HutangSupplierTable.tanggal]           = now
                            req.jatuhTempo?.let { ht[HutangSupplierTable.tanggalJatuhTempo] = LocalDate.parse(it).atStartOfDay() }
                            ht[HutangSupplierTable.status]            = "BELUM_LUNAS"
                            ht[HutangSupplierTable.fotoNotaUrl]       = "/nota/pembelian/$id"
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
                val now = LocalDateTime.now() // Untuk komisi/kas update

                val updatedCount = transaction {
                    // Dapatkan detail lama sebelum dihapus untuk mengembalikan stok
                    val oldDetails = PembelianDetailTable
                        .select { PembelianDetailTable.pembelianId eq EntityID(id, PembelianTable) }
                        .map { it[PembelianDetailTable.produkId].value to it[PembelianDetailTable.jumlah] }

                    // Kembalikan stok produk berdasarkan detail lama
                    oldDetails.forEach { (produkId, jumlah) ->
                        ProdukTable.update({ ProdukTable.id eq EntityID(produkId, ProdukTable) }) {
                            it[ProdukTable.stok] = ProdukTable.stok.minus(jumlah.toDouble()) // Asumsi stok adalah Double
                        }
                    }

                    // Hapus detail pembelian lama
                    PembelianDetailTable.deleteWhere { PembelianDetailTable.pembelianId eq EntityID(id, PembelianTable) }

                    // Perbarui header
                    val headerUpdated = PembelianTable.update({
                        (PembelianTable.id eq EntityID(id, PembelianTable)) and
                                (PembelianTable.entitasId eq entitasIdFromJwt)
                    }) { r ->
                        r[PembelianTable.metodePembayaran] = req.metodePembayaran
                        r[PembelianTable.status]           = if (req.metodePembayaran.equals("KREDIT", true)) "BELUM_LUNAS" else "LUNAS"
                        req.jatuhTempo?.let { r[PembelianTable.jatuhTempo] = LocalDate.parse(it).atStartOfDay() }
                        r[PembelianTable.total]            = req.detail.sumOf { it.hargaModal * it.jumlah } // Update total dari frontend
                    }

                    // Masukkan detail baru dan tambahkan stok
                    req.detail.forEach { item ->
                        val produkRow = ProdukTable.select { ProdukTable.id eq EntityID(UUID.fromString(item.produkId), ProdukTable) }.singleOrNull()
                            ?: throw IllegalArgumentException("Produk dengan ID ${item.produkId} tidak ditemukan.")

                        PembelianDetailTable.insert { d ->
                            d[PembelianDetailTable.pembelianId] = EntityID(id, PembelianTable)
                            d[PembelianDetailTable.produkId]    = EntityID(UUID.fromString(item.produkId), ProdukTable)
                            d[PembelianDetailTable.hargaModal]  = item.hargaModal
                            d[PembelianDetailTable.jumlah]      = item.jumlah
                            d[PembelianDetailTable.subtotal]    = item.hargaModal * item.jumlah
                            d[PembelianDetailTable.entitasId]   = entitasIdFromJwt
                        }
                        // Tambahkan stok produk berdasarkan jumlah baru
                        ProdukTable.update({ ProdukTable.id eq EntityID(UUID.fromString(item.produkId), ProdukTable) }) {
                            it[ProdukTable.stok] = ProdukTable.stok.plus(item.jumlah.toDouble()) // Asumsi stok adalah Double
                        }
                    }

                    // Handle update kas/hutang jika ada perubahan metode/total
                    val oldPembelian = PembelianTable.select { PembelianTable.id eq id }.singleOrNull()
                    val oldMetodePembayaran = oldPembelian?.get(PembelianTable.metodePembayaran)
                    val oldTotal = oldPembelian?.get(PembelianTable.total) ?: 0.0
                    val newTotal = req.detail.sumOf { it.hargaModal * it.jumlah }

                    // Jika metode pembayaran berubah
                    if (oldMetodePembayaran != req.metodePembayaran) {
                        // Hapus hutang terkait lama jika ada
                        if (oldMetodePembayaran.equals("KREDIT", true)) {
                            HutangSupplierTable.deleteWhere { HutangSupplierTable.pembelianId eq EntityID(id, PembelianTable) }
                        }
                        // Buat entri baru sesuai metode pembayaran baru
                        if (req.metodePembayaran.equals("KREDIT", true)) {
                            HutangSupplierTable.insert { ht ->
                                ht[HutangSupplierTable.entitasId]         = entitasIdFromJwt
                                ht[HutangSupplierTable.pembelianId]       = EntityID(id, PembelianTable)
                                ht[HutangSupplierTable.supplierId]        = EntityID(UUID.fromString(req.supplierId), SupplierTable)
                                ht[HutangSupplierTable.totalHutang]       = newTotal
                                ht[HutangSupplierTable.sisaHutang]        = newTotal
                                ht[HutangSupplierTable.tanggal]           = now
                                req.jatuhTempo?.let { ht[HutangSupplierTable.tanggalJatuhTempo] = LocalDate.parse(it).atStartOfDay() }
                                ht[HutangSupplierTable.status]            = "BELUM_LUNAS"
                                ht[HutangSupplierTable.fotoNotaUrl]       = oldPembelian?.get(PembelianTable.notaUrl) // Pertahankan URL nota lama
                            }
                        } else if (req.metodePembayaran.equals("TUNAI", true)) {
                            // Jika berubah ke TUNAI, Anda perlu mencatat pengeluaran kas
                            KasService.record(
                                entitasId  = entitasIdFromJwt.value,
                                kasId      = UUID.fromString(req.kasId!!), // asumsi kasId selalu ada untuk TUNAI
                                tanggal    = now,
                                jumlah     = newTotal,
                                tipe       = "KELUAR",
                                keterangan = "Koreksi Pembelian #$noFaktur (berubah menjadi Tunai)"
                            )
                        }
                    } else if (oldMetodePembayaran.equals("KREDIT", true) && req.metodePembayaran.equals("KREDIT", true)) {
                        // Jika tetap kredit, dan total berubah, update hutang
                        if (oldTotal != newTotal) {
                            HutangSupplierTable.update({ HutangSupplierTable.pembelianId eq EntityID(id, PembelianTable) }) {
                                it[HutangSupplierTable.totalHutang] = newTotal
                                it[HutangSupplierTable.sisaHutang] = newTotal // Asumsi sisa hutang reset saat update
                                req.jatuhTempo?.let { jt -> it[HutangSupplierTable.tanggalJatuhTempo] = LocalDate.parse(jt).atStartOfDay() }
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
                    // Ambil detail pembelian sebelum dihapus untuk mengembalikan stok
                    val oldDetails = PembelianDetailTable
                        .select { PembelianDetailTable.pembelianId eq EntityID(id, PembelianTable) }
                        .map { it[PembelianDetailTable.produkId].value to it[PembelianDetailTable.jumlah] }

                    // Kembalikan stok produk
                    oldDetails.forEach { (produkId, jumlah) ->
                        ProdukTable.update({ ProdukTable.id eq EntityID(produkId, ProdukTable) }) {
                            it[ProdukTable.stok] = ProdukTable.stok.minus(jumlah.toDouble()) // Asumsi stok adalah Double
                        }
                    }

                    // TODO: Perlu logika untuk menghapus file nota fisik dari server jika ada

                    // Hapus detail terlebih dahulu karena ada CASCADE
                    PembelianDetailTable.deleteWhere {
                        (PembelianDetailTable.pembelianId eq EntityID(id, PembelianTable)) and
                                (PembelianDetailTable.entitasId eq entitasIdFromJwt)
                    }
                    HutangSupplierTable.deleteWhere  {
                        (HutangSupplierTable.pembelianId eq EntityID(id, PembelianTable)) and
                                (HutangSupplierTable.entitasId  eq entitasIdFromJwt)
                    }
                    PembelianTable.deleteWhere      {
                        (PembelianTable.id eq EntityID(id, PembelianTable)) and
                                (PembelianTable.entitasId eq entitasIdFromJwt)
                    }
                }
                val httpStatusCode = if (deletedCount == 0) HttpStatusCode.NotFound else HttpStatusCode.NoContent
                val responseBody = if (deletedCount == 0) mapOf("error" to "Pembelian tidak ditemukan atau tidak memiliki akses") else null
                if (responseBody != null) {
                    call.respond(httpStatusCode, responseBody)
                } else {
                    call.respond(httpStatusCode)
                }
            }
        }
    }
}

// Helper generate noFaktur per-entitas
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