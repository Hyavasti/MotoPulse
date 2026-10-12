package com.example.motorapp.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motorapp.data.FuelLog
import com.example.motorapp.data.GarageBike
import com.example.motorapp.data.MotorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FuelUiState(
    val userBikes: List<GarageBike> = emptyList(),
    val activeBike: GarageBike? = null,
    val logs: List<FuelLog> = emptyList(),
    val averageEfficiencyKmL: Double = 0.0,
    val totalSpent: Double = 0.0,
    val isLoading: Boolean = false,
    val currentOdo: Double = 0.0
)

class FuelViewModel(private val repository: MotorRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(FuelUiState(isLoading = true))
    val uiState: StateFlow<FuelUiState> = _uiState.asStateFlow()

    init {
        // Initial fetch to prime the repository flows
        loadData()

        // Real-time synchronization across all tabs and screens
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
                if (activeBike == null) {
                    _uiState.update {
                        it.copy(
                            userBikes = bikes,
                            activeBike = null,
                            logs = emptyList(),
                            averageEfficiencyKmL = 0.0,
                            totalSpent = 0.0,
                            currentOdo = 0.0,
                            isLoading = false
                        )
                    }
                } else {
                    val logs = try {
                        repository.getFuelLogs(bikeId = activeBike.id)
                    } catch (e: Exception) {
                        Log.e("FuelViewModel", "Failed fetching logs for bike ${activeBike.id}: ${e.message}", e)
                        emptyList()
                    }

                    val validEfficiencies = logs
                        .mapNotNull { it.fuelEfficiencyKmL }
                        .filter { it > 0.0 }

                    val avgEff = if (validEfficiencies.isNotEmpty()) validEfficiencies.average() else 0.0
                    val totalCost = logs.sumOf { it.totalCost }

                    _uiState.update {
                        it.copy(
                            userBikes = bikes,
                            activeBike = activeBike,
                            logs = logs,
                            averageEfficiencyKmL = avgEff,
                            totalSpent = totalCost,
                            currentOdo = activeBike.currentOdometerKm,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun loadData(selectedBikeId: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // 1. Fetch user's registered motorcycles (populates repository flows)
                val bikes = repository.getGarageBikes()

                // 2. Determine active bike: selected > currently held active > first bike
                val active = when {
                    selectedBikeId != null -> bikes.find { it.id == selectedBikeId }
                    _uiState.value.activeBike != null -> bikes.find { it.id == _uiState.value.activeBike?.id }
                    else -> bikes.firstOrNull { it.isActive } ?: bikes.firstOrNull()
                }

                val currentBikeOdo = active?.currentOdometerKm ?: 0.0

                // 3. Fetch logs ONLY for the active motorcycle
                val logs: List<FuelLog> = if (active != null) {
                    repository.getFuelLogs(bikeId = active.id)
                } else {
                    emptyList()
                }

                val validEfficiencies = logs
                    .mapNotNull { it.fuelEfficiencyKmL }
                    .filter { it > 0.0 }

                val avgEff = if (validEfficiencies.isNotEmpty()) validEfficiencies.average() else 0.0
                val totalCost = logs.sumOf { it.totalCost }

                _uiState.update {
                    it.copy(
                        userBikes = bikes,
                        activeBike = active,
                        logs = logs,
                        averageEfficiencyKmL = avgEff,
                        totalSpent = totalCost,
                        isLoading = false,
                        currentOdo = currentBikeOdo
                    )
                }
            } catch (e: Exception) {
                Log.e("FuelViewModel", "Failed to load fuel data: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectBike(bike: GarageBike) {
        viewModelScope.launch {
            try {
                // setActiveVehicle broadcasts immediately across all viewmodels via repository flow
                repository.setActiveVehicle(bike)
            } catch (e: Exception) {
                Log.e("FuelViewModel", "Failed to switch bike: ${e.message}", e)
            }
        }
    }

    fun logRefuel(
        liters: Double,
        totalCost: Double,
        gasStation: String,
        fuelDate: String,
        customOdo: Double? = null
    ) {
        viewModelScope.launch {
            try {
                val activeBike = _uiState.value.activeBike
                val odoToUse = customOdo ?: activeBike?.currentOdometerKm ?: _uiState.value.currentOdo
                val previousOdo = _uiState.value.logs.firstOrNull()?.odometerKm

                repository.addFuelLog(
                    bikeId = activeBike?.id,
                    currentOdo = odoToUse,
                    liters = liters,
                    totalCost = totalCost,
                    previousOdo = previousOdo,
                    gasStation = gasStation,
                    fuelDate = fuelDate
                )

                // Refresh the active bike's logs
                if (activeBike != null) {
                    val logs = repository.getFuelLogs(bikeId = activeBike.id)
                    val validEfficiencies = logs
                        .mapNotNull { it.fuelEfficiencyKmL }
                        .filter { it > 0.0 }
                    val avgEff = if (validEfficiencies.isNotEmpty()) validEfficiencies.average() else 0.0
                    val totalCostSum = logs.sumOf { it.totalCost }

                    _uiState.update {
                        it.copy(
                            logs = logs,
                            averageEfficiencyKmL = avgEff,
                            totalSpent = totalCostSum,
                            currentOdo = odoToUse
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("FuelViewModel", "Failed to save fuel log: ${e.message}", e)
            }
        }
    }
}