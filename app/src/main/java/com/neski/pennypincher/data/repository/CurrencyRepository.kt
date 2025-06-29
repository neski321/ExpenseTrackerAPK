package com.neski.pennypincher.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.neski.pennypincher.data.models.Currency
import kotlinx.coroutines.tasks.await

object CurrencyRepository {
    private val db = FirebaseFirestore.getInstance()
    private var cachedCurrencies: List<Currency>? = null
    private var cachedCurrenciesUserId: String? = null

    suspend fun getAllCurrencies(userId: String, forceRefresh: Boolean = false): List<Currency> {
        if (!forceRefresh && cachedCurrencies != null && cachedCurrenciesUserId == userId) {
            return cachedCurrencies!!
        }
        
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("currencies")
                .get()
                .await()

            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Currency::class.java)?.copy(id = doc.id)
            }
            
            cachedCurrencies = list
            cachedCurrenciesUserId = userId
            list
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
        
        // Clear cache
        cachedCurrencies = null
        cachedCurrenciesUserId = null
    }

    suspend fun updateCurrency(userId: String, currency: Currency) {
        db.collection("users")
            .document(userId)
            .collection("currencies")
            .document(currency.id)
            .set(currency)
            .await()
        
        // Clear cache
        cachedCurrencies = null
        cachedCurrenciesUserId = null
    }

    suspend fun deleteCurrency(userId: String, currencyId: String) {
        db.collection("users")
            .document(userId)
            .collection("currencies")
            .document(currencyId)
            .delete()
            .await()
        
        // Clear cache
        cachedCurrencies = null
        cachedCurrenciesUserId = null
    }
}
