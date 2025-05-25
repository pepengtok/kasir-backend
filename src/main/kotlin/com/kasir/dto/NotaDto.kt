package com.kasir.dto
import kotlinx.serialization.Serializable
class NotaDto {
}// Contoh DTO baru (di file dto/NotaDto.kt)
@Serializable
data class NotaDataDto(
    val noFaktur: String?, // Nullable
    val tanggal: String, // String karena formatDate mengembalikannya
    val metodePembayaran: String,
    val jatuhTempo: String?, // String karena formatDate mengembalikannya
    val supplierName: String,
    val total: Double,
    val details: List<NotaDetailItemDto>, // List of specific DTO
    val status: String,
    val tokoNama: String,
    val tokoAlamat: String,
    val tokoPhone: String,
    val tokoFooterNota: String
)

@Serializable
data class NotaDetailItemDto(
    val id: String,
    val produkName: String,
    val jumlah: Int,
    val satuan: String?, // Nullable
    val hargaModal: Double,
    val subtotal: Double
)