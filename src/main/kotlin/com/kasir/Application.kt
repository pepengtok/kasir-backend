package com.kasir

import com.kasir.DatabaseFactory
import com.kasir.middleware.adminOnly
import com.kasir.middleware.salesOnly
import com.kasir.routes.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import java.util.Date

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // 1) Init DB
    DatabaseFactory.init()

    // 2) JWT config
    val jwtSecret   = System.getenv("JWT_SECRET") ?: "supersecret"
    val jwtIssuer   = "com.kasir"
    val jwtAudience = "com.kasir.audience"
    val jwtRealm    = "Aplikasi Kasir"

    // 3) Authentication plugin
    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer(jwtIssuer)
                    .withAudience(jwtAudience)
                    .build()
            )
            validate { cred ->
                cred.payload.getClaim("id").asString().takeIf { it.isNotEmpty() }
                    ?.let { JWTPrincipal(cred.payload) }
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid or expired token"))
            }
        }
    }

    // 4) JSON serialization
    install(ContentNegotiation) {
        json(Json {
            prettyPrint       = true
            isLenient         = true
            ignoreUnknownKeys = true
        })
    }

    // 5) Error handling
    install(StatusPages) {
        exception<ContentTransformationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to (cause.message ?: "Invalid JSON")))
        }
        exception<Throwable> { call, cause ->
            cause.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (cause.message ?: "Server error")))
        }
    }

    // 6) Routing
    routing {
        // Health-check
        get("/") {
            call.respondText("Aplikasi Kasir Kotlin Jalan 🚀")
        }

        // Public routes
        userRoutes()
        kategoriRoutes()          // sekarang /kategori bisa diakses publik

        // Protected admin/sales
        authenticate("auth-jwt") {
            route("/admin") {
                adminOnly {
                    produkRoutes()
                    supplierRoutes()
                    pelangganRoutes()
                    stockOpnameRoutes()
                    kasRoutes()
                    posRoutes()
                    piutangRoutes()
                    hutangRoutes()
                    orderanRoutes()
                    komisiRoutes()
                    laporanRoutes()
                    pembelianRoutes()
                }
            }
            route("/sales") {
                salesOnly {
                    // sales-only routes
                }
            }
        }
    }
}
