package com.kasir

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    fun init() {
        // Konfigurasi HikariCP untuk koneksi ke Supabase PostgreSQL
        val config = HikariConfig().apply {
            // JDBC URL dengan SSL mode
            jdbcUrl = "jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres?sslmode=require"
            driverClassName = "org.postgresql.Driver"

            // Kredensial sesuai JDBC Connection Info di Supabase
            username = "postgres.vqtquguxvrtbwsfrsyia"
            password = "T0k0bangun@n"

            // Pool settings
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"

            // Validasi konfigurasi sebelum dipakai
            validate()
        }

        // Inisialisasi koneksi Exposed
        Database.connect(HikariDataSource(config))
    }
}
