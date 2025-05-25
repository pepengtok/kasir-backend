package com.kasir.routes

import io.ktor.server.routing.*
import com.kasir.models.*
import com.kasir.dto.*
import com.kasir.service.KasService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import java.util.UUID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import java.time.LocalDate
import com.kasir.routes.helpers.getEntitasIdFromJwt
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.Serializable


fun Route.orderanRoutes() {
    route("/orderan") {
        // ==== SALES ====
        authenticate("jwt-sales") {
            fun PipelineContext<Unit, ApplicationCall>.getSalesContext(): Pair<EntityID<UUID>, EntityID<UUID>> {
                val principal = call.principal<JWTPrincipal>() ?: error("Unauthorized")
                val entitasUUID = UUID.fromString(principal.payload.getClaim("entitasId").asString())
                val salesUUID = UUID.fromString(principal.subject!!)
                return EntityID(entitasUUID, EntitasUsahaTable) to EntityID(salesUUID, SalesTable)
            }

            // 1) Sales membuat orderan
            post {
                val (entitasIdFromJwt, salesIdFromJwt) = getSalesContext()
                val req = call.receive<OrderanRequest>()

                // Konsolidasi transaksi untuk pembuatan header dan detail
                val orderanId = transaction {
                    val newOrderId = OrderanSalesTable.insertAndGetId { r ->
                        r[OrderanSalesTable.entitasId] = entitasIdFromJwt
                        r[salesId] = salesIdFromJwt
                        r[pelangganId] = EntityID(req.pelangganId, PelangganTable)
                        r[total] = req.total
                        r[metodePembayaran] = req.metodePembayaran
                        r[OrderanSalesTable.tempoHari] = req.tempoHari
                        r[OrderanSalesTable.status] = OrderStatus.MENUNGGU.name
                        r[OrderanSalesTable.tanggalOrder] = LocalDateTime.now()
                    }.value

                    req.detail.forEach { d ->
                        OrderanSalesDetailTable.insert { r ->
                            r[OrderanSalesDetailTable.entitasId] = entitasIdFromJwt
                            r[OrderanSalesDetailTable.orderanId] = EntityID(newOrderId, OrderanSalesTable)
                            r[OrderanSalesDetailTable.produkId] = d.produkId?.let { EntityID(it, ProdukTable) }
                            r[namaProduk] = d.namaProduk
                            r[OrderanSalesDetailTable.hargaJual] = d.hargaJual
                            r[jumlah] = d.jumlah
                            r[OrderanSalesDetailTable.subtotal] = d.subtotal
                        }
                    }
                    newOrderId
                }
                call.respond(HttpStatusCode.Created, IdResponse(orderanId.toString()))
            }

            // 2) Sales lihat semua orderan
            get {
                val (entitasIdFromJwt, salesIdFromJwt) = getSalesContext()
                val list = transaction {
                    OrderanSalesTable
                        .select {
                            (OrderanSalesTable.salesId eq salesIdFromJwt) and
                                    (OrderanSalesTable.entitasId eq entitasIdFromJwt)
                        }
                        .orderBy(OrderanSalesTable.tanggalOrder to SortOrder.DESC)
                        .map { row ->
                            OrderanSales(
                                id = row[OrderanSalesTable.id].value.toString(),
                                salesId = row[OrderanSalesTable.salesId].value,
                                pelangganId = row[OrderanSalesTable.pelangganId]?.value,
                                total = row[OrderanSalesTable.total],
                                metodePembayaran = row[OrderanSalesTable.metodePembayaran],
                                tempoHari = row[OrderanSalesTable.tempoHari],
                                status = row[OrderanSalesTable.status],
                                tanggalOrder = row[OrderanSalesTable.tanggalOrder].toString(),
                                entitasId = row[OrderanSalesTable.entitasId].value.toString()
                            )
                        }
                }
                call.respond(HttpStatusCode.OK, list)
            }

            // 3) Sales lihat detail orderan
            get("/{id}") {
                val (entitasIdFromJwt, salesIdFromJwt) = getSalesContext()
                val oid = runCatching { UUID.fromString(call.parameters["id"]!!) }
                    .getOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid ID")

                val (retrievedHeader, retrievedDetails) = transaction {
                    val header = OrderanSalesTable
                        .select {
                            (OrderanSalesTable.id eq oid) and
                                    (OrderanSalesTable.salesId eq salesIdFromJwt) and
                                    (OrderanSalesTable.entitasId eq entitasIdFromJwt)
                        }
                        .singleOrNull() ?: return@transaction null to emptyList()

                    val orderData = OrderanSales(
                        id = header[OrderanSalesTable.id].value.toString(),
                        salesId = header[OrderanSalesTable.salesId].value,
                        pelangganId = header[OrderanSalesTable.pelangganId]?.value,
                        total = header[OrderanSalesTable.total],
                        metodePembayaran = header[OrderanSalesTable.metodePembayaran],
                        tempoHari = header[OrderanSalesTable.tempoHari],
                        status = header[OrderanSalesTable.status],
                        tanggalOrder = header[OrderanSalesTable.tanggalOrder].toString(),
                        entitasId = header[OrderanSalesTable.entitasId].value.toString()
                    )

                    val detailsData = OrderanSalesDetailTable
                        .select {
                            (OrderanSalesDetailTable.orderanId eq oid) and
                                    (OrderanSalesDetailTable.entitasId eq entitasIdFromJwt)
                        }
                        .map { row ->
                            OrderanSalesDetail(
                                id = row[OrderanSalesDetailTable.id].value.toString(),
                                orderanId = row[OrderanSalesDetailTable.orderanId].value,
                                produkId = row[OrderanSalesDetailTable.produkId]?.value,
                                namaProduk = row[OrderanSalesDetailTable.namaProduk],
                                hargaJual = row[OrderanSalesDetailTable.hargaJual],
                                jumlah = row[OrderanSalesDetailTable.jumlah],
                                subtotal = row[OrderanSalesDetailTable.subtotal],
                                entitasId = row[OrderanSalesDetailTable.entitasId].value.toString()
                            )
                        }
                    orderData to detailsData
                }

                if (retrievedHeader == null) {
                    call.respond(HttpStatusCode.NotFound, "Orderan not found")
                } else {
                    call.respond(
                        HttpStatusCode.OK,
                        OrderanReview(
                            orderHeader = retrievedHeader,
                            orderDetail = retrievedDetails,
                            entitasId = entitasIdFromJwt.value.toString()
                        )
                    )
                }
            }

            // 4) Sales edit orderan saat MENUNGGU
            put("/{id}") {
                val (entitasIdFromJwt, salesIdFromJwt) = getSalesContext()
                val oid = runCatching { UUID.fromString(call.parameters["id"]!!) }
                    .getOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                val req = call.receive<OrderanRequest>()

                val (updatedHeaderData, updatedDetailsData) = transaction {
                    val statusNow = OrderanSalesTable
                        .select {
                            (OrderanSalesTable.id eq oid) and
                                    (OrderanSalesTable.salesId eq salesIdFromJwt) and
                                    (OrderanSalesTable.entitasId eq entitasIdFromJwt)
                        }
                        .singleOrNull()?.get(OrderanSalesTable.status) // Menggunakan singleOrNull()
                        ?: return@transaction null to emptyList() // Orderan tidak ditemukan atau tidak milik sales ini
                    check(statusNow == OrderStatus.MENUNGGU.name) { "Orderan hanya bisa diubah saat MENUNGGU" }

                    OrderanSalesTable.update({
                        (OrderanSalesTable.id eq oid) and
                                (OrderanSalesTable.salesId eq salesIdFromJwt) and
                                (OrderanSalesTable.entitasId eq entitasIdFromJwt)
                    }) {
                        it[total] = req.total
                        it[metodePembayaran] = req.metodePembayaran
                        it[OrderanSalesTable.tempoHari] = req.tempoHari
                    }

                    // Hapus semua detail lama
                    OrderanSalesDetailTable.deleteWhere {
                        (OrderanSalesDetailTable.orderanId eq oid) and
                                (OrderanSalesDetailTable.entitasId eq entitasIdFromJwt)
                    }
                    // Masukkan semua detail baru
                    req.detail.forEach { d ->
                        OrderanSalesDetailTable.insert { r ->
                            r[OrderanSalesDetailTable.entitasId] = entitasIdFromJwt
                            r[OrderanSalesDetailTable.orderanId] = EntityID(oid, OrderanSalesTable)
                            r[OrderanSalesDetailTable.produkId] = d.produkId?.let { EntityID(it, ProdukTable) }
                            r[namaProduk] = d.namaProduk
                            r[OrderanSalesDetailTable.hargaJual] = d.hargaJual
                            r[jumlah] = d.jumlah
                            r[OrderanSalesDetailTable.subtotal] = d.subtotal
                        }
                    }

                    val header = OrderanSalesTable.select { OrderanSalesTable.id eq oid }.single().let { row ->
                        OrderanSales(
                            id = row[OrderanSalesTable.id].value.toString(),
                            salesId = row[OrderanSalesTable.salesId].value,
                            pelangganId = row[OrderanSalesTable.pelangganId]?.value,
                            total = row[OrderanSalesTable.total],
                            metodePembayaran = row[OrderanSalesTable.metodePembayaran],
                            tempoHari = row[OrderanSalesTable.tempoHari],
                            status = row[OrderanSalesTable.status],
                            tanggalOrder = row[OrderanSalesTable.tanggalOrder].toString(),
                            entitasId = row[OrderanSalesTable.entitasId].value.toString()
                        )
                    }
                    val details = OrderanSalesDetailTable
                        .select { OrderanSalesDetailTable.orderanId eq oid }
                        .map { row ->
                            OrderanSalesDetail(
                                id = row[OrderanSalesDetailTable.id].value.toString(),
                                orderanId = row[OrderanSalesDetailTable.orderanId].value,
                                produkId = row[OrderanSalesDetailTable.produkId]?.value,
                                namaProduk = row[OrderanSalesDetailTable.namaProduk],
                                hargaJual = row[OrderanSalesDetailTable.hargaJual],
                                jumlah = row[OrderanSalesDetailTable.jumlah],
                                subtotal = row[OrderanSalesDetailTable.subtotal],
                                entitasId = row[OrderanSalesDetailTable.entitasId].value.toString()
                            )
                        }
                    header to details
                }
                if (updatedHeaderData == null) {
                    call.respond(HttpStatusCode.NotFound, "Orderan not found or not in MENUNGGU status.")
                } else {
                    call.respond(
                        HttpStatusCode.OK,
                        OrderanReview(
                            orderHeader = updatedHeaderData,
                            orderDetail = updatedDetailsData,
                            entitasId = entitasIdFromJwt.value.toString()
                        )
                    )
                }
            }
        }

        // ==== ADMIN ====
        authenticate("jwt-admin") {
            fun PipelineContext<Unit, ApplicationCall>.getAdminContextEntitasId(): EntityID<UUID> {
                val principal = call.principal<JWTPrincipal>() ?: error("Unauthorized")
                return EntityID(UUID.fromString(principal.payload.getClaim("entitasId").asString()), EntitasUsahaTable)
            }

            // 4) Admin lihat semua orderan
            get {
                val entitasIdFromJwt = getAdminContextEntitasId()
                val statusParam = call.request.queryParameters["status"]?.uppercase()
                val results = transaction {
                    var q = OrderanSalesTable.select { OrderanSalesTable.entitasId eq entitasIdFromJwt }
                    statusParam?.let { sp -> q = q.andWhere { OrderanSalesTable.status eq sp } }
                    q.orderBy(OrderanSalesTable.tanggalOrder to SortOrder.DESC)
                        .map { row ->
                            OrderanSales(
                                id = row[OrderanSalesTable.id].value.toString(),
                                salesId = row[OrderanSalesTable.salesId].value,
                                pelangganId = row[OrderanSalesTable.pelangganId]?.value,
                                total = row[OrderanSalesTable.total],
                                metodePembayaran = row[OrderanSalesTable.metodePembayaran],
                                tempoHari = row[OrderanSalesTable.tempoHari],
                                status = row[OrderanSalesTable.status],
                                tanggalOrder = row[OrderanSalesTable.tanggalOrder].toString(),
                                entitasId = row[OrderanSalesTable.entitasId].value.toString()
                            )
                        }
                }
                call.respond(HttpStatusCode.OK, results)
            }


            // 5) Admin lihat detail orderan
            get("/{id}") {
                val entitasIdFromJwt = getAdminContextEntitasId()
                val oid = runCatching { UUID.fromString(call.parameters["id"]!!) }
                    .getOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid ID")

                val (retrievedHeader, retrievedDetails) = transaction {
                    val header = OrderanSalesTable.select {
                        (OrderanSalesTable.id eq oid) and
                                (OrderanSalesTable.entitasId eq entitasIdFromJwt)
                    }.singleOrNull() ?: return@transaction null to emptyList()

                    val orderData = OrderanSales(
                        id = header[OrderanSalesTable.id].value.toString(),
                        salesId = header[OrderanSalesTable.salesId].value,
                        pelangganId = header[OrderanSalesTable.pelangganId]?.value,
                        total = header[OrderanSalesTable.total],
                        metodePembayaran = header[OrderanSalesTable.metodePembayaran],
                        tempoHari = header[OrderanSalesTable.tempoHari],
                        status = header[OrderanSalesTable.status],
                        tanggalOrder = header[OrderanSalesTable.tanggalOrder].toString(),
                        entitasId = header[OrderanSalesTable.entitasId].value.toString()
                    )

                    val detailsData = OrderanSalesDetailTable.select {
                        (OrderanSalesDetailTable.orderanId eq oid) and
                                (OrderanSalesDetailTable.entitasId eq entitasIdFromJwt)
                    }.map { row ->
                        OrderanSalesDetail(
                            id = row[OrderanSalesDetailTable.id].value.toString(),
                            orderanId = row[OrderanSalesDetailTable.orderanId].value,
                            produkId = row[OrderanSalesDetailTable.produkId]?.value,
                            namaProduk = row[OrderanSalesDetailTable.namaProduk],
                            hargaJual = row[OrderanSalesDetailTable.hargaJual],
                            jumlah = row[OrderanSalesDetailTable.jumlah],
                            subtotal = row[OrderanSalesDetailTable.subtotal],
                            entitasId = row[OrderanSalesDetailTable.entitasId].value.toString()
                        )
                    }
                    orderData to detailsData
                }

                if (retrievedHeader == null) {
                    call.respond(HttpStatusCode.NotFound, "Orderan not found")
                } else {
                    call.respond(
                        HttpStatusCode.OK,
                        OrderanReview(
                            orderHeader = retrievedHeader,
                            orderDetail = retrievedDetails,
                            entitasId = entitasIdFromJwt.value.toString()
                        )
                    )
                }
            }

            // 6) Admin approve orderan (memungkinkan tambah, hapus, update detail)
            put("/{id}/approve") {
                val entitasIdFromJwt = getAdminContextEntitasId()
                val oid = runCatching { UUID.fromString(call.parameters["id"]!!) }
                    .getOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                val reqs = call.receive<List<ApproveDetailRequest>>() // Menerima list detail yang diharapkan ada

                val result = transaction {
                    val currentOrder = OrderanSalesTable
                        .select { (OrderanSalesTable.id eq oid) and (OrderanSalesTable.entitasId eq entitasIdFromJwt) }
                        .singleOrNull()
                        ?: return@transaction null // Orderan tidak ditemukan

                    // Admin bisa approve dari status MANAPUN kecuali TERKIRIM atau DIBATALKAN
                    if (currentOrder[OrderanSalesTable.status] == OrderStatus.TERKIRIM.name ||
                        currentOrder[OrderanSalesTable.status] == OrderStatus.DIBATALKAN.name) {
                        return@transaction null // Mengembalikan null untuk menandakan kegagalan
                    }

                    // 1. Hapus semua detail yang tidak ada di request
                    val existingDetailIds = OrderanSalesDetailTable
                        .select { (OrderanSalesDetailTable.orderanId eq oid) and (OrderanSalesDetailTable.entitasId eq entitasIdFromJwt) }
                        .map { it[OrderanSalesDetailTable.id].value.toString() }
                        .toSet()

                    val detailIdsToKeep = reqs.map { it.detailId }.toSet()

                    val detailIdsToDelete = existingDetailIds.minus(detailIdsToKeep)

                    if (detailIdsToDelete.isNotEmpty()) {
                        OrderanSalesDetailTable.deleteWhere {
                            (OrderanSalesDetailTable.id inList detailIdsToDelete.map { UUID.fromString(it) }) and
                                    (OrderanSalesDetailTable.orderanId eq oid) and
                                    (OrderanSalesDetailTable.entitasId eq entitasIdFromJwt)
                        }
                    }

                    // Hitung ulang total
                    var newTotal = 0.0

                    reqs.forEach { d ->
                        val detailIdUuid = runCatching { UUID.fromString(d.detailId) }.getOrNull()

                        // Pastikan produkId di ApproveDetailRequest d.produkId adalah String
                        val produkUuid: UUID? = d.produkId?.let { runCatching { UUID.fromString(it) }.getOrNull() }

                        if (detailIdUuid != null && existingDetailIds.contains(d.detailId)) {
                            // Update detail yang sudah ada
                            OrderanSalesDetailTable.update({
                                (OrderanSalesDetailTable.id eq detailIdUuid) and
                                        (OrderanSalesDetailTable.entitasId eq entitasIdFromJwt)
                            }) { r ->
                                r[OrderanSalesDetailTable.produkId] = if (produkUuid != null) EntityID(produkUuid, ProdukTable) else null // Perbaikan di sini
                                r[namaProduk] = d.namaProduk
                                r[OrderanSalesDetailTable.hargaJual] = d.hargaJual
                                r[jumlah] = d.qty
                                r[OrderanSalesDetailTable.subtotal] = d.hargaJual * d.qty
                            }
                        } else {
                            // Insert detail baru (detailId akan diabaikan jika tidak ada di existingDetailIds atau null)
                            OrderanSalesDetailTable.insert { r ->
                                r[OrderanSalesDetailTable.entitasId] = entitasIdFromJwt
                                r[OrderanSalesDetailTable.orderanId] = EntityID(oid, OrderanSalesTable)
                                r[OrderanSalesDetailTable.produkId] = if (produkUuid != null) EntityID(produkUuid, ProdukTable) else null // Perbaikan di sini
                                r[namaProduk] = d.namaProduk
                                r[OrderanSalesDetailTable.hargaJual] = d.hargaJual
                                r[jumlah] = d.qty
                                r[OrderanSalesDetailTable.subtotal] = d.hargaJual * d.qty
                            }
                        }
                        newTotal += (d.hargaJual * d.qty)
                    }

                    // Update total di header orderan dan status
                    OrderanSalesTable.update({
                        (OrderanSalesTable.id eq oid) and
                                (OrderanSalesTable.entitasId eq entitasIdFromJwt)
                    }) {
                        it[OrderanSalesTable.status] = OrderStatus.DISETUJUI.name
                        it[total] = newTotal // Update total berdasarkan detail yang baru
                    }

                    val header = OrderanSalesTable.select { OrderanSalesTable.id eq oid }.single().let { row ->
                        OrderanSales(
                            id = row[OrderanSalesTable.id].value.toString(),
                            salesId = row[OrderanSalesTable.salesId].value,
                            pelangganId = row[OrderanSalesTable.pelangganId]?.value,
                            total = row[OrderanSalesTable.total],
                            metodePembayaran = row[OrderanSalesTable.metodePembayaran],
                            tempoHari = row[OrderanSalesTable.tempoHari],
                            status = row[OrderanSalesTable.status],
                            tanggalOrder = row[OrderanSalesTable.tanggalOrder].toString(),
                            entitasId = row[OrderanSalesTable.entitasId].value.toString()
                        )
                    }
                    val detailsData = OrderanSalesDetailTable
                        .select { OrderanSalesDetailTable.orderanId eq oid }
                        .map { row ->
                            OrderanSalesDetail(
                                id = row[OrderanSalesDetailTable.id].value.toString(),
                                orderanId = row[OrderanSalesDetailTable.orderanId].value,
                                produkId = row[OrderanSalesDetailTable.produkId]?.value,
                                namaProduk = row[OrderanSalesDetailTable.namaProduk],
                                hargaJual = row[OrderanSalesDetailTable.hargaJual],
                                jumlah = row[OrderanSalesDetailTable.jumlah],
                                subtotal = row[OrderanSalesDetailTable.subtotal],
                                entitasId = row[OrderanSalesDetailTable.entitasId].value.toString()
                            )
                        }
                    OrderanReview(
                        orderHeader = header,
                        orderDetail = detailsData,
                        entitasId = entitasIdFromJwt.value.toString()
                    )
                }

                if (result == null) {
                    call.respond(HttpStatusCode.BadRequest, "Orderan not found or cannot be approved (already TERKIRIM/DIBATALKAN).")
                } else {
                    call.respond(HttpStatusCode.OK, result)
                }
            }
            // 7) Admin tolak orderan
            put("/{id}/tolak") {
                val entitasIdFromJwt = getAdminContextEntitasId()
                val oid = runCatching { UUID.fromString(call.parameters["id"]!!) }
                    .getOrNull() ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid ID")

                val updatedRows = transaction {
                    OrderanSalesTable.update({
                        (OrderanSalesTable.id eq oid) and
                                (OrderanSalesTable.entitasId eq entitasIdFromJwt) and
                                (OrderanSalesTable.status neq OrderStatus.TERKIRIM.name) and // Tidak bisa ditolak jika sudah TERKIRIM
                                (OrderanSalesTable.status neq OrderStatus.DIBATALKAN.name) // Tidak bisa ditolak jika sudah DIBATALKAN
                    }) { it[OrderanSalesTable.status] = OrderStatus.DIBATALKAN.name }
                }

                if (updatedRows == 0) {
                    call.respond(HttpStatusCode.BadRequest, "Orderan not found or cannot be rejected (already TERKIRIM/DIBATALKAN).")
                } else {
                    call.respond(HttpStatusCode.OK, IdResponse(oid.toString()))
                }
            }

            post("/{id}/kirim") {
                // 1) Autentikasi & parse ID
                val entitasIdFromJwt = getAdminContextEntitasId()
                val oid = call.parameters["id"]?.let(UUID::fromString)
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                val req = call.receive<KirimOrderanRequest>()

                // 2) Jalankan semua dalam satu transaction
                val (status, body) = transaction {
                    // a) Ambil header order
                    val order = OrderanSalesTable
                        .selectAll()
                        .andWhere {
                            (OrderanSalesTable.id eq oid) and
                                    (OrderanSalesTable.entitasId eq entitasIdFromJwt)
                        }
                        .singleOrNull()
                        ?: return@transaction HttpStatusCode.NotFound to "Orderan not found"

                    if (order[OrderanSalesTable.status] != OrderStatus.DISETUJUI.name) {
                        return@transaction HttpStatusCode.BadRequest to "Orderan belum disetujui"
                    }

                    // b) Validasi kas untuk TUNAI
                    if (order[OrderanSalesTable.metodePembayaran].equals("TUNAI", true) && req.kasId == null) {
                        return@transaction HttpStatusCode.BadRequest to "Kas ID is required for TUNAI payment"
                    }

                    // c) Ambil detail order
                    val orderDetails = OrderanSalesDetailTable
                        .selectAll()
                        .andWhere { OrderanSalesDetailTable.orderanId eq oid }
                        .toList()

                    // d) Hitung tempo & jatuhTempo
                    val tempoDays = order[OrderanSalesTable.tempoHari] ?: 0
                    val jatuhTempo = LocalDate
                        .now()
                        .plusDays(tempoDays.toLong())
                        .atStartOfDay()
                    val nowDateTime = LocalDateTime.now()

                    // e) Buat transaksi penjualan
                    val penjualanId = PenjualanTable.insertAndGetId { r ->
                        r[PenjualanTable.entitasId]        = entitasIdFromJwt
                        r[PenjualanTable.pelangganId]      = order[OrderanSalesTable.pelangganId]!!
                        r[PenjualanTable.salesId]          = order[OrderanSalesTable.salesId]!!
                        r[PenjualanTable.total]            = order[OrderanSalesTable.total]
                        r[PenjualanTable.metodePembayaran] = order[OrderanSalesTable.metodePembayaran]
                        r[PenjualanTable.jatuhTempo]       = jatuhTempo
                        r[PenjualanTable.status]           =
                            if (order[OrderanSalesTable.metodePembayaran].equals("TUNAI", true))
                                "LUNAS" else "BELUM_LUNAS"
                        r[PenjualanTable.tanggal]          = nowDateTime
                    }.value

                    // f) Masukkan detail penjualan & update stok
                    orderDetails.forEach { det ->
                        val prodId   = det[OrderanSalesDetailTable.produkId]!!
                        val qtyInt   = det[OrderanSalesDetailTable.jumlah]
                        val qty      = qtyInt.toDouble()

                        // ambil data produk
                        val prodRow = ProdukTable
                            .selectAll()
                            .andWhere { ProdukTable.id eq prodId }
                            .singleOrNull()
                            ?: return@transaction HttpStatusCode.InternalServerError to
                                    "Produk dengan ID $prodId tidak ditemukan."

                        val hargaModal  = prodRow[ProdukTable.hargaModal]
                        val hargaJual   = det[OrderanSalesDetailTable.hargaJual]
                        val potensiLaba = (hargaJual - hargaModal) * qtyInt

                        // insert detail
                        PenjualanDetailTable.insert { pd ->
                            pd[PenjualanDetailTable.penjualanId] = EntityID(penjualanId, PenjualanTable)
                            pd[PenjualanDetailTable.produkId]    = prodId
                            pd[PenjualanDetailTable.hargaModal]  = hargaModal
                            pd[PenjualanDetailTable.hargaJual]   = hargaJual
                            pd[PenjualanDetailTable.jumlah]      = qtyInt
                            pd[PenjualanDetailTable.subtotal]    = det[OrderanSalesDetailTable.subtotal]
                            pd[PenjualanDetailTable.satuan]      = prodRow[ProdukTable.satuan]
                            pd[PenjualanDetailTable.entitasId]   = entitasIdFromJwt
                            pd[PenjualanDetailTable.potensiLaba] = potensiLaba
                        }

                        // update stok produk
                        val currentStok = prodRow[ProdukTable.stok]
                        ProdukTable.update({ ProdukTable.id eq prodId }) {
                            it[ProdukTable.stok] = currentStok - qty
                        }
                    }

                    // g) Catat kas atau piutang
                    if (order[OrderanSalesTable.metodePembayaran].equals("TUNAI", true)) {
                        KasService.record(
                            entitasId = entitasIdFromJwt.value,
                            kasId      = req.kasId!!,
                            tanggal    = nowDateTime,
                            jumlah     = order[OrderanSalesTable.total],
                            tipe       = "MASUK",
                            keterangan = "Pembayaran orderan $oid"
                        )
                    } else {
                        // piutang
                        val piutangId = PiutangPelangganTable.insertAndGetId { p ->
                            p[PiutangPelangganTable.entitasId]       = entitasIdFromJwt
                            p[PiutangPelangganTable.penjualanId]     = EntityID(penjualanId, PenjualanTable)
                            p[PiutangPelangganTable.pelangganId]     = order[OrderanSalesTable.pelangganId]!!
                            p[PiutangPelangganTable.totalPiutang]    = order[OrderanSalesTable.total]
                            p[PiutangPelangganTable.sisaPiutang]     = order[OrderanSalesTable.total]
                            p[PiutangPelangganTable.tanggalJatuhTempo] = jatuhTempo
                            p[PiutangPelangganTable.status]          = "BELUM_LUNAS"
                            p[PiutangPelangganTable.fotoNotaUrl]     = null
                            p[PiutangPelangganTable.tanggal] = nowDateTime.toLocalDate()
                        }.value

                        PembayaranPiutangPelangganTable.insert { b ->
                            b[PembayaranPiutangPelangganTable.entitasId]  = entitasIdFromJwt
                            b[PembayaranPiutangPelangganTable.piutangId]   = EntityID(piutangId, PiutangPelangganTable)
                            b[PembayaranPiutangPelangganTable.tanggalBayar] = nowDateTime
                            b[PembayaranPiutangPelangganTable.jumlahBayar] = 0.0
                            b[PembayaranPiutangPelangganTable.kasId]       = null
                            b[PembayaranPiutangPelangganTable.keterangan]  = "Pembukaan piutang orderan $oid"
                        }
                    }

                    // h) Hitung & insert komisi sales
                    val salesId   = order[OrderanSalesTable.salesId]!!
                    val komisiPct = SalesTable
                        .selectAll()
                        .andWhere { SalesTable.id eq salesId }
                        .singleOrNull()
                        ?.get(
                            if (order[OrderanSalesTable.metodePembayaran].equals("TUNAI", true))
                                SalesTable.komisiTunai
                            else
                                SalesTable.komisiPiutang
                        ) ?: 0.0

                    val profit = orderDetails.sumOf { det ->
                        val modal = ProdukTable
                            .selectAll()
                            .andWhere { ProdukTable.id eq det[OrderanSalesDetailTable.produkId]!! }
                            .singleOrNull()
                            ?.get(ProdukTable.hargaModal) ?: 0.0
                        (det[OrderanSalesDetailTable.hargaJual] - modal) * det[OrderanSalesDetailTable.jumlah]
                    }

                    KomisiSalesTable.insert { ks ->
                        ks[KomisiSalesTable.entitasId]     = entitasIdFromJwt
                        ks[KomisiSalesTable.salesId]       = salesId
                        ks[KomisiSalesTable.penjualanId]   = EntityID(penjualanId, PenjualanTable)
                        ks[KomisiSalesTable.komisiPersen]  = komisiPct
                        ks[KomisiSalesTable.nominalKomisi] = profit * komisiPct / 100.0
                        ks[KomisiSalesTable.status]        =
                            if (order[OrderanSalesTable.metodePembayaran].equals("TUNAI", true))
                                "DIBAYAR" else "PENDING"
                        ks[KomisiSalesTable.tanggalKomisi] = nowDateTime
                    }

                    // i) Update status orderan → TERKIRIM
                    OrderanSalesTable.update({ OrderanSalesTable.id eq oid }) {
                        it[OrderanSalesTable.status] = OrderStatus.TERKIRIM.name
                    }

                    HttpStatusCode.OK to IdResponse(oid.toString())
                }

                // 3) Kirim response
                call.respond(status, body)
            }
        }
    }
}