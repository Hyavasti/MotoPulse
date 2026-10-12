package com.example.motorapp.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

// Motorcycle Profile & Current State
@Serializable
data class VehicleProfile(
    val id: String = "",
    @SerialName("username") val username: String? = null,
    @SerialName("bike_name") val bikeName: String? = null,
    @SerialName("plate_number") val plateNumber: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("current_odometer_km") val currentOdometerKm: Double = 0.0,
    @SerialName("fuel_tank_capacity_liters") val fuelTankCapacityLiters: Double = 5.5
)

// High-wear component lifecycle tracking
@Serializable
data class MaintenancePart(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("bike_id") val bikeId: String? = null,
    @SerialName("part_name") val partName: String = "",
    val category: String = "Engine", // Engine, Transmission, Brakes, Tires, Electrical
    @SerialName("last_serviced_odometer_km") val lastServicedOdoKm: Double = 0.0,
    @SerialName("interval_km") val intervalKm: Double = 1500.0,
    val notes: String = ""
) {
    fun remainingKm(currentOdo: Double): Double =
        (lastServicedOdoKm + intervalKm) - currentOdo

    fun healthPercentage(currentOdo: Double): Float {
        if (intervalKm <= 0.0) return 0f
        val used = (currentOdo - lastServicedOdoKm).coerceAtLeast(0.0)
        val remaining = (intervalKm - used).coerceAtLeast(0.0)
        return ((remaining / intervalKm) * 100f).toFloat().coerceIn(0f, 100f)
    }
}

// Completed Service History Entry (Matches Supabase maintenance_history table)
@Serializable
data class MaintenanceRecord(
    val id: String? = null,
    @SerialName("user_id") val userId: String = "",
    @SerialName("bike_id") val bikeId: String? = null,
    @SerialName("part_name") val partName: String = "",
    val category: String = "",
    @SerialName("serviced_odometer_km") val servicedOdometerKm: Double = 0.0,
    @SerialName("service_date") val serviceDate: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

// User Profile & Account Data (Synced with Supabase profiles table)
@Serializable
data class UserProfile(
    @SerialName("id") val id: String = "",
    @SerialName("username") val username: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("bike_name") val bikeName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

// Fuel Fill-up Log (Unified)
@Serializable
data class FuelLog(
    val id: String? = null,
    @SerialName("user_id") val userId: String = "",
    @SerialName("bike_id") val bikeId: String? = null,
    @SerialName("odometer_km") val odometerKm: Double = 0.0,
    val liters: Double = 0.0,
    @SerialName("total_cost") val totalCost: Double = 0.0,
    @SerialName("fuel_efficiency_km_l") val fuelEfficiencyKmL: Double? = null,
    @SerialName("gas_station") val gasStation: String? = null,
    @SerialName("fuel_date") val fuelDate: String? = null,
    @SerialName("logged_at") val loggedAt: String? = null
)

// Reference OEM Specs (Synced with Supabase motorcycle_catalog table)
@Serializable
data class MotorcycleCatalogItem(
    val id: String = "",
    val brand: String = "",
    val category: String = "",
    @SerialName("model_name") val modelName: String = "",
    val version: String = "Standard",
    @SerialName("image_url") val imageUrls: Map<String, String> = emptyMap(),
    @SerialName("oil_capacity_ml") val oilCapacityMl: Int = 800,
    @SerialName("oil_overhaul_ml") val oilOverhaulMl: Int? = null,
    @SerialName("front_psi") val frontPsi: Int = 29,
    @SerialName("rear_psi") val rearPsi: Int = 33,
    @SerialName("spark_plug") val sparkPlug: String = "CPR9EA-9",
    @SerialName("transmission_type") val transmissionType: String = "CVT",
    @SerialName("available_colors") val availableColors: List<String> = emptyList()
) {
    fun getImageUrlForColor(color: String): String? {
        return imageUrls[color] ?: imageUrls["default"] ?: imageUrls.values.firstOrNull()
    }
}

// User Owned Bike in Garage (Synced with Supabase vehicles table)
@Serializable
data class GarageBike(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String = "",
    @SerialName("bike_name") val bikeName: String = "",
    @SerialName("plate_number") val plateNumber: String = "",
    @SerialName("model_id") val modelId: String = "",
    val brand: String = "",
    val category: String = "",
    val version: String = "Standard",
    val color: String = "Standard",
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("oil_capacity_ml") val oilCapacityMl: Int = 800,
    @SerialName("front_psi") val frontPsi: Int = 29,
    @SerialName("rear_psi") val rearPsi: Int = 33,
    @SerialName("spark_plug") val sparkPlug: String = "CPR9EA-9",
    @SerialName("transmission_type") val transmissionType: String = "CVT",
    @SerialName("current_odometer_km") val currentOdometerKm: Double = 0.0,
    @SerialName("oil_interval_km") val oilIntervalKm: Double = 2000.0,
    @SerialName("is_active") val isActive: Boolean = false
)

// ==========================================
// Chat & Community Messaging Models
// ==========================================

@Serializable
enum class RoomType {
    @SerialName("direct") DIRECT,
    @SerialName("convoy") CONVOY,
    @SerialName("regional") REGIONAL
}

// Synced with Supabase chat_rooms table
@Serializable
data class ChatRoom(
    val id: String = "",
    val type: RoomType = RoomType.DIRECT,
    val name: String? = null,
    val region: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

// Synced with Supabase chat_participants table
@Serializable
data class ChatParticipant(
    @SerialName("room_id") val roomId: String = "",
    @SerialName("user_id") val userId: String = "",
    val role: String = "member", // leader, member
    @SerialName("joined_at") val joinedAt: String? = null
)

// Synced with Supabase chat_messages table (Incoming / Query representation)
@Serializable
data class ChatMessage(
    val id: String = "",
    @SerialName("room_id") val roomId: String = "",
    @SerialName("sender_id") val senderId: String = "",
    val content: String = "",
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

// Outgoing payload DTO for inserting new messages
@Serializable
data class SendMessageDto(
    @SerialName("room_id") val roomId: String,
    @SerialName("sender_id") val senderId: String,
    val content: String
)

@Serializable
data class ConvoyRide(
    val id: String = "",
    @SerialName("room_id") val roomId: String = "",
    @SerialName("creator_id") val creatorId: String = "",
    @SerialName("destination_name") val destinationName: String = "",
    @SerialName("destination_lat") val destinationLat: Double = 0.0,
    @SerialName("destination_lng") val destinationLng: Double = 0.0,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CreateConvoyRideDto(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("room_id") val roomId: String,
    @SerialName("creator_id") val creatorId: String,
    @SerialName("destination_name") val destinationName: String,
    @SerialName("destination_lat") val destinationLat: Double,
    @SerialName("destination_lng") val destinationLng: Double,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class ConvoyRiderLocation(
    @SerialName("ride_id") val rideId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("has_joined") val hasJoined: Boolean = true,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val heading: Float = 0f,
    @SerialName("speed_kmh") val speedKmh: Float = 0f,
    @SerialName("updated_at") val updatedAt: String? = null
)