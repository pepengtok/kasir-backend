package com.kasir.models

import java.util.UUID

data class Kas(
    val id: UUID,
    val namaKas: String,
    val saldoAkhir: Double
)
