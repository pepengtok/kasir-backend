package com.kasir.routes

import com.kasir.models.StockOpname
import com.kasir.models.StockOpnameTable
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

fun Route.stockOpnameRoutes() {
    route("/stock-opname") {

        // GET semua stock opname
        get {
            val opnameList = transaction {
                StockOpnameTable.selectAll().map {
                    StockOpname(
                        id = it[StockOpnameTable.id],
                        tanggalOpname = it[StockOpnameTable.tanggalOpname],
                        produkId = it[StockOpnameTable.produkId],
                        stokSistem = it[StockOpnameTable.stokSistem],
                        stokFisik = it[StockOpnameTable.stokFisik],
                        selisih = it[StockOpnameTable.selisih]
                    )
                }
            }

            call.respond(opnameList)
        }

        // POST tambah stock opname
        post {
            val request = call.receive<StockOpname>()
            transaction {
                StockOpnameTable.insert {
                    it[id] = request.id
                    it[tanggalOpname] = request.tanggalOpname
                    it[produkId] = request.produkId
                    it[stokSistem] = request.stokSistem
                    it[stokFisik] = request.stokFisik
                    it[selisih] = request.selisih
                }
            }
            call.respondText("Stock Opname berhasil dicatat!", status = io.ktor.http.HttpStatusCode.Created)
        }
    }
}
