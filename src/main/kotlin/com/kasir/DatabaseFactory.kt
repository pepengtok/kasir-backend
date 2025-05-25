package com.kasir

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import com.kasir.models.*
object DatabaseFactory {
    fun init() {
        // Konfigurasi HikariCP untuk koneksi via Session Pooler (IPv4-compatible)
        val config = HikariConfig().apply {
            jdbcUrl         = "jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres?sslmode=require"
            driverClassName = "org.postgresql.Driver"

            // Kredensial Supabase-mu
            username        = "postgres.gvqzlfkdofvlfimzouov"
            password        = "T0k0bangun@n"

            // Pool settings
            maximumPoolSize        = 10
            isAutoCommit           = false
            transactionIsolation   = "TRANSACTION_REPEATABLE_READ"

            validate()  // Pastikan konfigurasi valid
        }

        // Inisialisasi koneksi database dengan Exposed + HikariCP
        Database.connect(HikariDataSource(config))

        transaction {
            SchemaUtils.create(
                EntitasUsahaTable,
                SupplierTable,
                KategoriProdukTable,
                ProdukTable,
                KasTable,
                PelangganTable,
                SalesTable,
                PenjualanTable,
                PenjualanDetailTable,
                PembelianTable,
                PembelianDetailTable,
                KasTransaksiTable,
                KasTransferTable,
                PiutangPelangganTable,
                KomisiSalesTable,
                HutangBankTable,
                HutangLainTable,
                AbsensiKaryawanTable,
                BiayaOperasionalTable,
                BiayaNonOperasionalTable,
                HutangSupplierTable,
                KaryawanTable,
                ModalTable,
                OrderanSalesDetailTable,
                OrderanSalesTable,
                PembayaranHutangBankTable,
                PembayaranPiutangLainTable,
                PembayaranHutangLainTable,
                PembayaranHutangSupplierTable,
                PembayaranPiutangPelangganTable,
                PembayaranPiutangKaryawanTable,
                PiutangKaryawanTable,
                PiutangLainTable,
                PriveTable,
                ReturPembelianTable,
                ReturPenjualanTable,
                StockOpnameTable,
                UsersTable

            )
        }
    }
}
