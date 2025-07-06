package com.neski.pennypincher.data.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import android.util.Log

object AuthRepository {
    private val auth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            Log.d("AuthRepository", "Attempting sign in for email: $email")
            auth.signInWithEmailAndPassword(email, password).await()
            val user = auth.currentUser
            Log.d("AuthRepository", "Sign in successful for user: ${user?.uid}")
            Result.success(user!!)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sign in failed", e)
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String): Result<FirebaseUser> {
        return try {
            Log.d("AuthRepository", "Attempting sign up for email: $email")
            auth.createUserWithEmailAndPassword(email, password).await()
            val user = auth.currentUser
            Log.d("AuthRepository", "Sign up successful for user: ${user?.uid}")
            Result.success(user!!)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sign up failed", e)
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
        SessionManager.signOut()
    }
    
    fun isUserLoggedIn(): Boolean {
        return SessionManager.isSessionValid()
    }
    
    fun getCurrentUserId(): String? {
        return SessionManager.getCurrentUserId()
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
    suspend fun refreshSessionAfterAuth() {
        try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                Log.d("AuthRepository", "Refreshing session for user: ${currentUser.uid}")
                // The SessionManager should handle this through its Firebase Auth state listener,
                // but we can also call it explicitly to ensure it happens
                SessionManager.refreshSession()
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error refreshing session", e)
        }
    }
}
