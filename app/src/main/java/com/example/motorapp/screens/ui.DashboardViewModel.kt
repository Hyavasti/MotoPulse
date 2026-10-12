package com.example.motorapp.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motorapp.data.GarageBike
import com.example.motorapp.data.MaintenancePart
import com.example.motorapp.data.MaintenanceRecord
import com.example.motorapp.data.MotorRepository
import com.example.motorapp.data.VehicleProfile
import com.example.motorapp.data.supabase
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive

data class DashboardUiState(
    val profile: VehicleProfile = VehicleProfile(),
    val userBikes: List<GarageBike> = emptyList(),
    val activeBike: GarageBike? = null,
    val parts: List<MaintenancePart> = emptyList(),
    val history: List<MaintenanceRecord> = emptyList(),
    val displayName: String = "",
    val isLoading: Boolean = false,
    val isUpdatingOdometer: Boolean = false,
    val isResettingPart: Boolean = false,
    val error: String? = null
)

class DashboardViewModel(
    private val repository: MotorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        // 1. Initial full fetch to hydrate the repository flows & user info
        loadDashboardData(showLoadingOverlay = true)

        // 2. Real-time sync: React immediately to additions, deletions, and switches from Garage
        observeRepositoryVehicleFlows()
    }

    private fun observeRepositoryVehicleFlows() {
        viewModelScope.launch {
            combine(
                repository.garageBikesFlow,
                repository.activeBikeFlow
            ) { bikes, activeBike ->
                Pair(bikes, activeBike)
            }.collect { (bikes, activeBike) ->
                // Fast-path: immediately reflect additions, deletions, or switches in the UI state
                _uiState.update { currentState ->
                    val updatedProfile = if (activeBike == null) {
                        currentState.profile.copy(
                            bikeName = "",
                            plateNumber = "",
                            imageUrl = "",
                            currentOdometerKm = 0.0
                        )
                    } else {
                        currentState.profile.copy(
                            bikeName = activeBike.bikeName,
                            plateNumber = activeBike.plateNumber,
                            imageUrl = activeBike.imageUrl ?: "",
                            currentOdometerKm = activeBike.currentOdometerKm
                        )
                    }

                    currentState.copy(
                        userBikes = bikes,
                        activeBike = activeBike,
                        profile = updatedProfile,
                        parts = if (activeBike == null) emptyList() else currentState.parts,
                        history = if (activeBike == null) emptyList() else currentState.history
                    )
                }

                // If active bike changed or was removed, refresh parts & service history for the new ID
                if (activeBike != null) {
                    val parts = repository.getParts(bikeId = activeBike.id)
                    val history = getMainMaintenanceHistorySafe(activeBike.id)
                    _uiState.update { it.copy(parts = parts, history = history) }
                }
            }
        }
    }

    fun refreshData() {
        loadDashboardData(showLoadingOverlay = false)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun loadDashboardData(
        selectedBikeId: String? = null,
        showLoadingOverlay: Boolean = true
    ) {
        viewModelScope.launch {
            if (showLoadingOverlay) {
                _uiState.update { it.copy(isLoading = true, error = null) }
            } else {
                _uiState.update { it.copy(error = null) }
            }

            try {
                // 1. Fetch garage bikes (this also populates repository.garageBikesFlow & activeBikeFlow)
                val bikes = repository.getGarageBikes()

                // 2. Resolve active vehicle safely
                val activeBike: GarageBike? = when {
                    bikes.isEmpty() -> null
                    selectedBikeId != null -> bikes.find { it.id == selectedBikeId } ?: bikes.firstOrNull()
                    bikes.any { it.isActive } -> bikes.firstOrNull { it.isActive }
                    _uiState.value.activeBike != null -> bikes.find { it.id == _uiState.value.activeBike?.id } ?: bikes.firstOrNull()
                    else -> bikes.firstOrNull()
                }

                // 3. Resolve username safely
                val rawProfile = repository.getProfile() ?: VehicleProfile()
                val userProfile = repository.getUserProfile()
                val currentUser = supabase.auth.currentUserOrNull()

                val metaUsername = try {
                    currentUser?.userMetadata?.get("username")?.jsonPrimitive?.content
                } catch (_: Exception) {
                    null
                }

                val resolvedUsername = when {
                    !userProfile.username.isNullOrBlank() -> userProfile.username
                    !rawProfile.username.isNullOrBlank() -> rawProfile.username
                    !metaUsername.isNullOrBlank() -> metaUsername
                    currentUser?.email?.isNotBlank() == true -> currentUser.email?.substringBefore("@").orEmpty()
                    else -> ""
                }

                val sanitizedProfile = if (activeBike == null) {
                    rawProfile.copy(
                        username = resolvedUsername,
                        bikeName = "",
                        plateNumber = "",
                        imageUrl = "",
                        currentOdometerKm = 0.0
                    )
                } else {
                    rawProfile.copy(
                        username = resolvedUsername,
                        bikeName = activeBike.bikeName,
                        plateNumber = activeBike.plateNumber,
                        imageUrl = activeBike.imageUrl ?: "",
                        currentOdometerKm = activeBike.currentOdometerKm
                    )
                }

                // 4. Fetch parts & history safely
                val targetBikeId = activeBike?.id
                val parts = if (!targetBikeId.isNullOrBlank()) {
                    repository.getParts(bikeId = targetBikeId)
                } else {
                    emptyList()
                }

                val history = if (!targetBikeId.isNullOrBlank()) {
                    getMainMaintenanceHistorySafe(targetBikeId)
                } else {
                    emptyList()
                }

                _uiState.update {
                    it.copy(
                        profile = sanitizedProfile,
                        userBikes = bikes,
                        activeBike = activeBike,
                        displayName = resolvedUsername,
                        parts = parts,
                        history = history,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Log.e("DashboardVM", "Failed to load dashboard: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Failed to load dashboard data."
                    )
                }
            }
        }
    }

    private suspend fun getMainMaintenanceHistorySafe(bikeId: String): List<MaintenanceRecord> {
        return try {
            repository.getMaintenanceHistory(bikeId = bikeId)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun selectBike(bike: GarageBike) {
        viewModelScope.launch {
            try {
                // repository.setActiveVehicle automatically updates repository flows
                repository.setActiveVehicle(bike)
            } catch (e: Exception) {
                Log.e("DashboardVM", "Error switching bike: ${e.message}", e)
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    fun updateOdometer(newOdo: Double) {
        val targetBike = _uiState.value.activeBike ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingOdometer = true) }
            try {
                repository.updateVehicleOdometer(targetBike.id, newOdo)
                _uiState.update { it.copy(isUpdatingOdometer = false) }
            } catch (e: Exception) {
                Log.e("DashboardVM", "Error updating odometer: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isUpdatingOdometer = false,
                        error = e.localizedMessage ?: "Failed to update odometer."
                    )
                }
            }
        }
    }

    fun uploadActiveBikePhoto(bikeId: String, imageBytes: ByteArray) {
        viewModelScope.launch {
            try {
                repository.uploadVehiclePhoto(bikeId, imageBytes)
            } catch (e: Exception) {
                Log.e("DashboardVM", "Failed to upload bike photo: ${e.message}", e)
                _uiState.update {
                    it.copy(error = e.localizedMessage ?: "Failed to upload photo.")
                }
            }
        }
    }

    fun resetPartService(partId: String, cost: Double = 0.0, notes: String = "") {
        viewModelScope.launch {
            val part = _uiState.value.parts.find { it.id == partId } ?: return@launch
            val activeBike = _uiState.value.activeBike
            val currentOdo = activeBike?.currentOdometerKm ?: _uiState.value.profile.currentOdometerKm

            _uiState.update { it.copy(isResettingPart = true) }
            try {
                repository.logServiceRecord(
                    part = part,
                    currentOdo = currentOdo,
                    bikeId = activeBike?.id
                )
                if (activeBike?.id != null) {
                    val parts = repository.getParts(bikeId = activeBike.id)
                    val history = getMainMaintenanceHistorySafe(activeBike.id)
                    _uiState.update { it.copy(parts = parts, history = history) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.localizedMessage ?: "Failed to log service record."
                    )
                }
            } finally {
                _uiState.update { it.copy(isResettingPart = false) }
            }
        }
    }
}