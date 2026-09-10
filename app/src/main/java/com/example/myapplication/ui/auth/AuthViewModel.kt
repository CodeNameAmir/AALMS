package com.example.myapplication.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.DatabaseManager
import com.example.myapplication.data.GoogleUserData
import com.example.myapplication.data.UserManager
import com.example.myapplication.data.model.DatabaseMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(
    private val userManager: UserManager,
    private val databaseManager: DatabaseManager
) : ViewModel() {

    val loggedInUserId = userManager.loggedInUserId.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val databaseMode = userManager.databaseMode.stateIn(viewModelScope, SharingStarted.Eagerly, DatabaseMode.LOCAL)
    val googleUser: StateFlow<GoogleUserData?> = userManager.googleUser.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun setDatabaseMode(mode: DatabaseMode) {
        viewModelScope.launch {
            userManager.setDatabaseMode(mode)
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun login(username: String, passwordHash: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val success = userManager.login(username, passwordHash)
                if (success) onSuccess()
                else _error.value = "Invalid username or password"
            } catch (e: Exception) {
                _error.value = "Error: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun register(username: String, passwordHash: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val success = userManager.register(username, passwordHash)
                if (success) onSuccess()
                else _error.value = "Username already exists"
            } catch (e: Exception) {
                _error.value = "Error: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun loginWithGoogle(
        idToken: String?,
        email: String,
        displayName: String?,
        photoUrl: String?,
        preferCloudDb: Boolean = true,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val success = userManager.loginWithGoogle(
                    idToken = idToken,
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl,
                    preferCloudDb = preferCloudDb
                )
                if (success) {
                    onSuccess()
                } else {
                    _error.value = "Could not sign in with Google account. Please try again."
                }
            } catch (e: Exception) {
                _error.value = "Google Sign-In failed: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _loading.value = false
            }
        }
    }
}

class AuthViewModelFactory(
    private val userManager: UserManager,
    private val databaseManager: DatabaseManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(userManager, databaseManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

