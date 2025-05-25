package com.myapp

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

object JwtConfig {
    private val secret    = System.getenv("JWT_SECRET")   ?: "very-secret-key"
    private val _issuer   = System.getenv("JWT_ISSUER")   ?: "com.kasir"
    private val _audience = System.getenv("JWT_AUDIENCE") ?: "com.kasir.audience"
    private const val validityInMs = 1000L * 60 * 60 * 24 * 7

    // expose ini ke Ktor
    val ISSUER   get() = _issuer
    val AUDIENCE get() = _audience

    private val algorithm = Algorithm.HMAC256(secret)

    // Now includes entitasId
    fun generateToken(userId: String, role: String, entitasId: String): String =
        JWT.create()
            .withSubject(userId)
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withClaim("role", role)
            .withClaim("entitasId", entitasId)
            .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
            .sign(algorithm)

    fun verifyAlgorithm() = algorithm

    const val realm = "Aplikasi Kasir"
}