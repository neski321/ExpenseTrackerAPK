package com.neski.pennypincher.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.neski.pennypincher.data.models.IncomeSource
import kotlinx.coroutines.tasks.await

object IncomeSourceRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getAllIncomeSources(userId: String): List<IncomeSource> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("incomeSources")
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(IncomeSource::class.java)?.copy(id = it.id) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addIncomeSource(userId: String, source: IncomeSource) {
        firestore.collection("users")
            .document(userId)
            .collection("incomeSources")
            .add(source)
            .await()
    }

    suspend fun updateIncomeSource(userId: String, source: IncomeSource) {
        if (source.id.isNotBlank()) {
            firestore.collection("users")
                .document(userId)
                .collection("incomeSources")
                .document(source.id)
                .set(source)
                .await()
        }
    }

    suspend fun deleteIncomeSource(userId: String, sourceId: String) {
        firestore.collection("users")
            .document(userId)
            .collection("incomeSources")
            .document(sourceId)
            .delete()
            .await()
    }
}
