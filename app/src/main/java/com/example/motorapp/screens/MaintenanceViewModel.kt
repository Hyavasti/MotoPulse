package com.example.motorapp.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.motorapp.data.GarageBike
import com.example.motorapp.data.MaintenanceItem
import com.example.motorapp.data.MotorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BikeSummary(
    val id: String,
    val bikeName: String,
    val brand: String = "",
    val category: String = "",
    val version: String = "",
    val sparkPlug: String = "",
    val currentOdometerKm: Double = 0.0,
    val transmissionType: String = "",
    val oilIntervalKm: Double = 2000.0
)

data class MaintenanceUiState(
    val userBikes: List<GarageBike> = emptyList(),
    val activeBike: BikeSummary? = null,
    val currentOdo: Double = 0.0,
    val items: List<MaintenanceItem> = emptyList(),
    val isLoading: Boolean = false
)

class MaintenanceViewModel(
    private val repository: MotorRepository
) : ViewModel() {

    // Keep isLoading permanently false to prevent triggering the MainScreen loading overlay
    private val _uiState = MutableStateFlow(MaintenanceUiState(isLoading = false))
    val uiState: StateFlow<MaintenanceUiState> = _uiState.asStateFlow()

    init {
        // Initial fetch to populate repository state
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
                            currentOdo = 0.0,
                            items = emptyList()
                        )
                    }
                } else {
                    val summary = BikeSummary(
                        id = activeBike.id,
                        bikeName = activeBike.bikeName,
                        brand = activeBike.brand,
                        category = activeBike.category,
                        version = activeBike.version,
                        sparkPlug = activeBike.sparkPlug,
                        currentOdometerKm = activeBike.currentOdometerKm,
                        transmissionType = activeBike.transmissionType,
                        oilIntervalKm = activeBike.oilIntervalKm
                    )

                    _uiState.update {
                        it.copy(
                            userBikes = bikes,
                            activeBike = summary,
                            currentOdo = summary.currentOdometerKm
                        )
                    }

                    loadChecklistForBike(summary)
                }
            }
        }
    }

    // --- Dynamic OEM & Mechanic Advice Checklist Engine ---
    private fun buildChecklistForBike(bike: BikeSummary): List<MaintenanceItem> {
        val name = bike.bikeName.lowercase()
        val version = bike.version.lowercase()
        val trans = bike.transmissionType.lowercase()
        val category = bike.category.lowercase()

        val isScooter = trans.contains("cvt") || trans.contains("automatic") || category.contains("scooter")
        val isBigBike = category.contains("big bike") || category.contains("sport") ||
                name.contains("ninja") || name.contains("z400") || name.contains("cb650") ||
                name.contains("rebel 500") || name.contains("mt-03") || name.contains("mt-07") ||
                name.contains("sv650")

        val isManual = trans.contains("manual") || isBigBike

        // Liquid-cooled bikes (equipped with radiators)
        val isLiquidCooled = name.contains("click") ||
                name.contains("nmax") ||
                name.contains("aerox") ||
                name.contains("adv") ||
                name.contains("pcx") ||
                name.contains("sniper") ||
                name.contains("winner") ||
                name.contains("giorno") ||
                (name.contains("raider") && name.contains("fi")) ||
                name.contains("ninja") ||
                name.contains("cb650") ||
                name.contains("rebel 500") ||
                name.contains("mt-03") ||
                name.contains("mt-07") ||
                name.contains("crf300") ||
                name.contains("wr155")

        // Bikes with replaceable paper or spin-on oil filter cartridges
        val hasReplaceableOilFilter = isBigBike ||
                name.contains("raider") ||
                name.contains("sniper") ||
                name.contains("gixxer") ||
                name.contains("crf") ||
                name.contains("wr155")

        // Bikes with rear drum brake shoes instead of hydraulic discs
        val hasRearDrum = (name.contains("click 125") ||
                (name.contains("click 160") && !version.contains("abs")) ||
                (name.contains("aerox") && !version.contains("abs") && !version.contains("s-abs")) ||
                (name.contains("pcx") && version.contains("cbs")) ||
                name.contains("beat") ||
                name.contains("mio") ||
                name.contains("fazzio") ||
                name.contains("burgman") ||
                name.contains("avenis") ||
                name.contains("wave") ||
                name.contains("smash") ||
                name.contains("barako") ||
                name.contains("tmx") ||
                name.contains("gd110") ||
                name.contains("ct125"))

        val isIridiumPlug = isBigBike || name.contains("crf300") || name.contains("cb650")

        val checklist = mutableListOf<MaintenanceItem>()

        // 1. ENGINE OIL
        checklist.add(
            MaintenanceItem(
                id = "engine_oil",
                title = "Engine Oil",
                category = "Fluids",
                lastReplacedKm = 0.0,
                intervalKm = if (isBigBike && bike.oilIntervalKm <= 2000.0) 6000.0 else bike.oilIntervalKm,
                notes = if (isScooter) {
                    """
                    Based on manual: Replace every 2,000 to 4,000 km.
                    Mechanic advice: Change every 1,500 to 2,000 km in heavy traffic or hot weather.
                    """.trimIndent()
                } else {
                    """
                    Based on manual: Replace every 2,000 to 3,000 km (up to 6,000 km on big bikes).
                    Mechanic advice: Change every 1,500 to 2,000 km for daily city commuting.
                    """.trimIndent()
                }
            )
        )

        // 2. OIL FILTER
        if (hasReplaceableOilFilter) {
            checklist.add(
                MaintenanceItem(
                    id = "oil_filter",
                    title = "Engine Oil Filter",
                    category = "Filters & Ignition",
                    lastReplacedKm = 0.0,
                    intervalKm = if (isBigBike) 10000.0 else 4000.0,
                    notes = """
                    Based on manual: Replace paper filter every 2 oil changes.
                    Mechanic advice: Replace every oil change because paper filters trap fine metal shavings.
                    """.trimIndent()
                )
            )
        }

        // 3. RADIATOR COOLANT
        if (isLiquidCooled) {
            checklist.add(
                MaintenanceItem(
                    id = "coolant",
                    title = "Radiator Coolant",
                    category = "Fluids",
                    lastReplacedKm = 0.0,
                    intervalKm = 24000.0,
                    notes = """
                    Based on manual: Flush every 24,000 km or 2 years; inspect reserve tank level regularly.
                    Mechanic advice: Top up reservoir using pre-mix 50/50 coolant; never use plain tap water.
                    """.trimIndent()
                )
            )
        }

        // 4. SPARK PLUG
        checklist.add(
            MaintenanceItem(
                id = "spark_plug",
                title = "Spark Plug${if (bike.sparkPlug.isNotBlank()) " (${bike.sparkPlug})" else ""}",
                category = "Filters & Ignition",
                lastReplacedKm = 0.0,
                intervalKm = if (isIridiumPlug) 24000.0 else 8000.0,
                notes = if (isIridiumPlug) {
                    """
                    Based on manual: Inspect at 12,000 km; replace at 24,000 km.
                    Mechanic advice: Do not scrape or file iridium tip; replace if starting feels sluggish.
                    """.trimIndent()
                } else {
                    """
                    Based on manual: Inspect/clean at 4,000 km; replace at 8,000 km.
                    Mechanic advice: Replace directly every 6,000 to 8,000 km for clean fuel burn and easy starting.
                    """.trimIndent()
                }
            )
        )

        // 5. AIR FILTER
        checklist.add(
            MaintenanceItem(
                id = "air_filter",
                title = "Air Filter",
                category = "Filters & Ignition",
                lastReplacedKm = 0.0,
                intervalKm = 12000.0,
                notes = """
                Based on manual: Replace every 12,000 to 16,000 km. Never blow with compressed air.
                Mechanic advice: Inspect every 3,000 km; replace immediately if soaked in oil or caked in dirt.
                """.trimIndent()
            )
        )

        // 6. FUEL FILTER / STRAINER
        checklist.add(
            MaintenanceItem(
                id = "fuel_filter",
                title = "Fuel Strainer / Filter",
                category = "Filters & Ignition",
                lastReplacedKm = 0.0,
                intervalKm = 15000.0,
                notes = """
                Based on manual: Inspect fuel pump tea-bag strainer at 15,000 km.
                Mechanic advice: Clean or replace if motorcycle jerks or loses power during wide-open throttle.
                """.trimIndent()
            )
        )

        // 7. DRIVETRAIN (CVT vs CHAIN)
        if (isScooter) {
            checklist.add(
                MaintenanceItem(
                    id = "gear_oil",
                    title = "Scooter Gear Oil",
                    category = "Fluids",
                    lastReplacedKm = 0.0,
                    intervalKm = 8000.0,
                    notes = """
                    Based on manual: Inspect at 4,000 km; replace every 8,000 km.
                    Mechanic advice: Replace every 2 engine oil changes (about 3,000 to 4,000 km) for clean gears.
                    """.trimIndent()
                )
            )
            checklist.add(
                MaintenanceItem(
                    id = "cvt_cleaning",
                    title = "CVT Cleaning & Tuning",
                    category = "Transmission & Drive",
                    lastReplacedKm = 0.0,
                    intervalKm = 8000.0,
                    notes = """
                    Based on manual: Clean and inspect flyball rollers and clutch pads every 8,000 km.
                    Mechanic advice: Clean every 3,000 to 5,000 km in dusty or rainy conditions to stop dragging or lag.
                    """.trimIndent()
                )
            )
            checklist.add(
                MaintenanceItem(
                    id = "drive_belt",
                    title = "CVT Drive Belt (V-Belt)",
                    category = "Transmission & Drive",
                    lastReplacedKm = 0.0,
                    intervalKm = 24000.0,
                    notes = """
                    Based on manual: Check for cracks at 8,000 km; replace at 24,000 km.
                    Mechanic advice: Replace between 18,000 and 20,000 km to avoid snapping while on the road.
                    """.trimIndent()
                )
            )
        } else {
            checklist.add(
                MaintenanceItem(
                    id = "chain_lube",
                    title = "Drive Chain Lube",
                    category = "Transmission & Drive",
                    lastReplacedKm = 0.0,
                    intervalKm = 800.0,
                    notes = """
                    Based on manual: Clean and lube every 500 to 1,000 km.
                    Mechanic advice: Clean with chain cleaner/kerosene, lube every 2 weeks or after heavy rain. Avoid used motor oil.
                    """.trimIndent()
                )
            )
            checklist.add(
                MaintenanceItem(
                    id = "chain_slack",
                    title = "Chain Slack & Sprockets",
                    category = "Transmission & Drive",
                    lastReplacedKm = 0.0,
                    intervalKm = 1000.0,
                    notes = """
                    Based on manual: Maintain 20 to 30 mm chain free play.
                    Mechanic advice: Always replace chain and both sprockets together when teeth start hooking.
                    """.trimIndent()
                )
            )
            if (isManual) {
                checklist.add(
                    MaintenanceItem(
                        id = "clutch_freeplay",
                        title = "Clutch Lever Free Play",
                        category = "Transmission & Drive",
                        lastReplacedKm = 0.0,
                        intervalKm = 5000.0,
                        notes = """
                        Based on manual: Maintain 10 to 15 mm free play at lever tip.
                        Mechanic advice: If clutch slips or gears feel stiff, lubricate inner cable or bleed fluid.
                        """.trimIndent()
                    )
                )
            }
        }

        // 8. BRAKES
        checklist.add(
            MaintenanceItem(
                id = "front_brake_pads",
                title = "Front Brake Pads",
                category = "Brakes & Chassis",
                lastReplacedKm = 0.0,
                intervalKm = 5000.0,
                notes = """
                Based on manual: Check pad groove thickness (minimum 1.0 mm thickness limit).
                Mechanic advice: Replace before pads reach bare metal to protect the rotor disc from gouging.
                """.trimIndent()
            )
        )

        checklist.add(
            MaintenanceItem(
                id = "rear_brakes",
                title = if (hasRearDrum) "Rear Brake Shoes (Drum)" else "Rear Brake Pads (Disc)",
                category = "Brakes & Chassis",
                lastReplacedKm = 0.0,
                intervalKm = 5000.0,
                notes = if (hasRearDrum) {
                    """
                    Based on manual: Adjust cable arm play to 15–25 mm; verify wear indicator arrow.
                    Mechanic advice: Clean brake dust inside drum during tire changes to stop squeaking.
                    """.trimIndent()
                } else {
                    """
                    Based on manual: Check hydraulic pad thickness indicator groove.
                    Mechanic advice: Flush fluid and grease slider pins when replacing pads.
                    """.trimIndent()
                }
            )
        )

        checklist.add(
            MaintenanceItem(
                id = "brake_fluid",
                title = "Brake Fluid",
                category = "Fluids",
                lastReplacedKm = 0.0,
                intervalKm = 20000.0,
                notes = """
                Based on manual: Flush and replace every 20,000 km or 2 years.
                Mechanic advice: Replace early if sight glass turns amber or dark brown to prevent spongy lever feel.
                """.trimIndent()
            )
        )

        // 9. TIRES, BATTERY & MECHANICAL
        checklist.add(
            MaintenanceItem(
                id = "tire_condition",
                title = "Tire Tread & Condition",
                category = "Brakes & Chassis",
                lastReplacedKm = 0.0,
                intervalKm = 15000.0,
                notes = """
                Based on manual: Replace when tread touches wear bars (TWI mark).
                Mechanic advice: Check cold tire pressure weekly; replace old tires after 3 to 4 years even if tread remains.
                """.trimIndent()
            )
        )

        checklist.add(
            MaintenanceItem(
                id = "battery_check",
                title = "Battery Health",
                category = "Electrical",
                lastReplacedKm = 0.0,
                intervalKm = 10000.0,
                notes = """
                Based on manual: Resting voltage should be 12.4V to 12.8V; running charge output 14.0V+.
                Mechanic advice: Modern Fi bikes will shut off or fail to start if battery drops below 11.8V.
                """.trimIndent()
            )
        )

        checklist.add(
            MaintenanceItem(
                id = "valve_clearance",
                title = "Valve Clearance (Tappets)",
                category = "Engine & Mechanical",
                lastReplacedKm = 0.0,
                intervalKm = 12000.0,
                notes = """
                Based on manual: Inspect every 12,000 km with feeler gauge while engine is completely cold.
                Mechanic advice: Adjust if engine ticks loudly or struggles to start in the morning.
                """.trimIndent()
            )
        )

        checklist.add(
            MaintenanceItem(
                id = "steering_bearings",
                title = "Handlebar & Wheel Bearings",
                category = "Brakes & Chassis",
                lastReplacedKm = 0.0,
                intervalKm = 15000.0,
                notes = """
                Based on manual: Check for smooth handlebar sweep and test wheel lateral play.
                Mechanic advice: Replace steering ball bearings if handlebars feel notched or catch in the center.
                """.trimIndent()
            )
        )

        return checklist
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                // Fetch vehicles through repository (populates garageBikesFlow & activeBikeFlow)
                val bikes = repository.getGarageBikes()
                val active = bikes.firstOrNull { it.isActive } ?: bikes.firstOrNull()

                if (active != null) {
                    val bikeSummary = BikeSummary(
                        id = active.id,
                        bikeName = active.bikeName,
                        brand = active.brand,
                        category = active.category,
                        version = active.version,
                        sparkPlug = active.sparkPlug,
                        currentOdometerKm = active.currentOdometerKm,
                        transmissionType = active.transmissionType,
                        oilIntervalKm = active.oilIntervalKm
                    )

                    _uiState.update {
                        it.copy(
                            userBikes = bikes,
                            activeBike = bikeSummary,
                            currentOdo = bikeSummary.currentOdometerKm
                        )
                    }

                    loadChecklistForBike(bikeSummary)
                } else {
                    val profile = repository.getProfile()
                    val profileBikeName = profile?.bikeName?.trim().orEmpty()

                    if (profileBikeName.isNotBlank()) {
                        val fallbackBike = BikeSummary(
                            id = "",
                            bikeName = profileBikeName,
                            currentOdometerKm = profile?.currentOdometerKm ?: 0.0,
                            transmissionType = "CVT",
                            oilIntervalKm = 2000.0
                        )
                        _uiState.update {
                            it.copy(
                                userBikes = bikes,
                                activeBike = fallbackBike,
                                currentOdo = fallbackBike.currentOdometerKm
                            )
                        }
                        loadChecklistForBike(fallbackBike)
                    } else {
                        _uiState.update {
                            it.copy(
                                userBikes = emptyList(),
                                activeBike = null,
                                currentOdo = 0.0,
                                items = emptyList()
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectBike(bike: GarageBike) {
        viewModelScope.launch {
            try {
                // Sets active vehicle across all screens via repository flow
                repository.setActiveVehicle(bike)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadChecklistForBike(bikeSummary: BikeSummary) {
        viewModelScope.launch {
            try {
                val baseChecklist = buildChecklistForBike(bikeSummary)

                val customParts = repository.getCustomParts(bikeSummary.id)
                val customItems = customParts.map { part ->
                    MaintenanceItem(
                        id = part.id,
                        title = part.partName,
                        category = part.category,
                        lastReplacedKm = part.lastServicedOdoKm,
                        intervalKm = part.intervalKm,
                        notes = part.notes
                    )
                }

                val fullChecklist = baseChecklist + customItems

                val history = repository.getMaintenanceHistory(bikeSummary.id)

                val resolvedItems = fullChecklist.map { item ->
                    val latestRecord = history
                        .filter { it.partName.equals(item.title, ignoreCase = true) }
                        .maxByOrNull { it.servicedOdometerKm }

                    if (latestRecord != null) {
                        item.copy(lastReplacedKm = latestRecord.servicedOdometerKm)
                    } else {
                        item
                    }
                }

                _uiState.update {
                    it.copy(items = resolvedItems)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateOilInterval(newIntervalKm: Double) {
        val activeBikeId = _uiState.value.activeBike?.id ?: ""

        _uiState.update { state ->
            val updatedItems = state.items.map { item ->
                if (item.title.contains("Engine Oil", ignoreCase = true)) {
                    item.copy(intervalKm = newIntervalKm)
                } else {
                    item
                }
            }
            val updatedActiveBike = state.activeBike?.copy(oilIntervalKm = newIntervalKm)
            val updatedUserBikes = state.userBikes.map { bike ->
                if (bike.id == activeBikeId) bike.copy(oilIntervalKm = newIntervalKm) else bike
            }
            state.copy(
                items = updatedItems,
                activeBike = updatedActiveBike,
                userBikes = updatedUserBikes
            )
        }

        if (activeBikeId.isNotBlank()) {
            viewModelScope.launch {
                try {
                    repository.updateVehicleOilInterval(activeBikeId, newIntervalKm)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun markItemServiced(itemId: String, serviceKm: Double) {
        val targetItem = _uiState.value.items.firstOrNull { it.id == itemId } ?: return
        val activeBikeId = _uiState.value.activeBike?.id ?: ""

        _uiState.update { state ->
            val updated = state.items.map { item ->
                if (item.id == itemId) item.copy(lastReplacedKm = serviceKm) else item
            }
            state.copy(items = updated)
        }

        viewModelScope.launch {
            try {
                repository.recordMaintenanceService(
                    bikeId = activeBikeId,
                    itemTitle = targetItem.title,
                    category = targetItem.category,
                    servicedOdo = serviceKm
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addCustomItem(title: String, category: String, lastReplacedKm: Double, intervalKm: Double, notes: String) {
        val activeBikeId = _uiState.value.activeBike?.id ?: ""

        viewModelScope.launch {
            try {
                val savedPart = repository.addCustomPart(
                    bikeId = activeBikeId,
                    partName = title,
                    category = category,
                    lastReplacedKm = lastReplacedKm,
                    intervalKm = intervalKm,
                    notes = notes
                )

                val newItem = MaintenanceItem(
                    id = savedPart.id,
                    title = savedPart.partName,
                    category = savedPart.category,
                    lastReplacedKm = savedPart.lastServicedOdoKm,
                    intervalKm = savedPart.intervalKm,
                    notes = savedPart.notes
                )

                _uiState.update { it.copy(items = it.items + newItem) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}