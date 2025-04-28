package com.kasir.middleware

import io.ktor.server.application.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*

/**
 * Middleware untuk route yang hanya boleh diakses oleh admin.
 * Memeriksa klaim "role" di JWTPrincipal.
 */
fun Route.adminOnly(build: Route.() -> Unit) {
    intercept(ApplicationCallPipeline.Call) {
        val principal = call.principal<JWTPrincipal>()
        // Ambil klaim role dengan safe-call
        val role = principal
            ?.payload
            ?.getClaim("role")
            ?.asString()
            ?: ""
        if (role != "admin") {
            call.respond(
                HttpStatusCode.Forbidden,
                mapOf("error" to "Forbidden: Admin only")
            )
            finish()
        }
    }
    build()
}

/**
 * Middleware untuk route yang hanya boleh diakses oleh sales.
 * Memeriksa klaim "role" di JWTPrincipal.
 */
fun Route.salesOnly(build: Route.() -> Unit) {
    intercept(ApplicationCallPipeline.Call) {
        val principal = call.principal<JWTPrincipal>()
        val role = principal
            ?.payload
            ?.getClaim("role")
            ?.asString()
            ?: ""
        if (role != "sales") {
            call.respond(
                HttpStatusCode.Forbidden,
                mapOf("error" to "Forbidden: Sales only")
            )
            finish()
        }
    }
    build()
}
