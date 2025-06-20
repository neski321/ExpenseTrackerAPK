package com.neski.pennypincher.data.models

data class Budget(
    val id: String = "",
    val categoryId: String = "",
    val amount: Double = 0.0,
    val startDate: Long = 0L,
    val endDate: Long = 0L
)