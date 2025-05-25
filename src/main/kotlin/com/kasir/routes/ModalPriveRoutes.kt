// File: src/main/kotlin/com/kasir/routes/ModalPriveRoutes.kt
package com.kasir.routes
import com.kasir.dto.*
import com.kasir.models.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.*
import java.time.LocalDateTime
import java.util.UUID
import com.kasir.dto.IdResponse
import com.kasir.dto.ModalListResponse
import com.kasir.dto.PriveListResponse

fun Route.modalPriveRoutes() {
    authenticate("jwt-auth") {
        // --- Modal ---
        route("/modal") {
            get {
                val entitasParam = call.request.queryParameters["entitas_id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "entitas_id wajib diisi")
                val entitasId = EntityID(UUID.fromString(entitasParam), EntitasUsahaTable)

                val modals = transaction {
                    ModalTable
                        .select { ModalTable.entitasId eq entitasId }
                        .map { row ->
                            ModalResponse(
                                id         = row[ModalTable.id].value.toString(),
                                kasId      = row[ModalTable.kasId].value.toString(),
                                tanggal    = row[ModalTable.tanggal].toString(),
                                nominal    = row[ModalTable.nominal],
                                keterangan = row[ModalTable.keterangan],
                                entitasId  = entitasParam // ✅ tambahkan
                            )
                        }
                }
                val kasOptions = transaction {
                    val query = when (call.request.queryParameters["kasType"]?.lowercase()) {
                        "besar" -> KasTable.select { (KasTable.namaKas eq "Kas Besar") and (KasTable.entitasId eq entitasId) }
                        "kecil" -> KasTable.select { (KasTable.namaKas eq "Kas Kecil") and (KasTable.entitasId eq entitasId) }
                        "bank"  -> KasTable.select {
                            (KasTable.namaKas neq "Kas Besar") and
                                    (KasTable.namaKas neq "Kas Kecil") and
                                    (KasTable.entitasId eq entitasId)
                        }
                        else    -> KasTable.select { KasTable.entitasId eq entitasId }
                    }
                    query.map { row ->
                        val nama = row[KasTable.namaKas]
                        val jenis = when (nama) {
                            "Kas Besar" -> "BESAR"
                            "Kas Kecil" -> "KECIL"
                            else        -> "BANK"
                        }
                        Kas(
                            id         = row[KasTable.id].value.toString(),
                            namaKas    = nama,
                            saldoAkhir = row[KasTable.saldoAkhir],
                            jenis      = jenis,
                            subJenis   = row[KasTable.subJenis],
                            entitasId  = entitasParam // ✅ tambahkan
                        )
                    }
                }

                call.respond(HttpStatusCode.OK, ModalListResponse(kasOptions, modals, entitasParam))
            }

            post {
                val req = call.receive<ModalRequest>()
                val newId = transaction {
                    // insert modal dan update kas & transaksi…
                    // return id.value
                    val id = ModalTable.insertAndGetId { row ->
                        row[ModalTable.kasId]     = EntityID(UUID.fromString(req.kasId), KasTable)
                        row[ModalTable.tanggal]   = LocalDateTime.parse(req.tanggal)
                        row[ModalTable.nominal]   = req.nominal
                        row[ModalTable.keterangan]= req.keterangan
                        row[ModalTable.entitasId] = EntityID(UUID.fromString(req.entitasId), EntitasUsahaTable) // ✅
                    }
                    val kasUUID = UUID.fromString(req.kasId)
                    val current = KasTable.select { KasTable.id eq EntityID(kasUUID, KasTable) }
                        .single()[KasTable.saldoAkhir]
                    val updatedSaldo = current + req.nominal
                    KasTransaksiTable.insert { tr ->
                        tr[KasTransaksiTable.kasId] = EntityID(kasUUID, KasTable)
                        tr[KasTransaksiTable.tanggal] = LocalDateTime.now()
                        tr[KasTransaksiTable.jumlah] = req.nominal
                        tr[KasTransaksiTable.keterangan] = "Modal masuk ${id.value}"
                        tr[KasTransaksiTable.tipe] = "MASUK"
                    }
                    KasTable.update({ KasTable.id eq EntityID(kasUUID, KasTable) }) {
                        it[KasTable.saldoAkhir] = updatedSaldo
                    }
                    id.value
                }
                call.respond(HttpStatusCode.Created, IdResponse(newId.toString()))
            }
        }

        // --- Prive ---
        // --- Prive ---
        route("/prive") {
            get {
                val kasType = call.request.queryParameters["kasType"]?.lowercase()
                val entitasParam = call.request.queryParameters["entitas_id"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, "entitas_id wajib diisi")
                val entitasId = EntityID(UUID.fromString(entitasParam), EntitasUsahaTable)
                val privs = transaction {
                    PriveTable
                        .select { PriveTable.entitasId eq entitasId }
                        .map { row ->
                            PriveResponse(
                                id         = row[PriveTable.id].value.toString(),
                                kasId      = row[PriveTable.kasId].value.toString(),
                                tanggal    = row[PriveTable.tanggal].toString(),
                                nominal    = row[PriveTable.nominal],
                                keterangan = row[PriveTable.keterangan],
                                entitasId  = entitasParam
                            )
                        }
                }
                val kasOptions = transaction {
                    val query = when (kasType) {
                        "besar" -> KasTable.select {
                            (KasTable.namaKas eq "Kas Besar") and (KasTable.entitasId eq entitasId)
                        }
                        "kecil" -> KasTable.select {
                            (KasTable.namaKas eq "Kas Kecil") and (KasTable.entitasId eq entitasId)
                        }
                        "bank" -> KasTable.select {
                            (KasTable.namaKas neq "Kas Besar") and
                                    (KasTable.namaKas neq "Kas Kecil") and
                                    (KasTable.entitasId eq entitasId)
                        }
                        else -> KasTable.select { KasTable.entitasId eq entitasId }
                    }

                    query.map { row ->
                        val nama = row[KasTable.namaKas]
                        val jenis = when (nama) {
                            "Kas Besar" -> "BESAR"
                            "Kas Kecil" -> "KECIL"
                            else        -> "BANK"
                        }
                        Kas(
                            id         = row[KasTable.id].value.toString(),
                            namaKas    = nama,
                            saldoAkhir = row[KasTable.saldoAkhir],
                            jenis      = jenis,
                            subJenis   = row[KasTable.subJenis],
                            entitasId  = entitasParam
                        )
                    }
                }
                call.respond(HttpStatusCode.OK, PriveListResponse(kasOptions, privs, entitasParam))
            }

            post {
                val req = call.receive<PriveRequest>()
                val newId = transaction {
                    val id = PriveTable.insertAndGetId { row ->
                        row[PriveTable.kasId]     = EntityID(UUID.fromString(req.kasId), KasTable)
                        row[PriveTable.tanggal]   = LocalDateTime.parse(req.tanggal)
                        row[PriveTable.nominal]   = req.nominal
                        row[PriveTable.keterangan]= req.keterangan
                        row[PriveTable.entitasId] = EntityID(UUID.fromString(req.entitasId), EntitasUsahaTable) // ✅
                    }

                    // Update saldo dan transaksi
                    val kasUUID = UUID.fromString(req.kasId)
                    val current = KasTable.select { KasTable.id eq EntityID(kasUUID, KasTable) }
                        .single()[KasTable.saldoAkhir]
                    val updatedSaldo = current - req.nominal
                    KasTransaksiTable.insert {
                        it[kasId]      = EntityID(kasUUID, KasTable)
                        it[tanggal]    = LocalDateTime.now()
                        it[jumlah]     = req.nominal
                        it[keterangan] = "Prive keluar ${id.value}"
                        it[tipe]       = "KELUAR"
                    }
                    KasTable.update({ KasTable.id eq EntityID(kasUUID, KasTable) }) {
                        it[saldoAkhir] = updatedSaldo
                    }

                    id.value
                }

                call.respond(HttpStatusCode.Created, IdResponse(newId.toString()))
            }
        }
    }
}