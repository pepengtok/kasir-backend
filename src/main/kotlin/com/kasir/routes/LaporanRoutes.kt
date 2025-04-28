package com.kasir.routes

import com.kasir.models.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Untuk filter laporan
data class FilterRequest(val tipe: String, val tanggal: String?)

fun Route.laporanRoutes() {
    route("/laporan") {

        // Fungsi bantu buat range tanggal
        fun generateRange(tipe: String, tanggalStr: String?): Pair<LocalDateTime, LocalDateTime> {
            val tanggal = LocalDate.parse(tanggalStr ?: "", DateTimeFormatter.ISO_DATE)
            return when (tipe) {
                "HARIAN" -> {
                    val start = tanggal.atStartOfDay()
                    val end = start.plusDays(1)
                    start to end
                }
                "BULANAN" -> {
                    val start = tanggal.withDayOfMonth(1).atStartOfDay()
                    val end = start.plusMonths(1)
                    start to end
                }
                "TAHUNAN" -> {
                    val start = tanggal.withDayOfYear(1).atStartOfDay()
                    val end = start.plusYears(1)
                    start to end
                }
                else -> {
                    // Semua data
                    LocalDateTime.MIN to LocalDateTime.MAX
                }
            }
        }

        // 🔥 Laporan Penjualan
        post("/penjualan") {
            val request = call.receive<FilterRequest>()
            val (start, end) = generateRange(request.tipe, request.tanggal)

            val data = transaction {
                PenjualanTable.select {
                    PenjualanTable.tanggal.between(start, end)
                }.map {
                    it[PenjualanTable.total]
                }
            }
            call.respond(data)
        }

        // 🔥 Laporan Pembelian
        post("/pembelian") {
            val request = call.receive<FilterRequest>()
            val (start, end) = generateRange(request.tipe, request.tanggal)

            val data = transaction {
                PembelianTable.select {
                    PembelianTable.tanggal.between(start, end)
                }.map {
                    it[PembelianTable.total]
                }
            }
            call.respond(data)
        }

        // 🔥 Laporan Arus Kas
        post("/kas") {
            val request = call.receive<FilterRequest>()
            val (start, end) = generateRange(request.tipe, request.tanggal)

            val data = transaction {
                KasTransaksiTable.select {
                    KasTransaksiTable.tanggal.between(start, end)
                }.map {
                    it[KasTransaksiTable.jumlah]
                }
            }
            call.respond(data)
        }

        // 🔥 Laporan Stok Barang
        get("/stok") {
            val data = transaction {
                ProdukTable.selectAll().map {
                    mapOf(
                        "nama_produk" to it[ProdukTable.namaProduk],
                        "stok" to it[ProdukTable.stok],
                        "harga_modal" to it[ProdukTable.hargaModal]
                    )

                }
            }
            call.respond(data)
        }

        // 🔥 Laporan Laba Rugi
        post("/laba-rugi") {
            val request = call.receive<FilterRequest>()
            val (start, end) = generateRange(request.tipe, request.tanggal)

            val totalPenjualan = transaction {
                PenjualanTable.select {
                    PenjualanTable.tanggal.between(start, end)
                }.sumOf { it[PenjualanTable.total] }
            }

            val totalPembelian = transaction {
                PembelianTable.select {
                    PembelianTable.tanggal.between(start, end)
                }.sumOf { it[PembelianTable.total] }
            }

            val labaRugi = totalPenjualan - totalPembelian
            call.respond(mapOf("laba_rugi" to labaRugi))
        }
    }
}
