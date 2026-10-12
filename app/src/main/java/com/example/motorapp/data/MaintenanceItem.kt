package com.example.motorapp.data

enum class MaintenanceStatus {
    GOOD,
    DUE_SOON,
    OVERDUE
}

data class MaintenanceItem(
    val id: String,
    val title: String,               // e.g., "Engine Oil", "Brake Pads"
    val category: String,            // "Fluids", "Brakes & Drive", "Filters & Ignition"
    val lastReplacedKm: Double,      // Odometer reading when last replaced
    val intervalKm: Double,          // Service interval (e.g., 2,500 km or 6,000 km)
    val notes: String = ""
) {
    fun getKmRemaining(currentOdo: Double): Double {
        val nextDueKm = lastReplacedKm + intervalKm
        return nextDueKm - currentOdo
    }

    fun getStatus(currentOdo: Double): MaintenanceStatus {
        val remaining = getKmRemaining(currentOdo)
        return when {
            remaining <= 0 -> MaintenanceStatus.OVERDUE
            remaining <= 500 -> MaintenanceStatus.DUE_SOON
            else -> MaintenanceStatus.GOOD
        }
    }

    fun getProgress(currentOdo: Double): Float {
        val used = (currentOdo - lastReplacedKm).coerceAtLeast(0.0)
        return (used / intervalKm).coerceIn(0.0, 1.0).toFloat()
    }
}