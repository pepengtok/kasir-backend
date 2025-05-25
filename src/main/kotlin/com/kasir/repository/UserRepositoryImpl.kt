package com.kasir.repository

import com.kasir.dto.UserDto
import com.kasir.models.UsersTable
import com.kasir.models.EntitasUsahaTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class UserRepositoryImpl : UserRepository {
    override fun findByEmail(email: String): UserDto? = transaction {
        UsersTable
            .select { UsersTable.email eq email }
            .map { row ->
                UserDto(
                    id        = row[UsersTable.id].value.toString(),
                    email     = row[UsersTable.email],
                    role      = row[UsersTable.role],
                    entitasId = row[UsersTable.entitasId].value.toString()
                )
            }
            .singleOrNull()
    }

    override fun createUser(
        email: String,
        plainPassword: String,
        role: String,
        entitasId: UUID
    ): UUID = transaction {
        UsersTable.insertAndGetId { row ->
            row[UsersTable.email]     = email
            row[UsersTable.password]  = plainPassword
            row[UsersTable.role]      = role
            row[UsersTable.entitasId] = EntityID(entitasId, EntitasUsahaTable)
        }.value
    }
}
