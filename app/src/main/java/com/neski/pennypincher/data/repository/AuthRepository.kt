package com.neski.pennypincher.data.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

object AuthRepository {
    private val auth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            val user = auth.currentUser
            Result.success(user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String): Result<FirebaseUser> {
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()
            val user = auth.currentUser
            Result.success(user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reauthenticateUser(currentEmail: String, password: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No logged-in user"))
            val credential = EmailAuthProvider.getCredential(currentEmail, password)
            user.reauthenticate(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEmailAddress(newEmail: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No logged-in user"))
            user.updateEmail(newEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        // Only sign out from Firebase
        auth.signOut()
    }
    
    fun isUserLoggedIn(): Boolean {
        // Only check FirebaseAuth
        return auth.currentUser != null
    }
    
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Explicitly refresh the session after successful authentication.
     * This ensures the session is properly saved even if the Firebase Auth state listener
     * doesn't fire immediately.
     */
    fun refreshSessionAfterAuth() {
        // No-op, session caching removed
    }
}
