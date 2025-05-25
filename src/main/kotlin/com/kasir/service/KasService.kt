package com.kasir.service

import com.kasir.models.EntitasUsahaTable
import com.kasir.models.KasTable
import com.kasir.models.KasTransaksiTable
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import java.util.UUID

object KasService {

    /**
     * Catat transaksi kas (MASUK/KELUAR), perbarui saldo, dan kembalikan ID transaksi baru.
     * Sekarang mendukung multi-entitas.
     *
     * @throws io.ktor.server.plugins.NotFoundException jika akun kas tidak ditemukan
     * @throws IllegalArgumentException jika tipe bukan MASUK/KELUAR atau jumlah <= 0
     * @throws IllegalArgumentException jika kas bukan milik entitas yang aktif
     */
    fun record(
        entitasId: UUID, // ✅ Tambahan untuk mendukung multi entitas
        kasId: UUID,
        tanggal: LocalDateTime,
        jumlah: Double,
        tipe: String,
        keterangan: String
    ): UUID = transaction {
        // 0) Validasi input
        require(jumlah > 0) { "Jumlah transaksi harus positif" }
        val t = tipe.uppercase()
        require(t == "MASUK" || t == "KELUAR") { "Tipe harus 'MASUK' atau 'KELUAR'" }

        // 1) Pastikan akun kas ada
        val kasRow = KasTable
            .select { KasTable.id eq EntityID(kasId, KasTable) }
            .singleOrNull()
            ?: throw NotFoundException("Akun kas dengan id $kasId tidak ditemukan")

        // 2) Validasi kas milik entitas yang sesuai
        if (kasRow[KasTable.entitasId].value != entitasId)
            throw IllegalArgumentException("Kas ini bukan milik entitas yang sedang aktif")

        // 3) Insert ke kas_transaksi dan ambil ID-nya
        val txId = KasTransaksiTable
            .insertAndGetId { row ->
                row[KasTransaksiTable.entitasId] = EntityID(entitasId, EntitasUsahaTable) // ✅ Tambahan penting
                row[KasTransaksiTable.kasId] = EntityID(kasId, KasTable)
                row[KasTransaksiTable.tanggal] = tanggal
                row[KasTransaksiTable.jumlah] = jumlah
                row[KasTransaksiTable.tipe] = t
                row[KasTransaksiTable.keterangan] = keterangan
            }
            .value

        // 4) Hitung saldo baru
        val currentSaldo = kasRow[KasTable.saldoAkhir]
        val updatedSaldo = if (t == "MASUK") currentSaldo + jumlah else currentSaldo - jumlah

        // 5) Update saldo akhir di KasTable
        KasTable.update({ KasTable.id eq EntityID(kasId, KasTable) }) {
            it[KasTable.saldoAkhir] = updatedSaldo
        }

        // 6) Kembalikan ID transaksi kas untuk referensi
        txId
    }
}