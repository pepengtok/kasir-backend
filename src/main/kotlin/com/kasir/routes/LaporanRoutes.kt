// src/main/kotlin/com/kasir/routes/LaporanRoutes.kt
package com.kasir.routes

import com.kasir.dto.*
import com.kasir.models.*
import com.kasir.service.*
import com.kasir.util.generateRange
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import org.jetbrains.exposed.dao.id.EntityID
import io.ktor.server.application.log

// --- IMPOR EKSPILISIT UNTUK Exposed ---
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.between
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.JoinType // Pastikan JoinType terimpor

fun Route.laporanRoutes(service: LaporanService) {
    println("⚙️ REGISTERED /admin/laporan routes — received LaporanService = $service")
    route("/laporan") { // Ini adalah route dasar untuk semua laporan, menghasilkan /admin/laporan

        // ==== Laporan Hutang Supplier ====
        get("/hutang-supplier") {
            val status = call.request.queryParameters["status"]
            val entitasIdParam = call.request.queryParameters["entitas_id"]
                ?: run {
                    call.respond(HttpStatusCode.BadRequest, "entitas_id wajib diisi")
                    return@get
                }
            val entitasId = EntityID(UUID.fromString(entitasIdParam), EntitasUsahaTable)

            val list = transaction {
                // Sintaks Join yang lebih eksplisit
                HutangSupplierTable.join(SupplierTable, JoinType.INNER) {
                    HutangSupplierTable.supplierId eq SupplierTable.id
                }.let { query ->
                    val base = if (status.isNullOrBlank()) query.selectAll()
                    else query.select { HutangSupplierTable.status eq status }

                    base.andWhere { HutangSupplierTable.entitasId eq entitasId }
                }.map { row ->
                    HutangSupplierEntryDto(
                        supplier           = row[SupplierTable.namaSupplier],
                        tanggalJatuhTempo= row[HutangSupplierTable.tanggalJatuhTempo].toString(),
                        totalHutang      = row[HutangSupplierTable.totalHutang],
                        sisaHutang       = row[HutangSupplierTable.sisaHutang],
                        status           = row[HutangSupplierTable.status],
                        pembelianId = row[HutangSupplierTable.pembelianId].value.toString(),
                        noFaktur         = null,
                        fotoNota         = row[HutangSupplierTable.fotoNotaUrl],
                        notaUrl          = row[HutangSupplierTable.fotoNotaUrl],
                        entitasId        = row[HutangSupplierTable.entitasId].value.toString()
                    )
                }
            }
            call.respond(HttpStatusCode.OK, list)
        }

        // -- Hutang Bank -- (Tidak ada join, jadi tidak perlu diubah)
        get("/hutang-bank") {
            val status = call.request.queryParameters["status"]
            val entitasIdParam = call.request.queryParameters["entitas_id"]
                ?: run {
                    call.respond(HttpStatusCode.BadRequest, "entitas_id wajib diisi")
                    return@get
                }
            val entitasId = EntityID(UUID.fromString(entitasIdParam), EntitasUsahaTable)

            val list: List<HutangBankEntryDto> = transaction {
                val base = HutangBankTable
                val q = if (status.isNullOrBlank()) base.selectAll()
                else base.select { base.status eq status }

                q.andWhere { base.entitasId eq entitasId }

                q.map { r ->
                    HutangBankEntryDto(
                        bank              = r[HutangBankTable.bankName],
                        tanggalJatuhTempo = r[HutangBankTable.tanggalJatuhTempo].toString(),
                        totalHutang       = r[HutangBankTable.totalHutang],
                        sisaHutang        = r[HutangBankTable.sisaHutang],
                        status            = r[HutangBankTable.status],
                        entitasId         = r[HutangBankTable.entitasId].value.toString()
                    )
                }
            }
            call.respond(HttpStatusCode.OK, list)
        }


        // -- Hutang Lain -- (Tidak ada join, jadi tidak perlu diubah)
        get("/hutang-lain") {
            val status = call.request.queryParameters["status"]
            val entitasIdParam = call.request.queryParameters["entitas_id"]
                ?: run {
                    call.respond(HttpStatusCode.BadRequest, "entitas_id wajib diisi")
                    return@get
                }
            val entitasId = EntityID(UUID.fromString(entitasIdParam), EntitasUsahaTable)

            val list: List<HutangLainEntryDto> = transaction {
                val base = HutangLainTable
                val q = if (status.isNullOrBlank()) base.selectAll()
                else base.select { base.status eq status }

                q.andWhere { base.entitasId eq entitasId }

                q.map { r ->
                    HutangLainEntryDto(
                        keterangan        = r[HutangLainTable.keterangan],
                        tanggalJatuhTempo = r[HutangLainTable.tanggalJatuhTempo].toString(),
                        totalHutang       = r[HutangLainTable.totalHutang],
                        sisaHutang        = r[HutangLainTable.sisaHutang],
                        status            = r[HutangLainTable.status],
                        entitasId         = r[HutangLainTable.entitasId].value.toString()
                    )
                }
            }
            call.respond(HttpStatusCode.OK, list)
        }


        // ==== Laporan Piutang Pelanggan ====
        get("/piutang/pelanggan") {
            val statusFilter = call.request.queryParameters["status"]
            val entitasIdParam = call.request.queryParameters["entitas_id"]
                ?: run {
                    call.respond(HttpStatusCode.BadRequest, "entitas_id wajib diisi")
                    return@get
                }
            val entitasId = EntityID(UUID.fromString(entitasIdParam), EntitasUsahaTable)

            val query = PiutangPelangganTable.join(
                PelangganTable, JoinType.INNER
            ) { PiutangPelangganTable.pelangganId eq PelangganTable.id } // Gunakan on clause eksplisit

            val list = transaction {
                val q = if (statusFilter.isNullOrBlank()) query.selectAll()
                else query.select { PiutangPelangganTable.status eq statusFilter }

                q.andWhere { PiutangPelangganTable.entitasId eq entitasId }

                q.map { row ->
                    PiutangPelangganEntryDto(
                        pelanggan         = row[PelangganTable.namaPelanggan],
                        tanggalJatuhTempo = row[PiutangPelangganTable.tanggalJatuhTempo].toString(),
                        totalPiutang      = row[PiutangPelangganTable.totalPiutang],
                        sisaPiutang       = row[PiutangPelangganTable.sisaPiutang],
                        status            = row[PiutangPelangganTable.status],
                        penjualanId       = row[PiutangPelangganTable.penjualanId].value.toString(),
                        noNota            = null,
                        notaUrl           = row[PiutangPelangganTable.fotoNotaUrl],
                        entitasId         = row[PiutangPelangganTable.entitasId].value.toString()
                    )
                }
            }

            call.respond(HttpStatusCode.OK, list)
        }


        // ==== Laporan Piutang Karyawan ====
        get("/piutang/karyawan") {
            val statusFilter = call.request.queryParameters["status"]
            val entitasIdParam = call.request.queryParameters["entitas_id"]
                ?: run {
                    call.respond(HttpStatusCode.BadRequest, "entitas_id wajib diisi")
                    return@get
                }
            val entitasId = EntityID(UUID.fromString(entitasIdParam), EntitasUsahaTable)

            val join = PiutangKaryawanTable
                .join(KaryawanTable, JoinType.INNER) { PiutangKaryawanTable.karyawanId eq KaryawanTable.id } // Gunakan on clause eksplisit

            val list = transaction {
                val slice = join.slice(
                    PiutangKaryawanTable.id,
                    KaryawanTable.nama,
                    PiutangKaryawanTable.tanggalJatuhTempo,
                    PiutangKaryawanTable.totalPiutang,
                    PiutangKaryawanTable.sisaPiutang,
                    PiutangKaryawanTable.status,
                    PiutangKaryawanTable.entitasId
                )
                val query = if (statusFilter.isNullOrBlank()) {
                    slice.selectAll()
                } else {
                    slice.select { PiutangKaryawanTable.status eq statusFilter }
                }.andWhere {
                    PiutangKaryawanTable.entitasId eq entitasId
                }

                query.map { row ->
                    PiutangKaryawanEntryDto(
                        id               = row[PiutangKaryawanTable.id].value.toString(),
                        pegawai          = row[KaryawanTable.nama],
                        tanggalJatuhTempo= row[PiutangKaryawanTable.tanggalJatuhTempo].toString(),
                        totalPiutang     = row[PiutangKaryawanTable.totalPiutang],
                        sisaPiutang      = row[PiutangKaryawanTable.sisaPiutang],
                        status           = row[PiutangKaryawanTable.status],
                        entitasId        = row[PiutangKaryawanTable.entitasId].value.toString()
                    )
                }
            }

            call.respond(HttpStatusCode.OK, list)
        }


        // ==== Laporan Piutang Lain ==== (Tidak ada join, tidak perlu diubah)
        get("/piutang/lain") {
            val statusFilter = call.request.queryParameters["status"]
            val entitasIdParam = call.request.queryParameters["entitas_id"]
                ?: run {
                    call.respond(HttpStatusCode.BadRequest, "entitas_id wajib diisi")
                    return@get
                }
            val entitasId = EntityID(UUID.fromString(entitasIdParam), EntitasUsahaTable)

            val list = transaction {
                val base = PiutangLainTable
                val q = if (statusFilter.isNullOrBlank()) base.selectAll()
                else base.select { base.status eq statusFilter }

                q.andWhere { base.entitasId eq entitasId }

                q.map { r ->
                    PiutangLainEntryDto(
                        id               = r[base.id].value.toString(),
                        keterangan       = r[base.keterangan],
                        tanggalJatuhTempo= r[base.tanggalJatuhTempo].toString(),
                        totalPiutang     = r[base.totalPiutang],
                        sisaPiutang      = r[base.sisaPiutang],
                        status           = r[base.status],
                        entitasId        = r[base.entitasId].value.toString()
                    )
                }
            }
            call.respond(HttpStatusCode.OK, list)
        }


        // ==== Laporan Pembayaran Hutang ====
        get("/pembayaran-hutang") {
            val entitasIdParam = call.request.queryParameters["entitas_id"]
                ?: run {
                    call.respond(HttpStatusCode.BadRequest, "entitas_id wajib diisi")
                    return@get
                }
            val entitasId = EntityID(UUID.fromString(entitasIdParam), EntitasUsahaTable)

            val list = transaction {
                val j2 = PembayaranHutangSupplierTable.join(
                    HutangSupplierTable, JoinType.INNER
                ) { PembayaranHutangSupplierTable.hutangSupplierId eq HutangSupplierTable.id
                }.join(
                    SupplierTable, JoinType.INNER
                ) { HutangSupplierTable.supplierId eq SupplierTable.id }

                j2.select { HutangSupplierTable.entitasId eq entitasId }
                    .map { row ->
                        PembayaranHutangEntryDto(
                            tanggal    = row[PembayaranHutangSupplierTable.tanggalBayar].toString(),
                            supplier   = row[SupplierTable.namaSupplier],
                            jumlah     = row[PembayaranHutangSupplierTable.jumlahBayar],
                            keterangan = row[PembayaranHutangSupplierTable.keterangan],
                            entitasId  = row[HutangSupplierTable.entitasId].value.toString()
                        )
                    }
            }
            call.respond(HttpStatusCode.OK, list)
        }


        // ==== Laporan Pembayaran Piutang ====
        get("/pembayaran-piutang") {
            val entitasIdParam = call.request.queryParameters["entitas_id"]
                ?: run {
                    call.respond(HttpStatusCode.BadRequest, "entitas_id wajib diisi")
                    return@get
                }
            val entitasId = EntityID(UUID.fromString(entitasIdParam), EntitasUsahaTable)

            val list = transaction {
                val join1 = PembayaranPiutangPelangganTable.join(
                    PiutangPelangganTable, JoinType.INNER
                ) { PembayaranPiutangPelangganTable.piutangId eq PiutangPelangganTable.id
                }.join(
                    PelangganTable, JoinType.INNER
                ) { PiutangPelangganTable.pelangganId eq PelangganTable.id }

                join1.select { PiutangPelangganTable.entitasId eq entitasId }
                    .map { row ->
                        PembayaranPiutangEntryDto(
                            tanggal    = row[PembayaranPiutangPelangganTable.tanggalBayar].toString(),
                            pelanggan  = row[PelangganTable.namaPelanggan],
                            jumlah     = row[PembayaranPiutangPelangganTable.jumlahBayar],
                            keterangan = row[PembayaranPiutangPelangganTable.keterangan],
                            entitasId  = row[PiutangPelangganTable.entitasId].value.toString()
                        )
                    }
            }
            call.respond(HttpStatusCode.OK, list)
        }

        // 🔥 Laporan Stok Barang (Tidak ada join, tidak perlu diubah)
        get("/persediaan") {
            val entitasIdParam = call.request.queryParameters["entitas_id"]
                ?: run {
                    call.respond(HttpStatusCode.BadRequest, "entitas_id wajib diisi")
                    return@get
                }
            val entitasId = EntityID(UUID.fromString(entitasIdParam), EntitasUsahaTable)

            val list = transaction {
                ProdukTable
                    .select { ProdukTable.entitasId eq entitasId }
                    .map { row ->
                        StokBarangEntryDto(
                            id            = row[ProdukTable.id].value.toString(),
                            nama_produk   = row[ProdukTable.namaProduk],
                            stok_masuk    = null,
                            stok_keluar   = null,
                            stok_terakhir = row[ProdukTable.stok],
                            harga_modal   = row[ProdukTable.hargaModal],
                            total_modal   = row[ProdukTable.stok] * row[ProdukTable.hargaModal]
                        )
                    }
            }
            call.respond(list)
        }


        // Endpoint filter persediaan
        post("/persediaan-filter") {
            val filter = call.receive<StokFilterRequestDto>()
            val entitasId = EntityID(UUID.fromString(filter.entitasId), EntitasUsahaTable)

            val list = transaction {
                ProdukTable
                    .select { ProdukTable.entitasId eq entitasId }
                    .map { row ->
                        val produkId = row[ProdukTable.id].value

                        // Query untuk stok masuk dengan filter tanggal
                        val totalPembelian = if (filter.tanggalMulai != null && filter.tanggalAkhir != null) {
                            try {
                                val tanggalMulai = LocalDate.parse(filter.tanggalMulai).atStartOfDay()
                                val tanggalAkhir = LocalDate.parse(filter.tanggalAkhir).atEndOfDay()
                                val pembelianSum = PembelianDetailTable.join(PembelianTable, JoinType.INNER) { PembelianDetailTable.pembelianId eq PembelianTable.id }
                                    .slice(PembelianDetailTable.jumlah.sum())
                                    .select {
                                        (PembelianDetailTable.produkId eq EntityID(produkId, ProdukTable)) and
                                                (PembelianTable.entitasId eq entitasId) and
                                                (PembelianTable.tanggal.between(tanggalMulai, tanggalAkhir))
                                    }
                                    .firstOrNull()?.get(PembelianDetailTable.jumlah.sum())?.toDouble() ?: 0.0
                                pembelianSum
                            } catch (e: Exception) {
                                application.log.error("Error querying pembelian: ${e.message}")
                                0.0
                            }
                        } else {
                            PembelianDetailTable.join(PembelianTable, JoinType.INNER) { PembelianDetailTable.pembelianId eq PembelianTable.id }
                                .slice(PembelianDetailTable.jumlah.sum())
                                .select {
                                    (PembelianDetailTable.produkId eq EntityID(produkId, ProdukTable)) and
                                            (PembelianTable.entitasId eq entitasId)
                                }
                                .firstOrNull()?.get(PembelianDetailTable.jumlah.sum())?.toDouble() ?: 0.0
                        }


                        // Query untuk stok keluar dengan filter tanggal
                        val totalPenjualan = if (filter.tanggalMulai != null && filter.tanggalAkhir != null) {
                            try {
                                val tanggalMulai = LocalDate.parse(filter.tanggalMulai).atStartOfDay()
                                val tanggalAkhir = LocalDate.parse(filter.tanggalAkhir).atEndOfDay()
                                val penjualanSum = PenjualanDetailTable.join(PenjualanTable, JoinType.INNER) { PenjualanDetailTable.penjualanId eq PenjualanTable.id }
                                    .slice(PenjualanDetailTable.jumlah.sum())
                                    .select {
                                        (PenjualanDetailTable.produkId eq EntityID(produkId, ProdukTable)) and
                                                (PenjualanTable.entitasId eq entitasId) and
                                                (PenjualanTable.tanggal.between(tanggalMulai, tanggalAkhir))
                                    }
                                    .firstOrNull()?.get(PenjualanDetailTable.jumlah.sum())?.toDouble() ?: 0.0
                                penjualanSum
                            } catch (e: Exception) {
                                application.log.error("Error querying penjualan: ${e.message}")
                                0.0
                            }
                        } else {
                            PenjualanDetailTable.join(PenjualanTable, JoinType.INNER) { PenjualanDetailTable.penjualanId eq PenjualanTable.id }
                                .slice(PenjualanDetailTable.jumlah.sum())
                                .select {
                                    (PenjualanDetailTable.produkId eq EntityID(produkId, ProdukTable)) and
                                            (PenjualanTable.entitasId eq entitasId)
                                }
                                .firstOrNull()?.get(PenjualanDetailTable.jumlah.sum())?.toDouble() ?: 0.0
                        }

                        StokBarangEntryDto(
                            id            = produkId.toString(),
                            nama_produk   = row[ProdukTable.namaProduk],
                            stok_masuk    = totalPembelian,
                            stok_keluar   = totalPenjualan,
                            stok_terakhir = row[ProdukTable.stok],
                            harga_modal   = row[ProdukTable.hargaModal],
                            total_modal   = row[ProdukTable.stok] * row[ProdukTable.hargaModal]
                        )
                    }
            }
            call.respond(list)
        }


        // ==== RIWAYAT PRODUK (Pembelian & Penjualan per produk) ====
        get("/riwayat-produk/{id}") {
            val uuid = runCatching { UUID.fromString(call.parameters["id"]!!) }
                .getOrElse { return@get call.respondText("Format UUID salah", status = HttpStatusCode.BadRequest) }

            val entitasIdParam = call.request.queryParameters["entitas_id"]
                ?: run {
                    call.respondText("entitas_id wajib diisi", status = HttpStatusCode.BadRequest)
                    return@get
                }
            val entitasId = EntityID(UUID.fromString(entitasIdParam), EntitasUsahaTable)

            // 🔹 Ambil pembelian berdasarkan produk + entitas
            val pembelian = transaction {
                PembelianDetailTable.join(PembelianTable, JoinType.INNER) { PembelianDetailTable.pembelianId eq PembelianTable.id }
                    .select {
                        (PembelianDetailTable.produkId eq EntityID(uuid, ProdukTable)) and
                                (PembelianTable.entitasId eq entitasId)
                    }
                    .map { row ->
                        RiwayatDetailDto(
                            tanggal = row[PembelianTable.tanggal].toString(),
                            jumlah = row[PembelianDetailTable.jumlah].toDouble(),
                            harga = row[PembelianDetailTable.hargaModal],
                            subtotal = row[PembelianDetailTable.subtotal]
                        )
                    }
            }


            // 🔹 Ambil penjualan berdasarkan produk + entitas
            val penjualan = transaction {
                PenjualanDetailTable.join(PenjualanTable, JoinType.INNER) { PenjualanDetailTable.penjualanId eq PenjualanTable.id }
                    .select {
                        (PenjualanDetailTable.produkId eq EntityID(uuid, ProdukTable)) and
                                (PenjualanTable.entitasId eq entitasId)
                    }
                    .map { row ->
                        RiwayatDetailDto(
                            tanggal = row[PenjualanTable.tanggal].toString(),
                            jumlah = row[PenjualanDetailTable.jumlah].toDouble(),
                            harga = row[PenjualanDetailTable.hargaJual], // Akses dari PenjualanDetailTable
                            subtotal = row[PenjualanDetailTable.subtotal]
                        )
                    }
            }

            call.respond(HttpStatusCode.OK, RiwayatResponseDto(pembelian, penjualan))
        }


        // 🔥 Laporan Komisi Sales
        post("/komisi") {
            val filter = call.receive<FilterRequestDto>()
            val (from, to) = generateRange(filter.tipe, filter.tanggal)
            val entitasId = EntityID(UUID.fromString(filter.entitasId), EntitasUsahaTable)

            val entries = transaction {
                val ks = KomisiSalesTable
                val pj = PenjualanTable
                val sales = SalesTable

                // Gunakan join berantai dengan sintaks yang eksplisit
                ks.join(pj, JoinType.INNER) { ks.penjualanId eq pj.id }
                    .join(sales, JoinType.INNER) { ks.salesId eq sales.id } // Join kedua
                    .slice(ks.columns + listOf(pj.id, pj.notaUrl, sales.nama))
                    .select {
                        (pj.entitasId eq entitasId) and
                                if (filter.tipe.equals("ALL", true)) Op.TRUE
                                else ks.tanggalKomisi.between(from, to)
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
                            canPay        = row[ks.status] == "PENDING",
                            notaId        = row[pj.id].value.toString(),
                            notaUrl       = row[pj.notaUrl],
                            entitasId     = row[ks.entitasId].value.toString()
                        )
                    }
            }

            call.respond(HttpStatusCode.OK, KomisiResponseDto(entries))
        }


        post("/detail-komisi") { // Path diubah
            val req = call.receive<DetailKomisiRequestDto>()
            val komisiUUID = UUID.fromString(req.komisiId)

            val komRow = transaction {
                KomisiSalesTable
                    .select { KomisiSalesTable.id eq komisiUUID }
                    .singleOrNull()
            }

            if (komRow == null) {
                call.respond(HttpStatusCode.NotFound, "Komisi tidak ditemukan")
                return@post
            }

            val penjualanUUID = komRow[KomisiSalesTable.penjualanId]

            val details = transaction {
                PenjualanDetailTable.join(PenjualanTable, JoinType.INNER) { PenjualanDetailTable.penjualanId eq PenjualanTable.id }
                    .select { PenjualanDetailTable.penjualanId eq penjualanUUID }
                    .map { row ->
                        TransactionDto(
                            id         = row[PenjualanDetailTable.id].value.toString(),
                            tanggal    = row[PenjualanTable.tanggal].toString(),
                            tipe       = "Penjualan",
                            jumlah     = row[PenjualanDetailTable.subtotal],
                            keterangan = row[PenjualanTable.noNota]
                        )
                    }
            }

            call.respond(HttpStatusCode.OK, details)
        }


        // 📄 Redirect Nota Penjualan (Tidak ada join, tidak perlu diubah)
        get("/nota/{penjualanId}") {
            val idParam = call.parameters["penjualanId"]
            if (idParam == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing penjualanId")
                return@get
            }
            // Ambil URL nota dari DB
            val notaPath: String? = transaction {
                PenjualanTable
                    .slice(PenjualanTable.notaUrl)
                    .select { PenjualanTable.id eq UUID.fromString(idParam) }
                    .singleOrNull()
                    ?.get(PenjualanTable.notaUrl)
            }
            if (notaPath.isNullOrBlank()) {
                call.respond(HttpStatusCode.NotFound, "Nota tidak ditemukan")
                return@get
            }
            // Redirect: absolute URL atau prefix Supabase
            if (notaPath.startsWith("http")) {
                call.respondRedirect(notaPath)
            } else {
                val supabaseBase = "https://xyz.supabase.co/storage/v1/object/public"
                call.respondRedirect("$supabaseBase$notaPath")
            }
        }

        // ==== RIWAYAT TRANSAKSI KAS ====
        post("/riwayat") {
            val filter = call.receive<FilterRequestDto>()
            val (from, to) = generateRange(filter.tipe, filter.tanggal)
            val entitasId = EntityID(UUID.fromString(filter.entitasId), EntitasUsahaTable)

            val list = transaction {
                KasTransaksiTable.join(KasTable, JoinType.INNER) { KasTransaksiTable.kasId eq KasTable.id }
                    .select {
                        (KasTransaksiTable.tanggal.between(from, to)) and
                                (KasTable.entitasId eq entitasId)
                    }
                    .orderBy(KasTransaksiTable.tanggal to SortOrder.ASC)
                    .map { row ->
                        RiwayatKasDto(
                            id         = row[KasTransaksiTable.id].value.toString(),
                            tanggal    = row[KasTransaksiTable.tanggal].toString(),
                            kasId      = row[KasTransaksiTable.kasId].value.toString(),
                            jumlah     = row[KasTransaksiTable.jumlah],
                            keterangan = row[KasTransaksiTable.keterangan],
                            tipe       = row[KasTransaksiTable.tipe],
                            entitasId  = row[KasTable.entitasId].value.toString()
                        )
                    }
            }

            call.respond(
                HttpStatusCode.OK,
                RiwayatKasResponseDto(data = list, entitasId = filter.entitasId)
            )
        }


        // ==== LAPORAN PEMBELIAN ====
        post("/pembelian") {
            val filter = call.receive<FilterRequestDto>()
            val entitasId = EntityID(UUID.fromString(filter.entitasId), EntitasUsahaTable)

            val mappedRows = transaction {
                val baseQuery = PembelianTable.join(SupplierTable, JoinType.INNER) { PembelianTable.supplierId eq SupplierTable.id }
                val rows = if (filter.tipe.uppercase() == "ALL") {
                    baseQuery.select { PembelianTable.entitasId eq entitasId }
                } else {
                    val (from, to) = generateRange(filter.tipe, filter.tanggal)
                    baseQuery.select {
                        (PembelianTable.tanggal.between(from, to)) and
                                (PembelianTable.entitasId eq entitasId)
                    }
                }

                rows.map { row ->
                    PembelianEntryDto(
                        id                = row[PembelianTable.id].value.toString(),
                        tanggal           = row[PembelianTable.tanggal].toString(),
                        noFaktur          = row[PembelianTable.noFaktur],
                        namaSupplier      = row[SupplierTable.namaSupplier],
                        metodePembayaran  = row[PembelianTable.metodePembayaran],
                        total             = row[PembelianTable.total],
                        entitasId         = row[PembelianTable.entitasId].value.toString()
                    )
                }
            }

            println("Total rows: ${mappedRows.size}")
            call.respond(HttpStatusCode.OK, mappedRows)
        }


        // Omset (Tidak ada join yang bermasalah, tidak perlu diubah)
        post("/omset") {
            val filter = call.receive<FilterRequestDto>()
            val (from, to) = generateRange(filter.tipe, filter.tanggal)
            val entitasId = EntityID(UUID.fromString(filter.entitasId), EntitasUsahaTable)

            val sumExpr = PenjualanTable.total.sum().alias("omset_sum")

            val entries = transaction {
                val base = PenjualanTable
                    .slice(PenjualanTable.salesId, sumExpr)
                    .select {
                        (PenjualanTable.entitasId eq entitasId) and
                                if (filter.tipe.equals("ALL", true)) Op.TRUE
                                else PenjualanTable.tanggal.between(from, to)
                    }
                    .groupBy(PenjualanTable.salesId)

                base.map { row ->
                    val sid = row[PenjualanTable.salesId]?.value?.toString() ?: "–"
                    OmsetEntryDto(
                        salesId   = sid,
                        total     = row[sumExpr] ?: 0.0,
                        entitasId = filter.entitasId
                    )
                }
            }

            call.respond(HttpStatusCode.OK, entries)
        }


        // ==== BIAYA (Laporan) ==== (Tidak ada join, tidak perlu diubah)
        get("/biaya") {
            val entitasParam = call.request.queryParameters["entitas_id"]
                ?: run {
                    call.respond(HttpStatusCode.BadRequest, "entitas_id wajib diisi")
                    return@get
                }

            val entitasId = EntityID(UUID.fromString(entitasParam), EntitasUsahaTable)

            val op = transaction {
                BiayaOperasionalTable
                    .select { BiayaOperasionalTable.entitasId eq entitasId }
                    .map { row ->
                        BiayaEntryDto(
                            id         = row[BiayaOperasionalTable.id].value.toString(),
                            tanggal    = row[BiayaOperasionalTable.tanggal].toString(),
                            nominal    = row[BiayaOperasionalTable.nominal],
                            jenis      = "operasional",
                            entitasId  = entitasParam
                        )
                    }
            }

            val non = transaction {
                BiayaNonOperasionalTable
                    .select { BiayaNonOperasionalTable.entitasId eq entitasId }
                    .map { row ->
                        BiayaEntryDto(
                            id         = row[BiayaNonOperasionalTable.id].value.toString(),
                            tanggal    = row[BiayaNonOperasionalTable.tanggal].toString(),
                            nominal    = row[BiayaNonOperasionalTable.nominal],
                            jenis      = "non-operasional",
                            entitasId  = entitasParam
                        )
                    }
            }

            call.respond(HttpStatusCode.OK, op + non)
        }


        // ==== LAPORAN LABA RUGI ==== (Hanya route dalam, tidak perlu diubah)
            post("/laba-rugi") {
                println("Received POST request to /admin/laporan/laba-rugi handler")
                val filter = call.receive<FilterRequestDto>()
                val (from, to) = generateRange(filter.tipe, filter.tanggal)
                val entitasId = EntityID(UUID.fromString(filter.entitasId), EntitasUsahaTable)

                // ---- Penjualan & Retur ----
                val grossPenjualan = transaction {
                    PenjualanTable
                        .select {
                            (PenjualanTable.entitasId eq entitasId) and
                                    (PenjualanTable.tanggal.between(from, to))
                        }
                        .sumOf { it[PenjualanTable.total] }
                }

                val totalReturPenjualan = transaction {
                    ReturPenjualanTable
                        .select {
                            (ReturPenjualanTable.entitasId eq entitasId) and
                                    (ReturPenjualanTable.tanggalRetur.between(from, to))
                        }
                        .sumOf { it[ReturPenjualanTable.jumlahRetur] }
                }

                val penjualanKotor = grossPenjualan
                val penjualanBersih = grossPenjualan - totalReturPenjualan


                // ---- Pembelian & Retur ----
                val grossPembelian = transaction {
                    PembelianTable
                        .select {
                            (PembelianTable.entitasId eq entitasId) and
                                    (PembelianTable.tanggal.between(from, to))
                        }
                        .sumOf { it[PembelianTable.total] }
                }

                val totalReturPembelian = transaction {
                    ReturPembelianTable
                        .select {
                            (ReturPembelianTable.entitasId eq entitasId) and
                                    (ReturPembelianTable.tanggalRetur.between(from, to))
                        }
                        .sumOf { it[ReturPembelianTable.jumlahRetur] }
                }

                val pembelianBersih = grossPembelian - totalReturPembelian

                // ---- Persediaan Awal & Akhir ----
                val persediaanAwal = transaction {
                    StockOpnameTable
                        .select {
                            (StockOpnameTable.entitasId eq entitasId) and
                                    (StockOpnameTable.tanggalOpname eq from)
                        }
                        .sumOf { row ->
                            val pid = row[StockOpnameTable.produkId]
                            val stokFisik = row[StockOpnameTable.stokFisik]
                            val hargaModal = ProdukTable
                                .select { ProdukTable.id eq pid }
                                .single()[ProdukTable.hargaModal]
                            stokFisik * hargaModal
                        }
                }

                val persediaanAkhir = transaction {
                    ProdukTable
                        .select { ProdukTable.entitasId eq entitasId }
                        .sumOf { it[ProdukTable.stok] * it[ProdukTable.hargaModal] }
                }

                val barangTersedia = persediaanAwal + pembelianBersih
                val hpp = barangTersedia - persediaanAkhir

                // ---- Biaya & Komisi ----
                val biayaOp = transaction {
                    BiayaOperasionalTable
                        .select { BiayaOperasionalTable.entitasId eq entitasId }
                        .sumOf { it[BiayaOperasionalTable.nominal] }
                }

                val biayaNonOp = transaction {
                    BiayaNonOperasionalTable
                        .select { BiayaNonOperasionalTable.entitasId eq entitasId }
                        .sumOf { it[BiayaNonOperasionalTable.nominal] }
                }

                val komisi = transaction {
                    KomisiSalesTable
                        .select { KomisiSalesTable.entitasId eq entitasId }
                        .sumOf { it[KomisiSalesTable.nominalKomisi] }
                }
                // ---- Laba ----
                val labaBruto = penjualanBersih - hpp
                val labaBersih = labaBruto - biayaOp - biayaNonOp - komisi

                call.respond(
                    HttpStatusCode.OK,
                    LabaRugiDto(
                        penjualanKotor      = penjualanKotor,
                        returPenjualan      = totalReturPenjualan,
                        penjualanBersih     = penjualanBersih,
                        pembelianGross      = grossPembelian,
                        returPembelian      = totalReturPembelian,
                        pembelianBersih     = pembelianBersih,
                        persediaanAwal      = persediaanAwal,
                        barangTersedia      = barangTersedia,
                        persediaanAkhir     = persediaanAkhir,
                        hpp                 = hpp,
                        labaBruto           = labaBruto,
                        biayaOperasional    = biayaOp,
                        biayaNonOperasional = biayaNonOp,
                        komisi              = komisi,
                        labaBersih          = labaBersih,
                        entitasId           = filter.entitasId
                    )
                )
            }

            // 2) Detail per‐transaksi (retur + semua)
            post("/detail-transaksi") { // Path diubah
                val filter = call.receive<FilterRequestDto>()
                val (from, to) = generateRange(filter.tipe, filter.tanggal)
                val details = service.getDetailTransactions(filter, from, to)
                call.respond(HttpStatusCode.OK, details)

        }
        // ==== NERACA ==== (Tidak ada join, tidak perlu diubah)
        get("/neraca") {
            val entitasParam = call.request.queryParameters["entitas_id"]
                ?: run {
                    call.respond(HttpStatusCode.BadRequest, "entitas_id wajib diisi")
                    return@get
                }
            val entitasId = EntityID(UUID.fromString(entitasParam), EntitasUsahaTable)

            val kas = transaction {
                KasTable
                    .select { KasTable.entitasId eq entitasId }
                    .sumOf { it[KasTable.saldoAkhir] }
            }

            val piutang = transaction {
                PiutangPelangganTable
                    .select { PiutangPelangganTable.entitasId eq entitasId }
                    .sumOf { it[PiutangPelangganTable.sisaPiutang] } +
                        PiutangKaryawanTable
                            .select { PiutangKaryawanTable.entitasId eq entitasId }
                            .sumOf { it[PiutangKaryawanTable.sisaPiutang] } +
                        PiutangLainTable
                            .select { PiutangLainTable.entitasId eq entitasId }
                            .sumOf { it[PiutangLainTable.sisaPiutang] }
            }

            val persediaan = transaction {
                ProdukTable
                    .select { ProdukTable.entitasId eq entitasId }
                    .sumOf { it[ProdukTable.stok] * it[ProdukTable.hargaModal] }
            }

            val hutang = transaction {
                HutangSupplierTable
                    .select { HutangSupplierTable.entitasId eq entitasId }
                    .sumOf { it[HutangSupplierTable.sisaHutang] } +
                        HutangBankTable
                            .select { HutangBankTable.entitasId eq entitasId }
                            .sumOf { it[HutangBankTable.sisaHutang] } +
                        HutangLainTable
                            .select { HutangLainTable.entitasId eq entitasId }
                            .sumOf { it[HutangLainTable.sisaHutang] }
            }

            val modal = transaction {
                ModalTable
                    .select { ModalTable.entitasId eq entitasId }
                    .sumOf { it[ModalTable.nominal] }
            }

            val prive = transaction {
                PriveTable
                    .select { PriveTable.entitasId eq entitasId }
                    .sumOf { it[PriveTable.nominal] }
            }

            call.respond(
                HttpStatusCode.OK,
                NeracaResponseDto(
                    aktiva     = NeracaAktiva(kas, piutang, persediaan),
                    pasiva     = NeracaPasiva(hutang, modal, prive),
                    entitasId  = entitasParam
                )
            )
        }


        // 🔥 Laporan Modal & Prive (Tidak ada join, tidak perlu diubah)
        get("/modal") {
            val entitasParam = call.request.queryParameters["entitas_id"]
                ?: run {
                    call.respond(HttpStatusCode.BadRequest, "entitas_id wajib diisi")
                    return@get
                }
            val entitasId = EntityID(UUID.fromString(entitasParam), EntitasUsahaTable)

            val response = transaction {
                val modals = ModalTable
                    .select { ModalTable.entitasId eq entitasId }
                    .map { row ->
                        ModalEntry(
                            id         = row[ModalTable.id].value.toString(),
                            tanggal    = row[ModalTable.tanggal].toString(),
                            kasId      = row[ModalTable.kasId].value.toString(),
                            nominal    = row[ModalTable.nominal],
                            entitasId  = entitasParam
                        )
                    }

                val kasOptions = KasTable
                    .select { KasTable.entitasId eq entitasId }
                    .map { row ->
                        KasOption(
                            id         = row[KasTable.id].value.toString(),
                            namaKas    = row[KasTable.namaKas],
                            entitasId  = entitasParam
                        )
                    }

                ModalResponseDto(
                    modals      = modals,
                    kasOptions  = kasOptions,
                    entitasId   = entitasParam
                )
            }

            call.respond(HttpStatusCode.OK, response)
        }

        get("/prive") {
            val entitasParam = call.request.queryParameters["entitas_id"]
                ?: run {
                    call.respond(HttpStatusCode.BadRequest, "entitas_id wajib diisi")
                    return@get
                }
            val entitasId = EntityID(UUID.fromString(entitasParam), EntitasUsahaTable)

            val list = transaction {
                PriveTable
                    .select { PriveTable.entitasId eq entitasId }
                    .map { row ->
                        PriveEntryDto(
                            id         = row[PriveTable.id].value.toString(),
                            tanggal    = row[PriveTable.tanggal].toString(),
                            kasId      = row[PriveTable.kasId].value.toString(),
                            nominal    = row[PriveTable.nominal],
                            entitasId  = entitasParam
                        )
                    }
            }

            call.respond(HttpStatusCode.OK, list)
        }
    }
}
// Utility untuk generate rentang waktu filter (harian/bulanan/tahunan)
private fun generateRange(
    tipe: String,
    tanggalStr: String?
): Pair<LocalDateTime, LocalDateTime> {
    val dateOnly = tanggalStr?.substringBefore('T')
        ?: return LocalDateTime.MIN to LocalDateTime.MAX

    val date = LocalDate.parse(dateOnly, DateTimeFormatter.ISO_DATE)

    return when (tipe.uppercase()) {
        "HARIAN" -> {
            val start = date.atStartOfDay()
            start to start.plusDays(1)
        }

        "BULANAN" -> {
            val start = date.withDayOfMonth(1).atStartOfDay()
            val end = start.plusMonths(1)
            start to end
        }

        "TAHUNAN" -> {
            val start = date.withDayOfYear(1).atStartOfDay()
            val end = start.plusYears(1)
            start to end
        }

        else -> LocalDateTime.MIN to LocalDateTime.MAX
    }
}

// Extension function untuk LocalDate agar mudah mendapatkan akhir hari
private fun LocalDate.atEndOfDay(): LocalDateTime {
    return this.plusDays(1).atStartOfDay().minusNanos(1)
}