package com.kasir

import com.auth0.jwt.JWT
import com.kasir.DatabaseFactory
import com.kasir.middleware.adminBesarOnly
import com.kasir.middleware.adminOnly
import com.kasir.middleware.salesOnly
import com.kasir.routes.*
import com.kasir.serializers.LocalDateTimeSerializer
import com.kasir.serializers.UUIDSerializer
import com.kasir.service.LaporanService
import com.kasir.service.LaporanServiceImpl
import com.myapp.JwtConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
// Import ini tidak lagi dibutuhkan jika polymorphic(Any::class) tidak dipakai untuk LinkedHashMap/ArrayList
// import kotlinx.serialization.modules.polymorphic
// import kotlinx.serialization.modules.subclass
import org.slf4j.event.Level
import java.time.LocalDateTime
import java.util.UUID
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.http.content.static
import io.ktor.server.http.content.files
import io.ktor.server.http.content.staticRootFolder
import java.io.File
// Hapus import ini jika tidak digunakan secara eksplisit di sini
// import java.util.LinkedHashMap
// import java.util.ArrayList

fun main() {
    embeddedServer(
        Netty,
        host = "0.0.0.0",
        port = 8080,
        module = Application::module
    ).start(wait = true)
}


fun Application.module() {
    install(CallLogging) {
        level = Level.INFO
    }
    DatabaseFactory.init()

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
            // PASTIKAN: Class discriminator tidak diatur jika tidak diperlukan untuk Map<String, Any?>
            // classDiscriminator = null // Opsional: Coba ini jika masalah terus berlanjut

            serializersModule = SerializersModule {
                contextual(UUIDSerializer)
                contextual(LocalDateTime::class, LocalDateTimeSerializer)

                // HAPUS SEMUA BLOK POLYMORPHIC KOSONG INI.
                // Jika Anda tidak mendaftarkan subtype konkret di dalamnya, mereka tidak diperlukan
                // dan bahkan dapat menyebabkan validasi gagal.
                // Ini berarti kita akan mengandalkan serializer bawaan untuk Map/List
                // dan serializer kontekstual yang sudah ada.
                // Jika Anda memiliki DTO kompleks yang membutuhkan polimorfisme,
                // maka baru tambahkan blok polymorphic dengan subclass konkret yang relevan.
            }
        })
    }

    install(CORS) {
        anyHost()
        allowCredentials = true
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
    }
    // 4) Authentication: jwt-auth (semua role), jwt-admin, jwt-sales
    install(Authentication) {
        // 4a) provider umum: token valid dari siapa saja (admin, admin_besar, sales)
        jwt("jwt-auth") {
            realm = JwtConfig.realm
            verifier(
                JWT.require(JwtConfig.verifyAlgorithm())
                    .withIssuer(JwtConfig.ISSUER)
                    .withAudience(JwtConfig.AUDIENCE)
                    .build()
            )
            validate { creds ->
                creds.payload.getClaim("role").asString()?.takeIf {
                    it in listOf("admin_besar", "admin", "sales")
                }?.let { JWTPrincipal(creds.payload) }
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token invalid atau expired"))
            }
        }

        // 4b) hanya admin / admin_besar
        jwt("jwt-admin") {
            realm = JwtConfig.realm
            verifier(
                JWT.require(JwtConfig.verifyAlgorithm())
                    .withIssuer(JwtConfig.ISSUER)
                    .withAudience(JwtConfig.AUDIENCE)
                    .build()
            )
            validate { creds ->
                creds.payload.getClaim("role").asString()?.takeIf {
                    it == "admin" || it == "admin_besar"
                }?.let { JWTPrincipal(creds.payload) }
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token admin invalid atau expired"))
            }
        }

        // 4c) hanya sales
        jwt("jwt-sales") {
            realm = JwtConfig.realm
            verifier(
                JWT.require(JwtConfig.verifyAlgorithm())
                    .withIssuer(JwtConfig.ISSUER)
                    .withAudience(JwtConfig.AUDIENCE)
                    .build()
            )
            validate { creds ->
                creds.payload.getClaim("role").asString()?.takeIf {
                    it == "sales"
                }?.let { JWTPrincipal(creds.payload) }
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token sales invalid atau expired"))
            }
        }
    }

    // 5) Global error handling
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            cause.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (cause.message ?: "Server error")))
        }
    }

    val laporanService: LaporanService = LaporanServiceImpl()
    registerAdminRoutes()
    routing {
        // publik
        get("/") { call.respondText("Aplikasi Kasir Kotlin 🚀") }
        userRoutes()
        notaRoutes() // Ini tidak menyebabkan error karena mengembalikan HTML
        authenticate("jwt-auth") {
            get("/auth/me") {
                val payload = call.principal<JWTPrincipal>()!!.payload
                call.respond(mapOf(
                    "id" to payload.subject,
                    "role" to payload.getClaim("role").asString()
                ))
            }
        }

        // admin & admin_besar
        authenticate("jwt-admin") {
            route("/admin") {
                adminOnly {
                    kategoriRoutes()
                    produkRoutes()
                    supplierRoutes()
                    pelangganRoutes()
                    stockOpnameRoutes()
                    kasRoutes()
                    penjualanRoutes() // <-- Pastikan ini mengembalikan DTO spesifik
                    pembelianRoutes() // <-- Pastikan ini mengembalikan DTO spesifik
                    komisiRoutes()
                    orderanRoutes()
                    karyawanRoutes()
                    salesRoutes()
                    biayaRoutes()
                    piutangRoutes()
                    returRoutes()
                    hutangRoutes()
                    modalPriveRoutes()
                    laporanRoutes(laporanService)
                }
            }
        }

        authenticate("jwt-sales") {
            route("/sales") {
                salesOnly {
                    orderanRoutes()
                    komisiRoutes()
                }
            }
        }

        staticResources("/static", "static")
        static("/uploads") {
            staticRootFolder = File("uploads")
            files("nota_pembelian")
            files("nota_penjualan")
        }
    }
}