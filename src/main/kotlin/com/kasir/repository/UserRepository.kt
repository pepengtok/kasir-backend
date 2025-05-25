// src/main/kotlin/com/kasir/repository/UserRepository.kt
package com.kasir.repository

import com.kasir.dto.UserDto
import java.util.UUID

/**
 * Abstraksi untuk operasi CRUD pada tabel users.
 */
interface UserRepository {
    /** Cari user berdasarkan email */
    fun findByEmail(email: String): UserDto?

    /**
     * Buat user baru, kembalikan UUID yang di‐generate.
     *
     * @param email         alamat email user
     * @param plainPassword password dalam bentuk plain text (akan di‐hash di service)
     * @param role          peran user (mis. "ADMIN", "SALES")
     * @param entitasId     UUID entitas usaha yang dipilih saat registrasi
     */
    fun createUser(
        email: String,
        plainPassword: String,
        role: String,
        entitasId: UUID
    ): UUID
}
