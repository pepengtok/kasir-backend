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
import org.jetbrains.exposed.sql.javatime.datetime

data class PembayaranPiutangRequest(
    val piutangId: UUID,
    val jumlahBayar: Double,
    val kasId: UUID
)

fun Route.piutangRoutes() {
    route("/piutang") {

        post("/bayar") {
            val request = call.receive<PembayaranPiutangRequest>()

            val pembayaranBerhasil = transaction {
                val sisaPiutang = PiutangPelangganTable.select { PiutangPelangganTable.id eq request.piutangId }
                    .firstOrNull()
                    ?.get(PiutangPelangganTable.sisaPiutang)
                    ?: 0.0

                if (request.jumlahBayar > sisaPiutang) {
                    false
                } else {
                    // Insert pembayaran ke tabel pembayaran_piutang_pelanggan
                    PembayaranPiutangPelangganTable.insert {
                        it[id] = UUID.randomUUID()
                        it[piutangId] = request.piutangId
                        it[jumlahBayar] = request.jumlahBayar
                        it[tanggalBayar] = LocalDateTime.now()
                    }

                    // Insert pemasukan ke kas
                    KasTransaksiTable.insert {
                        it[id] = UUID.randomUUID()
                        it[kasId] = request.kasId
                        it[tanggal] = LocalDateTime.now()
                        it[keterangan] = "Pembayaran Piutang"
                        it[jumlah] = request.jumlahBayar
                        it[tipe] = "MASUK"
                    }

                    // Update saldo kas
                    KasTable.update({ KasTable.id eq request.kasId }) {
                        it.update(KasTable.saldoAkhir, KasTable.saldoAkhir.plus(request.jumlahBayar))
                    }

                    true
                }
            }

            if (!pembayaranBerhasil) {
                call.respondText("Jumlah pembayaran melebihi sisa piutang!", status = io.ktor.http.HttpStatusCode.BadRequest)
            } else {
                call.respondText("Pembayaran piutang berhasil!", status = io.ktor.http.HttpStatusCode.OK)
            }
        }
    }
}
