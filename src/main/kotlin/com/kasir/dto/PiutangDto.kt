package com.kasir.dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class PiutangKaryawanSummary(
    val id: String,
    val karyawanId: String,
    val nama: String,
    val tanggal: String,
    val totalPiutang: Double,
    val sisaPiutang: Double,
    val tanggalJatuhTempo: String,
    val status: String,
    val entitasId: String

)

@Serializable
data class PiutangKaryawanHistory(
    val id: String,
    val tanggalBayar: String,
    val jumlahBayar: Double,
    val kasId: String,
    val keterangan: String? = null
)


@Serializable
data class PiutangPelangganSummaryDto(
    val id: String,
    val noNota: String,
    val nama: String,
    val totalPiutang: Double,
    val sisaPiutang: Double,
    val jatuhTempo: String,
    val status: String,
    val fotoNotaUrl: String? = null,
    val entitasId: String
)

@Serializable
data class PiutangPelangganHistoryDto(
    val id: String,
    val tanggalBayar: String,
    val jumlahBayar: Double,
    @Contextual val kasId: UUID?,
    val keterangan: String? = null
)

@Serializable
data class PiutangPelangganBayarRequestDto(
    val kasId: String,
    val tanggalBayar: String,
    val jumlahBayar: Double,
    val keterangan: String? = null
)

@Serializable
data class PiutangLainSummaryDto(
    val id: String,
    val keterangan: String,
    val totalPiutang: Double,
    val sisaPiutang: Double,
    val jatuhTempo: String,
    val status: String,
    val entitasId: String
)

@Serializable
data class PiutangLainBayarRequestDto(
    val kasId: String,
    val tanggalBayar: String,
    val jumlahBayar: Double,
    val keterangan: String? = null
)
@Serializable
data class PiutangKaryawanEntryDto(
    val id: String,
    val pegawai: String,
    val tanggalJatuhTempo: String,
    val totalPiutang: Double,
    val sisaPiutang: Double,
    val status: String,
    val entitasId: String
)

/* DTO untuk entry ringkasan piutang pelanggan
*/
@Serializable
data class PiutangPelangganEntryDto(
    val pelanggan: String,
    val tanggalJatuhTempo: String,
    val totalPiutang: Double,
    val sisaPiutang: Double,
    val status: String,
    val penjualanId: String? = null,
    val noNota: String? = null,
    val notaUrl: String? = null,
    val entitasId: String
)


// DTO untuk piutang-lain
@Serializable
data class PiutangLainEntryDto(
    val id: String,
    val keterangan: String,
    val tanggalJatuhTempo: String,
    val totalPiutang: Double,
    val sisaPiutang: Double,
    val status: String,
    val entitasId: String
)


/**
 * DTO untuk histori pembayaran piutang lain-lain
 * @param id            UUID pembayaran piutang lain
 * @param tanggalBayar  Waktu pembayaran (ISO-8601 string)
 * @param jumlahBayar   Nominal yang dibayarkan
 * @param kasId         UUID akun kas yang digunakan
 * @param keterangan    Keterangan pembayaran (nullable)
 */
@Serializable
data class PiutangLainHistoryDto(
    val id: String,
    val tanggalBayar: String,
    val jumlahBayar: Double,
    val kasId: String,
    val keterangan: String? = null
)




/**
 * DTO untuk entry laporan pembayaran piutang
 * @param tanggal   Tanggal pembayaran (ISO-8601 string)
 * @param pelanggan Nama pelanggan
 * @param jumlah    Nominal bayar
 * @param keterangan Keterangan pembayaran
 */
@Serializable
data class PembayaranPiutangEntryDto(
    val tanggal: String,
    val pelanggan: String,
    val jumlah: Double,
    val keterangan: String,
    val entitasId: String
)

@Serializable
public data class PiutangPelangganUpdateRequestDto(
    val totalPiutang: Double,
    val sisaPiutang: Double,
    val status: String,
    val jatuhTempo: String?,
    val fotoNotaUrl: String?
)




/** Ringkasan piutang lain-lain */
@Serializable
data class PiutangLainSummary(
    val id: String,
    val keterangan: String,
    val totalPiutang: Double,
    val sisaPiutang: Double,
    val jatuhTempo: String,
    val status: String,
    val entitasId: String // ✅ tambahan
)

/** Request bayar piutang lain-lain */
@Serializable
data class PiutangLainBayarRequest(
    val kasId: String,
    val tanggalBayar: String,
    val jumlahBayar: Double,
    val keterangan: String? = null
)



@Serializable
data class PiutangKaryawanBayarRequest(
    val kasId: String,
    val tanggalBayar: String,
    val jumlahBayar: Double,
    val keterangan: String? = null
)



@Serializable
data class PiutangPelangganSummary(
    val id: String,
    val noNota: String,
    val nama: String,
    val totalPiutang: Double,
    val sisaPiutang: Double,
    val jatuhTempo: String,
    val status: String,
    val fotoNotaUrl: String? = null,
    val entitasId: String // ✅ tambahan penting
)


@Serializable
data class PiutangPelangganHistory(
    val id: String,
    val tanggalBayar: String,
    val jumlahBayar: Double,
    val kasId: String,
    val keterangan: String
)

@Serializable
data class PiutangPelangganBayarRequest(
    val kasId: String,
    val tanggalBayar: String,
    val jumlahBayar: Double,
    val keterangan: String? = null
)
data class PembayaranPiutangPelanggan(
    @Contextual val id: UUID,
    @Contextual val piutangId: UUID,
    @Contextual val tanggalBayar: LocalDateTime,
    val jumlahBayar: Double,
    @Contextual val kasId: UUID,
    val keterangan: String,
    @Contextual val entitasId: UUID // ✅ Tambahan penting
)
