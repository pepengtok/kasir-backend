// src/main/kotlin/com/kasir/models/UserTable.kt
package com.kasir.models

import org.jetbrains.exposed.dao.id.UUIDTable

object UserTable : UUIDTable("users") {
    val nama     = varchar("nama", 50)
    val email    = varchar("email", 100).uniqueIndex()
    val password = varchar("password", 60)
    val role     = varchar("role", 20)
}
