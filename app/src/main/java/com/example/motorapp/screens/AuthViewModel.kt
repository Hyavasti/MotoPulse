package com.example.motorapp.screens

import android.app.Application
import android.content.Context
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.motorapp.data.UserProfile
import com.example.motorapp.data.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val userId: String) : AuthUiState
    data class EmailVerificationRequired(val email: String) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = supabase.auth
    private val prefs = application.getSharedPreferences("motopulse_auth_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<AuthUiState>(
        auth.currentUserOrNull()?.let { AuthUiState.Success(it.id) } ?: AuthUiState.Idle
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun getSavedUsername(): String = prefs.getString("saved_username", "") ?: ""
    fun isRememberMeEnabled(): Boolean = prefs.getBoolean("remember_me_enabled", false)

    private fun saveCredentials(username: String, remember: Boolean) {
        prefs.edit().apply {
            putBoolean("remember_me_enabled", remember)
            if (remember) {
                putString("saved_username", username)
            } else {
                remove("saved_username")
            }
            apply()
        }
    }

    private fun extractErrorMessage(e: Exception): String {
        val raw = e.localizedMessage ?: e.message ?: ""
        // Strips any Supabase HTTP payload / URL / header dump after the core message
        return raw.substringBefore("\nURL:").trim()
    }

    fun signIn(username: String, pass: String, rememberMe: Boolean = false) {
        val cleanUsername = username.trim()
        if (cleanUsername.isBlank() || pass.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your username and password.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val profile = supabase.from("profiles")
                    .select {
                        filter { eq("username", cleanUsername) }
                    }
                    .decodeSingleOrNull<UserProfile>()

                if (profile == null) {
                    _uiState.value = AuthUiState.Error("Username '$cleanUsername' not found.")
                    return@launch
                }

                val accountEmail = profile.email
                if (accountEmail.isNullOrBlank()) {
                    _uiState.value = AuthUiState.Error("No email associated with account '$cleanUsername'.")
                    return@launch
                }

                auth.signInWith(Email) {
                    this.email = accountEmail
                    this.password = pass
                }

                val current = auth.currentUserOrNull()
                if (current != null) {
                    saveCredentials(cleanUsername, rememberMe)
                    _uiState.value = AuthUiState.Success(current.id)
                } else {
                    _uiState.value = AuthUiState.Error("Please confirm your email before signing in.")
                }
            } catch (e: Exception) {
                val rawMsg = extractErrorMessage(e)
                _uiState.value = when {
                    rawMsg.contains("invalid login credentials", ignoreCase = true) ||
                            rawMsg.contains("invalid credentials", ignoreCase = true) -> {
                        AuthUiState.Error("Invalid username or password.")
                    }
                    rawMsg.contains("email not confirmed", ignoreCase = true) -> {
                        AuthUiState.Error("Please verify your email before logging in.")
                    }
                    rawMsg.contains("http", ignoreCase = true) || rawMsg.contains("URL:", ignoreCase = true) -> {
                        AuthUiState.Error("Sign-in failed. Please try again.")
                    }
                    else -> AuthUiState.Error(rawMsg.ifBlank { "Invalid credentials." })
                }
            }
        }
    }

    fun signUp(username: String, email: String, phoneNumber: String, pass: String) {
        val cleanUsername = username.trim()
        val cleanEmail = email.trim()
        val cleanPhone = phoneNumber.trim()

        if (cleanUsername.isBlank() || cleanEmail.isBlank() || pass.isBlank()) {
            _uiState.value = AuthUiState.Error("All required fields must be filled.")
            return
        }
        if (cleanUsername.length < 3) {
            _uiState.value = AuthUiState.Error("Username must be at least 3 characters long.")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            _uiState.value = AuthUiState.Error("Please enter a valid email address.")
            return
        }
        if (cleanPhone.isNotBlank() && (cleanPhone.length != 11 || !cleanPhone.startsWith("09"))) {
            _uiState.value = AuthUiState.Error("Must be an 11-digit number starting with 09.")
            return
        }
        if (pass.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters long.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                auth.signUpWith(Email) {
                    this.email = cleanEmail
                    this.password = pass
                    this.data = buildJsonObject {
                        put("username", cleanUsername)
                        put("phone_number", cleanPhone)
                    }
                }

                val newUser = auth.currentUserOrNull()
                if (newUser != null) {
                    _uiState.value = AuthUiState.Success(newUser.id)
                } else {
                    _uiState.value = AuthUiState.EmailVerificationRequired(cleanEmail)
                }
            } catch (e: Exception) {
                val rawMsg = extractErrorMessage(e)

                _uiState.value = when {
                    rawMsg.contains("already taken", ignoreCase = true) ||
                            rawMsg.contains("profiles_username_unique", ignoreCase = true) ||
                            rawMsg.contains("duplicate key value", ignoreCase = true) -> {
                        AuthUiState.Error("Username '$cleanUsername' is already taken.")
                    }
                    rawMsg.contains("user already registered", ignoreCase = true) ||
                            rawMsg.contains("already registered", ignoreCase = true) ||
                            rawMsg.contains("email already exists", ignoreCase = true) -> {
                        AuthUiState.Error("An account with this email already exists.")
                    }
                    rawMsg.contains("Username is required", ignoreCase = true) -> {
                        AuthUiState.Error("Username cannot be empty.")
                    }
                    rawMsg.contains("http", ignoreCase = true) || rawMsg.contains("URL:", ignoreCase = true) -> {
                        AuthUiState.Error("Sign-up failed. Please check your network and try again.")
                    }
                    else -> {
                        AuthUiState.Error(rawMsg.ifBlank { "Sign-up failed. Please try again." })
                    }
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    fun signOut() {
        viewModelScope.launch {
            auth.signOut()
            _uiState.value = AuthUiState.Idle
        }
    }
}