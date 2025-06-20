package com.neski.pennypincher.data.models

import java.util.Date

data class Expense(
    val id: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val date: Date = Date(), // Store as timestamp (ms)
    val categoryId: String = "",
    val currencyId: String = "",
    val isSubscription: Boolean = false,
    val paymentMethodId: String? = null,
    val nextDueDate: Date = Date()
)
