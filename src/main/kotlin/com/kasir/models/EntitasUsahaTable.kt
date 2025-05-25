package com.kasir.models

import org.jetbrains.exposed.dao.id.UUIDTable

object EntitasUsahaTable : UUIDTable("entitas_usaha") {
    val nama = varchar("nama", 150)
}
