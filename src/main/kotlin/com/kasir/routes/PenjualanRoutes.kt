package com.kasir.routes

import com.kasir.routes.helpers.getEntitasIdFromJwt
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
import org.jetbrains.exposed.sql.* // Impor utama Exposed DSL
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
                val produkIdFilter = call.request.queryParameters["produk_id"]

                val list = transaction {
                    // Tahap 1: Mulai dengan PenjualanTable, lakukan join pertama.
                    // Hasilnya adalah objek Join.
                    val initialJoin: Join = PenjualanTable
                        .join(PelangganTable, JoinType.LEFT, additionalConstraint = { PenjualanTable.pelangganId eq PelangganTable.id })
                        .join(SalesTable, JoinType.LEFT, additionalConstraint = { PenjualanTable.salesId eq SalesTable.id })
                        .join(KasTable, JoinType.LEFT, additionalConstraint = { PenjualanTable.kasId eq KasTable.id })

                    // Tahap 2: Buat variabel mutable untuk chain join berikutnya.
                    // Tipe awal bisa diinfer sebagai Join dari initialJoin.
                    var currentJoinedChain = initialJoin

                    // Tahap 3: Tambahkan JOIN detail+produk jika perlu, tetap pada objek Join.
                    if (produkIdFilter != null) {
                        val produkUUID = runCatching { UUID.fromString(produkIdFilter) }.getOrNull()
                            ?: throw IllegalArgumentException("Invalid produk_id format")

                        currentJoinedChain = currentJoinedChain
                            .join(PenjualanDetailTable, JoinType.INNER, additionalConstraint = { PenjualanDetailTable.penjualanId eq PenjualanTable.id })
                            .join(ProdukTable, JoinType.INNER, additionalConstraint = { PenjualanDetailTable.produkId eq ProdukTable.id })
                    }

                    // Tahap 4: Setelah semua JOIN selesai, baru SELECT -> menghasilkan Query
                    var query = currentJoinedChain .selectAll()  .andWhere { PenjualanTable.entitasId eq entitasIdFromJwt }

                    // Tahap 5: Terapkan filter tambahan pada objek Query
                    if (produkIdFilter != null) {
                        val produkUUID = runCatching { UUID.fromString(produkIdFilter) }.getOrNull()
                            ?: throw IllegalArgumentException("Invalid produk_id format")
                        query = query.andWhere { PenjualanDetailTable.produkId eq EntityID(produkUUID, ProdukTable) }
                    }

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

                                val produkIdEntity = EntityID(
                                    UUID.fromString(produkIdFilter),
                                    ProdukTable
                                )
                                PenjualanDetailTable
                                    .join(
                                        ProdukTable,
                                        JoinType.INNER,
                                        additionalConstraint = { PenjualanDetailTable.produkId eq ProdukTable.id }
                                    )
                                    .selectAll()
                                    .andWhere {
                                        (PenjualanDetailTable.penjualanId eq row[PenjualanTable.id]) and
                                                (PenjualanDetailTable.produkId   eq produkIdEntity)
                                    }
                                    .map { dt ->
                                        PenjualanDetailResponseDto(
                                            id          = dt[PenjualanDetailTable.id].value.toString(),
                                            produkId    = dt[PenjualanDetailTable.produkId]?.value?.toString().orEmpty(),
                                            namaProduk  = dt[ProdukTable.namaProduk],
                                            satuan      = dt[PenjualanDetailTable.satuan],
                                            jumlah      = dt[PenjualanDetailTable.jumlah],
                                            hargaJual   = dt[PenjualanDetailTable.hargaJual],
                                            hargaModal  = dt[PenjualanDetailTable.hargaModal],
                                            subtotal    = dt[PenjualanDetailTable.subtotal],
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
                // 1) Autentikasi & parse entitasId
                val entitasIdFromJwt = try {
                    getEntitasIdFromJwt()
                } catch (e: IllegalArgumentException) {
                    return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
                }

                // 2) Ambil dan validasi path-param ID
                val idParam = call.parameters["id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing ID"))
                val penjualanUUID = runCatching { UUID.fromString(idParam) }
                    .getOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID format"))

                // 3) Transaksi DB
                val (penjualanDto, detailList) = transaction {
                    // a) Bangun JOIN chain untuk header
                    val headerJoin = PenjualanTable
                        .join(
                            PelangganTable, JoinType.LEFT,
                            additionalConstraint = { PenjualanTable.pelangganId eq PelangganTable.id }
                        )
                        .join(
                            SalesTable, JoinType.LEFT,
                            additionalConstraint = { PenjualanTable.salesId eq SalesTable.id }
                        )
                        .join(
                            KasTable, JoinType.LEFT,
                            additionalConstraint = { PenjualanTable.kasId eq KasTable.id }
                        )

                    // b) Ambil satu baris penjualan
                    val penjRow = headerJoin
                        .selectAll()
                        .andWhere {
                            (PenjualanTable.id eq EntityID(penjualanUUID, PenjualanTable)) and
                                    (PenjualanTable.entitasId eq entitasIdFromJwt)
                        }
                        .singleOrNull()
                        ?: return@transaction null to emptyList()

                    // c) Mapping header ke DTO
                    val headerDto = PenjualanResponseDto(
                        id            = penjRow[PenjualanTable.id].value.toString(),
                        pelangganId   = penjRow[PenjualanTable.pelangganId]?.value?.toString(),
                        namaPelanggan = penjRow[PelangganTable.namaPelanggan],
                        salesId       = penjRow[PenjualanTable.salesId]?.value?.toString(),
                        namaSales     = penjRow[SalesTable.nama],
                        tanggal       = penjRow[PenjualanTable.tanggal].toString(),
                        metodePembayaran = penjRow[PenjualanTable.metodePembayaran],
                        status        = penjRow[PenjualanTable.status],
                        total         = penjRow[PenjualanTable.total],
                        noNota        = penjRow[PenjualanTable.noNota],
                        notaUrl       = penjRow[PenjualanTable.notaUrl],
                        kasId         = penjRow[PenjualanTable.kasId]?.value?.toString(),
                        namaKas       = penjRow[KasTable.namaKas],
                        jatuhTempo    = penjRow[PenjualanTable.jatuhTempo]?.toString(),
                        entitasId     = penjRow[PenjualanTable.entitasId].value.toString(),
                        createdAt     = penjRow[PenjualanTable.createdAt].toString(),
                        detail        = emptyList()  // sementara, akan di‐isi di bawah
                    )

                    // d) Bangun JOIN chain untuk detail
                    val detailJoin = PenjualanDetailTable
                        .join(
                            ProdukTable,
                            JoinType.INNER,
                            additionalConstraint = { PenjualanDetailTable.produkId eq ProdukTable.id }
                        )

                    // e) Ambil semua baris detail untuk penjualan ini
                    val details = detailJoin
                        .selectAll()
                        .andWhere {
                            (PenjualanDetailTable.penjualanId eq EntityID(penjualanUUID, PenjualanTable)) and
                                    (PenjualanDetailTable.entitasId  eq entitasIdFromJwt)
                        }
                        .map { dt ->
                            PenjualanDetailResponseDto(
                                id          = dt[PenjualanDetailTable.id].value.toString(),
                                produkId    = dt[PenjualanDetailTable.produkId]?.value?.toString().orEmpty(),
                                namaProduk  = dt[ProdukTable.namaProduk],
                                satuan      = dt[PenjualanDetailTable.satuan],
                                jumlah      = dt[PenjualanDetailTable.jumlah],
                                hargaJual   = dt[PenjualanDetailTable.hargaJual],
                                hargaModal  = dt[PenjualanDetailTable.hargaModal],
                                subtotal    = dt[PenjualanDetailTable.subtotal],
                                potensiLaba = dt[PenjualanDetailTable.potensiLaba]
                            )
                        }

                    // f) Kembalikan header + detail
                    headerDto.copy(detail = details) to details
                }

                // 4) Response
                if (penjualanDto == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Penjualan tidak ditemukan"))
                } else {
                    call.respond(HttpStatusCode.OK, PenjualanResponseWithDetailDto(penjualanDto, detailList))
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
                        r[entitasId] = entitasIdFromJwt
                        r[PenjualanTable.noNota] = noNota
                        r[tanggal] = LocalDate.parse(req.tanggal).atStartOfDay()
                        req.pelangganId?.let { r[pelangganId] = EntityID(UUID.fromString(it), PelangganTable) }
                        req.salesId?.let { r[salesId] = EntityID(UUID.fromString(it), SalesTable) }
                        r[total] = req.total
                        r[metodePembayaran] = req.metodePembayaran
                        r[status] = req.status
                        req.kasId?.let { r[kasId] = EntityID(UUID.fromString(it), KasTable) }
                        req.jatuhTempo?.let { r[jatuhTempo] = LocalDate.parse(it).atStartOfDay() }
                        r[createdAt] = now
                        r[notaUrl] = null
                    }

                    val generatedNotaUrl = "/nota-penjualan/${penjId.value}"
                    PenjualanTable.update({ PenjualanTable.id eq penjId }) {
                        it[notaUrl] = generatedNotaUrl
                    }

                    req.items.forEach { d ->
                        val produkRow = ProdukTable.select { ProdukTable.id eq EntityID(UUID.fromString(d.produkId), ProdukTable) }.singleOrNull()
                            ?: throw IllegalArgumentException("Produk dengan ID ${d.produkId} tidak ditemukan.")

                        PenjualanDetailTable.insert { dt ->
                            dt[entitasId] = entitasIdFromJwt
                            dt[penjualanId] = EntityID(penjId.value, PenjualanTable)
                            dt[produkId] = EntityID(UUID.fromString(d.produkId), ProdukTable)
                            dt[hargaModal] = produkRow[ProdukTable.hargaModal]
                            dt[hargaJual] = d.hargaJual
                            dt[jumlah] = d.jumlah.toInt()
                            dt[subtotal] = d.subtotal
                            dt[potensiLaba] = (d.hargaJual - produkRow[ProdukTable.hargaModal]) * d.jumlah
                            dt[satuan] = d.satuan
                        }
                        ProdukTable.update({ ProdukTable.id eq EntityID(UUID.fromString(d.produkId), ProdukTable) }) {
                            val currentStok = ProdukTable.select { ProdukTable.id eq EntityID(UUID.fromString(d.produkId), ProdukTable) }.single()[stok]
                            it[stok] = currentStok.minus(d.jumlah.toDouble())
                        }
                    }

                    if (req.metodePembayaran.equals("PIUTANG", true)) {
                        PiutangPelangganTable.insert { pt ->
                            pt[entitasId] = entitasIdFromJwt
                            pt[penjualanId] = EntityID(penjId.value, PenjualanTable)
                            pt[pelangganId] = EntityID(UUID.fromString(req.pelangganId!!), PelangganTable)
                            pt[tanggal] = LocalDate.parse(req.tanggal)
                            req.jatuhTempo?.let { pt[tanggalJatuhTempo] = LocalDate.parse(it).atStartOfDay() }
                            pt[totalPiutang] = req.total
                            pt[sisaPiutang] = req.total
                            pt[status] = "BELUM_LUNAS"
                            pt[fotoNotaUrl] = generatedNotaUrl
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
                                ks[entitasId] = entitasIdFromJwt
                                ks[penjualanId] = EntityID(penjId.value, PenjualanTable)
                                ks[salesId] = EntityID(UUID.fromString(sid), SalesTable)
                                ks[KomisiSalesTable.komisiPersen] = komisiPersen ?: 0.0
                                ks[KomisiSalesTable.nominalKomisi] = nominalKomisi
                                ks[status] = if (req.metodePembayaran.equals("TUNAI", true)) "DIBAYAR" else "PENDING"
                                ks[tanggalKomisi] = now
                            }
                        }
                    }
                    penjId.value
                }
                call.respond(HttpStatusCode.Created, mapOf("id" to newId.toString()))
            }

            // 4) Update penjualan
            put("/{id}") {
                val entitasIdFromJwt = try {
                    getEntitasIdFromJwt()
                } catch (e: IllegalArgumentException) {
                    return@put call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
                }
                val id = runCatching { UUID.fromString(call.parameters["id"]!!) }
                    .getOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
                val req = call.receive<PenjualanRequestDto>()

                val updatedCount = transaction {
                    val oldDetails = PenjualanDetailTable
                        .select { PenjualanDetailTable.penjualanId eq EntityID(id, PenjualanTable) }
                        .map { row ->row[PenjualanDetailTable.produkId]!!.value to row[PenjualanDetailTable.jumlah]  }

                    oldDetails.forEach { (produkId, jumlah) ->
                        ProdukTable.update({ ProdukTable.id eq EntityID(produkId, ProdukTable) }) {
                            val currentStok = ProdukTable.select { ProdukTable.id eq EntityID(produkId, ProdukTable) }.single()[stok]
                            it[stok] = currentStok.plus(jumlah.toDouble())
                        }
                    }

                    PenjualanDetailTable.deleteWhere { penjualanId eq EntityID(id, PenjualanTable) }

                    val headerUpdated = PenjualanTable.update({
                        (PenjualanTable.id eq EntityID(id, PenjualanTable)) and
                                (PenjualanTable.entitasId eq entitasIdFromJwt)
                    }) { r ->
                        r[total] = req.total
                        r[metodePembayaran] = req.metodePembayaran
                        r[status] = req.status
                        req.kasId?.let { r[KasTable.id] = EntityID(UUID.fromString(it), KasTable) }
                        req.jatuhTempo?.let { r[jatuhTempo] = LocalDate.parse(it).atStartOfDay() }
                        req.noNota?.let { r[noNota] = it }
                    }

                    req.items.forEach { d ->
                        val produkRow = ProdukTable.select { ProdukTable.id eq EntityID(UUID.fromString(d.produkId), ProdukTable) }.singleOrNull()
                            ?: throw IllegalArgumentException("Produk dengan ID ${d.produkId} tidak ditemukan.")

                        PenjualanDetailTable.insert { dt ->
                            dt[entitasId] = entitasIdFromJwt
                            dt[penjualanId] = EntityID(id, PenjualanTable)
                            dt[produkId] = EntityID(UUID.fromString(d.produkId), ProdukTable)
                            dt[hargaModal] = produkRow[ProdukTable.hargaModal]
                            dt[hargaJual] = d.hargaJual
                            dt[jumlah] = d.jumlah.toInt()
                            dt[subtotal] = d.subtotal
                            dt[potensiLaba] = (d.hargaJual - produkRow[ProdukTable.hargaModal]) * d.jumlah
                            dt[satuan] = d.satuan
                        }
                        ProdukTable.update({ ProdukTable.id eq EntityID(UUID.fromString(d.produkId), ProdukTable) }) {
                            val currentStok = ProdukTable.select { ProdukTable.id eq EntityID(UUID.fromString(d.produkId), ProdukTable) }.single()[stok]
                            it[stok] = currentStok.minus(d.jumlah.toDouble())
                        }
                    }

                    val oldPenjualan = PenjualanTable.select { PenjualanTable.id eq id }.singleOrNull()
                    val oldMetodePembayaran = oldPenjualan?.get(PenjualanTable.metodePembayaran)
                    val now = LocalDateTime.now()

                    if (oldMetodePembayaran != req.metodePembayaran) {
                        if (oldMetodePembayaran.equals("PIUTANG", true)) {
                            PiutangPelangganTable.deleteWhere { penjualanId eq EntityID(id, PenjualanTable) }
                        }
                        if (req.metodePembayaran.equals("PIUTANG", true)) {
                            PiutangPelangganTable.insert { pt ->
                                pt[entitasId] = entitasIdFromJwt
                                pt[penjualanId] = EntityID(id, PenjualanTable)
                                pt[pelangganId] = EntityID(UUID.fromString(req.pelangganId!!), PelangganTable)
                                pt[tanggal] = LocalDate.parse(req.tanggal)
                                req.jatuhTempo?.let { pt[tanggalJatuhTempo] = LocalDate.parse(it).atStartOfDay() }
                                pt[totalPiutang] = req.total
                                pt[sisaPiutang] = req.total
                                pt[status] = "BELUM_LUNAS"
                                pt[fotoNotaUrl] = oldPenjualan?.get(PenjualanTable.notaUrl)
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
                            it[totalPiutang] = req.total
                            it[sisaPiutang] = req.total
                            req.jatuhTempo?.let { jt -> it[tanggalJatuhTempo] = LocalDate.parse(jt).atStartOfDay() }
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

                        KomisiSalesTable.deleteWhere { penjualanId eq EntityID(id, PenjualanTable) }

                        if (nominalKomisi > 0) {
                            KomisiSalesTable.insert { ks ->
                                ks[entitasId] = entitasIdFromJwt
                                ks[penjualanId] = EntityID(id, PenjualanTable)
                                ks[salesId] = EntityID(UUID.fromString(req.salesId), SalesTable)
                                ks[KomisiSalesTable.komisiPersen] = komisiPersen ?: 0.0
                                ks[KomisiSalesTable.nominalKomisi] = nominalKomisi
                                ks[status] = if (req.metodePembayaran.equals("TUNAI", true)) "DIBAYAR" else "PENDING"
                                ks[tanggalKomisi] = now
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
                val entitasIdFromJwt = try {
                    getEntitasIdFromJwt()
                } catch (e: IllegalArgumentException) {
                    return@delete call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
                }
                val id = runCatching { UUID.fromString(call.parameters["id"]!!) }
                    .getOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))

                val deletedPenjualanCount = transaction {
                    val oldDetails = PenjualanDetailTable
                        .select { PenjualanDetailTable.penjualanId eq EntityID(id, PenjualanTable) }
                        .map { row -> row[PenjualanDetailTable.produkId]!!.value to row[PenjualanDetailTable.jumlah] }

                    oldDetails.forEach { (produkId, jumlah) ->
                        ProdukTable.update({ ProdukTable.id eq EntityID(produkId, ProdukTable) }) {
                            val currentStok = ProdukTable.select { ProdukTable.id eq EntityID(produkId, ProdukTable) }.single()[stok]
                            it[stok] = currentStok.plus(jumlah.toDouble())
                        }
                    }

                    PenjualanDetailTable.deleteWhere {
                        (penjualanId eq EntityID(id, PenjualanTable)) and
                                (entitasId eq entitasIdFromJwt)
                    }
                    PiutangPelangganTable.deleteWhere {
                        (penjualanId eq EntityID(id, PenjualanTable)) and
                                (entitasId eq entitasIdFromJwt)
                    }
                    KomisiSalesTable.deleteWhere {
                        (penjualanId eq EntityID(id, PenjualanTable)) and
                                (entitasId eq entitasIdFromJwt)
                    }
                    PenjualanTable.deleteWhere {
                        (PenjualanTable.id eq EntityID(id, PenjualanTable)) and
                                (entitasId eq entitasIdFromJwt)
                    }
                }
                val httpStatusCode = if (deletedPenjualanCount == 0) HttpStatusCode.NotFound else HttpStatusCode.NoContent
                val responseBody = if (deletedPenjualanCount == 0) mapOf("error" to "Penjualan tidak ditemukan atau tidak memiliki akses") else null
                if (responseBody != null) {
                    call.respond(httpStatusCode, responseBody)
                } else {
                    call.respond(httpStatusCode)
                }
            }
        }
    }
}

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