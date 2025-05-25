package com.kasir.service

import com.kasir.dto.FilterRequestDto
import com.kasir.dto.TransactionDto
import com.kasir.models.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.between
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID
import com.kasir.models.EntitasUsahaTable
import java.time.LocalDate // Tetap gunakan java.time.LocalDate
import org.jetbrains.exposed.dao.id.EntityID // Tambahkan impor ini jika diperlukan


// Data class untuk Laba Rugi Summary (sesuai yang Anda pakai di frontend)
data class LabaRugiSummary(
    val penjualanKotor: Double = 0.0,
    val returPenjualan: Double = 0.0,
    val penjualanBersih: Double = 0.0,
    val persediaanAwal: Double = 0.0,
    val pembelianGross: Double = 0.0,
    val returPembelian: Double = 0.0,
    val pembelianBersih: Double = 0.0,
    val barangTersedia: Double = 0.0,
    val persediaanAkhir: Double = 0.0,
    val hpp: Double = 0.0,
    val labaBruto: Double = 0.0,
    val biayaOperasional: Double = 0.0,
    val biayaNonOperasional: Double = 0.0,
    val komisi: Double = 0.0,
    val labaBersih: Double = 0.0
)

interface LaporanService {
    fun getDetailTransactions(
        filter: FilterRequestDto,
        start: LocalDateTime,
        end: LocalDateTime
    ): List<TransactionDto>

    fun getLabaRugiHarian(entitasId: UUID, tanggal: LocalDate): LabaRugiSummary
    fun getLabaRugiBulanan(entitasId: UUID, tahun: Int, bulan: Int): LabaRugiSummary
    fun getLabaRugiTahunan(entitasId: UUID, tahun: Int): LabaRugiSummary
}

class LaporanServiceImpl : LaporanService {
    override fun getDetailTransactions(
        filter: FilterRequestDto,
        start: LocalDateTime,
        end: LocalDateTime
    ): List<TransactionDto> = when (filter.tipe.uppercase()) {

        "PENJUALAN" -> transaction {
            PenjualanTable
                .select {
                    PenjualanTable.tanggal.between(start, end) and
                            (PenjualanTable.entitasId eq EntityID(UUID.fromString(filter.entitasId), EntitasUsahaTable))
                }
                .map { row ->
                    TransactionDto(
                        id = row[PenjualanTable.id].value.toString(),
                        tanggal = row[PenjualanTable.tanggal].toString(),
                        tipe = "Penjualan",
                        jumlah = row[PenjualanTable.total],
                        keterangan = null
                    )
                }
        }

        "PEMBELIAN" -> transaction {
            PembelianTable
                .select {
                    PembelianTable.tanggal.between(start, end) and
                            (PembelianTable.entitasId eq EntityID(UUID.fromString(filter.entitasId), EntitasUsahaTable))
                }
                .map { row ->
                    TransactionDto(
                        id = row[PembelianTable.id].value.toString(),
                        tanggal = row[PembelianTable.tanggal].toString(),
                        tipe = "Pembelian",
                        jumlah = row[PembelianTable.total],
                        keterangan = null
                    )
                }
        }

        "RETUR_PENJUALAN", "RETURPENJUALAN" -> transaction {
            ReturPenjualanTable
                .select {
                    ReturPenjualanTable.tanggalRetur.between(start, end) and
                            (ReturPenjualanTable.entitasId eq EntityID(UUID.fromString(filter.entitasId), EntitasUsahaTable))
                }
                .map { row ->
                    TransactionDto(
                        id = row[ReturPenjualanTable.id].value.toString(),
                        tanggal = row[ReturPenjualanTable.tanggalRetur].toString(),
                        tipe = "Retur Penjualan",
                        jumlah = row[ReturPenjualanTable.jumlahRetur],
                        keterangan = row[ReturPenjualanTable.keterangan]
                    )
                }
        }


        "RETUR_PEMBELIAN", "RETURPEMBELIAN" -> transaction {
            ReturPembelianTable
                .select {
                    ReturPembelianTable.tanggalRetur.between(start, end) and
                            (ReturPembelianTable.entitasId eq EntityID(UUID.fromString(filter.entitasId), EntitasUsahaTable))
                }
                .map { row ->
                    TransactionDto(
                        id = row[ReturPembelianTable.id].value.toString(),
                        tanggal = row[ReturPembelianTable.tanggalRetur].toString(),
                        tipe = "Retur Pembelian",
                        jumlah = row[ReturPembelianTable.jumlahRetur],
                        keterangan = row[ReturPembelianTable.keterangan]
                    )
                }
        }

        "BIAYAOPERASIONAL", "BIAYA OPERASIONAL" -> transaction {
            BiayaOperasionalTable
                .select {
                    BiayaOperasionalTable.tanggal.between(start, end) and
                            (BiayaOperasionalTable.entitasId eq EntityID(UUID.fromString(filter.entitasId), EntitasUsahaTable))
                }
                .map { row ->
                    TransactionDto(
                        id = row[BiayaOperasionalTable.id].value.toString(),
                        tanggal = row[BiayaOperasionalTable.tanggal].toString(),
                        tipe = "Biaya Operasional",
                        jumlah = row[BiayaOperasionalTable.nominal],
                        keterangan = row[BiayaOperasionalTable.keterangan]
                    )
                }
        }


        "BIAYANONOPERASIONAL", "BIAYA NON OPERASIONAL" -> transaction {
            BiayaNonOperasionalTable
                .select {
                    BiayaNonOperasionalTable.tanggal.between(start, end) and
                            (BiayaNonOperasionalTable.entitasId eq EntityID(UUID.fromString(filter.entitasId), EntitasUsahaTable))
                }
                .map { row ->
                    TransactionDto(
                        id = row[BiayaNonOperasionalTable.id].value.toString(),
                        tanggal = row[BiayaNonOperasionalTable.tanggal].toString(),
                        tipe = "Biaya Non Operasional",
                        jumlah = row[BiayaNonOperasionalTable.nominal],
                        keterangan = row[BiayaNonOperasionalTable.keterangan]
                    )
                }
        }
        "KOMISI" -> transaction {
            KomisiSalesTable
                .select {
                    KomisiSalesTable.tanggalKomisi.between(start, end) and
                            (KomisiSalesTable.entitasId eq EntityID(UUID.fromString(filter.entitasId), EntitasUsahaTable))
                }
                .map { row ->
                    TransactionDto(
                        id = row[KomisiSalesTable.id].value.toString(),
                        tanggal = row[KomisiSalesTable.tanggalKomisi].toString(),
                        tipe = "Komisi",
                        jumlah = row[KomisiSalesTable.nominalKomisi],
                        keterangan = null
                    )
                }
        }

        else -> emptyList()
    }

    // ✅ Implementasi fungsi-fungsi laba rugi di LaporanServiceImpl
    override fun getLabaRugiHarian(entitasId: UUID, tanggal: LocalDate): LabaRugiSummary = transaction {
        val startOfDay = tanggal.atStartOfDay()
        val endOfDay = tanggal.plusDays(1).atStartOfDay()

        val penjualanKotor = PenjualanTable
            .select { (PenjualanTable.entitasId eq entitasId) and (PenjualanTable.tanggal.between(startOfDay, endOfDay)) }
            .sumOf { it[PenjualanTable.total] }

        val returPenjualan = ReturPenjualanTable
            .select { (ReturPenjualanTable.entitasId eq entitasId) and (ReturPenjualanTable.tanggalRetur.between(startOfDay, endOfDay)) }
            .sumOf { it[ReturPenjualanTable.jumlahRetur] }

        val penjualanBersih = penjualanKotor - returPenjualan

        val pembelianGross = PembelianTable
            .select { (PembelianTable.entitasId eq entitasId) and (PembelianTable.tanggal.between(startOfDay, endOfDay)) }
            .sumOf { it[PembelianTable.total] }

        val returPembelian = ReturPembelianTable
            .select { (ReturPembelianTable.entitasId eq entitasId) and (ReturPembelianTable.tanggalRetur.between(startOfDay, endOfDay)) }
            .sumOf { it[ReturPembelianTable.jumlahRetur] }

        val pembelianBersih = pembelianGross - returPembelian

        val persediaanAwal = StockOpnameTable
            .select { (StockOpnameTable.entitasId eq entitasId) and (StockOpnameTable.tanggalOpname eq tanggal.atStartOfDay()) } // ✅ FIX: Compare LocalDate.atStartOfDay() with LocalDateTime
            .sumOf { row ->
                val produkId = row[StockOpnameTable.produkId] // produkId di sini sudah EntityID<UUID>
                val stokFisik = row[StockOpnameTable.stokFisik]
                val hargaModal = ProdukTable
                    .select { ProdukTable.id eq produkId }.singleOrNull()?.get(ProdukTable.hargaModal) ?: 0.0 // ✅ FIX: produkId sudah EntityID
                stokFisik * hargaModal
            }

        val persediaanAkhir = ProdukTable
            .select { ProdukTable.entitasId eq entitasId }
            .sumOf { it[ProdukTable.stok] * it[ProdukTable.hargaModal] }

        val barangTersedia = persediaanAwal + pembelianBersih
        val hpp = barangTersedia - persediaanAkhir

        val biayaOperasional = BiayaOperasionalTable
            .select { (BiayaOperasionalTable.entitasId eq entitasId) and (BiayaOperasionalTable.tanggal.between(startOfDay, endOfDay)) }
            .sumOf { it[BiayaOperasionalTable.nominal] }

        val biayaNonOperasional = BiayaNonOperasionalTable
            .select { (BiayaNonOperasionalTable.entitasId eq entitasId) and (BiayaNonOperasionalTable.tanggal.between(startOfDay, endOfDay)) }
            .sumOf { it[BiayaNonOperasionalTable.nominal] }

        val komisi = KomisiSalesTable
            .select { (KomisiSalesTable.entitasId eq entitasId) and (KomisiSalesTable.tanggalKomisi.between(startOfDay, endOfDay)) }
            .sumOf { it[KomisiSalesTable.nominalKomisi] }

        val labaBruto = penjualanBersih - hpp
        val labaBersih = labaBruto - biayaOperasional - biayaNonOperasional - komisi

        LabaRugiSummary(
            penjualanKotor, returPenjualan, penjualanBersih,
            pembelianGross, returPembelian, pembelianBersih,
            persediaanAwal, barangTersedia, persediaanAkhir,
            hpp, labaBruto, biayaOperasional, biayaNonOperasional, komisi, labaBersih
        )
    }

    override fun getLabaRugiBulanan(entitasId: UUID, tahun: Int, bulan: Int): LabaRugiSummary = transaction {
        val startOfMonth = LocalDate.of(tahun, bulan, 1).atStartOfDay()
        val endOfMonth = startOfMonth.plusMonths(1)

        val penjualanKotor = PenjualanTable
            .select { (PenjualanTable.entitasId eq entitasId) and (PenjualanTable.tanggal.between(startOfMonth, endOfMonth)) }
            .sumOf { it[PenjualanTable.total] }

        val returPenjualan = ReturPenjualanTable
            .select { (ReturPenjualanTable.entitasId eq entitasId) and (ReturPenjualanTable.tanggalRetur.between(startOfMonth, endOfMonth)) }
            .sumOf { it[ReturPenjualanTable.jumlahRetur] }

        val penjualanBersih = penjualanKotor - returPenjualan

        val pembelianGross = PembelianTable
            .select { (PembelianTable.entitasId eq entitasId) and (PembelianTable.tanggal.between(startOfMonth, endOfMonth)) }
            .sumOf { it[PembelianTable.total] }

        val returPembelian = ReturPembelianTable
            .select { (ReturPembelianTable.entitasId eq entitasId) and (ReturPembelianTable.tanggalRetur.between(startOfMonth, endOfMonth)) }
            .sumOf { it[ReturPembelianTable.jumlahRetur] }

        val pembelianBersih = pembelianGross - returPembelian

        val persediaanAwal = StockOpnameTable
            .select { (StockOpnameTable.entitasId eq entitasId) and (StockOpnameTable.tanggalOpname eq LocalDate.of(tahun, bulan, 1).atStartOfDay()) } // ✅ FIX: Compare LocalDate.atStartOfDay() with LocalDateTime
            .sumOf { row ->
                val produkId = row[StockOpnameTable.produkId]
                val stokFisik = row[StockOpnameTable.stokFisik]
                val hargaModal = ProdukTable
                    .select { ProdukTable.id eq produkId }.singleOrNull()?.get(ProdukTable.hargaModal) ?: 0.0 // ✅ FIX: produkId sudah EntityID
                stokFisik * hargaModal
            }

        val persediaanAkhir = ProdukTable
            .select { ProdukTable.entitasId eq entitasId }
            .sumOf { it[ProdukTable.stok] * it[ProdukTable.hargaModal] }

        val barangTersedia = persediaanAwal + pembelianBersih
        val hpp = barangTersedia - persediaanAkhir

        val biayaOperasional = BiayaOperasionalTable
            .select { (BiayaOperasionalTable.entitasId eq entitasId) and (BiayaOperasionalTable.tanggal.between(startOfMonth, endOfMonth)) }
            .sumOf { it[BiayaOperasionalTable.nominal] }

        val biayaNonOperasional = BiayaNonOperasionalTable
            .select { (BiayaNonOperasionalTable.entitasId eq entitasId) and (BiayaNonOperasionalTable.tanggal.between(startOfMonth, endOfMonth)) }
            .sumOf { it[BiayaNonOperasionalTable.nominal] }

        val komisi = KomisiSalesTable
            .select { (KomisiSalesTable.entitasId eq entitasId) and (KomisiSalesTable.tanggalKomisi.between(startOfMonth, endOfMonth)) }
            .sumOf { it[KomisiSalesTable.nominalKomisi] }

        val labaBruto = penjualanBersih - hpp
        val labaBersih = labaBruto - biayaOperasional - biayaNonOperasional - komisi

        LabaRugiSummary(
            penjualanKotor, returPenjualan, penjualanBersih,
            pembelianGross, returPembelian, pembelianBersih,
            persediaanAwal, barangTersedia, persediaanAkhir,
            hpp, labaBruto, biayaOperasional, biayaNonOperasional, komisi, labaBersih
        )
    }


    override fun getLabaRugiTahunan(entitasId: UUID, tahun: Int): LabaRugiSummary = transaction {
        val startOfYear = LocalDate.of(tahun, 1, 1).atStartOfDay()
        val endOfYear = startOfYear.plusYears(1)

        val penjualanKotor = PenjualanTable
            .select { (PenjualanTable.entitasId eq entitasId) and (PenjualanTable.tanggal.between(startOfYear, endOfYear)) }
            .sumOf { it[PenjualanTable.total] }

        val returPenjualan = ReturPenjualanTable
            .select { (ReturPenjualanTable.entitasId eq entitasId) and (ReturPenjualanTable.tanggalRetur.between(startOfYear, endOfYear)) }
            .sumOf { it[ReturPenjualanTable.jumlahRetur] }

        val penjualanBersih = penjualanKotor - returPenjualan

        val pembelianGross = PembelianTable
            .select { (PembelianTable.entitasId eq entitasId) and (PembelianTable.tanggal.between(startOfYear, endOfYear)) }
            .sumOf { it[PembelianTable.total] }

        val returPembelian = ReturPembelianTable
            .select { (ReturPembelianTable.entitasId eq entitasId) and (ReturPembelianTable.tanggalRetur.between(startOfYear, endOfYear)) }
            .sumOf { it[ReturPembelianTable.jumlahRetur] }

        val pembelianBersih = pembelianGross - returPembelian

        val persediaanAwal = StockOpnameTable
            .select { (StockOpnameTable.entitasId eq entitasId) and (StockOpnameTable.tanggalOpname eq LocalDate.of(tahun, 1, 1).atStartOfDay()) } // ✅ FIX: Compare LocalDate.atStartOfDay() with LocalDateTime
            .sumOf { row ->
                val produkId = row[StockOpnameTable.produkId]
                val stokFisik = row[StockOpnameTable.stokFisik]
                val hargaModal = ProdukTable
                    .select { ProdukTable.id eq produkId }.singleOrNull()?.get(ProdukTable.hargaModal) ?: 0.0
                stokFisik * hargaModal
            }

        val persediaanAkhir = ProdukTable
            .select { ProdukTable.entitasId eq entitasId }
            .sumOf { it[ProdukTable.stok] * it[ProdukTable.hargaModal] }

        val barangTersedia = persediaanAwal + pembelianBersih
        val hpp = barangTersedia - persediaanAkhir

        val biayaOperasional = BiayaOperasionalTable
            .select { (BiayaOperasionalTable.entitasId eq entitasId) and (BiayaOperasionalTable.tanggal.between(startOfYear, endOfYear)) }
            .sumOf { it[BiayaOperasionalTable.nominal] }

        val biayaNonOperasional = BiayaNonOperasionalTable
            .select { (BiayaNonOperasionalTable.entitasId eq entitasId) and (BiayaNonOperasionalTable.tanggal.between(startOfYear, endOfYear)) }
            .sumOf { it[BiayaNonOperasionalTable.nominal] }

        val komisi = KomisiSalesTable
            .select { (KomisiSalesTable.entitasId eq entitasId) and (KomisiSalesTable.tanggalKomisi.between(startOfYear, endOfYear)) }
            .sumOf { it[KomisiSalesTable.nominalKomisi] }

        val labaBruto = penjualanBersih - hpp
        val labaBersih = labaBruto - biayaOperasional - biayaNonOperasional - komisi

        LabaRugiSummary(
            penjualanKotor, returPenjualan, penjualanBersih,
            pembelianGross, returPembelian, pembelianBersih,
            persediaanAwal, barangTersedia, persediaanAkhir,
            hpp, labaBruto, biayaOperasional, biayaNonOperasional, komisi, labaBersih
        )
    }
}