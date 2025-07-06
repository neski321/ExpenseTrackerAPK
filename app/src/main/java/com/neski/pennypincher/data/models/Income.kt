package com.neski.pennypincher.data.models

import java.util.Date

data class Income(
    val id: String = "",
    val amount: Double = 0.0,
    val date: Date = Date(),
    val incomeSourceId: String = "",
    val currencyId: String = ""
)
