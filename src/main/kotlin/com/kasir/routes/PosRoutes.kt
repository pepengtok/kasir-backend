package com.kasir.routes

import com.kasir.models.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.javatime.datetime

data class PenjualanRequest(
    val pelangganId: UUID?,
    val salesId: UUID?,
    val total: Double,
    val metodePembayaran: String,
    val kasId: UUID,
    val detail: List<DetailItemRequest>
)

data class DetailItemRequest(
    val produkId: UUID,
    val hargaJual: Double,
    val jumlah: Int,
    val subtotal: Double
)

fun Route.posRoutes() {
    route("/pos") {

        post {
            val request = call.receive<PenjualanRequest>()
            val penjualanId = UUID.randomUUID()

            transaction {
                // Insert header penjualan
                PenjualanTable.insert {
                    it[id] = penjualanId
                    it[tanggal] = LocalDateTime.now()
                    it[pelangganId] = request.pelangganId
                    it[salesId] = request.salesId
                    it[total] = request.total
                    it[metodePembayaran] = request.metodePembayaran
                    it[status] = if (request.metodePembayaran == "KONTAN") "LUNAS" else "BELUM_LUNAS"
                    it[kasId] = request.kasId
                }

                // Insert detail per item
                request.detail.forEach { item ->
                    PenjualanDetailTable.insert {
                        it[id] = UUID.randomUUID()
                        it[PenjualanDetailTable.penjualanId] = penjualanId
                        it[produkId] = item.produkId
                        it[hargaJual] = item.hargaJual
                        it[jumlah] = item.jumlah
                        it[subtotal] = item.subtotal
                    }

                    // Update stok produk (kurangi stok)
                    ProdukTable.update({ ProdukTable.id eq item.produkId }) {
                        it.update(ProdukTable.stok, ProdukTable.stok.minus(item.jumlah))
                    }
                }

                // Tambahkan pemasukan kas kalau kontan
                if (request.metodePembayaran == "KONTAN") {
                    KasTransaksiTable.insert {
                        it[id] = UUID.randomUUID()
                        it[kasId] = request.kasId
                        it[tanggal] = LocalDateTime.now()
                        it[keterangan] = "Pembayaran Penjualan Kontan"
                        it[jumlah] = request.total
                        it[tipe] = "MASUK"
                    }

                    // Update saldo kas
                    KasTable.update({ KasTable.id eq request.kasId }) {
                        it.update(KasTable.saldoAkhir, KasTable.saldoAkhir.plus(request.total))
                    }
                }
            }

            call.respondText("Transaksi berhasil disimpan!", status = io.ktor.http.HttpStatusCode.Created)
        }
    }
}
