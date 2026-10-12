package com.example.motorapp.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motorapp.data.GarageBike
import com.example.motorapp.data.MaintenanceRecord
import com.example.motorapp.data.MotorRepository
import com.example.motorapp.data.supabase
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountUiState(
    val username: String = "Rider",
    val email: String = "",
    val bikeName: String = "",
    val garageBikes: List<GarageBike> = emptyList(),
    val maintenanceHistory: List<MaintenanceRecord> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshingMaintenance: Boolean = false,
    val isUpdating: Boolean = false,
    val errorMessage: String? = null,
    val updateSuccessMessage: String? = null
)

class AccountViewModel(
    private val repository: MotorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        loadAccountData()
    }

    fun loadAccountData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val profileDeferred = async { repository.getUserProfile() }
                val bikesDeferred = async { repository.getGarageBikes() }
                val historyDeferred = async { repository.getMaintenanceHistory() }

                val profile = profileDeferred.await()
                val bikes = bikesDeferred.await()
                val rawHistory = historyDeferred.await()

                // Only retain records associated with vehicles that currently exist in the garage
                val validBikeIds = bikes.map { it.id }.toSet()
                val filteredHistory = if (bikes.isEmpty()) {
                    emptyList()
                } else {
                    rawHistory.filter { it.bikeId in validBikeIds }
                }

                val sessionEmail = supabase.auth.currentUserOrNull()?.email.orEmpty()
                val resolvedUsername = profile.username?.takeIf { it.isNotBlank() } ?: "Rider"
                val resolvedEmail = profile.email?.takeIf { it.isNotBlank() } ?: sessionEmail

                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        username = resolvedUsername,
                        email = resolvedEmail,
                        bikeName = profile.bikeName.orEmpty(),
                        garageBikes = bikes,
                        maintenanceHistory = filteredHistory
                    )
                }
            } catch (e: Exception) {
                Log.e("AccountViewModel", "Failed to load account data: ${e.message}", e)
                val fallbackEmail = supabase.auth.currentUserOrNull()?.email.orEmpty()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        email = fallbackEmail,
                        errorMessage = e.localizedMessage ?: "Failed to load account data"
                    )
                }
            }
        }
    }

    fun refreshMaintenanceHistory(bikeId: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingMaintenance = true, errorMessage = null) }
            try {
                val recordsDeferred = async { repository.getMaintenanceHistory(bikeId) }
                val bikesDeferred = async { repository.getGarageBikes() }

                val rawRecords = recordsDeferred.await()
                val bikes = bikesDeferred.await()

                val validBikeIds = bikes.map { it.id }.toSet()
                val filteredRecords = if (bikes.isEmpty()) {
                    emptyList()
                } else {
                    rawRecords.filter { it.bikeId in validBikeIds }
                }

                _uiState.update {
                    it.copy(
                        maintenanceHistory = filteredRecords,
                        garageBikes = bikes,
                        isRefreshingMaintenance = false
                    )
                }
            } catch (e: Exception) {
                Log.e("AccountViewModel", "Failed to refresh maintenance history: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isRefreshingMaintenance = false,
                        errorMessage = e.localizedMessage ?: "Failed to refresh maintenance history"
                    )
                }
            }
        }
    }

    fun updateUsername(newName: String) {
        if (newName.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true, errorMessage = null) }
            try {
                repository.updateUsername(newName)
                _uiState.update {
                    it.copy(
                        username = newName,
                        isUpdating = false,
                        updateSuccessMessage = "Profile updated successfully!"
                    )
                }
            } catch (e: Exception) {
                Log.e("AccountViewModel", "Failed to update username: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        errorMessage = e.localizedMessage ?: "Failed to update profile"
                    )
                }
            }
        }
    }

    fun clearFeedbackMessages() {
        _uiState.update { it.copy(errorMessage = null, updateSuccessMessage = null) }
    }
}