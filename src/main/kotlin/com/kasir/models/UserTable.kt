package com.kasir.models

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object UsersTable : UUIDTable("users") {
    val email     = text("email").uniqueIndex()
    val password  = text("password")
    val role      = text("role")
    val entitasId = reference(
        name    = "entitas_id",
        foreign = EntitasUsahaTable,
        onDelete = ReferenceOption.CASCADE
    )
    val createdAt = timestamp("created_at")
        .clientDefault { Instant.now() }
}