package com.neski.pennypincher.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.content.res.Configuration

/**
 * SessionManager handles authentication and theme state for the current app session only.
 *
 * Features:
 * - No session persistence: user must log in every time the app is launched
 * - No local storage: no SharedPreferences or disk caching
 * - Firebase Auth state synchronization (in-memory only)
 * - Theme preference is in-memory only, resets to system default on app launch
 * - Reactive state management using StateFlow
 *
 * Usage:
 * 1. Initialize in MainActivity.onCreate(): SessionManager.initialize(this)
 * 2. Observe authentication state: SessionManager.isLoggedIn.collectAsState()
 * 3. Get current user: SessionManager.currentUser.collectAsState()
 * 4. Observe theme state: SessionManager.isDarkTheme.collectAsState()
 * 5. Toggle theme: SessionManager.toggleTheme()
 * 6. Sign out: SessionManager.signOut()
 */
object SessionManager {
    private val auth = FirebaseAuth.getInstance()
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()
    
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()
    
    /**
     * Initialize the SessionManager with the application context.
     * This should be called once in MainActivity.onCreate().
     * Sets the default theme to system theme on first launch.
     * @param context Application context
     */
    fun initialize(context: Context) {
        // Use system theme as default
        val uiMode = context.resources.configuration.uiMode
        val isSystemDark = (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        _isDarkTheme.value = isSystemDark
        // Always sign out on app launch to prevent session restoration
        auth.signOut()
        // Set up Firebase Auth state listener
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            _isLoggedIn.value = user != null
        }
    }
    
    /**
     * Toggle the current theme (in-memory only).
     */
    fun toggleTheme() {
        val newTheme = !_isDarkTheme.value
        _isDarkTheme.value = newTheme
    }
    
    /**
     * Set the theme preference explicitly (in-memory only).
     * @param isDarkTheme Whether the dark theme should be enabled
     */
    fun setTheme(isDarkTheme: Boolean) {
        _isDarkTheme.value = isDarkTheme
    }
    
    /**
     * Get the current theme preference.
     * @return true if dark theme is enabled, false otherwise
     */
    fun getCurrentTheme(): Boolean {
        return _isDarkTheme.value
    }
    
    /**
     * Sign out the current user and clear the session.
     */
    fun signOut() {
        auth.signOut()
    }
    
    /**
     * Get the current user ID from Firebase Auth.
     * @return The current user ID or null if not logged in
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
} 