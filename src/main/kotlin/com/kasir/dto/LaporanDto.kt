package com.kasir.dto

import kotlinx.serialization.Serializable

@Serializable
data class KomisiEntry(
    val id: String,
    val tanggalKomisi: String,
    val salesId: String,
    val komisiPersen: Double,
    val nominalKomisi: Double,
    val status: String
)

@Serializable
data class SalesEntryDto(
    val id: String,
    val namaSales: String
)



@Serializable
data class PembelianEntryDto(
    val id: String,
    val tanggal: String,
    val noFaktur: String?,
    val namaSupplier: String?,
    val metodePembayaran: String,
    val total: Double,
    val entitasId: String
)

@Serializable
data class KasEntryDto(
    val id: String,
    val tanggal: String,
    val amount: Double,
    val entitasId: String
)

@Serializable
data class BiayaEntryDto(
    val id: String,
    val tanggal: String,
    val nominal: Double,
    val jenis: String,
    val entitasId: String
)

@Serializable
data class NeracaAktiva(
    val kas: Double,
    val piutang: Double,
    val persediaan: Double
)

@Serializable
data class NeracaPasiva(
    val hutang: Double,
    val modal: Double,
    val prive: Double
)

@Serializable
data class NeracaResponseDto(
    val aktiva: NeracaAktiva,
    val pasiva: NeracaPasiva,
    val entitasId: String
)
@Serializable
data class ModalEntry(
    val id: String,
    val tanggal: String,
    val kasId: String,
    val nominal: Double,
    val entitasId: String
)

@Serializable
data class KasOption(
    val id: String,
    val namaKas: String,
    val entitasId: String
)

@Serializable
data class ModalResponseDto(
    val modals: List<ModalEntry>,
    val kasOptions: List<KasOption>,
    val entitasId: String
)

@Serializable
data class PriveEntryDto(
    val id: String,
    val tanggal: String,
    val kasId: String,
    val nominal: Double,
    val entitasId: String
)

@Serializable
data class OmsetEntryDto(
    val salesId: String,
    val total: Double,
    val entitasId: String
)

@Serializable
data class ReturPenjualanEntryDto(
    val id: String,
    val tanggalRetur: String,
    val jumlahRetur: Double,
    val keterangan: String,
    val pelanggan: String,
    val penjualanId: String,
    val kasId: String,
    val entitasId: String
)
@Serializable
data class ReturPembelianEntryDto(
    val id: String,
    val tanggalRetur: String,
    val jumlahRetur: Double,
    val keterangan: String,
    val supplier: String,
    val pembelianId: String,
    val kasId: String,
    val entitasId: String
)

@Serializable
data class LabaRugiDto(
    val penjualanKotor: Double,
    val returPenjualan: Double,
    val penjualanBersih: Double,
    val pembelianGross: Double,
    val returPembelian: Double,
    val pembelianBersih: Double,
    val persediaanAwal: Double,
    val barangTersedia: Double,
    val persediaanAkhir: Double,
    val hpp: Double,
    val labaBruto: Double,
    val biayaOperasional: Double,
    val biayaNonOperasional: Double,
    val komisi: Double,
    val labaBersih: Double,
    val entitasId: String
)
