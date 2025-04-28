package com.kasir.routes

import com.kasir.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*

data class PembelianRequest(
    val supplierId: UUID,
    val total: Double,
    val metodePembayaran: String, // TUNAI / KREDIT
    val kasId: UUID,
    val detail: List<DetailItemRequest>
)

fun Route.pembelianRoutes() {
    route("/pembelian") {

        post {
            val request = call.receive<PembelianRequest>()
            val pembelianId = UUID.randomUUID()

            transaction {
                // Insert ke header pembelian
                PembelianTable.insert {
                    it[id] = pembelianId
                    it[supplierId] = request.supplierId
                    it[total] = request.total
                    it[tanggal] = LocalDateTime.now()
                    it[metodePembayaran] = request.metodePembayaran
                    it[status] = if (request.metodePembayaran == "TUNAI") "LUNAS" else "BELUM_LUNAS"
                }

                // Insert detail barang yang dibeli
                request.detail.forEach { item ->
                    PembelianDetailTable.insert {
                        it[id] = UUID.randomUUID()
                        it[HutangSupplierTable.pembelianId] = pembelianId
                        it[produkId] = item.produkId
                        it[hargaBeli] = item.hargaJual // pakai harga jual dari request karena belum ada harga beli
                        it[jumlah] = item.jumlah
                        it[subtotal] = item.subtotal
                    }

                    // Tambahkan stok produk
                    ProdukTable.update({ ProdukTable.id eq item.produkId }) {
                        it[stok] = ProdukTable.stok + item.jumlah
                    }
                }

                // Jika TUNAI maka kurangi kas
                if (request.metodePembayaran == "TUNAI") {
                    KasTransaksiTable.insert {
                        it[id] = UUID.randomUUID()
                        it[kasId] = request.kasId
                        it[tanggal] = LocalDateTime.now()
                        it[keterangan] = "Pembayaran Pembelian Tunai"
                        it[jumlah] = request.total
                        it[tipe] = "KELUAR"
                    }

                    KasTable.update({ KasTable.id eq request.kasId }) {
                        it[saldoAkhir] = KasTable.saldoAkhir - request.total
                    }
                }

                // Jika KREDIT maka tambahkan hutang ke supplier
                if (request.metodePembayaran == "KREDIT") {
                    HutangSupplierTable.insert {
                        it[id] = UUID.randomUUID()
                        it[supplierId] = request.supplierId
                        it[totalHutang] = request.total
                        it[sisaHutang] = request.total
                        it[tanggal] = LocalDateTime.now()
                    }
                }
            }

            call.respondText("Pembelian berhasil disimpan!", status = HttpStatusCode.Created)
        }
    }
}
