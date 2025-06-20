package com.neski.pennypincher.data.models

data class SavingGoal(
    val id: String = "",
    val name: String = "",
    val amount: Double = 0.0,
    val targetDate: Long? = null,
    val currencyId: String = ""
)
