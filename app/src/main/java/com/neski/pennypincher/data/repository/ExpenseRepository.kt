package com.neski.pennypincher.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.neski.pennypincher.data.models.Expense
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

object ExpenseRepository {
    private val db = FirebaseFirestore.getInstance()
    private var cachedExpenses: List<Expense>? = null
    private var cachedExpensesUserId: String? = null

    suspend fun getAllExpenses(userId: String, forceRefresh: Boolean = false): List<Expense> {
        if (!forceRefresh && cachedExpenses != null && cachedExpensesUserId == userId) {
            return cachedExpenses!!
        }

        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("expenses")
                .get()
                .await()

            val list = snapshot.documents.mapNotNull {
                it.toObject(Expense::class.java)?.copy(id = it.id)
            }.sortedByDescending { it.date }

            cachedExpenses = list
            cachedExpensesUserId = userId
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTotalExpenses(userId: String, forceRefresh: Boolean = false): Double {
        val expenses = getAllExpenses(userId, forceRefresh)
        return expenses.sumOf { it.amount }
    }

    suspend fun getThisMonthTotal(userId: String, forceRefresh: Boolean = false): Double {
        val now = Calendar.getInstance()
        val thisMonth = now.get(Calendar.MONTH)
        val thisYear = now.get(Calendar.YEAR)

        val all = getAllExpenses(userId, forceRefresh)
        return all.filter {
            val cal = Calendar.getInstance().apply { time = it.date }
            cal.get(Calendar.MONTH) == thisMonth && cal.get(Calendar.YEAR) == thisYear
        }.sumOf { it.amount }
    }

    suspend fun getThisWeekCount(userId: String, forceRefresh: Boolean = false): Int {
        val now = Calendar.getInstance()

        val startOfWeek = now.clone() as Calendar
        startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        startOfWeek.set(Calendar.HOUR_OF_DAY, 0)
        startOfWeek.set(Calendar.MINUTE, 0)
        startOfWeek.set(Calendar.SECOND, 0)
        startOfWeek.set(Calendar.MILLISECOND, 0)


        val endOfWeek = now.clone() as Calendar
        endOfWeek.time = startOfWeek.time
        endOfWeek.add(Calendar.DAY_OF_MONTH, 6)
        endOfWeek.set(Calendar.HOUR_OF_DAY, 23)
        endOfWeek.set(Calendar.MINUTE, 59)
        endOfWeek.set(Calendar.SECOND, 59)
        endOfWeek.set(Calendar.MILLISECOND, 999)

        val all = getAllExpenses(userId, forceRefresh)

        return all.count {
            it.date.after(startOfWeek.time) && it.date.before(endOfWeek.time)
        }
    }

    suspend fun getMonthlySpending(userId: String, forceRefresh: Boolean = false): Map<String, Double> {
        val all = getAllExpenses(userId, forceRefresh)

        val format = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()

        return all
            .groupBy {
                format.format(it.date)
            }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .toList()
            .sortedByDescending { (month, _) ->
                format.parse(month)
            }
            .take(6)
            .reversed()
            .toMap()
    }

    suspend fun getExpensesByPage(userId: String, pageSize: Int, pageNumber: Int): List<Expense> {
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("expenses")
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.drop(pageNumber * pageSize).take(pageSize).mapNotNull {
                it.toObject(Expense::class.java)?.copy(id = it.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addExpense(userId: String, expense: Expense) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(userId)
            .collection("expenses")
            .document(expense.id)
            .set(expense)
    }

    fun updateExpense(userId: String, expense: Expense) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("expenses")
            .document(expense.id)
            .set(expense)
    }

    suspend fun deleteExpense(userId: String, expenseId: String) {
        try {
            db.collection("users")
                .document(userId)
                .collection("expenses")
                .document(expenseId)
                .delete()
                .await()
            // Invalidate cache if needed
            if (cachedExpenses != null && cachedExpensesUserId == userId) {
                cachedExpenses = cachedExpenses?.filterNot { it.id == expenseId }
            }
        } catch (e: Exception) {
            // Handle error if needed
        }
    }

    suspend fun getExpensesByCategory(userId: String, categoryId: String, forceRefresh: Boolean = false): List<Expense> {
        val all = getAllExpenses(userId, forceRefresh)
        return all.filter { it.categoryId == categoryId }
    }
}