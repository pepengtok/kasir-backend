package com.kasir.middleware

import io.ktor.server.application.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import io.ktor.http.*
import io.ktor.server.routing.*

// helper untuk ambil klaim "role" dari JWT
private fun ApplicationCall.currentRole(): String? =
    principal<JWTPrincipal>()
        ?.payload
        ?.getClaim("role")
        ?.asString()

/**
 * Middleware untuk admin_besar & admin_kecil
 */
fun Route.adminOnly(build: Route.() -> Unit) {
    intercept(ApplicationCallPipeline.Call) {
        val role = call.currentRole()
        if (role !in setOf("admin_besar", "admin_kecil")) {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "❌ Akses hanya untuk admin."))
            finish()
        }
    }
    build()
}

/**
 * Middleware hanya untuk admin_besar
 */
fun Route.adminBesarOnly(build: Route.() -> Unit) {
    intercept(ApplicationCallPipeline.Call) {
        val role = call.currentRole()
        if (role != "admin_besar") {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "❌ Akses hanya untuk admin besar."))
            finish()
        }
    }
    build()
}

/**
 * Middleware hanya untuk sales (atau admin_besar, jika perlu supervisor access)
 */
fun Route.salesOnly(build: Route.() -> Unit) {
    intercept(ApplicationCallPipeline.Call) {
        val role = call.currentRole()
        if (role != "sales" /*|| role == null*/) {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "❌ Akses hanya untuk sales."))
            finish()
        }
    }
    build()
}
