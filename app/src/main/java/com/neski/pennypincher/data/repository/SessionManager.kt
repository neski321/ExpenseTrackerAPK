package com.neski.pennypincher.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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
            Log.d("SessionManager", "Firebase Auth state changed: user=${user?.uid}")
            _currentUser.value = user
            
            if (user != null) {
                // User is signed in, save session
                saveSession(user)
                _isLoggedIn.value = true
                Log.d("SessionManager", "User signed in: ${user.uid}")
            } else {
                // User is signed out, clear session
                clearSession()
                Log.d("SessionManager", "User signed out, session cleared")
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
        
        Log.d("SessionManager", "Checking cached session: isLoggedInCached=$isLoggedInCached, userId=$userId")
        
        if (isLoggedInCached && userId != null) {
            // Check if session is not too old (30 days)
            val currentTime = System.currentTimeMillis()
            val sessionAge = currentTime - sessionTimestamp
            val maxSessionAge = 30L * 24L * 60L * 60L * 1000L // 30 days in milliseconds
            
            Log.d("SessionManager", "Session age: ${sessionAge}ms, max age: ${maxSessionAge}ms")
            
            if (sessionAge > maxSessionAge) {
                // Session is too old, clear it
                Log.d("SessionManager", "Session expired, clearing")
                clearSession()
                return
            }
            
            // There's a cached session, check with Firebase
            val currentFirebaseUser = auth.currentUser
            Log.d("SessionManager", "Firebase current user: ${currentFirebaseUser?.uid}")
            
            if (currentFirebaseUser != null && currentFirebaseUser.uid == userId) {
                // Firebase session is still valid
                _isLoggedIn.value = true
                _currentUser.value = currentFirebaseUser
                Log.d("SessionManager", "Firebase session valid, user logged in")
            } else if (currentFirebaseUser == null) {
                // Firebase doesn't have a current user, but we have a valid cached session
                // This can happen when the app is restarted. We'll trust the cached session
                // and let the Firebase Auth state listener handle the validation
                _isLoggedIn.value = true
                Log.d("SessionManager", "No Firebase user but valid cached session, trusting cache")
                // Note: _currentUser will be set by the Firebase Auth state listener when it fires
            } else {
                // Firebase has a different user, clear the cached session
                Log.d("SessionManager", "Firebase user mismatch, clearing session")
                clearSession()
            }
        } else {
            Log.d("SessionManager", "No cached session found")
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
     * Force refresh the Firebase Auth state.
     * This can be called when we have a cached session but Firebase doesn't have a current user.
     */
    suspend fun forceRefreshAuthState() {
        try {
            Log.d("SessionManager", "Force refreshing auth state")
            
            // Try to get the current user from Firebase
            val currentUser = auth.currentUser
            Log.d("SessionManager", "Force refresh - Firebase current user: ${currentUser?.uid}")
            
            if (currentUser != null) {
                // Firebase has a current user, update our state
                _currentUser.value = currentUser
                _isLoggedIn.value = true
                saveSession(currentUser)
                Log.d("SessionManager", "Force refresh - User found and session saved")
            } else {
                // Firebase doesn't have a current user, check if we have a valid cached session
                val cachedUserId = getCachedUserId()
                val isLoggedInCached = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
                
                Log.d("SessionManager", "Force refresh - No Firebase user, cached: $isLoggedInCached, cachedUserId: $cachedUserId")
                
                if (isLoggedInCached && cachedUserId != null) {
                    // We have a valid cached session, but Firebase doesn't have a current user
                    // This might be a temporary state. Let's try to refresh the Firebase Auth
                    // by calling a method that forces Firebase to check its state
                    auth.currentUser?.let { user ->
                        if (user.uid == cachedUserId) {
                            _currentUser.value = user
                            _isLoggedIn.value = true
                            saveSession(user)
                            Log.d("SessionManager", "Force refresh - User restored from cache")
                        } else {
                            clearSession()
                            Log.d("SessionManager", "Force refresh - User mismatch, session cleared")
                        }
                    } ?: run {
                        Log.d("SessionManager", "Force refresh - No Firebase user after check, keeping cached session")
                    }
                }
            }
        } catch (e: Exception) {
            // If there's an error refreshing, clear the session to be safe
            Log.e("SessionManager", "Force refresh error", e)
            clearSession()
        }
    }
    
    /**
     * Wait for Firebase Auth to initialize and restore the user session.
     * This is useful when the app starts and Firebase Auth might take time to restore the session.
     */
    suspend fun waitForAuthInitialization() {
        try {
            Log.d("SessionManager", "Waiting for Firebase Auth initialization")
            
            // Check if we have a valid cached session first
            val cachedUserId = getCachedUserId()
            val isLoggedInCached = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
            
            if (isLoggedInCached && cachedUserId != null) {
                // We have a valid cached session, trust it immediately
                _isLoggedIn.value = true
                Log.d("SessionManager", "Trusting cached session immediately: $cachedUserId")
            }
            
            // Wait a bit for Firebase Auth to initialize
            kotlinx.coroutines.delay(1000)
            
            // Check if Firebase has a current user now
            val currentUser = auth.currentUser
            Log.d("SessionManager", "After wait - Firebase current user: ${currentUser?.uid}")
            
            if (currentUser != null) {
                // Firebase has restored the user session
                _currentUser.value = currentUser
                _isLoggedIn.value = true
                saveSession(currentUser)
                Log.d("SessionManager", "Firebase Auth initialized with user: ${currentUser.uid}")
            } else if (isLoggedInCached && cachedUserId != null) {
                // Still no Firebase user, but we have a valid cached session
                // Keep the cached session and wait for Firebase to catch up
                Log.d("SessionManager", "Keeping cached session while Firebase initializes")
            }
        } catch (e: Exception) {
            Log.e("SessionManager", "Error waiting for auth initialization", e)
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
    
    /**
     * Debug method to log the current session status.
     * This can be used to troubleshoot session issues.
     */
    fun logSessionStatus() {
        val isLoggedInCached = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val userId = prefs.getString(KEY_USER_ID, null)
        val sessionTimestamp = prefs.getLong(KEY_SESSION_TIMESTAMP, 0L)
        val currentFirebaseUser = auth.currentUser
        
        Log.d("SessionManager", "=== Session Status ===")
        Log.d("SessionManager", "Cached login state: $isLoggedInCached")
        Log.d("SessionManager", "Cached user ID: $userId")
        Log.d("SessionManager", "Session timestamp: $sessionTimestamp")
        Log.d("SessionManager", "Firebase current user: ${currentFirebaseUser?.uid}")
        Log.d("SessionManager", "Current login state: ${_isLoggedIn.value}")
        Log.d("SessionManager", "Current user state: ${_currentUser.value?.uid}")
        Log.d("SessionManager", "=====================")
    }
    
    /**
     * Test method to verify session persistence.
     * This can be called to test if the session is working correctly.
     */
    fun testSessionPersistence(): Boolean {
        val isLoggedInCached = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val userId = prefs.getString(KEY_USER_ID, null)
        val currentFirebaseUser = auth.currentUser
        
        val hasValidCache = isLoggedInCached && userId != null
        val hasFirebaseUser = currentFirebaseUser != null
        val cacheMatchesFirebase = hasFirebaseUser && hasValidCache && currentFirebaseUser.uid == userId
        
        Log.d("SessionManager", "=== Session Test ===")
        Log.d("SessionManager", "Has valid cache: $hasValidCache")
        Log.d("SessionManager", "Has Firebase user: $hasFirebaseUser")
        Log.d("SessionManager", "Cache matches Firebase: $cacheMatchesFirebase")
        Log.d("SessionManager", "===================")
        
        return hasValidCache || hasFirebaseUser
    }
    
    /**
     * Manually clear the session for testing purposes.
     * This can be used to test the login flow.
     */
    fun clearSessionForTesting() {
        Log.d("SessionManager", "Manually clearing session for testing")
        clearSession()
    }
} 