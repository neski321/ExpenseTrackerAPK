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

    suspend fun deleteCategory(userId: String, categoryId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(userId)
            .collection("categories")
            .document(categoryId)
            .delete()
            .await()
    }

    suspend fun addCategory(userId: String, category: Category) {
        val db = FirebaseFirestore.getInstance()
        try {
            db.collection("users")
                .document(userId)
                .collection("categories")
                .document(category.id)
                .set(category)
                .await()
        } catch (e: Exception) {
            // Optional: log or throw
            throw e
        }
    }

}

