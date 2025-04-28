package com.kasir.routes

import com.kasir.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

data class PembayaranHutangRequest(
    val hutangId: UUID,
    val jumlahBayar: Double,
    val kasId: UUID
)

fun Route.hutangRoutes() {
    route("/hutang") {

        post("/bayar") {
            try {
                val request = call.receive<PembayaranHutangRequest>()

                transaction {
                    val sisaHutang = HutangSupplierTable
                        .select { HutangSupplierTable.id eq request.hutangId }
                        .firstOrNull()
                        ?.get(HutangSupplierTable.sisaHutang) ?: 0.0

                    if (request.jumlahBayar > sisaHutang) {
                        throw IllegalArgumentException("Jumlah pembayaran melebihi sisa hutang!")
                    }

                    // Simpan pembayaran ke tabel pembayaran_hutang_supplier
                    PembayaranHutangSupplierTable.insert {
                        it[id] = UUID.randomUUID()
                        it[hutangId] = request.hutangId
                        it[jumlahBayar] = request.jumlahBayar
                        it[tanggalBayar] = LocalDateTime.now()
                    }

                    // Insert transaksi kas
                    KasTransaksiTable.insert {
                        it[id] = UUID.randomUUID()
                        it[kasId] = request.kasId
                        it[tanggal] = LocalDateTime.now()
                        it[keterangan] = "Pembayaran Hutang"
                        it[jumlah] = request.jumlahBayar
                        it[tipe] = "KELUAR"
                    }

                    // Update saldo kas
                    KasTable.update({ KasTable.id eq request.kasId }) {
                        with(SqlExpressionBuilder) {
                            it[KasTable.saldoAkhir] = KasTable.saldoAkhir - request.jumlahBayar
                        }
                    }
                }

                call.respondText("Pembayaran berhasil disimpan", status = HttpStatusCode.Created)
            } catch (e: IllegalArgumentException) {
                call.respondText(e.message ?: "Terjadi kesalahan", status = HttpStatusCode.BadRequest)
            } catch (e: Exception) {
                call.respondText("Gagal menyimpan pembayaran: ${e.localizedMessage}", status = HttpStatusCode.InternalServerError)
            }
        }
    }
}
