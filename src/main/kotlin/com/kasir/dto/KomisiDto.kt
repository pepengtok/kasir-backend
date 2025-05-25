// File: src/main/kotlin/com/kasir/dto/KomisiDto.kt
package com.kasir.dto
import kotlinx.serialization.Serializable

/**
 * DTO untuk satu entri komisi sales (dipakai di laporan /komisi)
 * @param id             UUID komisi_sales
 * @param tanggalKomisi  Timestamp komisi dicatat (ISO-8601 string)
 * @param salesId        UUID salesman yang dapat komisi
 * @param komisiPersen   Persentase komisi (%)
 * @param nominalKomisi  Nominal komisi (total * persen)
 * @param status         Status: "PENDING" atau "PAID"
 * @param canPay  true jika status == "PENDING" dan penjualan sudah "LUNAS"
 */
@Serializable
data class KomisiEntryDto(
    val id: String,
    val tanggalKomisi: String,
    val salesId: String,
    val namaSales: String, // PERBAIKAN: Menambahkan field namaSales
    val penjualanId: String,
    val komisiPersen: Double,
    val nominalKomisi: Double,
    val status: String,
    val canPay: Boolean,
    val notaId: String,
    val notaUrl: String?,
    val entitasId: String
)


@Serializable
data class DetailKomisiRequestDto(
    val komisiId: String
)

/**
 * DTO untuk request menandai komisi telah dibayar
 *
 * @param komisiId      UUID komisi_sales
 * @param kasId         UUID akun kas yang digunakan
 * @param tanggalBayar  ISO-8601 string
 */
@Serializable
data class BayarKomisiRequestDto(
    val komisiId: String,
    val kasId: String,
    val tanggalBayar: String
)
/**
 * DTO untuk request batch pembayaran komisi
 * @param komisiIds list ID komisi yang akan dibayar
 * @param kasId     UUID akun kas yang digunakan untuk pembayaran
 */
@Serializable
data class BatchBayarKomisiRequest(
    val komisiIds: List<String>,
    val kasId: String,
    val entitasId: String // ✅ WAJIB
)

/**
 * DTO untuk response batch pembayaran komisi
 * @param paidCount  jumlah komisi yang berhasil dibayar
 * @param totalAmount total nominal komisi yang dibayarkan
 */
@Serializable
data class BatchBayarKomisiResponse(
    val paidCount: Int,
    val totalAmount: Double
)
@Serializable
data class KomisiResponseDto(
    val entries: List<KomisiEntryDto>
)
/**
 * DTO untuk response komisi sales
 */
@Serializable
data class KomisiSalesResponse(
    val id: String,
    val salesId: String,
    val penjualanId: String,
    val komisiPersen: Double,
    val nominalKomisi: Double,
    val status: String,
    val tanggalKomisi: String,
    val entitasId: String
)

/**
 * DTO untuk request menandai komisi dibayar
 */
@Serializable
data class BayarKomisiRequest(
    val komisiId: String,
    val entitasId: String
)