package com.neski.pennypincher.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neski.pennypincher.data.repository.AuthRepository
import com.neski.pennypincher.data.repository.SessionManager
import com.neski.pennypincher.ui.navigation.ExpenseNavigationManager
import com.neski.pennypincher.ui.navigation.NavigationEvent
import com.neski.pennypincher.ui.navigation.NavigationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.util.Log

data class AppState(
    val isLoggedIn: Boolean = false,
    val currentUser: String = "",
    val isDarkTheme: Boolean = false,
    val isLoading: Boolean = true,
    val navigationState: NavigationState = NavigationState("loading")
)

sealed class AppEvent {
    object Initialize : AppEvent()
    object ToggleTheme : AppEvent()
    object Logout : AppEvent()
    data class Navigate(val event: NavigationEvent) : AppEvent()
}

class AppStateManager : ViewModel() {
    private val _appState = MutableStateFlow(AppState())
    val appState: StateFlow<AppState> = _appState.asStateFlow()
    
    private val navigationManager = ExpenseNavigationManager()
    
    init {
        // Observe navigation state changes
        viewModelScope.launch {
            navigationManager.navigationState.value.let { navState ->
                _appState.value = _appState.value.copy(navigationState = navState)
            }
        }
        
        // Observe authentication state
        viewModelScope.launch {
            SessionManager.isLoggedIn.collect { isLoggedIn ->
                _appState.value = _appState.value.copy(
                    isLoggedIn = isLoggedIn,
                    isLoading = false
                )
                
                if (!isLoggedIn) {
                    handleNavigationEvent(NavigationEvent.NavigateToRoute("welcome"))
                } else {
                    handleNavigationEvent(NavigationEvent.NavigateToRoute("dashboard"))
                }
            }
        }
        
        // Observe current user
        viewModelScope.launch {
            SessionManager.currentUser.collect { user ->
                _appState.value = _appState.value.copy(
                    currentUser = user?.uid ?: ""
                )
            }
        }
        
        // Observe theme state
        viewModelScope.launch {
            SessionManager.isDarkTheme.collect { isDarkTheme ->
                _appState.value = _appState.value.copy(isDarkTheme = isDarkTheme)
            }
        }
        
        // Force refresh auth state on app start to handle cached sessions
        viewModelScope.launch {
            SessionManager.logSessionStatus()
            // Wait for Firebase Auth to initialize properly
            SessionManager.waitForAuthInitialization()
            SessionManager.forceRefreshAuthState()
            SessionManager.logSessionStatus()
            
            // Test session persistence
            val sessionWorking = SessionManager.testSessionPersistence()
            Log.d("AppStateManager", "Session persistence test result: $sessionWorking")
        }
    }
    
    fun handleEvent(event: AppEvent) {
        when (event) {
            is AppEvent.Initialize -> {
                // Already handled in init
            }
            
            is AppEvent.ToggleTheme -> {
                SessionManager.toggleTheme()
            }
            
            is AppEvent.Logout -> {
                viewModelScope.launch {
                    AuthRepository.signOut()
                    handleNavigationEvent(NavigationEvent.ClearNavigationStack)
                }
            }
            
            is AppEvent.Navigate -> {
                handleNavigationEvent(event.event)
            }
        }
    }
    
    private fun handleNavigationEvent(event: NavigationEvent) {
        navigationManager.handleNavigationEvent(event)
        _appState.value = _appState.value.copy(
            navigationState = navigationManager.navigationState.value
        )
    }
} 