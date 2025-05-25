// File: AbsensiRoutes.kt (versi lengkap + proteksi gaji mingguan)
package com.kasir.routes

import com.kasir.dto.AbsensiKaryawan
import com.kasir.dto.AbsensiKaryawanRequest
import com.kasir.models.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.kasir.middleware.adminOnly
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun Route.absensiRoutes() {
    authenticate("jwt-auth") {
        adminOnly {
            route("/absensi") {

                // GET all absensi
                get {
                    val list = transaction {
                        AbsensiKaryawanTable.selectAll().map { row ->
                            AbsensiKaryawan(
                                id = row[AbsensiKaryawanTable.id].value.toString(),
                                karyawanId = row[AbsensiKaryawanTable.karyawanId].value.toString(),
                                tanggal = row[AbsensiKaryawanTable.tanggal].toString(),
                                statusAbsen = row[AbsensiKaryawanTable.statusAbsen],
                                keterangan = row[AbsensiKaryawanTable.keterangan],
                                createdAt = row[AbsensiKaryawanTable.createdAt].toString(),
                                entitasId = row[AbsensiKaryawanTable.entitasId].value.toString()
                            )
                        }
                    }
                    call.respond(list)
                }

                // GET absensi by ID
                get("/{id}") {
                    val idParam =
                        call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing id")
                    val data = transaction {
                        AbsensiKaryawanTable
                            .select { AbsensiKaryawanTable.id eq UUID.fromString(idParam) }
                            .mapNotNull { row ->
                                AbsensiKaryawan(
                                    id = row[AbsensiKaryawanTable.id].value.toString(),
                                    karyawanId = row[AbsensiKaryawanTable.karyawanId].value.toString(),
                                    tanggal = row[AbsensiKaryawanTable.tanggal].toString(),
                                    statusAbsen = row[AbsensiKaryawanTable.statusAbsen],
                                    keterangan = row[AbsensiKaryawanTable.keterangan],
                                    createdAt = row[AbsensiKaryawanTable.createdAt].toString(),
                                    entitasId = row[AbsensiKaryawanTable.entitasId].value.toString()
                                )
                            }.singleOrNull()
                    }
                    data?.let { call.respond(it) } ?: call.respond(HttpStatusCode.NotFound, "Data tidak ditemukan")
                }

                // POST absensi (masuk/pulang)
                post {
                    val req = call.receive<AbsensiKaryawanRequest>()
                    val id = transaction {
                        AbsensiKaryawanTable.insertAndGetId {
                            it[karyawanId] = EntityID(UUID.fromString(req.karyawanId), KaryawanTable)
                            it[tanggal] = LocalDate.parse(req.tanggal)
                            it[statusAbsen] = req.statusAbsen
                            it[keterangan] = req.keterangan ?: ""

                        }
                    }
                    call.respond(HttpStatusCode.Created, mapOf("id" to id.value.toString()))
                }

                // PUT update absensi
                put("/{id}") {
                    val idParam = call.parameters["id"]?.let(UUID::fromString)
                        ?: return@put call.respond(HttpStatusCode.BadRequest, "ID tidak valid")
                    val req = call.receive<AbsensiKaryawanRequest>()

                    transaction {
                        AbsensiKaryawanTable.update({ AbsensiKaryawanTable.id eq idParam }) {
                            it[karyawanId] = EntityID(UUID.fromString(req.karyawanId), KaryawanTable)
                            it[tanggal] = LocalDate.parse(req.tanggal)
                            it[statusAbsen] = req.statusAbsen
                            it[keterangan] = req.keterangan ?: ""
                        }
                    }
                    call.respond(HttpStatusCode.OK, "Berhasil diupdate")
                }

                // DELETE absensi
                delete("/{id}") {
                    val idParam = call.parameters["id"]?.let(UUID::fromString)
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, "ID tidak valid")
                    transaction {
                        AbsensiKaryawanTable.deleteWhere { AbsensiKaryawanTable.id eq idParam }
                    }
                    call.respond(HttpStatusCode.OK, "Berhasil dihapus")
                }

                // GET laporan gaji mingguan dan input ke pengeluaran kas
                get("/laporan-gaji/{tahun}/{minggu}/{kasId}") {
                    val tahun = call.parameters["tahun"]?.toIntOrNull() ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Tahun tidak valid"
                    )
                    val minggu = call.parameters["minggu"]?.toIntOrNull() ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Minggu tidak valid"
                    )
                    val kasId = call.parameters["kasId"]?.let(UUID::fromString) ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Kas ID tidak valid"
                    )

                    val laporan = transaction {
                        KaryawanTable.selectAll().map { karyawan ->
                            val karyawanId = karyawan[KaryawanTable.id].value
                            val nama = karyawan[KaryawanTable.nama]

                            val absenMingguIni = AbsensiKaryawanTable.select {
                                AbsensiKaryawanTable.karyawanId eq karyawanId and
                                        (AbsensiKaryawanTable.statusAbsen eq "masuk")
                            }.map { row ->
                                row[AbsensiKaryawanTable.tanggal]
                            }.filter { tanggal ->
                                val w = tanggal.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())
                                val y = tanggal.year
                                w == minggu && y == tahun
                            }

                            val jumlahHadir = absenMingguIni.size
                            val totalGaji = jumlahHadir * 50000

                            // Cek apakah gaji minggu ini sudah dicatat sebelumnya
                            val sudahAda = KasTransaksiTable.select {
                                KasTransaksiTable.kasId eq kasId and
                                        (KasTransaksiTable.keterangan eq "Gaji minggu ke-$minggu $tahun - $nama")
                            }.any()

                            if (totalGaji > 0 && !sudahAda) {
                                KasTransaksiTable.insert {
                                    it[KasTransaksiTable.kasId] = EntityID(kasId, KasTable)
                                    it[keterangan] = "Gaji minggu ke-$minggu $tahun - $nama"
                                    it[jumlah] = totalGaji.toDouble()
                                    it[tipe] = "keluar"
                                    it[tanggal] = LocalDateTime.now()

                                }
                            }

                            mapOf(
                                "karyawan" to nama,
                                "jumlah_hadir" to jumlahHadir,
                                "gaji_dibayar" to if (sudahAda) 0 else totalGaji,
                                "status" to if (sudahAda) "sudah dibayar" else "dibayar sekarang"
                            )
                        }
                    }
                    call.respond(laporan)
                }
            }
        }
    }}