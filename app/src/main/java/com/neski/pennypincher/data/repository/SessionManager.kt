package com.neski.pennypincher.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SessionManager handles authentication session caching and persistence.
 * 
 * Features:
 * - Automatic session persistence using SharedPreferences
 * - Firebase Auth state synchronization
 * - Session expiration (30 days)
 * - Automatic session validation on app start
 * - Reactive state management using StateFlow
 * - Theme preference caching and persistence
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
    private const val PREFS_NAME = "PennyPincherSession"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_SESSION_TIMESTAMP = "session_timestamp"
    private const val KEY_IS_DARK_THEME = "is_dark_theme"
    
    private lateinit var prefs: SharedPreferences
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
     * 
     * @param context Application context
     */
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Load cached theme preference
        loadThemePreference()
        
        // Set up Firebase Auth state listener
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            _isLoggedIn.value = user != null
            
            if (user != null) {
                // User is signed in, save session
                saveSession(user)
            } else {
                // User is signed out, clear session
                clearSession()
            }
        }
        
        // Check if there's a cached session
        checkCachedSession()
    }
    
    /**
     * Load the cached theme preference from SharedPreferences.
     */
    private fun loadThemePreference() {
        val isDarkThemeCached = prefs.getBoolean(KEY_IS_DARK_THEME, false)
        _isDarkTheme.value = isDarkThemeCached
    }
    
    /**
     * Save the current theme preference to SharedPreferences.
     * 
     * @param isDarkTheme Whether the dark theme is enabled
     */
    private fun saveThemePreference(isDarkTheme: Boolean) {
        prefs.edit().apply {
            putBoolean(KEY_IS_DARK_THEME, isDarkTheme)
        }.apply()
        _isDarkTheme.value = isDarkTheme
    }
    
    /**
     * Toggle the current theme and save the preference.
     */
    fun toggleTheme() {
        val newTheme = !_isDarkTheme.value
        saveThemePreference(newTheme)
    }
    
    /**
     * Set the theme preference explicitly.
     * 
     * @param isDarkTheme Whether the dark theme should be enabled
     */
    fun setTheme(isDarkTheme: Boolean) {
        saveThemePreference(isDarkTheme)
    }
    
    /**
     * Get the current theme preference.
     * 
     * @return true if dark theme is enabled, false otherwise
     */
    fun getCurrentTheme(): Boolean {
        return _isDarkTheme.value
    }
    
    /**
     * Check for existing cached session and validate it with Firebase.
     * This is called during initialization to restore the user's session.
     */
    private fun checkCachedSession() {
        val isLoggedInCached = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val userId = prefs.getString(KEY_USER_ID, null)
        val sessionTimestamp = prefs.getLong(KEY_SESSION_TIMESTAMP, 0L)
        
        if (isLoggedInCached && userId != null) {
            // Check if session is not too old (30 days)
            val currentTime = System.currentTimeMillis()
            val sessionAge = currentTime - sessionTimestamp
            val maxSessionAge = 30L * 24L * 60L * 60L * 1000L // 30 days in milliseconds
            
            if (sessionAge > maxSessionAge) {
                // Session is too old, clear it
                clearSession()
                return
            }
            
            // There's a cached session, but we need to verify with Firebase
            val currentFirebaseUser = auth.currentUser
            if (currentFirebaseUser != null && currentFirebaseUser.uid == userId) {
                // Firebase session is still valid
                _isLoggedIn.value = true
                _currentUser.value = currentFirebaseUser
            } else {
                // Cached session is invalid, clear it
                clearSession()
            }
        }
    }
    
    /**
     * Save the current user session to SharedPreferences.
     * 
     * @param user The Firebase user to save
     */
    private fun saveSession(user: FirebaseUser) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, user.uid)
            putString(KEY_USER_EMAIL, user.email)
            putLong(KEY_SESSION_TIMESTAMP, System.currentTimeMillis())
        }.apply()
    }
    
    /**
     * Clear all session data from SharedPreferences.
     */
    private fun clearSession() {
        prefs.edit().apply {
            remove(KEY_IS_LOGGED_IN)
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
            remove(KEY_SESSION_TIMESTAMP)
        }.apply()
        _isLoggedIn.value = false
        _currentUser.value = null
    }
    
    /**
     * Get the cached user ID from SharedPreferences.
     * 
     * @return The cached user ID or null if not found
     */
    fun getCachedUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }
    
    /**
     * Get the cached user email from SharedPreferences.
     * 
     * @return The cached user email or null if not found
     */
    fun getCachedUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }
    
    /**
     * Check if the current session is valid by comparing Firebase user with cached data.
     * 
     * @return true if the session is valid, false otherwise
     */
    fun isSessionValid(): Boolean {
        val currentUser = auth.currentUser
        val cachedUserId = getCachedUserId()
        
        return currentUser != null && 
               cachedUserId != null && 
               currentUser.uid == cachedUserId
    }
    
    /**
     * Sign out the current user and clear the session.
     */
    fun signOut() {
        auth.signOut()
        // The auth state listener will handle clearing the session
    }
    
    /**
     * Get the current user ID from Firebase Auth.
     * 
     * @return The current user ID or null if not logged in
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    /**
     * Refresh the session timestamp to extend the session validity.
     */
    fun refreshSession() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            saveSession(currentUser)
        }
    }
    
    /**
     * Clear all session data and cached information.
     * This can be used for a complete logout or data reset.
     */
    fun clearAllData() {
        clearSession()
        clearThemePreference()
        // Clear all cached data from other repositories
        // This could be expanded to clear all app data if needed
    }

    /**
     * Clear the theme preference from SharedPreferences.
     * This resets the theme to the default (light theme).
     */
    fun clearThemePreference() {
        prefs.edit().apply {
            remove(KEY_IS_DARK_THEME)
        }.apply()
        _isDarkTheme.value = false
    }
} 