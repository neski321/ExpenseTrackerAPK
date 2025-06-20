package com.neski.pennypincher.data.models

data class Income(
    val id: String = "",
    val amount: Double = 0.0,
    val date: Long = 0L,
    val sourceId: String = "",
    val currencyId: String = ""
)
