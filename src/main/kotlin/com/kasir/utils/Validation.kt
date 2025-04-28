package com.kasir.utils

// Fungsi utilitas untuk validasi input

/**
 * Cek format email sederhana.
 * Valid jika terdapat karakter sebelum '@', domain, dan extension minimal 2 huruf.
 */
fun isValidEmail(email: String): Boolean =
    Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$").matches(email)

/**
 * Cek kekuatan password.
 * Minimal panjang 8 karakter. Bisa diperluas dengan kriteria lain.
 */
fun isStrongPassword(pw: String): Boolean =
    pw.length >= 8
