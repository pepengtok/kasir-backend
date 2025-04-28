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

data class BayarKomisiRequest(
    val komisiId: UUID
)

fun Route.komisiRoutes() {
    route("/komisi") {

        // GET semua komisi sales
        get {
            val komisiList = transaction {
                KomisiSalesTable.selectAll().map {
                    KomisiSales(
                        id = it[KomisiSalesTable.id],
                        salesId = it[KomisiSalesTable.salesId],
                        penjualanId = it[KomisiSalesTable.penjualanId],
                        komisiPersen = it[KomisiSalesTable.komisiPersen],
                        nominalKomisi = it[KomisiSalesTable.nominalKomisi],
                        status = it[KomisiSalesTable.status],
                        tanggalKomisi = it[KomisiSalesTable.tanggalKomisi]
                    )
                }
            }
            call.respond(komisiList)
        }

        // POST tandai komisi sudah dibayar
        post("/bayar") {
            val request = call.receive<BayarKomisiRequest>()

            transaction {
                KomisiSalesTable.update({ KomisiSalesTable.id eq request.komisiId }) {
                    it[status] = "DIBAYAR"
                }
            }

            call.respondText("Komisi berhasil ditandai sebagai sudah dibayar!", status = io.ktor.http.HttpStatusCode.OK)
        }
    }
}
