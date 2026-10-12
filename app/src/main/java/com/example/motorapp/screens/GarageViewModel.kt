package com.example.motorapp.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motorapp.data.GarageBike
import com.example.motorapp.data.MotorRepository
import com.example.motorapp.data.MotorcycleCatalogItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GarageUiState(
    val catalog: List<MotorcycleCatalogItem> = emptyList(),
    val userBikes: List<GarageBike> = emptyList(),
    val activeBike: GarageBike? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,

    // Step filtering state
    val selectedBrand: String? = null,
    val selectedCategory: String? = null,
    val searchQuery: String = ""
) {
    val availableBrands: List<String>
        get() = catalog.map { it.brand }.filter { it.isNotBlank() }.distinct().sorted()

    val availableCategories: List<String>
        get() = catalog
            .filter { selectedBrand == null || it.brand.equals(selectedBrand, ignoreCase = true) }
            .map { it.category }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

    val filteredCatalog: List<MotorcycleCatalogItem>
        get() = catalog.filter { item ->
            val matchesBrand = selectedBrand == null || item.brand.equals(selectedBrand, ignoreCase = true)
            val matchesCategory = selectedCategory == null || item.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    item.modelName.contains(searchQuery, ignoreCase = true) ||
                    item.version.contains(searchQuery, ignoreCase = true) ||
                    item.brand.contains(searchQuery, ignoreCase = true)

            matchesBrand && matchesCategory && matchesSearch
        }
}

class GarageViewModel(private val repository: MotorRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(GarageUiState(isLoading = true))
    val uiState: StateFlow<GarageUiState> = _uiState.asStateFlow()

    init {
        // Initial fetch for catalog and repository cache
        loadData(showLoadingOverlay = true)

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
                _uiState.update {
                    it.copy(
                        userBikes = bikes,
                        activeBike = activeBike
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun refreshGarage() {
        loadData(showLoadingOverlay = true)
    }

    fun loadData(selectedBikeId: String? = null, showLoadingOverlay: Boolean = true) {
        viewModelScope.launch {
            if (showLoadingOverlay) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(errorMessage = null) }
            }

            try {
                val catalog = repository.getMotorcycleCatalog()
                // getGarageBikes updates repository.garageBikesFlow and activeBikeFlow automatically
                val bikes = repository.getGarageBikes()

                val active = when {
                    selectedBikeId != null -> bikes.find { it.id == selectedBikeId } ?: bikes.lastOrNull()
                    _uiState.value.activeBike != null -> bikes.find { it.id == _uiState.value.activeBike?.id } ?: bikes.firstOrNull()
                    else -> bikes.firstOrNull { it.isActive } ?: bikes.firstOrNull()
                }

                if (selectedBikeId != null && active != null) {
                    repository.setActiveVehicle(active)
                }

                _uiState.update {
                    it.copy(
                        catalog = catalog,
                        userBikes = bikes,
                        activeBike = active,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("GarageVM", "Error loading garage: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to load garage."
                    )
                }
            }
        }
    }

    // --- CATALOG STEP FILTERING ACTIONS ---

    fun selectBrand(brand: String?) {
        _uiState.update {
            it.copy(
                selectedBrand = brand,
                selectedCategory = null
            )
        }
    }

    fun selectCategory(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearCatalogFilters() {
        _uiState.update {
            it.copy(
                selectedBrand = null,
                selectedCategory = null,
                searchQuery = ""
            )
        }
    }

    // --- GARAGE ACTIONS ---

    fun selectBike(bike: GarageBike) {
        viewModelScope.launch {
            try {
                // repository.setActiveVehicle triggers immediate emission across all ViewModels
                repository.setActiveVehicle(bike)
            } catch (e: Exception) {
                Log.e("GarageVM", "Failed to sync active vehicle to profile: ${e.message}", e)
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }

    fun addBikeFromCatalog(
        item: MotorcycleCatalogItem,
        customName: String,
        plateNumber: String,
        color: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val generatedId = java.util.UUID.randomUUID().toString()

                val newBike = GarageBike(
                    id = generatedId,
                    bikeName = customName.ifBlank { "${item.modelName} ${item.version}".trim() },
                    plateNumber = plateNumber,
                    modelId = item.id,
                    brand = item.brand,
                    category = item.category,
                    version = item.version,
                    color = color.ifBlank { "Standard" },
                    imageUrl = null,
                    oilCapacityMl = item.oilCapacityMl,
                    frontPsi = item.frontPsi,
                    rearPsi = item.rearPsi,
                    sparkPlug = item.sparkPlug,
                    transmissionType = item.transmissionType,
                    currentOdometerKm = 0.0,
                    isActive = true
                )

                // saveGarageBike broadcasts instantly to garageBikesFlow & activeBikeFlow
                repository.saveGarageBike(newBike)
                clearCatalogFilters()
            } catch (e: Exception) {
                Log.e("GarageVM", "Error saving garage bike: ${e.message}", e)
                _uiState.update { it.copy(errorMessage = e.localizedMessage ?: "Failed to save motorcycle.") }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun deleteBike(bike: GarageBike) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null) }
            try {
                if (bike.id.isNotBlank()) {
                    // deleteGarageBike broadcasts remaining bikes instantly via repository flow
                    repository.deleteGarageBike(bike.id)
                }
            } catch (e: Exception) {
                Log.e("GarageVM", "Error deleting bike: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        errorMessage = e.localizedMessage ?: "Failed to delete motorcycle."
                    )
                }
            } finally {
                _uiState.update { it.copy(isDeleting = false) }
            }
        }
    }
}