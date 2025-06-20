package com.neski.pennypincher.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.neski.pennypincher.data.models.Category
import kotlinx.coroutines.tasks.await

object CategoryRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getAllCategories(userId: String): List<Category> {
        return try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("categories")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val cat = doc.toObject(Category::class.java)?.copy(id = doc.id)
                cat
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

}

