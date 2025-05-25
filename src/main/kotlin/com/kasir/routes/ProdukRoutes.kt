package com.kasir.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import com.kasir.models.* // Pastikan ini mengimpor ProdukTable, SupplierTable, KategoriProdukTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.dao.id.EntityID
import java.util.UUID
import com.kasir.models.EntitasUsahaTable
import io.ktor.server.auth.principal
import io.ktor.server.auth.jwt.JWTPrincipal
import com.kasir.dto.*




fun Route.produkRoutes() {
    authenticate("jwt-auth") { // Pastikan rute ini dilindungi JWT dengan benar
        route("/produk") {
            // GET all produk (filtered by entitasId dari JWT)
            get {
                val principal = call.principal<JWTPrincipal>()!!
                val entitasUUID = UUID.fromString(principal.payload.getClaim("entitasId").asString())

                val list = transaction {
                    ProdukTable
                        .join(SupplierTable, JoinType.LEFT) { ProdukTable.supplierId eq SupplierTable.id }
                        .join(KategoriProdukTable, JoinType.INNER) { ProdukTable.kategoriId eq KategoriProdukTable.id }
                        .select { ProdukTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable) }
                        .map { row ->
                            ProdukResponseDto(
                                id = row[ProdukTable.id].value.toString(),
                                namaProduk = row[ProdukTable.namaProduk],
                                kodeProduk = row[ProdukTable.kodeProduk],
                                hargaModal = row[ProdukTable.hargaModal],
                                hargaJual1 = row[ProdukTable.hargaJual1],
                                hargaJual2 = row[ProdukTable.hargaJual2],
                                hargaJual3 = row[ProdukTable.hargaJual3],
                                stok = row[ProdukTable.stok],
                                satuan = row[ProdukTable.satuan],
                                supplier = row[ProdukTable.supplierId]?.let { SupplierDto(it.value.toString(), row[SupplierTable.namaSupplier]) },
                                kategori = KategoriProdukDto(
                                    id = row[KategoriProdukTable.id].value.toString(),
                                    namaKategori = row[KategoriProdukTable.namaKategori],
                                    entitasId = row[KategoriProdukTable.entitasId].value.toString() // ✅ PERBAIKAN DI SINI
                                ),
                                entitas_id = row[ProdukTable.entitasId].value.toString(),
                                berat_gram = row[ProdukTable.beratGram],
                                panjang_cm = row[ProdukTable.panjangCm],
                                lebar_cm = row[ProdukTable.lebarCm],
                                tinggi_cm = row[ProdukTable.tinggiCm]
                            )
                        }
                }
                call.respond(HttpStatusCode.OK, list)
            }

            // GET produk by ID
            get("/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val entitasUUID = UUID.fromString(principal.payload.getClaim("entitasId").asString())
                val productId = UUID.fromString(call.parameters["id"]!!)

                val produkResponse = transaction {
                    ProdukTable
                        .join(SupplierTable, JoinType.LEFT) { ProdukTable.supplierId eq SupplierTable.id }
                        .join(KategoriProdukTable, JoinType.INNER) { ProdukTable.kategoriId eq KategoriProdukTable.id }
                        .select { (ProdukTable.id eq EntityID(productId, ProdukTable)) and (ProdukTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable)) }
                        .singleOrNull()
                        ?.let { row ->
                            ProdukResponseDto(
                                id = row[ProdukTable.id].value.toString(),
                                namaProduk = row[ProdukTable.namaProduk],
                                kodeProduk = row[ProdukTable.kodeProduk],
                                hargaModal = row[ProdukTable.hargaModal],
                                hargaJual1 = row[ProdukTable.hargaJual1],
                                hargaJual2 = row[ProdukTable.hargaJual2],
                                hargaJual3 = row[ProdukTable.hargaJual3],
                                stok = row[ProdukTable.stok],
                                satuan = row[ProdukTable.satuan],
                                supplier = row[ProdukTable.supplierId]?.let { SupplierDto(it.value.toString(), row[SupplierTable.namaSupplier]) },
                                kategori = KategoriProdukDto(
                                    id = row[KategoriProdukTable.id].value.toString(),
                                    namaKategori = row[KategoriProdukTable.namaKategori],
                                    entitasId = row[KategoriProdukTable.entitasId].value.toString() // ✅ PERBAIKAN DI SINI
                                ),
                                entitas_id = row[ProdukTable.entitasId].value.toString(),
                                berat_gram = row[ProdukTable.beratGram],
                                panjang_cm = row[ProdukTable.panjangCm],
                                lebar_cm = row[ProdukTable.lebarCm],
                                tinggi_cm = row[ProdukTable.tinggiCm]
                            )
                        }
                }
                if (produkResponse == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Produk tidak ditemukan"))
                } else {
                    call.respond(HttpStatusCode.OK, produkResponse)
                }
            }

            // POST (Create) produk
            post {
                val principal = call.principal<JWTPrincipal>()!!
                val entitasUUID = UUID.fromString(principal.payload.getClaim("entitasId").asString())
                val produkRequest = call.receive<ProdukRequestDto>()

                val newProductId = transaction {
                    ProdukTable.insertAndGetId {
                        it[ProdukTable.entitasId] = EntityID(entitasUUID, EntitasUsahaTable)
                        it[ProdukTable.namaProduk] = produkRequest.namaProduk
                        it[ProdukTable.kodeProduk] = produkRequest.kodeProduk
                        it[ProdukTable.hargaModal] = produkRequest.hargaModal
                        it[ProdukTable.hargaJual1] = produkRequest.hargaJual1
                        it[ProdukTable.hargaJual2] = produkRequest.hargaJual2
                        it[ProdukTable.hargaJual3] = produkRequest.hargaJual3
                        it[ProdukTable.stok] = produkRequest.stok
                        it[ProdukTable.satuan] = produkRequest.satuan
                        it[ProdukTable.supplierId] = produkRequest.supplierId?.let { sid -> EntityID(UUID.fromString(sid), SupplierTable) }
                        it[ProdukTable.kategoriId] = EntityID(UUID.fromString(produkRequest.kategoriId), KategoriProdukTable)
                        it[ProdukTable.beratGram] = produkRequest.berat_gram
                        it[ProdukTable.panjangCm] = produkRequest.panjang_cm
                        it[ProdukTable.lebarCm] = produkRequest.lebar_cm
                        it[ProdukTable.tinggiCm] = produkRequest.tinggi_cm
                    }.value
                }
                call.respond(HttpStatusCode.Created, mapOf("id" to newProductId.toString()))
            }

            // PUT (Update) produk
            put("/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val entitasUUID = UUID.fromString(principal.payload.getClaim("entitasId").asString())
                val productId = UUID.fromString(call.parameters["id"]!!)
                val produkRequest = call.receive<ProdukRequestDto>()

                val updatedCount = transaction {
                    ProdukTable.update({ (ProdukTable.id eq EntityID(productId, ProdukTable)) and (ProdukTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable)) }) {
                        it[ProdukTable.namaProduk] = produkRequest.namaProduk
                        it[ProdukTable.kodeProduk] = produkRequest.kodeProduk
                        it[ProdukTable.hargaModal] = produkRequest.hargaModal
                        it[ProdukTable.hargaJual1] = produkRequest.hargaJual1
                        it[ProdukTable.hargaJual2] = produkRequest.hargaJual2
                        it[ProdukTable.hargaJual3] = produkRequest.hargaJual3
                        it[ProdukTable.stok] = produkRequest.stok
                        it[ProdukTable.satuan] = produkRequest.satuan
                        it[ProdukTable.supplierId] = produkRequest.supplierId?.let { sid -> EntityID(UUID.fromString(sid), SupplierTable) }
                        it[ProdukTable.kategoriId] = EntityID(UUID.fromString(produkRequest.kategoriId), KategoriProdukTable)
                        it[ProdukTable.beratGram] = produkRequest.berat_gram
                        it[ProdukTable.panjangCm] = produkRequest.panjang_cm
                        it[ProdukTable.lebarCm] = produkRequest.lebar_cm
                        it[ProdukTable.tinggiCm] = produkRequest.tinggi_cm
                    }
                }
                if (updatedCount > 0) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Produk berhasil diperbarui"))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Produk tidak ditemukan atau tidak ada perubahan"))
                }
            }

            // DELETE produk
            delete("/{id}") {
                val principal = call.principal<JWTPrincipal>()!!
                val entitasUUID = UUID.fromString(principal.payload.getClaim("entitasId").asString())
                val productId = UUID.fromString(call.parameters["id"]!!)

                val deletedRowCount = transaction {
                    ProdukTable.deleteWhere { (ProdukTable.id eq EntityID(productId, ProdukTable)) and (ProdukTable.entitasId eq EntityID(entitasUUID, EntitasUsahaTable)) }
                }
                if (deletedRowCount > 0) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Produk tidak ditemukan"))
                }
            }
        }
    }
}