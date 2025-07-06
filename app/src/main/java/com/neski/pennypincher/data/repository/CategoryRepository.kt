package com.neski.pennypincher.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.neski.pennypincher.data.models.Category
import kotlinx.coroutines.tasks.await

object CategoryRepository {
    private val db = FirebaseFirestore.getInstance()
    private var cachedCategories: List<Category>? = null
    private var cachedCategoriesUserId: String? = null

    suspend fun getAllCategories(userId: String, forceRefresh: Boolean = false): List<Category> {
        if (!forceRefresh && cachedCategories != null && cachedCategoriesUserId == userId) {
            return cachedCategories!!
        }
        return try {
            val snapshot = db
                .collection("users")
                .document(userId)
                .collection("categories")
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Category::class.java)?.copy(id = doc.id)
            }
            cachedCategories = list
            cachedCategoriesUserId = userId
            list
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

    suspend fun updateCategory(userId: String, category: Category) {
        val db = FirebaseFirestore.getInstance()
        try {
            db.collection("users")
                .document(userId)
                .collection("categories")
                .document(category.id)
                .set(category)
                .await()
            
            // Clear cache
            cachedCategories = null
            cachedCategoriesUserId = null
        } catch (e: Exception) {
            throw e
        }
    }

}

