package com.neski.pennypincher.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.neski.pennypincher.data.models.PaymentMethod
import kotlinx.coroutines.tasks.await

object PaymentMethodRepository {
    suspend fun getAllPaymentMethods(userId: String): List<PaymentMethod> {
        return try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("paymentMethods")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(PaymentMethod::class.java)?.copy(id = doc.id)
            }
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
}
