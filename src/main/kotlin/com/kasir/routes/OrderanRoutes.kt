package com.kasir.routes

import com.kasir.models.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.util.UUID

data class OrderanRequest(
    val salesId: UUID,
    val pelangganId: UUID?,
    val total: Double,
    val detail: List<DetailItemRequest>
)

data class ApproveOrderanRequest(
    val orderanId: UUID
)

fun Route.orderanRoutes() {
    route("/orderan") {

        // Sales input order baru
        post {
            val request = call.receive<OrderanRequest>()
            val orderanId = UUID.randomUUID()

            transaction {
                OrderanSalesTable.insert {
                    it[id] = orderanId
                    it[salesId] = request.salesId
                    it[pelangganId] = request.pelangganId
                    it[total] = request.total
                    it[status] = "MENUNGGU"
                    it[tanggalOrder] = LocalDateTime.now()
                }

                request.detail.forEach { item ->
                    OrderanSalesDetailTable.insert {
                        it[id] = UUID.randomUUID()
                        it[OrderanSalesDetailTable.orderanId] = orderanId
                        it[produkId] = item.produkId
                        it[hargaJual] = item.hargaJual
                        it[jumlah] = item.jumlah
                        it[subtotal] = item.subtotal
                    }
                }
            }

            call.respondText("Orderan berhasil dibuat dan menunggu persetujuan.", status = io.ktor.http.HttpStatusCode.Created)
        }

        // Admin lihat semua orderan
        get {
            val orderanList = transaction {
                OrderanSalesTable.selectAll().map {
                    OrderanSales(
                        id = it[OrderanSalesTable.id],
                        salesId = it[OrderanSalesTable.salesId],
                        pelangganId = it[OrderanSalesTable.pelangganId],
                        total = it[OrderanSalesTable.total],
                        status = it[OrderanSalesTable.status],
                        tanggalOrder = it[OrderanSalesTable.tanggalOrder]
                    )
                }
            }
            call.respond(orderanList)
        }

        // Admin approve orderan
        post("/approve") {
            val request = call.receive<ApproveOrderanRequest>()

            transaction {
                OrderanSalesTable.update({ OrderanSalesTable.id eq request.orderanId }) {
                    it[status] = "DISETUJUI"
                }
            }

            call.respondText("Orderan berhasil disetujui!", status = io.ktor.http.HttpStatusCode.OK)
        }

        // Admin reject orderan
        post("/reject") {
            val request = call.receive<ApproveOrderanRequest>()

            transaction {
                OrderanSalesTable.update({ OrderanSalesTable.id eq request.orderanId }) {
                    it[status] = "DITOLAK"
                }
            }

            call.respondText("Orderan berhasil ditolak!", status = io.ktor.http.HttpStatusCode.OK)
        }
    }
}
