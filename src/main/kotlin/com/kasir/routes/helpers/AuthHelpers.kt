package com.kasir.routes.helpers

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.util.pipeline.PipelineContext
import org.jetbrains.exposed.dao.id.EntityID
import com.kasir.models.EntitasUsahaTable
import io.ktor.server.application.call
import java.util.UUID

// Helper function untuk mendapatkan entitasId dari JWT
fun PipelineContext<Unit, ApplicationCall>.getEntitasIdFromJwt(): EntityID<UUID> {
    val principal = call.principal<JWTPrincipal>()
        ?: throw IllegalArgumentException("JWT Principal not found or invalid.") // Ini sudah lebih aman
    val entitasIdClaim = principal.payload.getClaim("entitasId")
        ?: throw IllegalArgumentException("Entitas ID claim not found in JWT.")
    val entitasUUIDString = entitasIdClaim.asString()
        ?: throw IllegalArgumentException("Entitas ID claim is not a string.")

    val entitasUUID = runCatching { UUID.fromString(entitasUUIDString) }
        .getOrNull() ?: throw IllegalArgumentException("Invalid Entitas ID UUID format in JWT.")

    return EntityID(entitasUUID, EntitasUsahaTable)
}