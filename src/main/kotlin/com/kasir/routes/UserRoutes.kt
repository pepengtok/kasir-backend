// src/main/kotlin/com/kasir/routes/UserRoutes.kt
package com.kasir.routes

import com.kasir.dto.AuthResponse
import com.kasir.dto.LoginRequest
import com.kasir.dto.RegisterRequestDto
import com.kasir.dto.UserDto
import com.kasir.models.*
import com.kasir.utils.isValidEmail
import com.kasir.utils.isStrongPassword
import com.myapp.JwtConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.insert
import org.mindrot.jbcrypt.BCrypt
import org.jetbrains.exposed.dao.id.EntityID // Tambahkan import ini
import java.util.UUID // Tambahkan import ini jika belum ada

fun Route.userRoutes() {
    route("/auth") {
        // 1) REGISTER
        post("/register") {
            val req = call.receive<RegisterRequestDto>()
            // PERBAIKAN: Validasi entitasId tidak boleh null
            if (req.email.isBlank() || req.password.isBlank() || req.role.isBlank() || req.entitasId.isBlank()) { // Tambahkan req.entitasId.isBlank()
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Semua field (email, password, role, entitasId) wajib diisi"))
            }
            if (!isValidEmail(req.email)) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Format email tidak valid"))
            }
            if (!isStrongPassword(req.password)) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Password minimal 8 karakter"))
            }
            // Validasi entitasId adalah UUID yang valid
            val entitasUUID = try {
                UUID.fromString(req.entitasId)
            } catch (e: IllegalArgumentException) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Format entitasId tidak valid (harus UUID)"))
            }
            // Optional: Cek apakah entitasId benar-benar ada di EntitasUsahaTable
            val entitasExists = transaction {
                EntitasUsahaTable.select { EntitasUsahaTable.id eq EntityID(entitasUUID, EntitasUsahaTable) }.any()
            }
            if (!entitasExists) {
                return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Entitas Usaha dengan ID tersebut tidak ditemukan"))
            }

            val exists = transaction {
                UsersTable.select { UsersTable.email eq req.email }.any()
            }
            if (exists) {
                return@post call.respond(HttpStatusCode.Conflict, mapOf("error" to "Email sudah terdaftar"))
            }

            val hashed = BCrypt.hashpw(req.password, BCrypt.gensalt())
            transaction {
                UsersTable.insert { row ->
                    row[UsersTable.email]    = req.email
                    row[UsersTable.password] = hashed
                    row[UsersTable.role]     = req.role
                    row[UsersTable.entitasId]= EntityID(entitasUUID, EntitasUsahaTable) // PERBAIKAN: Simpan entitasId dari DTO
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("message" to "User berhasil didaftarkan!"))
        }

        // 2) LOGIN
        post("/login") {
            val req = call.receive<LoginRequest>()
            val row = transaction {
                UsersTable.select { UsersTable.email eq req.email }
                    .singleOrNull()
            } ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Email tidak ditemukan"))

            if (!BCrypt.checkpw(req.password, row[UsersTable.password])) {
                return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Password salah"))
            }

            val user = UserDto(
                id = row[UsersTable.id].value.toString(),
                email = row[UsersTable.email],
                role = row[UsersTable.role],
                entitasId = row[UsersTable.entitasId].value.toString()
            )

            val token = JwtConfig.generateToken(
                userId    = user.id,
                role      = user.role,
                entitasId = user.entitasId
            )

            call.respond(HttpStatusCode.OK, AuthResponse(token, user))
        }

        // 3) PROFILE (protected, siapa pun dengan token valid)
        authenticate("jwt-admin", "jwt-sales") {
            get("/auth/me") {
                val principal = call.principal<JWTPrincipal>()!!
                val id        = principal.subject!!
                val email     = principal.payload.getClaim("email").asString()
                val role      = principal.payload.getClaim("role").asString()
                val entitasId = principal.payload.getClaim("entitasId").asString() // Ambil entitasId dari JWT
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "id"        to id,
                        "email"     to email,
                        "role"      to role,
                        "entitasId" to entitasId // Tambahkan entitasId ke respons
                    )
                )
            }
        }
    }
}