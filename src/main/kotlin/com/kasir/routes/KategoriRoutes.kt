package com.kasir.routes

import com.kasir.models.KategoriProduk
import com.kasir.models.KategoriProdukTable
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

/**
 * Wrapper untuk serialisasi List<KategoriProduk>
 */
@Serializable
data class KategoriList(val items: List<KategoriProduk>)

/**
 * DTO untuk request POST /kategori
 */
@Serializable
data class KategoriRequest(val nama: String)

/**
 * Extension function untuk operasi kategori.
 */
fun Route.kategoriRoutes() {
    route("/kategori") {

        // GET /kategori -> daftar semua kategori
        get {
            val list = transaction {
                KategoriProdukTable.selectAll().map {
                    KategoriProduk(
                        id = it[KategoriProdukTable.id].toString(),
                        namaKategori = it[KategoriProdukTable.namaKategori]
                    )
                }
            }
            call.respond(KategoriList(list))
        }

        // POST /kategori -> tambah kategori baru
        post {
            val req = call.receive<KategoriRequest>()

            // Validasi input
            if (req.nama.isBlank()) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Nama kategori wajib diisi")
                )
            }

            val newId = UUID.randomUUID()
            val created = transaction {
                KategoriProdukTable.insert { row ->
                    row[KategoriProdukTable.id] = newId
                    row[KategoriProdukTable.namaKategori] = req.nama
                }
                // Kembalikan data objek yang di-insert
                KategoriProduk(
                    id = newId.toString(),
                    namaKategori = req.nama
                )
            }

            call.respond(HttpStatusCode.Created, created)
        }
    }
}
