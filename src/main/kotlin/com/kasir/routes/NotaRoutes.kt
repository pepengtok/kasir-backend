package com.kasir.routes

import com.kasir.models.PembelianTable
import com.kasir.models.PenjualanTable
import com.kasir.models.EntitasUsahaTable
import com.kasir.models.SupplierTable
import com.kasir.models.ProdukTable
import com.kasir.models.PembelianDetailTable
import com.kasir.models.PelangganTable
import com.kasir.models.PenjualanDetailTable

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.html.* // Import ini untuk HTML DSL
import kotlinx.html.* // Import ini untuk HTML DSL
import io.ktor.server.routing.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

// Helper function to format currency (copied from frontend)
fun formatRupiah(angka: Double?): String {
    return if (angka == null) "-" else "Rp" + "%,.0f".format(angka).replace(',', '.')
}

// Helper function to format LocalDateTime (for tanggal pembelian)
fun formatDate(date: LocalDateTime?): String {
    if (date == null) return "-"
    return date.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
}

// Helper function to format LocalDate (for tanggal penjualan dan jatuhTempo)
fun formatDate(date: LocalDate?): String {
    if (date == null) return "-"
    return date.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
}

// Helper function to convert numbers to words in Indonesian
fun terbilang(nilai: Double?): String {
    val number = nilai?.toLong() ?: 0L
    if (number == 0L) return "Nol Rupiah"

    val satuan = arrayOf("", "Satu", "Dua", "Tiga", "Empat", "Lima", "Enam", "Tujuh", "Delapan", "Sembilan", "Sepuluh", "Sebelas")

    fun bilang(n: Long): String {
        if (n < 12) return satuan[n.toInt()]
        if (n < 20) return bilang(n - 10) + " Belas"
        if (n < 100) {
            val puluhanIndex = (n / 10).toInt()
            val satuanIndex = (n % 10).toInt()
            return arrayOf("", "Sepuluh", "Dua Puluh", "Tiga Puluh", "Empat Puluh", "Lima Puluh", "Enam Puluh", "Tujuh Puluh", "Delapan Puluh", "Sembilan Puluh")[puluhanIndex] +
                    (if (satuanIndex > 0) " " + satuan[satuanIndex] else "")
        }
        if (n < 200) return "Seratus " + bilang(n - 100)
        if (n < 1000) return satuan[(n / 100).toInt()] + " Ratus " + bilang(n % 100)
        if (n < 2000) return "Seribu " + bilang(n - 1000)
        if (n < 1000000) return bilang(n / 1000) + " Ribu " + bilang(n % 1000)
        if (n < 1000000000) return bilang(n / 1000000) + " Juta " + bilang(n % 1000000)
        return ""
    }

    return "${bilang(number).trim()} Rupiah".trim()
}


fun Route.notaRoutes() {
    route("/nota") { // Ini public
    // Helper untuk mendapatkan entitasId dari JWT (sekarang bisa null jika tidak ada token)
    fun ApplicationCall.getEntitasIdFromOptionalJwt(): UUID? {
        val principal = principal<JWTPrincipal>()
        return principal?.payload?.getClaim("entitasId")?.asString()?.let { UUID.fromString(it) }
    }

        // Rute untuk nota pembelian
        get("/pembelian/{id}") {
            val entitasUUIDFromToken = call.getEntitasIdFromOptionalJwt()
            val pembelianId = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID pembelian tidak ada"))

            val transactionResult = transaction { // Ini mengembalikan Pair<HttpStatusCode, Map<String, Any?>>
                val pembelianUUID = runCatching { UUID.fromString(pembelianId) }.getOrNull()
                    ?: return@transaction Pair(HttpStatusCode.BadRequest, mapOf<String, Any?>("error" to "Invalid UUID format for purchase ID"))

                val header = PembelianTable
                    .join(SupplierTable, JoinType.INNER) { PembelianTable.supplierId eq SupplierTable.id }
                    .select {
                        val condition = PembelianTable.id eq pembelianUUID
                        if (entitasUUIDFromToken != null) {
                            condition and (PembelianTable.entitasId eq EntityID(entitasUUIDFromToken, EntitasUsahaTable))
                        } else {
                            condition
                        }
                    }
                    .singleOrNull()
                    ?: return@transaction Pair(HttpStatusCode.NotFound, mapOf<String, Any?>("error" to "Data pembelian tidak ditemukan"))

                // Detail pembelian dipetakan ke Map<String, Any?>
                val details = PembelianDetailTable
                    .join(ProdukTable, JoinType.INNER) { PembelianDetailTable.produkId eq ProdukTable.id }
                    .select {
                        val condition = PembelianDetailTable.pembelianId eq pembelianUUID
                        if (entitasUUIDFromToken != null) {
                            condition and (PembelianDetailTable.entitasId eq EntityID(entitasUUIDFromToken, EntitasUsahaTable))
                        } else {
                            condition
                        }
                    }
                    .map { row ->
                        mapOf<String, Any?>(
                            "id" to row[PembelianDetailTable.id].value.toString(),
                            "produkName" to row[ProdukTable.namaProduk],
                            "jumlah" to row[PembelianDetailTable.jumlah],
                            "satuan" to row[ProdukTable.satuan], // Mengambil satuan dari ProdukTable
                            "hargaModal" to row[PembelianDetailTable.hargaModal],
                            "subtotal" to row[PembelianDetailTable.subtotal]
                        )
                    }

                val entitasInfo = (entitasUUIDFromToken ?: header[PembelianTable.entitasId].value).let { idEntitas ->
                    EntitasUsahaTable
                        .select { EntitasUsahaTable.id eq EntityID(idEntitas, EntitasUsahaTable) }
                        .singleOrNull()
                }

                // Data nota utama dipetakan ke Map<String, Any?>
                val notaDataMap = mapOf<String, Any?>(
                    "noFaktur" to header[PembelianTable.noFaktur],
                    "tanggal" to header[PembelianTable.tanggal],
                    "metodePembayaran" to header[PembelianTable.metodePembayaran],
                    "jatuhTempo" to header[PembelianTable.jatuhTempo],
                    "supplierName" to header[SupplierTable.namaSupplier],
                    "total" to header[PembelianTable.total],
                    "details" to details, // Ini adalah List<Map<String, Any?>>
                    "status" to header[PembelianTable.status],
                    "tokoNama" to (entitasInfo?.getOrNull(EntitasUsahaTable.nama) ?: "Nama Toko Default"), // Use getOrNull and fallback
                    "tokoAlamat" to "Alamat Toko Default", // Tetap hardcode
                    "tokoPhone" to "0000-0000",             // Tetap hardcode
                    "tokoFooterNota" to "Terima kasih atas kepercayaan Anda berbelanja!" // Footer dari NotaPembelian.jsx
                )

                Pair(HttpStatusCode.OK, notaDataMap)
            }

            // Pindahkan call.respond di luar blok transaction dan perbaiki struktur if/else
            if (transactionResult.first != HttpStatusCode.OK) {
                call.respond(transactionResult.first, transactionResult.second)
            } else { // Ini adalah blok 'else' yang benar untuk HttpStatusCode.OK
                @Suppress("UNCHECKED_CAST") // Suppress karena kita yakin dengan struktur data
                val notaData = transactionResult.second as Map<String, Any?> // Gunakan Any?
                call.respondHtml(HttpStatusCode.OK) {
                    lang = "id"
                    head {
                        meta(charset = "UTF-8")
                        title("Nota Pembelian - ${notaData["noFaktur"] as String? ?: "Nota"}") // Gunakan as? dan fallback
                        style {
                            unsafe {
                                +"""
                                    @page {
                                        size: A5 landscape;
                                        margin: 0.5cm;
                                    }
                                    body {
                                        font-family: "Courier New", monospace;
                                        font-size: 10pt;
                                        line-height: 1.2;
                                        margin: 0;
                                        padding: 0;
                                        box-sizing: border-box;
                                    }
                                    .container {
                                        width: 100%;
                                        max-width: 210mm; /* A5 width */
                                        height: 100%;
                                        max-height: 148mm; /* A5 height */
                                        padding: 0.5cm;
                                    }
                                    .header {
                                        display: flex;
                                        justify-content: space-between;
                                        margin-bottom: 10px;
                                        padding-bottom: 4px;
                                        border-bottom: 1px solid #000;
                                    }
                                    .company-info {
                                        text-align: left;
                                    }
                                    .invoice-info {
                                        text-align: right;
                                    }
                                    table {
                                        width: 100%;
                                        border-collapse: collapse;
                                        margin-bottom: 6px;
                                    }
                                    th, td {
                                        border: 1px solid #000;
                                        padding: 4px;
                                    }
                                    th {
                                        text-align: center;
                                        font-weight: bold;
                                    }
                                    .text-center {
                                        text-align: center;
                                    }
                                    .text-right {
                                        text-align: right;
                                    }
                                    .totals {
                                        display: flex;
                                        justify-content: space-between;
                                        margin: 10px 0;
                                        font-size: 11pt;
                                        font-weight: bold;
                                    }
                                    .signatures {
                                        display: flex;
                                        justify-content: space-between;
                                        margin-top: 30px;
                                    }
                                    .signature {
                                        text-align: center;
                                        width: 150px;
                                    }
                                    .signature-line {
                                        margin-top: 50px;
                                        border-top: 1px solid #000;
                                    }
                                    .dot-matrix-line {
                                        border-top: 1px dashed #000;
                                        margin: 5px 0;
                                    }
                                    @media print {
                                        /* Hide non-print elements if any */
                                    }
                                    """
                            }
                        }
                        // SKRIP UNTUK OTOMATIS CETAK
                        script {
                            unsafe {
                                +"""
                                window.onload = function() {
                                    window.print();
                                    // Opsional: Tutup jendela setelah mencetak.
                                    // window.onafterprint = function() {
                                    //     window.close();
                                    // };
                                };
                                """
                            }
                        }
                    }
                    body {
                        div("container") {
                            div("header") {
                                div("company-info") {
                                    h1 { style = "font-size: 18pt; margin: 0;"; +(notaData["tokoNama"] as String) }
                                    p { style = "font-weight: bold; margin: 0;"; +(notaData["tokoNama"] as String) }
                                    p { style = "margin: 5px 0 0 0;"; strong { +"No Faktur:" }; +" ${notaData["noFaktur"] as String? ?: "-"}" } // Handle nullable noFaktur
                                    p {
                                        strong { +"Status:" }
                                        span {
                                            val status = notaData["status"] as String
                                            style = "margin-left: 8px; padding: 2px 8px; border-radius: 9999px; font-size: 0.75rem; font-weight: 500; background-color: ${if (status == "LUNAS") "#dcfce7" else "#fef9c3"}; color: ${if (status == "LUNAS") "#166534" else "#854d0e"};"
                                            +(if (status == "LUNAS") "LUNAS" else "BELUM LUNAS")
                                        }
                                    }
                                }
                                div("invoice-info") {
                                    p { style = "margin: 0;"; strong { +"Tanggal:" }; +" ${formatDate(notaData["tanggal"] as LocalDateTime?)}" }
                                    p { style = "margin: 0;"; strong { +"Metode Pembayaran:" }; +" ${notaData["metodePembayaran"] as String}" }
                                    (notaData["jatuhTempo"] as LocalDateTime?)?.let {
                                        p { style = "margin: 0;"; strong { +"Jatuh Tempo:" }; +" ${formatDate(it.toLocalDate())}" }
                                    }
                                    p { style = "margin-top: 8px;"; strong { +"Supplier:" }; +" ${notaData["supplierName"] as String}" }
                                    p { style = "margin: 0;"; strong { +"Alamat:" }; +" ${notaData["tokoAlamat"] as String}" }
                                    p { style = "margin: 0;"; strong { +"Telp:" }; +" ${notaData["tokoPhone"] as String}" }
                                }
                            }
                            div("dot-matrix-line") {}
                            table {
                                thead {
                                    tr {
                                        th { style = "width: 5%;"; +"No" }
                                        th { style = "width: 45%; text-align: left;"; +"Nama Produk" }
                                        th { style = "width: 15%;"; +"Jumlah" }
                                        th { style = "width: 15%; text-align: right;"; +"Harga Modal" }
                                        th { style = "width: 20%; text-align: right;"; +"Subtotal" }
                                    }
                                }
                                tbody {
                                    @Suppress("UNCHECKED_CAST")
                                    val details = notaData["details"] as List<Map<String, Any?>> // Gunakan Any?
                                    if (details.isNotEmpty()) {
                                        details.forEachIndexed { idx, item ->
                                            tr {
                                                td("text-center") { +(idx + 1).toString() }
                                                td { +(item["produkName"] as String) }
                                                td("text-center") { +"${item["jumlah"] as Int} ${item["satuan"] as String? ?: "-"}" } // Handle nullable satuan
                                                td("text-right") { +formatRupiah(item["hargaModal"] as Double?) }
                                                td("text-right") { +formatRupiah(item["subtotal"] as Double?) }
                                            }
                                        }
                                    } else {
                                        tr {
                                            td { attributes["colspan"] = "5"; style = "text-align: center;"; +"Tidak ada data produk" }
                                        }
                                    }
                                }
                                tfoot {
                                    tr {
                                        td { attributes["colspan"] = "4"; style = "text-align: right; font-weight: bold;"; +"Grand Total:" }
                                        td { style = "text-align: right; font-weight: bold;"; +formatRupiah(notaData["total"] as Double?) }
                                    }
                                }
                            }
                            div("dot-matrix-line") {}
                            div("totals") { // Totals section
                                div {
                                    strong { +"Terbilang:" }
                                    +" ${terbilang(notaData["total"] as Double?)}"
                                }
                                div {
                                    strong { +"Grand Total:" }
                                    +" ${formatRupiah(notaData["total"] as Double?)}"
                                }
                            }
                            div("signatures") {
                                div("signature") {
                                    p { +"Penerima" }
                                    div("signature-line") {}
                                }
                                div("signature") {
                                    p { +"Hormat Kami" }
                                    div("signature-line") {}
                                }
                            }
                            div("dot-matrix-line") {}
                            p {
                                style = "text-align: center; font-size: 8pt; margin-top: 10px;";
                                +(notaData["tokoFooterNota"] as String)
                            }
                        }
                    }
                }
            }
        }

        // Rute untuk nota penjualan
        get("/penjualan/{id}") {
            val entitasUUIDFromToken = call.getEntitasIdFromOptionalJwt()
            val penjualanId = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID penjualan tidak ada"))

            val transactionResult = transaction {
                val penjualanUUID = runCatching { UUID.fromString(penjualanId) }.getOrNull()
                    ?: return@transaction Pair(HttpStatusCode.BadRequest, mapOf<String, Any?>("error" to "Invalid UUID format for sales ID"))

                val header = PenjualanTable
                    .join(PelangganTable, JoinType.LEFT) { PenjualanTable.pelangganId eq PelangganTable.id }
                    .select {
                        val condition = PenjualanTable.id eq penjualanUUID
                        if (entitasUUIDFromToken != null) {
                            condition and (PenjualanTable.entitasId eq EntityID(entitasUUIDFromToken, EntitasUsahaTable))
                        } else {
                            condition
                        }
                    }
                    .singleOrNull()
                    ?: return@transaction Pair(HttpStatusCode.NotFound, mapOf<String, Any?>("error" to "Data penjualan tidak ditemukan"))

                // Detail penjualan dipetakan ke Map<String, Any?>
                val details = PenjualanDetailTable
                    .join(ProdukTable, JoinType.INNER) { PenjualanDetailTable.produkId eq ProdukTable.id }
                    .select {
                        val condition = PenjualanDetailTable.penjualanId eq penjualanUUID
                        if (entitasUUIDFromToken != null) {
                            condition and (PenjualanDetailTable.entitasId eq EntityID(entitasUUIDFromToken, EntitasUsahaTable))
                        } else {
                            condition
                        }
                    }
                    .map { row ->
                        mapOf<String, Any?>(
                            "id" to row[PenjualanDetailTable.id].value.toString(),
                            "namaProduk" to row[ProdukTable.namaProduk],
                            "jumlah" to row[PenjualanDetailTable.jumlah],
                            "satuan" to row[ProdukTable.satuan], // Mengambil satuan dari ProdukTable
                            "hargaJual" to row[PenjualanDetailTable.hargaJual],
                            "subtotal" to row[PenjualanDetailTable.subtotal]
                        )
                    }

                val entitasInfo = (entitasUUIDFromToken ?: header[PenjualanTable.entitasId].value).let { idEntitas ->
                    EntitasUsahaTable
                        .select { EntitasUsahaTable.id eq EntityID(idEntitas, EntitasUsahaTable) }
                        .singleOrNull()
                }

                // Data nota utama dipetakan ke Map<String, Any?>
                val notaDataMap = mapOf<String, Any?>(
                    "noNota" to header[PenjualanTable.noNota],
                    "tanggal" to header[PenjualanTable.tanggal],
                    "metodePembayaran" to header[PenjualanTable.metodePembayaran],
                    "jatuhTempo" to header[PenjualanTable.jatuhTempo],
                    "namaPelanggan" to header[PelangganTable.namaPelanggan],
                    "total" to header[PenjualanTable.total],
                    "details" to details, // Ini adalah List<Map<String, Any?>>
                    "status" to header[PenjualanTable.status],
                    "tokoNama" to (entitasInfo?.getOrNull(EntitasUsahaTable.nama) ?: "Nama Toko Default"), // Use getOrNull and fallback
                    "tokoAlamat" to "Alamat Toko Default", // Hardcode
                    "tokoPhone" to "0000-0000",             // Hardcode
                    "tokoFooterNota" to "Terima kasih atas pembelian Anda!" // Hardcode
                )
                Pair(HttpStatusCode.OK, notaDataMap)
            }

            // Pindahkan call.respond di luar blok transaction dan perbaiki struktur if/else
            if (transactionResult.first != HttpStatusCode.OK) {
                call.respond(transactionResult.first, transactionResult.second)
            } else { // Ini adalah blok 'else' yang benar untuk HttpStatusCode.OK
                @Suppress("UNCHECKED_CAST")
                val notaData = transactionResult.second as Map<String, Any?>
                call.respondHtml(HttpStatusCode.OK) {
                    lang = "id"
                    head {
                        meta(charset = "UTF-8")
                        title("Nota Penjualan - ${notaData["noNota"] as String? ?: "Nota"}") // Handle nullable noNota
                        style {
                            unsafe {
                                +"""
                                    @page {
                                        size: A5 landscape;
                                        margin: 0.5cm;
                                    }
                                    body {
                                        font-family: "Courier New", monospace;
                                        font-size: 10pt;
                                        line-height: 1.2;
                                        margin: 0;
                                        padding: 0;
                                        box-sizing: border-box;
                                    }
                                    .container {
                                        width: 100%;
                                        max-width: 210mm; /* A5 width */
                                        height: 100%;
                                        max-height: 148mm; /* A5 height */
                                        padding: 0.5cm;
                                    }
                                    .header {
                                        display: flex;
                                        justify-content: space-between;
                                        margin-bottom: 10px;
                                        padding-bottom: 4px;
                                        border-bottom: 1px solid #000;
                                    }
                                    .company-info {
                                        text-align: left;
                                    }
                                    .invoice-info {
                                        text-align: right;
                                    }
                                    table {
                                        width: 100%;
                                        border-collapse: collapse;
                                        margin-bottom: 6px;
                                    }
                                    th, td {
                                        border: 1px solid #000;
                                        padding: 4px;
                                    }
                                    th {
                                        text-align: center;
                                        font-weight: bold;
                                    }
                                    .text-center {
                                        text-align: center;
                                    }
                                    .text-right {
                                        text-align: right;
                                    }
                                    .totals {
                                        display: flex;
                                        justify-content: space-between;
                                        margin: 10px 0;
                                        font-size: 11pt;
                                        font-weight: bold;
                                    }
                                    .signatures {
                                        display: flex;
                                        justify-content: space-between;
                                        margin-top: 20px;
                                    }
                                    .signature {
                                        text-align: center;
                                        width: 150px;
                                    }
                                    .signature-line {
                                        margin-top: 40px;
                                        border-top: 1px solid #000;
                                    }
                                    .dot-matrix-line {
                                        border-top: 1px dashed #000;
                                        margin: 5px 0;
                                    }
                                    @media print {
                                        /* Hide non-print elements if any */
                                    }
                                    """
                            }
                        }
                        // SKRIP UNTUK OTOMATIS CETAK
                        script {
                            unsafe {
                                +"""
                                window.onload = function() {
                                    window.print();
                                    // Opsional: Tutup jendela setelah mencetak.
                                    // window.onafterprint = function() {
                                    //     window.close();
                                    // };
                                };
                                """
                            }
                        }
                    }
                    body {
                        div("container") {
                            div("header") {
                                div("company-info") {
                                    h1 { style = "font-size: 18pt; margin: 0;"; +(notaData["tokoNama"] as String) }
                                    p { style = "font-weight: bold; margin: 0;"; +(notaData["tokoNama"] as String) }
                                    p { style = "margin: 5px 0 0 0;"; strong { +"No Nota:" }; +" ${notaData["noNota"] as String? ?: "-"}" }
                                    p {
                                        strong { +"Status:" }
                                        span {
                                            val status = notaData["status"] as String
                                            style = "margin-left: 8px; padding: 2px 8px; border-radius: 9999px; font-size: 0.75rem; font-weight: 500; background-color: ${if (status == "LUNAS") "#dcfce7" else "#fef9c3"}; color: ${if (status == "LUNAS") "#166534" else "#854d0e"};"
                                            +(if (status == "LUNAS") "LUNAS" else "BELUM LUNAS")
                                        }
                                    }
                                }
                                div("invoice-info") {
                                    p { style = "margin: 0;"; strong { +"Tanggal:" }; +" ${formatDate(notaData["tanggal"] as LocalDate?)}" }
                                    p { style = "margin: 0;"; strong { +"Metode Pembayaran:" }; +" ${notaData["metodePembayaran"] as String}" }
                                    (notaData["jatuhTempo"] as LocalDate?)?.let {
                                        if ((notaData["metodePembayaran"] as String).equals("PIUTANG", ignoreCase = true)) { // Conditional rendering for PIUTANG
                                            p { style = "margin: 0;"; strong { +"Jatuh Tempo:" }; +" ${formatDate(it)}" }
                                        }
                                    }
                                    (notaData["namaPelanggan"] as String?)?.let {
                                        p { style = "margin-top: 8px;"; strong { +"Pelanggan:" }; +" ${it}" }
                                    } ?: run {
                                        p { style = "margin-top: 8px;"; strong { +"Pelanggan:" }; +" Umum" }
                                    }
                                    p { style = "margin: 0;"; strong { +"Alamat:" }; +" ${notaData["tokoAlamat"] as String}" }
                                    p { style = "margin: 0;"; strong { +"Telp:" }; +" ${notaData["tokoPhone"] as String}" }
                                }
                            }
                            div("dot-matrix-line") {}
                            table {
                                thead {
                                    tr {
                                        th { style = "width: 5%;"; +"No" }
                                        th { style = "width: 45%; text-align: left;"; +"Nama Item" }
                                        th { style = "width: 15%;"; +"Jumlah" }
                                        th { style = "width: 15%; text-align: right;"; +"Harga" }
                                        th { style = "width: 20%; text-align: right;"; +"Total" }
                                    }
                                }
                                tbody {
                                    @Suppress("UNCHECKED_CAST")
                                    val details = notaData["details"] as List<Map<String, Any?>> // Gunakan Any?
                                    if (details.isNotEmpty()) {
                                        details.forEachIndexed { idx, item ->
                                            tr {
                                                td("text-center") { +(idx + 1).toString() }
                                                td { +(item["namaProduk"] as String) }
                                                td("text-center") { +"${item["jumlah"] as Int} ${item["satuan"] as String? ?: "-"}" } // Handle nullable satuan
                                                td("text-right") { +formatRupiah(item["hargaJual"] as Double?) }
                                                td("text-right") { +formatRupiah(item["subtotal"] as Double?) }
                                            }
                                        }
                                    } else {
                                        tr {
                                            td { attributes["colspan"] = "5"; style = "text-align: center;"; +"Tidak ada data produk" }
                                        }
                                    }
                                }
                                tfoot {
                                    tr {
                                        td { attributes["colspan"] = "4"; style = "text-align: right; font-weight: bold;"; +"Grand Total:" }
                                        td { style = "text-align: right; font-weight: bold;"; +formatRupiah(notaData["total"] as Double?) }
                                    }
                                }
                            }
                            div("dot-matrix-line") {}
                            div("totals") { // Totals section
                                div {
                                    strong { +"Terbilang:" }
                                    +" ${terbilang(notaData["total"] as Double?)}"
                                }
                                div {
                                    strong { +"Metode Pembayaran:" }
                                    +" ${notaData["metodePembayaran"] as String}"
                                }
                                div { // Grand Total
                                    strong { +"Grand Total:" }
                                    +" ${formatRupiah(notaData["total"] as Double?)}"
                                }
                            }
                            div("signatures") {
                                div("signature") {
                                    p { +"Penerima" }
                                    div("signature-line") {}
                                }
                                div("signature") {
                                    p { +"Hormat Kami" }
                                    div("signature-line") {}
                                }
                            }
                            div("dot-matrix-line") {}
                            p {
                                style = "text-align: center; font-size: 8pt; margin-top: 10px;";
                                +(notaData["tokoFooterNota"] as String)
                            }
                        }
                    }
                }
            }
        }
    }
}