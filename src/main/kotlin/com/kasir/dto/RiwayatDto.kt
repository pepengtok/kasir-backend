package com.kasir.dto

import kotlinx.serialization.Serializable

/**
 * DTO untuk detail riwayat transaksi pembelian/penjualan per produk
 * @param tanggal  Tanggal transaksi
 * @param jumlah   Jumlah unit yang terlibat
 * @param harga    Harga per unit pada transaksi
 * @param subtotal Total pada transaksi (jumlah * harga)
 */
@Serializable
data class RiwayatDetailDto(
    val tanggal: String,
    val jumlah: Double,
    val harga: Double,
    val subtotal: Double
)
/**
 * DTO untuk membungkus daftar riwayat pembelian dan penjualan
 * @param pembelian Daftar detail riwayat pembelian
 * @param penjualan Daftar detail riwayat penjualan
 */

@Serializable
data class RiwayatResponseDto(
    val pembelian: List<RiwayatDetailDto>,
    val penjualan: List<RiwayatDetailDto>
)