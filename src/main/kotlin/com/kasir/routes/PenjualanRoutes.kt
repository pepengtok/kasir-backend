package com.kasir.routes

import com.kasir.routes.helpers.getEntitasIdFromJwt // Mengimpor fungsi helper dari AuthHelpers.kt
import com.kasir.dto.*
import com.kasir.models.*
import com.kasir.service.KasService
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
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

fun Route.penjualanRoutes() {
    authenticate("jwt-auth") {
        route("/penjualan") {

            // 1) List semua penjualan (header saja, atau dengan detail jika ada filter produk_id)
            get {
                val entitasIdFromJwt = try {
                    getEntitasIdFromJwt()
                } catch (e: IllegalArgumentException) {
                    return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
                }

                val filterType = call.request.queryParameters["filterType"]
                val startDateParam = call.request.queryParameters["startDate"]
                val endDateParam = call.request.queryParameters["endDate"]
                val bulanParam = call.request.queryParameters["bulan"]
                val tahunParam = call.request.queryParameters["tahun"]
                val methodFilter = call.request.queryParameters["methodFilter"]
                val produkIdFilter = call.request.queryParameters["produk_id"] // Ambil filter produk_id

                val list = transaction {
                    var query: Query = PenjualanTable
                        .join(PelangganTable, JoinType.LEFT) { PenjualanTable.pelangganId eq PelangganTable.id }
                        .join(SalesTable, JoinType.LEFT) { PenjualanTable.salesId eq SalesTable.id }
                        .join(KasTable, JoinType.LEFT) { PenjualanTable.kasId eq KasTable.id }
                        .select { PenjualanTable.entitasId eq entitasIdFromJwt }

                    // Jika ada filter produk_id, tambahkan kondisi join dan filter ke query
                    if (produkIdFilter != null) {
                        val produkUUID = runCatching { UUID.fromString(produkIdFilter) }.getOrNull()
                            ?: throw IllegalArgumentException("Invalid produk_id format")

                        query = query.join(PenjualanDetailTable, JoinType.INNER) { PenjualanDetailTable.penjualanId eq PenjualanTable.id }
                            .andWhere { PenjualanDetailTable.produkId eq EntityID(produkUUID, ProdukTable) }
                            .join(ProdukTable, JoinType.INNER) { PenjualanDetailTable.produkId eq ProdukTable.id }
                    } // <-- KURUNG KURAWAL INI ADALAH YANG SEHARUSNYA UNTUK BLOK IF PRODUKIDFILTER

                    when (filterType) {
                        "HARI" -> {
                            if (startDateParam != null && endDateParam != null) {
                                val startDate = LocalDate.parse(startDateParam)
                                val endDate = LocalDate.parse(endDateParam)
                                val startDateTime = startDate.atStartOfDay()
                                val endDateTime = endDate.atTime(23, 59, 59, 999999999)
                                query = query.andWhere { PenjualanTable.tanggal.between(startDateTime, endDateTime) }
                            }
                        }
                        "BULAN" -> {
                            if (tahunParam != null && bulanParam != null) {
                                val year = tahunParam.toInt()
                                val month = bulanParam.toInt()
                                val firstDayOfMonth = LocalDate.of(year, month, 1)
                                val lastDayOfMonth = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth())
                                val startDateTime = firstDayOfMonth.atStartOfDay()
                                val endDateTime = lastDayOfMonth.atTime(23, 59, 59, 999999999)
                                query = query.andWhere { PenjualanTable.tanggal.between(startDateTime, endDateTime) }
                            }
                        }
                        "TAHUN" -> {
                            if (tahunParam != null) {
                                val year = tahunParam.toInt()
                                val firstDayOfYear = LocalDate.of(year, 1, 1)
                                val lastDayOfYear = LocalDate.of(year, 12, 31)
                                val startDateTime = firstDayOfYear.atStartOfDay()
                                val endDateTime = lastDayOfYear.atTime(23, 59, 59, 999999999)
                                query = query.andWhere { PenjualanTable.tanggal.between(startDateTime, endDateTime) }
                            }
                        }
                    }

                    if (methodFilter != "ALL" && methodFilter != null) {
                        query = query.andWhere { PenjualanTable.metodePembayaran eq methodFilter }
                    }

                    query.orderBy(
                        PenjualanTable.tanggal to SortOrder.DESC,
                        PenjualanTable.createdAt to SortOrder.DESC
                    )
                        .map { row ->
                            val detailsList = if (produkIdFilter != null) {
                                PenjualanDetailTable
                                    .join(ProdukTable, JoinType.INNER) { PenjualanDetailTable.produkId eq ProdukTable.id }
                                    .select {
                                        (PenjualanDetailTable.penjualanId eq row[PenjualanTable.id]) and
                                                (PenjualanDetailTable.produkId eq EntityID(UUID.fromString(produkIdFilter), ProdukTable))
                                    }
                                    .map { dt ->
                                        PenjualanDetailResponseDto(
                                            id = dt[PenjualanDetailTable.id].value.toString(),
                                            produkId = dt.getOrNull(PenjualanDetailTable.produkId)?.value?.toString() ?: "",
                                            namaProduk = dt[ProdukTable.namaProduk],
                                            satuan = dt[PenjualanDetailTable.satuan],
                                            jumlah = dt[PenjualanDetailTable.jumlah],
                                            hargaJual = dt[PenjualanDetailTable.hargaJual],
                                            hargaModal = dt[PenjualanDetailTable.hargaModal],
                                            subtotal = dt[PenjualanDetailTable.subtotal],
                                            potensiLaba = dt[PenjualanDetailTable.potensiLaba]
                                        )
                                    }
                            } else {
                                emptyList()
                            }
                            PenjualanResponseDto(
                                id = row[PenjualanTable.id].value.toString(),
                                pelangganId = row[PenjualanTable.pelangganId]?.value?.toString(),
                                namaPelanggan = row[PelangganTable.namaPelanggan],
                                salesId = row[PenjualanTable.salesId]?.value?.toString(),
                                namaSales = row[SalesTable.nama],
                                tanggal = row[PenjualanTable.tanggal].toString(),
                                metodePembayaran = row[PenjualanTable.metodePembayaran],
                                status = row[PenjualanTable.status],
                                total = row[PenjualanTable.total],
                                noNota = row[PenjualanTable.noNota],
                                notaUrl = row[PenjualanTable.notaUrl],
                                kasId = row[PenjualanTable.kasId]?.value?.toString(),
                                namaKas = row[KasTable.namaKas],
                                jatuhTempo = row[PenjualanTable.jatuhTempo]?.toString(),
                                entitasId = row[PenjualanTable.entitasId].value.toString(),
                                createdAt = row[PenjualanTable.createdAt].toString(),
                                detail = detailsList
                            )
                        }
                }
                call.respond(HttpStatusCode.OK, list)
            }

            // 2) Ambil satu penjualan + detail (untuk NotaPenjualan.jsx)
            get("/{id}") {
                val entitasIdFromJwt = try {
                    getEntitasIdFromJwt()
                } catch (e: IllegalArgumentException) {
                    return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
                }
                val idParam = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing ID"))
                val penjualanUUID = runCatching { UUID.fromString(idParam) }
                    .getOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID format"))

                val (penjualanResponse, detailResponses) = transaction {
                    val penjRow = PenjualanTable
                        .join(PelangganTable, JoinType.LEFT) { PenjualanTable.pelangganId eq PelangganTable.id }
                        .join(SalesTable, JoinType.LEFT) { PenjualanTable.salesId eq SalesTable.id }
                        .join(KasTable, JoinType.LEFT) { PenjualanTable.kasId eq KasTable.id }
                        .select {
                            (PenjualanTable.id eq EntityID(penjualanUUID, PenjualanTable)) and
                                    (PenjualanTable.entitasId eq entitasIdFromJwt)
                        }
                        .singleOrNull() ?: return@transaction null to emptyList()

                    val details = PenjualanDetailTable
                        .join(ProdukTable, JoinType.INNER) { PenjualanDetailTable.produkId eq ProdukTable.id }
                        .select {
                            (PenjualanDetailTable.penjualanId eq EntityID(penjualanUUID, PenjualanTable)) and
                                    (PenjualanDetailTable.entitasId eq entitasIdFromJwt)
                        }
                        .map { dt ->
                            PenjualanDetailResponseDto(
                                id = dt[PenjualanDetailTable.id].value.toString(),
                                produkId = dt.getOrNull(PenjualanDetailTable.produkId)?.value?.toString() ?: "",
                                namaProduk = dt[ProdukTable.namaProduk],
                                satuan = dt[PenjualanDetailTable.satuan],
                                jumlah = dt[PenjualanDetailTable.jumlah],
                                hargaJual = dt[PenjualanDetailTable.hargaJual],
                                hargaModal = dt[PenjualanDetailTable.hargaModal],
                                subtotal = dt[PenjualanDetailTable.subtotal],
                                potensiLaba = dt[PenjualanDetailTable.potensiLaba]
                            )
                        }

                    val penjualanDto = PenjualanResponseDto(
                        id = penjRow[PenjualanTable.id].value.toString(),
                        pelangganId = penjRow[PenjualanTable.pelangganId]?.value?.toString(),
                        namaPelanggan = penjRow[PelangganTable.namaPelanggan],
                        salesId = penjRow[PenjualanTable.salesId]?.value?.toString(),
                        namaSales = penjRow[SalesTable.nama],
                        tanggal = penjRow[PenjualanTable.tanggal].toString(),
                        metodePembayaran = penjRow[PenjualanTable.metodePembayaran],
                        status = penjRow[PenjualanTable.status],
                        total = penjRow[PenjualanTable.total],
                        noNota = penjRow[PenjualanTable.noNota],
                        notaUrl = penjRow[PenjualanTable.notaUrl],
                        kasId = penjRow[PenjualanTable.kasId]?.value?.toString(),
                        namaKas = penjRow[KasTable.namaKas],
                        jatuhTempo = penjRow[PenjualanTable.jatuhTempo]?.toString(),
                        entitasId = penjRow[PenjualanTable.entitasId].value.toString(),
                        createdAt = penjRow[PenjualanTable.createdAt].toString()
                    )
                    penjualanDto to details
                }

                if (penjualanResponse == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Penjualan tidak ditemukan"))
                } else {
                    call.respond(HttpStatusCode.OK, PenjualanResponseWithDetailDto(penjualanResponse, detailResponses))
                }
            }


            // 3) Create penjualan
            post {
                val entitasIdFromJwt = try {
                    getEntitasIdFromJwt()
                } catch (e: IllegalArgumentException) {
                    return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
                }
                val req = call.receive<PenjualanRequestDto>()
                val now = LocalDateTime.now()
                val noNota = req.noNota ?: generateNotaUrut(entitasIdFromJwt.value)

                val newId = transaction {
                    val penjId = PenjualanTable.insertAndGetId { r ->
                        r[PenjualanTable.entitasId] = entitasIdFromJwt
                        r[PenjualanTable.noNota] = noNota
                        r[PenjualanTable.tanggal] = LocalDate.parse(req.tanggal).atStartOfDay()
                        req.pelangganId?.let { r[PenjualanTable.pelangganId] = EntityID(UUID.fromString(it), PelangganTable) }
                        req.salesId?.let { r[PenjualanTable.salesId] = EntityID(UUID.fromString(it), SalesTable) }
                        r[PenjualanTable.total] = req.total
                        r[PenjualanTable.metodePembayaran] = req.metodePembayaran
                        r[PenjualanTable.status] = req.status
                        req.kasId?.let { r[PenjualanTable.kasId] = EntityID(UUID.fromString(it), KasTable) }
                        req.jatuhTempo?.let { r[PenjualanTable.jatuhTempo] = LocalDate.parse(it).atStartOfDay() }
                        r[PenjualanTable.createdAt] = now
                        r[PenjualanTable.notaUrl] = null
                    }

                    val generatedNotaUrl = "/nota-penjualan/${penjId.value}"
                    PenjualanTable.update({ PenjualanTable.id eq penjId }) {
                        it[PenjualanTable.notaUrl] = generatedNotaUrl
                    }

                    req.items.forEach { d ->
                        val produkRow = ProdukTable.select { ProdukTable.id eq EntityID(UUID.fromString(d.produkId), ProdukTable) }.singleOrNull()
                            ?: throw IllegalArgumentException("Produk dengan ID ${d.produkId} tidak ditemukan.")

                        PenjualanDetailTable.insert { dt ->
                            dt[PenjualanDetailTable.entitasId] = entitasIdFromJwt
                            dt[PenjualanDetailTable.penjualanId] = EntityID(penjId.value, PenjualanTable)
                            dt[PenjualanDetailTable.produkId] = EntityID(UUID.fromString(d.produkId), ProdukTable)
                            dt[PenjualanDetailTable.hargaModal] = produkRow[ProdukTable.hargaModal]
                            dt[PenjualanDetailTable.hargaJual] = d.hargaJual
                            dt[PenjualanDetailTable.jumlah] = d.jumlah.toInt()
                            dt[PenjualanDetailTable.subtotal] = d.subtotal
                            dt[PenjualanDetailTable.potensiLaba] = (d.hargaJual - produkRow[ProdukTable.hargaModal]) * d.jumlah
                            dt[PenjualanDetailTable.satuan] = d.satuan
                        }
                        ProdukTable.update({ ProdukTable.id eq EntityID(UUID.fromString(d.produkId), ProdukTable) }) {
                            it[ProdukTable.stok] = ProdukTable.stok.minus(d.jumlah.toDouble())
                        }
                    }

                    if (req.metodePembayaran.equals("PIUTANG", true)) {
                        PiutangPelangganTable.insert { pt ->
                            pt[PiutangPelangganTable.entitasId] = entitasIdFromJwt
                            pt[PiutangPelangganTable.penjualanId] = EntityID(penjId.value, PenjualanTable)
                            pt[PiutangPelangganTable.pelangganId] = EntityID(UUID.fromString(req.pelangganId!!), PelangganTable)
                            pt[PiutangPelangganTable.tanggal] = LocalDate.parse(req.tanggal)
                            req.jatuhTempo?.let { pt[PiutangPelangganTable.tanggalJatuhTempo] = LocalDate.parse(it).atStartOfDay() }
                            pt[PiutangPelangganTable.totalPiutang] = req.total
                            pt[PiutangPelangganTable.sisaPiutang] = req.total
                            pt[PiutangPelangganTable.status] = "BELUM_LUNAS"
                            pt[PiutangPelangganTable.fotoNotaUrl] = generatedNotaUrl
                        }
                    }

                    if (req.metodePembayaran.equals("TUNAI", true)) {
                        KasService.record(
                            entitasId = entitasIdFromJwt.value,
                            kasId = UUID.fromString(req.kasId!!),
                            tanggal = now,
                            jumlah = req.total,
                            tipe = "MASUK",
                            keterangan = "Penjualan #$noNota"
                        )
                    }

                    req.salesId?.let { sid ->
                        val salesRow = SalesTable.select { SalesTable.id eq EntityID(UUID.fromString(sid), SalesTable) }
                            .firstOrNull()
                        val komisiPersen = if (req.metodePembayaran.equals("TUNAI", true))
                            salesRow?.get(SalesTable.komisiTunai)
                        else
                            salesRow?.get(SalesTable.komisiPiutang)

                        val totalUntung = req.items.sumOf { d ->
                            val produkRow = ProdukTable.select { ProdukTable.id eq EntityID(UUID.fromString(d.produkId), ProdukTable) }.single()
                            (d.hargaJual - produkRow[ProdukTable.hargaModal]) * d.jumlah
                        }
                        val nominalKomisi = (totalUntung * (komisiPersen ?: 0.0)) / 100.0

                        if (nominalKomisi > 0) {
                            KomisiSalesTable.insert { ks ->
                                ks[KomisiSalesTable.entitasId] = entitasIdFromJwt
                                ks[KomisiSalesTable.penjualanId] = EntityID(penjId.value, PenjualanTable)
                                ks[KomisiSalesTable.salesId] = EntityID(UUID.fromString(sid), SalesTable)
                                ks[KomisiSalesTable.komisiPersen] = komisiPersen ?: 0.0
                                ks[KomisiSalesTable.nominalKomisi] = nominalKomisi
                                ks[KomisiSalesTable.status] = if (req.metodePembayaran.equals("TUNAI", true)) "DIBAYAR" else "PENDING"
                                ks[KomisiSalesTable.tanggalKomisi] = now
                            }
                        }
                    }
                    penjId.value
                }
                call.respond(HttpStatusCode.Created, mapOf("id" to newId.toString()))
            }

            // 4) Update penjualan
            put("/{id}") {
                val entitasIdFromJwt = getEntitasIdFromJwt()
                val id = runCatching { UUID.fromString(call.parameters["id"]!!) }
                    .getOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
                val req = call.receive<PenjualanRequestDto>()

                val updatedCount = transaction {
                    // Dapatkan detail lama sebelum dihapus untuk mengembalikan stok
                    val oldDetails = PenjualanDetailTable
                        .select { PenjualanDetailTable.penjualanId eq EntityID(id, PenjualanTable) }
                        .map { it[PenjualanDetailTable.produkId].value to it[PenjualanDetailTable.jumlah] }

                    // Kembalikan stok produk berdasarkan detail lama
                    oldDetails.forEach { (produkId, jumlah) ->
                        ProdukTable.update({ ProdukTable.id eq EntityID(produkId, ProdukTable) }) {
                            it[ProdukTable.stok] = ProdukTable.stok.plus(jumlah.toDouble())
                        }
                    }

                    // Hapus detail penjualan lama
                    PenjualanDetailTable.deleteWhere { PenjualanDetailTable.penjualanId eq EntityID(id, PenjualanTable) }

                    // Perbarui header
                    val headerUpdated = PenjualanTable.update({
                        (PenjualanTable.id eq EntityID(id, PenjualanTable)) and
                                (PenjualanTable.entitasId eq entitasIdFromJwt)
                    }) { r ->
                        r[PenjualanTable.total] = req.total
                        r[PenjualanTable.metodePembayaran] = req.metodePembayaran
                        r[PenjualanTable.status] = req.status
                        req.kasId?.let { r[PenjualanTable.kasId] = EntityID(UUID.fromString(it), KasTable) }
                        req.jatuhTempo?.let { r[PenjualanTable.jatuhTempo] = LocalDate.parse(it).atStartOfDay() }
                        req.noNota?.let { r[PenjualanTable.noNota] = it }
                    }

                    // Masukkan detail baru dan kurangi stok
                    req.items.forEach { d ->
                        val produkRow = ProdukTable.select { ProdukTable.id eq EntityID(UUID.fromString(d.produkId), ProdukTable) }.singleOrNull()
                            ?: throw IllegalArgumentException("Produk dengan ID ${d.produkId} tidak ditemukan.")

                        PenjualanDetailTable.insert { dt ->
                            dt[PenjualanDetailTable.entitasId] = entitasIdFromJwt
                            dt[PenjualanDetailTable.penjualanId] = EntityID(id, PenjualanTable)
                            dt[PenjualanDetailTable.produkId] = EntityID(UUID.fromString(d.produkId), ProdukTable)
                            dt[PenjualanDetailTable.hargaModal] = produkRow[ProdukTable.hargaModal]
                            dt[PenjualanDetailTable.hargaJual] = d.hargaJual
                            dt[PenjualanDetailTable.jumlah] = d.jumlah.toInt()
                            dt[PenjualanDetailTable.subtotal] = d.subtotal
                            dt[PenjualanDetailTable.potensiLaba] = (d.hargaJual - produkRow[ProdukTable.hargaModal]) * d.jumlah
                            dt[PenjualanDetailTable.satuan] = d.satuan
                        }
                        ProdukTable.update({ ProdukTable.id eq EntityID(UUID.fromString(d.produkId), ProdukTable) }) {
                            it[ProdukTable.stok] = ProdukTable.stok.minus(d.jumlah.toDouble())
                        }
                    }

                    // Update Piutang/Kas/Komisi (Ini bisa lebih kompleks, disarankan di service layer)
                    val oldPenjualan = PenjualanTable.select { PenjualanTable.id eq id }.singleOrNull()
                    val oldMetodePembayaran = oldPenjualan?.get(PenjualanTable.metodePembayaran)
                    val now = LocalDateTime.now()

                    if (oldMetodePembayaran != req.metodePembayaran) {
                        if (oldMetodePembayaran.equals("PIUTANG", true)) {
                            PiutangPelangganTable.deleteWhere { PiutangPelangganTable.penjualanId eq EntityID(id, PenjualanTable) }
                        }
                        if (req.metodePembayaran.equals("PIUTANG", true)) {
                            PiutangPelangganTable.insert { pt ->
                                pt[PiutangPelangganTable.entitasId] = entitasIdFromJwt
                                pt[PiutangPelangganTable.penjualanId] = EntityID(id, PenjualanTable)
                                pt[PiutangPelangganTable.pelangganId] = EntityID(UUID.fromString(req.pelangganId!!), PelangganTable)
                                pt[PiutangPelangganTable.tanggal] = LocalDate.parse(req.tanggal)
                                req.jatuhTempo?.let { pt[PiutangPelangganTable.tanggalJatuhTempo] = LocalDate.parse(it).atStartOfDay() }
                                pt[PiutangPelangganTable.totalPiutang] = req.total
                                pt[PiutangPelangganTable.sisaPiutang] = req.total
                                pt[PiutangPelangganTable.status] = "BELUM_LUNAS"
                                pt[PiutangPelangganTable.fotoNotaUrl] = oldPenjualan?.get(PenjualanTable.notaUrl)
                            }
                        } else if (req.metodePembayaran.equals("TUNAI", true)) {
                            KasService.record(
                                entitasId = entitasIdFromJwt.value,
                                kasId = UUID.fromString(req.kasId!!),
                                tanggal = now,
                                jumlah = req.total,
                                tipe = "MASUK",
                                keterangan = "Koreksi Penjualan #${oldPenjualan?.get(PenjualanTable.noNota) ?: "N/A"} (berubah menjadi Tunai)"
                            )
                        }
                    } else if (oldMetodePembayaran.equals("PIUTANG", true) && req.metodePembayaran.equals("PIUTANG", true)) {
                        PiutangPelangganTable.update({ PiutangPelangganTable.penjualanId eq EntityID(id, PenjualanTable) }) {
                            it[PiutangPelangganTable.totalPiutang] = req.total
                            it[PiutangPelangganTable.sisaPiutang] = req.total
                            req.jatuhTempo?.let { jt -> it[PiutangPelangganTable.tanggalJatuhTempo] = LocalDate.parse(jt).atStartOfDay() }
                        }
                    }

                    if (req.salesId != null) {
                        val salesRow = SalesTable.select { SalesTable.id eq EntityID(UUID.fromString(req.salesId), SalesTable) }
                            .firstOrNull()
                        val komisiPersen = if (req.metodePembayaran.equals("TUNAI", true))
                            salesRow?.get(SalesTable.komisiTunai)
                        else
                            salesRow?.get(SalesTable.komisiPiutang)

                        val totalUntung = req.items.sumOf { d ->
                            val produkRow = ProdukTable.select { ProdukTable.id eq EntityID(UUID.fromString(d.produkId), ProdukTable) }.single()
                            (d.hargaJual - produkRow[ProdukTable.hargaModal]) * d.jumlah
                        }
                        val nominalKomisi = (totalUntung * (komisiPersen ?: 0.0)) / 100.0

                        KomisiSalesTable.deleteWhere { KomisiSalesTable.penjualanId eq EntityID(id, PenjualanTable) }

                        if (nominalKomisi > 0) {
                            KomisiSalesTable.insert { ks ->
                                ks[KomisiSalesTable.entitasId] = entitasIdFromJwt
                                ks[KomisiSalesTable.penjualanId] = EntityID(id, PenjualanTable)
                                ks[KomisiSalesTable.salesId] = EntityID(UUID.fromString(req.salesId), SalesTable)
                                ks[KomisiSalesTable.komisiPersen] = komisiPersen ?: 0.0
                                ks[KomisiSalesTable.nominalKomisi] = nominalKomisi
                                ks[KomisiSalesTable.status] = if (req.metodePembayaran.equals("TUNAI", true)) "DIBAYAR" else "PENDING"
                                ks[KomisiSalesTable.tanggalKomisi] = now
                            }
                        }
                    }
                    1 // Mengembalikan 1 untuk menandakan update berhasil
                }
                val httpStatusCode = if (updatedCount == 0) HttpStatusCode.NotFound else HttpStatusCode.OK
                val responseBody = if (updatedCount == 0) mapOf("error" to "Penjualan tidak ditemukan atau tidak memiliki akses") else mapOf("message" to "Penjualan berhasil diperbarui")
                call.respond(httpStatusCode, responseBody)
            }

            // 5) Delete penjualan
            delete("/{id}") {
                val entitasIdFromJwt = getEntitasIdFromJwt()
                val id = runCatching { UUID.fromString(call.parameters["id"]!!) }
                    .getOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))

                val deletedPenjualanCount = transaction {
                    // Ambil detail penjualan sebelum dihapus untuk mengembalikan stok
                    val oldDetails = PenjualanDetailTable
                        .select { PenjualanDetailTable.penjualanId eq EntityID(id, PenjualanTable) }
                        .map { it[PenjualanDetailTable.produkId].value to it[PenjualanDetailTable.jumlah] }

                    // Kembalikan stok produk
                    oldDetails.forEach { (produkId, jumlah) ->
                        ProdukTable.update({ ProdukTable.id eq EntityID(produkId, ProdukTable) }) {
                            it[ProdukTable.stok] = ProdukTable.stok.plus(jumlah.toDouble())
                        }
                    }

                    // Hapus detail terlebih dahulu
                    PenjualanDetailTable.deleteWhere {
                        (PenjualanDetailTable.penjualanId eq EntityID(id, PenjualanTable)) and
                                (PenjualanDetailTable.entitasId eq entitasIdFromJwt)
                    }
                    // Hapus piutang terkait
                    PiutangPelangganTable.deleteWhere {
                        (PiutangPelangganTable.penjualanId eq EntityID(id, PenjualanTable)) and
                                (PiutangPelangganTable.entitasId eq entitasIdFromJwt)
                    }
                    // Hapus komisi terkait
                    KomisiSalesTable.deleteWhere {
                        (KomisiSalesTable.penjualanId eq EntityID(id, PenjualanTable)) and
                                (KomisiSalesTable.entitasId eq entitasIdFromJwt)
                    }
                    // Akhirnya, hapus penjualan utama
                    PenjualanTable.deleteWhere {
                        (PenjualanTable.id eq EntityID(id, PenjualanTable)) and
                                (PenjualanTable.entitasId eq entitasIdFromJwt)
                    }
                }
                val httpStatusCode = if (deletedPenjualanCount == 0) HttpStatusCode.NotFound else HttpStatusCode.NoContent
                val responseBody = if (deletedPenjualanCount == 0) mapOf("error" to "Penjualan tidak ditemukan atau tidak memiliki akses") else null
                if (responseBody != null) {
                    call.respond(httpStatusCode, responseBody)
                } else {
                    call.respond(httpStatusCode) // NoContent tidak perlu body
                }
            }
        }
    }
}

// Helper generate noNota per-entitas
private fun generateNotaUrut(entitasId: UUID): String = transaction {
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    val prefix = "PNJ-$today"
    val last = PenjualanTable
        .select {
            (PenjualanTable.noNota like "$prefix%") and
                    (PenjualanTable.entitasId eq EntityID(entitasId, EntitasUsahaTable))
        }
        .orderBy(PenjualanTable.noNota to SortOrder.DESC)
        .limit(1)
        .firstOrNull()
        ?.get(PenjualanTable.noNota)
    val next = last?.substringAfterLast("-")?.toIntOrNull()?.plus(1) ?: 1
    "$prefix-${"%04d".format(next)}"
}