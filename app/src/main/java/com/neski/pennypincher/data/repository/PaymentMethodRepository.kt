package com.neski.pennypincher.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.neski.pennypincher.data.models.PaymentMethod
import kotlinx.coroutines.tasks.await

object PaymentMethodRepository {
    private val db = FirebaseFirestore.getInstance()
    private var cachedMethods: List<PaymentMethod>? = null
    private var cachedMethodsUserId: String? = null

    suspend fun getAllPaymentMethods(userId: String, forceRefresh: Boolean = false): List<PaymentMethod> {
        if (!forceRefresh && cachedMethods != null && cachedMethodsUserId == userId) {
            return cachedMethods!!
        }
        return try {
            val snapshot = db
                .collection("users")
                .document(userId)
                .collection("paymentMethods")
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.toObject(PaymentMethod::class.java)?.copy(id = doc.id)
            }
            cachedMethods = list
            cachedMethodsUserId = userId
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addPaymentMethod(userId: String, method: PaymentMethod) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("paymentMethods")
            .document(method.id)
            .set(method)
            .await()
    }

    suspend fun deletePaymentMethod(userId: String, methodId: String) {
        db.collection("users")
            .document(userId)
            .collection("paymentMethods")
            .document(methodId)
            .delete()
            .await()
        cachedMethods = null
        cachedMethodsUserId = null
    }

    suspend fun updatePaymentMethod(userId: String, method: PaymentMethod) {
        db.collection("users")
            .document(userId)
            .collection("paymentMethods")
            .document(method.id)
            .set(method)
            .await()
        cachedMethods = null
        cachedMethodsUserId = null
    }
}
