package com.kasir.routes

import com.kasir.models.RegisterRequest
import com.kasir.models.LoginRequest
import com.kasir.models.User
import com.kasir.models.UserTable
import com.kasir.models.AuthResponse
import com.kasir.utils.isValidEmail
import com.kasir.utils.isStrongPassword
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.routing.post
import io.ktor.server.routing.get
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.util.UUID
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date

/**
 * Define extension function for auth routes.
 */
fun Route.userRoutes() {
    route("/auth") {
        // REGISTER USER
        post("/register") {
            val req = call.receive<RegisterRequest>()
            // Validate input
            when {
                req.nama.isBlank() || req.email.isBlank() || req.password.isBlank() || req.role.isBlank() ->
                    return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Semua field wajib diisi"))
                !isValidEmail(req.email) ->
                    return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Format email tidak valid"))
                !isStrongPassword(req.password) ->
                    return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Password minimal 8 karakter"))
            }
            // Check duplicate email
            val exists = transaction { UserTable.select { UserTable.email eq req.email }.any() }
            if (exists) {
                return@post call.respond(HttpStatusCode.Conflict, mapOf("error" to "Email sudah terdaftar"))
            }
            // Hash password and save
            val hashed = BCrypt.hashpw(req.password, BCrypt.gensalt())
            val userId = UUID.randomUUID()
            transaction {
                UserTable.insert { row ->
                    row[UserTable.id]       = userId
                    row[UserTable.nama]     = req.nama
                    row[UserTable.email]    = req.email
                    row[UserTable.password] = hashed
                    row[UserTable.role]     = req.role
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("message" to "User berhasil didaftarkan!"))
        }
        // LOGIN USER
        post("/login") {
            val req = call.receive<LoginRequest>()
            // Find user by email
            val row = transaction { UserTable.select { UserTable.email eq req.email }.singleOrNull() }
                ?: return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Email tidak ditemukan"))
            // Check password
            if (!BCrypt.checkpw(req.password, row[UserTable.password])) {
                return@post call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Password salah"))
            }
            // Build user object
            val user = User(
                id    = row[UserTable.id].value.toString(),
                nama  = row[UserTable.nama],
                email = row[UserTable.email],
                role  = row[UserTable.role]
            )
            // Generate JWT
            val jwtSecret   = System.getenv("JWT_SECRET") ?: "supersecret"
            val jwtIssuer   = "com.kasir"
            val jwtAudience = "com.kasir.audience"
            val token = JWT.create()
                .withIssuer(jwtIssuer)
                .withAudience(jwtAudience)
                .withClaim("id", user.id)
                .withClaim("role", user.role)
                .withExpiresAt(Date(System.currentTimeMillis() + 3_600_000))
                .sign(Algorithm.HMAC256(jwtSecret))
            // Respond with token and user
            call.respond(HttpStatusCode.OK, AuthResponse(token, user))
        }
    }
}
