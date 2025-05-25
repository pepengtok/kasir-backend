package com.kasir.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Generate rentang waktu berdasarkan tipe filter:
 * - HARIAN: from start of day to next day
 * - BULANAN: from first of month to first of next month
 * - TAHUNAN: from first of year to first of next year
 */
fun generateRange(
    tipe: String,
    tanggalStr: String?
): Pair<LocalDateTime, LocalDateTime> {
    // kalau null, kembalikan rentang tak terhingga
    val dateOnly = tanggalStr?.substringBefore('T')
        ?: return LocalDateTime.MIN to LocalDateTime.MAX

    // parse string "yyyy-MM-dd" jadi LocalDate
    val date = LocalDate.parse(dateOnly, DateTimeFormatter.ISO_DATE)

    return when (tipe.uppercase()) {
        "HARIAN" -> {
            val start = date.atStartOfDay()
            start to start.plusDays(1)
        }
        "BULANAN" -> {
            val start = date.withDayOfMonth(1).atStartOfDay()
            val end   = start.plusMonths(1)
            start to end
        }
        "TAHUNAN" -> {
            val start = date.withDayOfYear(1).atStartOfDay()
            val end   = start.plusYears(1)
            start to end
        }
        else -> LocalDateTime.MIN to LocalDateTime.MAX
    }
}