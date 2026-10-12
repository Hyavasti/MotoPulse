package com.example.motorapp.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motorapp.data.FuelLog
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

private val LeafGreen = Color(0xFF16A34A)
private val LightScreenBg = Color(0xFFF8FAFC)
private val CardBg = Color(0xFFFFFFFF)
private val TextDark = Color(0xFF0F172A)
private val TextSubtle = Color(0xFF64748B)
private val CardBorderColor = Color(0xFFE2E8F0)
private val FieldBorderDark = Color(0xFFCBD5E1)
private val LabelColorDark = Color(0xFF475569)

// Common fuel brands in the Philippines
private val PhilippineGasStations = listOf(
    "Petron",
    "Shell",
    "Caltex",
    "Seaoil",
    "Phoenix",
    "Cleanfuel",
    "Unioil",
    "Flying V",
    "Petro Gazz",
    "PTT",
    "Total",
    "Jetti",
    "RePhil",
    "Seaquest",
    "Uno Fuel",
    "Other / Independent"
)

@Composable
fun FuelLogScreen(
    viewModel: FuelViewModel,
    onNavigateToGarage: () -> Unit = {},
    onOpenDrawer: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val activeBike = state.activeBike
    val activeOdometer = activeBike?.currentOdometerKm ?: state.currentOdo
    val hasNoBike = activeBike == null || (state.userBikes.isEmpty() && activeBike.bikeName.isBlank())

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightScreenBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Header with Drawer Toggle & Add Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Drawer Hamburger Button
                    Surface(
                        color = CardBg,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, CardBorderColor),
                        modifier = Modifier
                            .size(42.dp)
                            .clickable { onOpenDrawer() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Drawer",
                                tint = TextDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Fuel & Efficiency",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = if (!hasNoBike && activeBike != null) {
                                "${activeBike.bikeName} • %,d km".format(activeOdometer.roundToInt())
                            } else {
                                "Track consumption & gas expenses"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = LeafGreen
                        )
                    }
                }

                if (!hasNoBike) {
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = LeafGreen,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Log",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Body: Full-height Empty State vs Refuel Content
            if (hasNoBike) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(LeafGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TwoWheeler,
                                contentDescription = null,
                                tint = LeafGreen,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "No Motorcycle Selected",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Pick your motorcycle model and version to auto-load its specs",
                            fontSize = 13.sp,
                            color = TextSubtle,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onNavigateToGarage,
                            colors = ButtonDefaults.buttonColors(containerColor = LeafGreen),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Pick Motorcycle",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Bike Selection Chips
                    item {
                        val sortedBikes = remember(state.userBikes) {
                            state.userBikes.sortedBy { it.bikeName }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "SELECT MOTORCYCLE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = TextSubtle
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(sortedBikes, key = { it.id }) { bike ->
                                    val isSelected = bike.id == activeBike?.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.selectBike(bike) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.TwoWheeler,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else LeafGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = "${bike.bikeName} (%,d km)".format(bike.currentOdometerKm.roundToInt()),
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LeafGreen,
                                            selectedLabelColor = Color.White,
                                            containerColor = CardBg,
                                            labelColor = TextDark
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Summary Stats Card
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MiniStatChip(
                                label = "AVG EFFICIENCY",
                                value = if (state.averageEfficiencyKmL > 0) "%.1f km/L".format(state.averageEfficiencyKmL) else "-- km/L",
                                accent = LeafGreen,
                                modifier = Modifier.weight(1f)
                            )
                            MiniStatChip(
                                label = "TOTAL SPENT",
                                value = "₱ %,.0f".format(state.totalSpent),
                                accent = TextDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Section Header with Refresh Button
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "REFUEL LOGS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = TextSubtle
                            )

                            IconButton(
                                onClick = {
                                    viewModel.loadData(selectedBikeId = activeBike?.id)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh logs",
                                    tint = LeafGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Logs List
                    if (state.logs.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No fuel entries yet. Tap + to record gas.",
                                    color = TextSubtle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        items(state.logs) { log ->
                            FuelEntryCard(log)
                        }
                    }
                }
            }
        }

        if (showAddDialog && activeBike != null) {
            AddFuelDialog(
                bikeName = activeBike.bikeName,
                currentOdo = activeOdometer,
                onDismiss = { showAddDialog = false },
                onConfirm = { liters, cost, station, date ->
                    viewModel.logRefuel(
                        liters = liters,
                        totalCost = cost,
                        gasStation = station,
                        fuelDate = date
                    )
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun MiniStatChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = TextSubtle
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}

@Composable
private fun FuelEntryCard(log: FuelLog) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(LeafGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalGasStation,
                        contentDescription = null,
                        tint = LeafGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "%,d km".format(log.odometerKm.roundToInt()),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextDark
                    )
                    Text(
                        text = "${log.liters} L • ₱ %,.2f".format(log.totalCost),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSubtle
                    )
                    val details = listOfNotNull(
                        log.gasStation?.takeUnless { it.isBlank() },
                        log.fuelDate?.takeUnless { it.isBlank() }
                    ).joinToString(" • ")

                    if (details.isNotBlank()) {
                        Text(
                            text = details,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = LeafGreen
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = log.fuelEfficiencyKmL?.let { "%.1f km/L".format(it) } ?: "--",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = LeafGreen
                )
                Text(
                    text = "Efficiency",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSubtle
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFuelDialog(
    bikeName: String,
    currentOdo: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double, String, String) -> Unit
) {
    var litersText by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    var gasStationText by remember { mutableStateOf("") }
    var isStationExpanded by remember { mutableStateOf(false) }

    val filteredStations = remember(gasStationText) {
        val query = gasStationText.trim()
        if (query.isEmpty() || PhilippineGasStations.any { it.equals(query, ignoreCase = true) }) {
            PhilippineGasStations
        } else {
            PhilippineGasStations.filter {
                it.contains(query, ignoreCase = true)
            }
        }
    }

    val displayFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var selectedDateText by remember { mutableStateOf(displayFormatter.format(Date())) }
    var showDatePicker by remember { mutableStateOf(false) }

    val (minEpochUtc, maxEpochUtc, currentYear) = remember {
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val maxUtc = utcCalendar.timeInMillis
        val year = utcCalendar.get(Calendar.YEAR)

        utcCalendar.add(Calendar.DAY_OF_YEAR, -7)
        utcCalendar.set(Calendar.HOUR_OF_DAY, 0)
        utcCalendar.set(Calendar.MINUTE, 0)
        utcCalendar.set(Calendar.SECOND, 0)
        utcCalendar.set(Calendar.MILLISECOND, 0)
        val minUtc = utcCalendar.timeInMillis

        Triple(minUtc, maxUtc, year)
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis in minEpochUtc..maxEpochUtc
            }

            override fun isSelectableYear(year: Int): Boolean {
                return year == currentYear
            }
        }
    )

    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextDark,
        unfocusedTextColor = TextDark,
        focusedBorderColor = LeafGreen,
        unfocusedBorderColor = FieldBorderDark,
        focusedLabelColor = LeafGreen,
        unfocusedLabelColor = LabelColorDark,
        cursorColor = LeafGreen,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Add Refuel for $bikeName",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextDark
                )
                Text(
                    text = "Current Odometer: %,d km".format(currentOdo.roundToInt()),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LeafGreen
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(
                    expanded = isStationExpanded,
                    onExpandedChange = { isStationExpanded = it }
                ) {
                    OutlinedTextField(
                        value = gasStationText,
                        onValueChange = {
                            gasStationText = it
                            isStationExpanded = true
                        },
                        label = { Text("Gas Station", fontWeight = FontWeight.SemiBold) },
                        placeholder = { Text("Select or search station") },
                        leadingIcon = {
                            Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = LeafGreen)
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (gasStationText.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            gasStationText = ""
                                            isStationExpanded = true
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear input",
                                            tint = TextSubtle,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStationExpanded)
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = inputColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true)
                    )

                    if (filteredStations.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = isStationExpanded,
                            onDismissRequest = { isStationExpanded = false },
                            modifier = Modifier
                                .background(Color.White)
                                .heightIn(max = 220.dp)
                        ) {
                            filteredStations.forEach { station ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = station,
                                            fontSize = 14.sp,
                                            color = TextDark,
                                            fontWeight = if (station.equals(gasStationText.trim(), ignoreCase = true)) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Normal
                                            }
                                        )
                                    },
                                    onClick = {
                                        gasStationText = station
                                        isStationExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = selectedDateText,
                        onValueChange = {},
                        enabled = false,
                        label = { Text("Date Loaded (Within 7 days)", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = LeafGreen)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = TextDark,
                            disabledBorderColor = FieldBorderDark,
                            disabledLabelColor = LabelColorDark,
                            disabledLeadingIconColor = LeafGreen,
                            disabledContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = litersText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                            litersText = if (input.startsWith("0") && input.length > 1 && input[1] != '.') {
                                input.trimStart('0')
                            } else input
                        }
                    },
                    label = { Text("Liters Loaded (L)", fontWeight = FontWeight.SemiBold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = inputColors,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = costText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                            costText = if (input.startsWith("0") && input.length > 1 && input[1] != '.') {
                                input.trimStart('0')
                            } else input
                        }
                    },
                    label = { Text("Total Cost (₱)", fontWeight = FontWeight.SemiBold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = inputColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lit = litersText.toDoubleOrNull() ?: 0.0
                    val cost = costText.toDoubleOrNull() ?: 0.0
                    if (lit > 0.0 && cost > 0.0) {
                        onConfirm(lit, cost, gasStationText.trim(), selectedDateText)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LeafGreen),
                shape = RoundedCornerShape(10.dp),
                enabled = litersText.isNotBlank() && costText.isNotBlank()
            ) {
                Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextDark, fontWeight = FontWeight.Bold)
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val utcFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            selectedDateText = utcFormatter.format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = LeafGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextSubtle)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = LeafGreen,
                    todayDateBorderColor = LeafGreen
                )
            )
        }
    }
}