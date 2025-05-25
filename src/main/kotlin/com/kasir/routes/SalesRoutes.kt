// File: src/main/kotlin/com/kasir/routes/SalesRoutes.kt
package com.kasir.routes

import com.kasir.dto.Sales
import com.kasir.dto.SalesRequest
import com.kasir.models.SalesTable
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

fun Route.salesRoutes() {
    authenticate("jwt-auth") {
        route("/sales") {

            // GET all sales
            get {
                val salesList = transaction {
                    SalesTable.selectAll().map { row ->
                        Sales(
                            id            = row[SalesTable.id].value.toString(),
                            nama     = row[SalesTable.nama],
                            komisiTunai   = row[SalesTable.komisiTunai],
                            komisiPiutang = row[SalesTable.komisiPiutang],
                            createdAt     = row[SalesTable.createdAt].toString()
                        )
                    }
                }
                call.respond(HttpStatusCode.OK, salesList)
            }

            // POST create new sales
            post {
                val request = call.receive<SalesRequest>()
                val newId = transaction {
                    SalesTable.insertAndGetId {
                        it[nama]     = request.nama
                        it[komisiTunai]   = request.komisiTunai
                        it[komisiPiutang] = request.komisiPiutang
                        it[createdAt]     = LocalDateTime.now()
                    }
                }
                call.respond(HttpStatusCode.Created, mapOf("id" to newId.value.toString()))
            }

            // PUT update sales by id
            put("/{id}") {
                val uuid = call.parameters["id"]
                    ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing or invalid id"))

                val request = call.receive<SalesRequest>()
                transaction {
                    SalesTable.update({ SalesTable.id eq uuid }) {
                        it[nama]     = request.nama
                        it[komisiTunai]   = request.komisiTunai
                        it[komisiPiutang] = request.komisiPiutang
                    }
                }
                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            }

            // DELETE sales by id
            delete("/{id}") {
                val uuid = call.parameters["id"]
                    ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing or invalid id"))

                transaction {
                    SalesTable.deleteWhere { SalesTable.id eq uuid }
                }
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
