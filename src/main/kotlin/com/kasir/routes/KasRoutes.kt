package com.kasir.routes

import com.kasir.models.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus

fun Route.kasRoutes() {
    route("/kas") {

        get {
            val kasList = transaction {
                KasTable.selectAll().map {
                    Kas(
                        id = it[KasTable.id],
                        namaKas = it[KasTable.namaKas],
                        saldoAkhir = it[KasTable.saldoAkhir]
                    )
                }
            }
            call.respond(kasList)
        }

        post {
            val request = call.receive<Kas>()
            transaction {
                KasTable.insert {
                    it[id] = request.id
                    it[namaKas] = request.namaKas
                    it[saldoAkhir] = request.saldoAkhir
                }
            }
            call.respondText("Kas berhasil ditambahkan!", status = io.ktor.http.HttpStatusCode.Created)
        }
    }

    route("/kas-transaksi") {

        post {
            val request = call.receive<KasTransaksi>()
            transaction {
                KasTransaksiTable.insert {
                    it[id] = request.id
                    it[kasId] = request.kasId
                    it[tanggal] = request.tanggal
                    it[keterangan] = request.keterangan
                    it[jumlah] = request.jumlah
                    it[tipe] = request.tipe
                }

                when (request.tipe) {
                    "MASUK" -> {
                        KasTable.update({ KasTable.id eq request.kasId }) {
                            it[saldoAkhir] = KasTable.saldoAkhir plus request.jumlah
                        }
                    }
                    "KELUAR" -> {
                        KasTable.update({ KasTable.id eq request.kasId }) {
                            it[saldoAkhir] = KasTable.saldoAkhir minus request.jumlah
                        }
                    }
                    else -> { /* Kosongkan jika tipe tidak valid */ }
                }
            }
            call.respondText("Transaksi kas berhasil ditambahkan!", status = io.ktor.http.HttpStatusCode.Created)
        }
    }

    route("/kas-transfer") {

        post {
            val request = call.receive<KasTransfer>()
            transaction {
                KasTransferTable.insert {
                    it[id] = request.id
                    it[kasAsalId] = request.kasAsalId
                    it[kasTujuanId] = request.kasTujuanId
                    it[tanggal] = request.tanggal
                    it[jumlah] = request.jumlah
                }

                KasTable.update({ KasTable.id eq request.kasAsalId }) {
                    it[saldoAkhir] = KasTable.saldoAkhir minus request.jumlah
                }

                KasTable.update({ KasTable.id eq request.kasTujuanId }) {
                    it[saldoAkhir] = KasTable.saldoAkhir plus request.jumlah
                }
            }
            call.respondText("Transfer kas berhasil diproses!", status = io.ktor.http.HttpStatusCode.Created)
        }
    }
}
