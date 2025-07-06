package com.neski.pennypincher.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.neski.pennypincher.data.models.Income
import kotlinx.coroutines.tasks.await

object IncomeRepository {
    private val db = FirebaseFirestore.getInstance()
    private var cachedIncome: List<Income>? = null
    private var cachedIncomeUserId: String? = null

    suspend fun getAllIncome(userId: String, forceRefresh: Boolean = false): List<Income> {
        if (!forceRefresh && cachedIncome != null && cachedIncomeUserId == userId) {
            return cachedIncome!!
        }

        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("incomes")
                .get()
                .await()

            val list = snapshot.documents.mapNotNull {
                it.toObject(Income::class.java)?.copy(id = it.id)
            }

            cachedIncome = list
            cachedIncomeUserId = userId
            println("Found ${snapshot.documents.size} income docs")
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTotalIncome(userId: String, forceRefresh: Boolean = false): Double {
        val income = getAllIncome(userId, forceRefresh)
        return income.sumOf { it.amount }
    }

    suspend fun addIncome(userId: String, income: Income) {
        db.collection("users")
            .document(userId)
            .collection("incomes")
            .document(income.id)
            .set(income)
            .await()

        // Clear cache
        cachedIncome = null
        cachedIncomeUserId = null
    }

    suspend fun updateIncome(userId: String, income: Income) {
        if (income.id.isBlank()) return

        db.collection("users")
            .document(userId)
            .collection("incomes")
            .document(income.id)
            .set(income)
            .await()

        // Clear cache
        cachedIncome = null
        cachedIncomeUserId = null
    }

    suspend fun deleteIncome(userId: String, incomeId: String) {
        db.collection("users")
            .document(userId)
            .collection("incomes")
            .document(incomeId)
            .delete()
            .await()

        // Clear cache
        cachedIncome = null
        cachedIncomeUserId = null
    }
}
