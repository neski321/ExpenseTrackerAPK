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
            Result.success(auth.currentUser!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String): Result<FirebaseUser> {
        return try {
            auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(auth.currentUser!!)
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
        SessionManager.signOut()
    }
    
    fun isUserLoggedIn(): Boolean {
        return SessionManager.isSessionValid()
    }
    
    fun getCurrentUserId(): String? {
        return SessionManager.getCurrentUserId()
    }
}
