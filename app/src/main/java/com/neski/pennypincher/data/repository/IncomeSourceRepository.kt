package com.neski.pennypincher.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.neski.pennypincher.data.models.IncomeSource
import kotlinx.coroutines.tasks.await

object IncomeSourceRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private var cachedSources: List<IncomeSource>? = null
    private var cachedSourcesUserId: String? = null

    suspend fun getAllIncomeSources(userId: String, forceRefresh: Boolean = false): List<IncomeSource> {
        if (!forceRefresh && cachedSources != null && cachedSourcesUserId == userId) {
            return cachedSources!!
        }
        
        return try {
            val list = firestore.collection("users")
                .document(userId)
                .collection("incomeSources")
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(IncomeSource::class.java)?.copy(id = it.id) }
            
            cachedSources = list
            cachedSourcesUserId = userId
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addIncomeSource(userId: String, source: IncomeSource) {
        firestore.collection("users")
            .document(userId)
            .collection("incomeSources")
            .document(source.id)
            .set(source)
            .await()
        
        // Clear cache
        cachedSources = null
        cachedSourcesUserId = null
    }

    suspend fun updateIncomeSource(userId: String, source: IncomeSource) {
        if (source.id.isNotBlank()) {
            firestore.collection("users")
                .document(userId)
                .collection("incomeSources")
                .document(source.id)
                .set(source)
                .await()
            
            // Clear cache
            cachedSources = null
            cachedSourcesUserId = null
        }
    }

    suspend fun deleteIncomeSource(userId: String, sourceId: String) {
        firestore.collection("users")
            .document(userId)
            .collection("incomeSources")
            .document(sourceId)
            .delete()
            .await()
        
        // Clear cache
        cachedSources = null
        cachedSourcesUserId = null
    }
}
