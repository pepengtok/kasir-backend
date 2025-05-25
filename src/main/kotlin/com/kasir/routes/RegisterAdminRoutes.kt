package com.kasir.routes

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.patch
import io.ktor.client.request.setBody

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType as KtorContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.patch
import io.ktor.server.routing.routing
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Menyediakan route untuk operasi admin (membuat dan memperbarui user)
 */
fun Application.registerAdminRoutes() {
    // Konfigurasi HttpClient dengan plugin ContentNegotiation
    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    // Ambil service_role key dari environment
    val serviceRoleKey = System.getenv("SUPABASE_SERVICE_ROLE_KEY")
        ?: error("Environment variable SUPABASE_SERVICE_ROLE_KEY belum diset!")
    val supabaseUrl = "https://gvqzlfkdofvlfimzouov.supabase.co"

    routing {
        post("/admin/create-user") {
            val req = call.receive<CreateUserRequest>()
            val payload = SupabaseCreateUserRequest(
                email = req.email,
                password = req.password,
                userMetadata = UserMetadata(
                    nama = req.nama,
                    role = req.role,
                    entitasId = req.entitasId
                )
            )

            val response: HttpResponse = httpClient.post("$supabaseUrl/auth/v1/admin/users") {
                header(HttpHeaders.Authorization, "Bearer $serviceRoleKey")
                header("apikey", serviceRoleKey)
                contentType(KtorContentType.Application.Json)
                setBody(payload)
            }

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                call.respond(HttpStatusCode.Created, "User berhasil dibuat.")
            } else {
                val err = response.bodyAsText()
                call.respond(HttpStatusCode.BadRequest, "Gagal membuat user: $err")
            }
        }

        patch("/admin/update-user") {
            val req = call.receive<UpdateUserRequest>()
            val payload = UpdateUserMetadataRequest(
                userMetadata = UserMetadata(
                    nama = req.nama,
                    role = req.role,
                    entitasId = req.entitasId
                )
            )

            val response: HttpResponse = httpClient.patch("$supabaseUrl/auth/v1/admin/users/${req.uid}") {
                header(HttpHeaders.Authorization, "Bearer $serviceRoleKey")
                header("apikey", serviceRoleKey)
                contentType(KtorContentType.Application.Json)
                setBody(payload)
            }

            if (response.status == HttpStatusCode.OK) {
                call.respond(HttpStatusCode.OK, "User berhasil diperbarui.")
            } else {
                val err = response.bodyAsText()
                call.respond(HttpStatusCode.BadRequest, "Gagal memperbarui user: $err")
            }
        }
    }
}

// ===== Data Transfer Objects =====

@Serializable
public data class CreateUserRequest(
    val email: String,
    val password: String,
    val nama: String,
    val role: String,
    val entitasId: String
)

@Serializable
public data class UpdateUserRequest(
    val uid: String,
    val nama: String,
    val role: String,
    val entitasId: String
)

@Serializable
public data class SupabaseCreateUserRequest(
    val email: String,
    val password: String,
    @SerialName("user_metadata")
    val userMetadata: UserMetadata
)

@Serializable
public data class UpdateUserMetadataRequest(
    @SerialName("user_metadata")
    val userMetadata: UserMetadata
)

@Serializable
public data class UserMetadata(
    val nama: String,
    val role: String,
    val entitasId: String
)
