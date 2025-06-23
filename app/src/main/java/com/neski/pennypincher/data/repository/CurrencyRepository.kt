package com.neski.pennypincher.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.neski.pennypincher.data.models.Currency
import kotlinx.coroutines.tasks.await

object CurrencyRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getAllCurrencies(userId: String, forceRefresh: Boolean): List<Currency> {
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("currencies")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Currency::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addCurrency(userId: String, currency: Currency) {
        db.collection("users")
            .document(userId)
            .collection("currencies")
            .document(currency.id)
            .set(currency)
            .await()
    }

    suspend fun updateCurrency(userId: String, currency: Currency) {
        db.collection("users")
            .document(userId)
            .collection("currencies")
            .document(currency.id)
            .set(currency)
            .await()
    }

    suspend fun deleteCurrency(userId: String, currencyId: String) {
        db.collection("users")
            .document(userId)
            .collection("currencies")
            .document(currencyId)
            .delete()
            .await()
    }
}
