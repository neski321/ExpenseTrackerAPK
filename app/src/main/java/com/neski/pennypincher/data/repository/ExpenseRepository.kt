package com.neski.pennypincher.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.neski.pennypincher.data.models.Expense
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

object ExpenseRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getAllExpenses(userId: String): List<Expense> {
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("expenses")
                .get()
                .await()

            snapshot.documents.mapNotNull {
                it.toObject(Expense::class.java)?.copy(id = it.id)
            }.sortedByDescending { it.date }
        } catch (e: Exception) {
            //println("Error fetching expenses: ${e.message}")
            emptyList()
        }
    }

    suspend fun getThisMonthTotal(userId: String): Double {
        val now = Calendar.getInstance()
        val thisMonth = now.get(Calendar.MONTH)
        val thisYear = now.get(Calendar.YEAR)

        val all = getAllExpenses(userId)
        return all.filter {
            val cal = Calendar.getInstance().apply { time = it.date }
            cal.get(Calendar.MONTH) == thisMonth && cal.get(Calendar.YEAR) == thisYear
        }.sumOf { it.amount }
    }

    suspend fun getThisWeekCount(userId: String): Int {
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

        val all = getAllExpenses(userId)

        return all.count {
            it.date.after(startOfWeek.time) && it.date.before(endOfWeek.time)
        }
    }

    suspend fun getMonthlySpending(userId: String): Map<String, Double> {
        val all = getAllExpenses(userId)

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




}