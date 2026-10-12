package com.example.motorapp.data

import android.util.Log
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Serializable
data class VehicleImageUrlUpdate(
    val image_url: String?
)

class MotorRepository(private val userId: String) {

    private fun getCurrentDateString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // --- SHARED REACTIVE STREAMS ACROSS ALL SCREENS ---
    private val _garageBikesFlow = MutableStateFlow<List<GarageBike>>(emptyList())
    val garageBikesFlow: StateFlow<List<GarageBike>> = _garageBikesFlow.asStateFlow()

    private val _activeBikeFlow = MutableStateFlow<GarageBike?>(null)
    val activeBikeFlow: StateFlow<GarageBike?> = _activeBikeFlow.asStateFlow()

    private fun updateLocalVehicleCache(bikes: List<GarageBike>) {
        _garageBikesFlow.value = bikes
        _activeBikeFlow.value = bikes.firstOrNull { it.isActive } ?: bikes.firstOrNull()
    }

    // --- PROFILE & ODOMETER ---
    suspend fun getProfile(): VehicleProfile? = withContext(Dispatchers.IO) {
        try {
            supabase.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeList<VehicleProfile>()
                .firstOrNull()
        } catch (e: Exception) {
            Log.e("MotorRepository", "Error fetching vehicle profile: ${e.message}", e)
            null
        }
    }

    suspend fun getUserProfile(): UserProfile = withContext(Dispatchers.IO) {
        try {
            supabase.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeList<UserProfile>()
                .firstOrNull() ?: UserProfile(id = userId)
        } catch (e: Exception) {
            Log.e("MotorRepository", "Error fetching user profile: ${e.message}", e)
            UserProfile(id = userId)
        }
    }

    suspend fun updateUsername(newUsername: String) = withContext(Dispatchers.IO) {
        try {
            supabase.from("profiles").update(
                mapOf("username" to newUsername)
            ) {
                filter { eq("id", userId) }
            }
        } catch (e: Exception) {
            Log.e("MotorRepository", "Error updating username: ${e.message}", e)
            throw e
        }
    }

    suspend fun updateVehicleOdometer(bikeId: String, newOdo: Double) = withContext(Dispatchers.IO) {
        if (bikeId.isBlank()) return@withContext

        // Optimistically update memory flow
        val updatedList = _garageBikesFlow.value.map {
            if (it.id == bikeId) it.copy(currentOdometerKm = newOdo) else it
        }
        updateLocalVehicleCache(updatedList)

        try {
            supabase.from("vehicles").update(
                mapOf("current_odometer_km" to newOdo)
            ) {
                filter {
                    eq("id", bikeId)
                    eq("user_id", userId)
                }
            }

            supabase.from("profiles").update(
                mapOf("current_odometer_km" to newOdo)
            ) {
                filter { eq("id", userId) }
            }
        } catch (e: Exception) {
            Log.e("MotorRepository", "Error updating vehicle odo: ${e.message}", e)
        }
    }

    suspend fun updateVehicleOilInterval(bikeId: String, intervalKm: Double) = withContext(Dispatchers.IO) {
        if (bikeId.isBlank()) return@withContext

        try {
            supabase.from("vehicles").update(
                mapOf("oil_interval_km" to intervalKm)
            ) {
                filter {
                    eq("id", bikeId)
                    eq("user_id", userId)
                }
            }
        } catch (e: Exception) {
            Log.e("MotorRepository", "Error updating oil interval: ${e.message}", e)
            throw e
        }
    }

    suspend fun updateOdometer(newOdo: Double) = withContext(Dispatchers.IO) {
        try {
            val activeBike = _activeBikeFlow.value ?: getGarageBikes().firstOrNull { it.isActive } ?: getGarageBikes().firstOrNull()

            if (activeBike != null && activeBike.id.isNotBlank()) {
                updateVehicleOdometer(activeBike.id, newOdo)
            } else {
                supabase.from("profiles").update(
                    mapOf("current_odometer_km" to newOdo)
                ) {
                    filter { eq("id", userId) }
                }
            }
        } catch (e: Exception) {
            Log.e("MotorRepository", "Error updating odometer: ${e.message}", e)
        }
    }

    suspend fun setActiveVehicle(bike: GarageBike) = withContext(Dispatchers.IO) {
        if (bike.id.isBlank()) return@withContext

        // Instant local broadcast to all screens
        val updated = _garageBikesFlow.value.map {
            it.copy(isActive = (it.id == bike.id))
        }
        updateLocalVehicleCache(updated)

        try {
            supabase.from("vehicles").update(
                mapOf("is_active" to false)
            ) {
                filter { eq("user_id", userId) }
            }

            supabase.from("vehicles").update(
                mapOf("is_active" to true)
            ) {
                filter {
                    eq("id", bike.id)
                    eq("user_id", userId)
                }
            }

            supabase.from("profiles").update(
                mapOf(
                    "bike_name" to bike.bikeName,
                    "plate_number" to bike.plateNumber,
                    "image_url" to bike.imageUrl,
                    "current_odometer_km" to bike.currentOdometerKm
                )
            ) {
                filter { eq("id", userId) }
            }
        } catch (e: Exception) {
            Log.e("MotorRepository", "Could not sync active vehicle to database: ${e.message}", e)
        }
    }

    // --- PARTS & MAINTENANCE ---
    suspend fun getParts(bikeId: String? = null): List<MaintenancePart> = withContext(Dispatchers.IO) {
        try {
            supabase.from("parts")
                .select {
                    filter {
                        eq("user_id", userId)
                        if (!bikeId.isNullOrBlank()) {
                            eq("bike_id", bikeId)
                        }
                    }
                }
                .decodeList<MaintenancePart>()
        } catch (e: Exception) {
            Log.e("MotorRepository", "Error fetching parts: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getCustomParts(bikeId: String? = null): List<MaintenancePart> = withContext(Dispatchers.IO) {
        getParts(bikeId)
    }

    suspend fun addCustomPart(
        bikeId: String?,
        partName: String,
        category: String,
        lastReplacedKm: Double,
        intervalKm: Double,
        notes: String
    ): MaintenancePart = withContext(Dispatchers.IO) {
        val newPart = MaintenancePart(
            id = UUID.randomUUID().toString(),
            userId = userId,
            bikeId = bikeId?.ifBlank { null },
            partName = partName,
            category = category,
            lastServicedOdoKm = lastReplacedKm,
            intervalKm = intervalKm,
            notes = notes
        )

        try {
            supabase.from("parts").insert(newPart)
            newPart
        } catch (e: Exception) {
            Log.e("MotorRepository", "Error adding custom part: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteCustomPart(partId: String) = withContext(Dispatchers.IO) {
        try {
            supabase.from("parts").delete {
                filter {
                    eq("id", partId)
                    eq("user_id", userId)
                }
            }
        } catch (e: Exception) {
            Log.e("MotorRepository", "Error deleting custom part: ${e.message}", e)
            throw e
        }
    }

    suspend fun logServiceRecord(
        part: MaintenancePart,
        currentOdo: Double,
        bikeId: String? = null,
        serviceDate: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val entry = MaintenanceRecord(
                userId = userId,
                bikeId = bikeId?.ifBlank { null },
                partName = part.partName,
                category = part.category,
                servicedOdometerKm = currentOdo,
                serviceDate = serviceDate ?: getCurrentDateString()
            )

            supabase.from("maintenance_history").insert(entry)

            if (part.id.isNotBlank()) {
                supabase.from("parts").update(
                    mapOf("last_serviced_odometer_km" to currentOdo)
                ) {
                    filter { eq("id", part.id) }
                }
            }
        } catch (e: Exception) {
            Log.e("MotorRepository", "Error logging service record: ${e.message}", e)
            throw e
        }
    }

    suspend fun recordMaintenanceService(
        bikeId: String?,
        itemTitle: String,
        category: String,
        servicedOdo: Double,
        serviceDate: String? = null
    ) = withContext(Dispatchers.IO) {
        val entry = MaintenanceRecord(
            userId = userId,
            bikeId = bikeId?.ifBlank { null },
            partName = itemTitle,
            category = category,
            servicedOdometerKm = servicedOdo,
            serviceDate = serviceDate ?: getCurrentDateString()
        )

        try {
            supabase.from("maintenance_history").insert(entry)
        } catch (e: Exception) {
            Log.e("MotorRepository", "Error recording maintenance service: ${e.message}", e)
            throw e
        }
    }

    suspend fun getMaintenanceHistory(bikeId: String? = null): List<MaintenanceRecord> = withContext(Dispatchers.IO) {
        try {
            supabase.from("maintenance_history")
                .select {
                    filter {
                        eq("user_id", userId)
                        if (!bikeId.isNullOrBlank()) {
                            eq("bike_id", bikeId)
                        }
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<MaintenanceRecord>()
        } catch (e: Exception) {
            Log.e("MotorRepository", "Error fetching maintenance history: ${e.message}", e)
            emptyList()
        }
    }

    // --- FUEL TRACKING & LIVE EFFICIENCY ---
    suspend fun getFuelLogs(bikeId: String? = null): List<FuelLog> = withContext(Dispatchers.IO) {
        try {
            supabase.from("fuel_logs")
                .select {
                    filter {
                        eq("user_id", userId)
                        if (!bikeId.isNullOrBlank()) {
                            eq("bike_id", bikeId)
                        }
                    }
                    order("logged_at", Order.DESCENDING)
                }
                .decodeList<FuelLog>()
        } catch (e: Exception) {
            Log.e("MotorRepository", "Error fetching fuel logs: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun addFuelLog(
        bikeId: String?,
        currentOdo: Double,
        liters: Double,
        totalCost: Double,
        previousOdo: Double?,
        gasStation: String = "",
        fuelDate: String = ""
    ) = withContext(Dispatchers.IO) {
        val efficiency = if (previousOdo != null && currentOdo > previousOdo && liters > 0.0) {
            (currentOdo - previousOdo) / liters
        } else null

        val entry = FuelLog(
            userId = userId,
            bikeId = bikeId?.ifBlank { null },
            odometerKm = currentOdo,
            liters = liters,
            totalCost = totalCost,
            fuelEfficiencyKmL = efficiency,
            gasStation = gasStation,
            fuelDate = fuelDate.ifBlank { getCurrentDateString() }
        )

        supabase.from("fuel_logs").insert(entry)

        if (!bikeId.isNullOrBlank()) {
            updateVehicleOdometer(bikeId, currentOdo)
        } else {
            updateOdometer(currentOdo)
        }
    }

    // --- MOTORCYCLE CATALOG ---
    suspend fun getMotorcycleCatalog(
        brand: String? = null,
        category: String? = null
    ): List<MotorcycleCatalogItem> = withContext(Dispatchers.IO) {
        try {
            supabase.from("motorcycle_catalog")
                .select {
                    filter {
                        if (!brand.isNullOrBlank()) {
                            eq("brand", brand)
                        }
                        if (!category.isNullOrBlank()) {
                            eq("category", category)
                        }
                    }
                    order("brand", Order.ASCENDING)
                    order("model_name", Order.ASCENDING)
                }
                .decodeList<MotorcycleCatalogItem>()
        } catch (e: Exception) {
            Log.e("MotorRepository", "Error fetching motorcycle catalog: ${e.message}", e)
            emptyList()
        }
    }

    // --- VEHICLES & GARAGE ---
    suspend fun getGarageBikes(): List<GarageBike> = withContext(Dispatchers.IO) {
        val bikes = try {
            supabase.from("vehicles")
                .select {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<GarageBike>()
        } catch (_: Exception) {
            try {
                supabase.from("vehicles")
                    .select {
                        filter { eq("user_id", userId) }
                        order("id", Order.ASCENDING)
                    }
                    .decodeList<GarageBike>()
            } catch (e: Exception) {
                Log.e("MotorRepository", "Error fetching garage bikes: ${e.message}", e)
                _garageBikesFlow.value
            }
        }

        updateLocalVehicleCache(bikes)
        bikes
    }

    suspend fun saveGarageBike(bike: GarageBike) = withContext(Dispatchers.IO) {
        val bikeId = bike.id.ifBlank { UUID.randomUUID().toString() }
        val bikeWithUser = bike.copy(
            id = bikeId,
            userId = userId,
            isActive = true
        )

        val currentList = _garageBikesFlow.value.map { it.copy(isActive = false) }
        val updatedList = currentList + bikeWithUser
        updateLocalVehicleCache(updatedList)

        supabase.from("vehicles").insert(bikeWithUser)
        setActiveVehicle(bikeWithUser)
    }

    suspend fun uploadVehiclePhoto(bikeId: String, imageBytes: ByteArray): String = withContext(Dispatchers.IO) {
        try {
            val fileName = "user_vehicles/${userId}_${bikeId}_${UUID.randomUUID()}.jpg"
            val bucket = supabase.storage.from("motorcycles")

            // 1. Upload file to Supabase storage
            bucket.upload(fileName, imageBytes) {
                upsert = true
            }

            // 2. Obtain public CDN URL
            val publicUrl = bucket.publicUrl(fileName)

            // 3. Persist image_url directly into the vehicles table
            supabase.from("vehicles").update(
                mapOf("image_url" to publicUrl)
            ) {
                filter {
                    eq("id", bikeId)
                    eq("user_id", userId)
                }
            }

            // 4. Update the reactive local state immediately (Zero latency)
            val updatedBikes = _garageBikesFlow.value.map { bike ->
                if (bike.id == bikeId) bike.copy(imageUrl = publicUrl) else bike
            }
            _garageBikesFlow.value = updatedBikes

            val currentActive = _activeBikeFlow.value
            if (currentActive?.id == bikeId) {
                _activeBikeFlow.value = currentActive.copy(imageUrl = publicUrl)
            }

            publicUrl
        } catch (e: Exception) {
            Log.e("MotorRepository", "Error uploading vehicle photo: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteGarageBike(bikeId: String): List<GarageBike> = withContext(Dispatchers.IO) {
        val remainingBikes = _garageBikesFlow.value.filterNot { it.id == bikeId }
        updateLocalVehicleCache(remainingBikes)

        try {
            try {
                supabase.from("maintenance_history").delete {
                    filter {
                        eq("bike_id", bikeId)
                        eq("user_id", userId)
                    }
                }
                supabase.from("fuel_logs").delete {
                    filter {
                        eq("bike_id", bikeId)
                        eq("user_id", userId)
                    }
                }
                supabase.from("parts").delete {
                    filter {
                        eq("bike_id", bikeId)
                        eq("user_id", userId)
                    }
                }
            } catch (e: Exception) {
                Log.w("MotorRepository", "Failed cleaning up bike child records: ${e.message}")
            }

            supabase.from("vehicles").delete {
                filter {
                    eq("id", bikeId)
                    eq("user_id", userId)
                }
            }

            if (remainingBikes.isEmpty()) {
                supabase.from("profiles").update(
                    mapOf(
                        "bike_name" to null,
                        "plate_number" to null,
                        "image_url" to null,
                        "current_odometer_km" to 0.0
                    )
                ) {
                    filter { eq("id", userId) }
                }
            } else {
                val hasActive = remainingBikes.any { it.isActive }
                if (!hasActive) {
                    val nextActive = remainingBikes.first()
                    setActiveVehicle(nextActive)
                }
            }

            remainingBikes
        } catch (e: Exception) {
            Log.e("MotorRepository", "Error deleting garage bike: ${e.message}", e)
            throw e
        }
    }
}