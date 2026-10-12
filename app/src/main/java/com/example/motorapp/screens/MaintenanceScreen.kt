package com.example.motorapp.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motorapp.data.MaintenanceItem
import com.example.motorapp.data.MaintenanceStatus
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private val LeafGreen = Color(0xFF16A34A)
private val LightScreenBg = Color(0xFFF8FAFC)
private val CardBg = Color(0xFFFFFFFF)
private val TextDark = Color(0xFF0F172A)
private val TextSubtle = Color(0xFF64748B)
private val CardBorderColor = Color(0xFFE2E8F0)

private val WarningAmber = Color(0xFFD97706)
private val DangerRed = Color(0xFFDC2626)

private val PresetOilIntervals = listOf(1500.0, 2000.0, 2500.0, 3000.0)

@Composable
fun MaintenanceScreen(
    viewModel: MaintenanceViewModel,
    onNavigateToGarage: () -> Unit = {},
    onOpenDrawer: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var selectedItemForService by remember { mutableStateOf<MaintenanceItem?>(null) }
    var showCustomIntervalDialog by remember { mutableStateOf(false) }
    var showAddCustomItemDialog by remember { mutableStateOf(false) }

    val activeBike = state.activeBike
    val currentOdo = activeBike?.currentOdometerKm ?: state.currentOdo
    val hasNoBike = activeBike == null || (state.userBikes.isEmpty() && activeBike.bikeName.isBlank())

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    val engineOilItem = state.items.firstOrNull { it.title.contains("Engine Oil", ignoreCase = true) }
    val currentOilInterval = engineOilItem?.intervalKm ?: 1500.0

    val overdueCount = state.items.count { it.getStatus(currentOdo) == MaintenanceStatus.OVERDUE }
    val dueSoonCount = state.items.count { it.getStatus(currentOdo) == MaintenanceStatus.DUE_SOON }

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
                            text = "PMS & Consumables",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = if (!hasNoBike && activeBike != null && activeBike.bikeName.isNotBlank()) {
                                "${activeBike.bikeName} • %,d km".format(currentOdo.roundToInt())
                            } else {
                                "Keep your bike in prime condition"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = LeafGreen
                        )
                    }
                }

                if (!hasNoBike) {
                    FloatingActionButton(
                        onClick = { showAddCustomItemDialog = true },
                        containerColor = LeafGreen,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Item", modifier = Modifier.size(22.dp))
                    }
                }
            }

            // Body Content
            if (state.isLoading && state.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LeafGreen)
                }
            } else if (hasNoBike) {
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
                    if (state.userBikes.isNotEmpty()) {
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
                    }

                    // Engine Oil Interval Selector Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = CardBg,
                            border = BorderStroke(1.dp, CardBorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Opacity,
                                            contentDescription = null,
                                            tint = LeafGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "OIL INTERVAL",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp,
                                            color = TextSubtle
                                        )
                                    }
                                    Text(
                                        text = "Every %,d km".format(currentOilInterval.roundToInt()),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LeafGreen
                                    )
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(PresetOilIntervals) { interval ->
                                        val isSelected = currentOilInterval == interval
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.updateOilInterval(interval) },
                                            label = {
                                                Text(
                                                    text = "%,d km".format(interval.roundToInt()),
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = LeafGreen,
                                                selectedLabelColor = Color.White,
                                                containerColor = LightScreenBg,
                                                labelColor = TextDark
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }

                                    item {
                                        val isCustom = currentOilInterval !in PresetOilIntervals
                                        FilterChip(
                                            selected = isCustom,
                                            onClick = { showCustomIntervalDialog = true },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = if (isCustom) Color.White else LeafGreen
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = if (isCustom) "%,d km (Custom)".format(currentOilInterval.roundToInt()) else "Custom",
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Medium
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = LeafGreen,
                                                selectedLabelColor = Color.White,
                                                containerColor = LightScreenBg,
                                                labelColor = TextDark
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Health Status Overview Cards
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MiniHealthCard(
                                label = "ACTION REQUIRED",
                                value = "$overdueCount Overdue",
                                accentColor = if (overdueCount > 0) DangerRed else LeafGreen,
                                modifier = Modifier.weight(1f)
                            )
                            MiniHealthCard(
                                label = "ATTENTION SOON",
                                value = "$dueSoonCount Due Soon",
                                accentColor = if (dueSoonCount > 0) WarningAmber else LeafGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Section Header
                    item {
                        Text(
                            text = "MAINTENANCE SCHEDULE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextSubtle,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Group items by category
                    val groupedItems = state.items.groupBy { it.category }

                    groupedItems.forEach { (category, items) ->
                        item {
                            Text(
                                text = category.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LeafGreen,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        items(items, key = { it.id }) { item ->
                            MaintenanceRowCard(
                                item = item,
                                currentOdo = currentOdo,
                                onServiceClick = { selectedItemForService = item }
                            )
                        }
                    }
                }
            }
        }

        // Service Confirmation Dialog
        selectedItemForService?.let { item ->
            AlertDialog(
                onDismissRequest = { selectedItemForService = null },
                shape = RoundedCornerShape(20.dp),
                containerColor = Color.White,
                title = {
                    Text(
                        text = "Mark as Replaced?",
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        text = "This updates '${item.title}' last serviced odometer reading to %,d km and logs the entry to history.".format(currentOdo.roundToInt()),
                        color = TextSubtle,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.markItemServiced(item.id, currentOdo)
                            selectedItemForService = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LeafGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Confirm Replaced", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedItemForService = null }) {
                        Text("Cancel", color = TextDark)
                    }
                }
            )
        }

        // Custom Oil Interval Dialog
        if (showCustomIntervalDialog) {
            var customIntervalText by remember { mutableStateOf(currentOilInterval.roundToInt().toString()) }

            AlertDialog(
                onDismissRequest = { showCustomIntervalDialog = false },
                shape = RoundedCornerShape(20.dp),
                containerColor = Color.White,
                title = {
                    Text("Custom Oil Interval", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 18.sp)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Set replacement interval in kilometers:",
                            fontSize = 13.sp,
                            color = TextSubtle
                        )
                        OutlinedTextField(
                            value = customIntervalText,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.all { it.isDigit() }) customIntervalText = input
                            },
                            label = { Text("Interval (km)", fontWeight = FontWeight.SemiBold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextDark,
                                unfocusedTextColor = TextDark,
                                focusedBorderColor = LeafGreen,
                                focusedLabelColor = LeafGreen,
                                cursorColor = LeafGreen
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val interval = customIntervalText.toDoubleOrNull() ?: 1500.0
                            if (interval > 0) {
                                viewModel.updateOilInterval(interval)
                            }
                            showCustomIntervalDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LeafGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomIntervalDialog = false }) {
                        Text("Cancel", color = TextDark)
                    }
                }
            )
        }

        // Add Custom Item Dialog
        if (showAddCustomItemDialog) {
            var itemTitle by remember { mutableStateOf("") }
            var itemCategory by remember { mutableStateOf("Custom") }
            var itemInterval by remember { mutableStateOf("5000") }
            var itemNotes by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddCustomItemDialog = false },
                shape = RoundedCornerShape(20.dp),
                containerColor = Color.White,
                title = {
                    Text("Add Maintenance Item", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 18.sp)
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = itemTitle,
                            onValueChange = { itemTitle = it },
                            label = { Text("Item Name (e.g., Brake Cable)") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = itemCategory,
                            onValueChange = { itemCategory = it },
                            label = { Text("Category (e.g., Fluids, Chassis)") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = itemInterval,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.all { it.isDigit() }) itemInterval = input
                            },
                            label = { Text("Service Interval (km)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = itemNotes,
                            onValueChange = { itemNotes = it },
                            label = { Text("Notes (optional)") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (itemTitle.isNotBlank()) {
                                viewModel.addCustomItem(
                                    title = itemTitle.trim(),
                                    category = itemCategory.trim().ifBlank { "Custom" },
                                    lastReplacedKm = currentOdo,
                                    intervalKm = itemInterval.toDoubleOrNull() ?: 5000.0,
                                    notes = itemNotes.trim()
                                )
                                showAddCustomItemDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LeafGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add Item", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCustomItemDialog = false }) {
                        Text("Cancel", color = TextDark)
                    }
                }
            )
        }
    }
}

@Composable
private fun MiniHealthCard(
    label: String,
    value: String,
    accentColor: Color,
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
                color = TextSubtle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MaintenanceRowCard(
    item: MaintenanceItem,
    currentOdo: Double,
    onServiceClick: () -> Unit
) {
    val status = item.getStatus(currentOdo)
    val remaining = item.getKmRemaining(currentOdo)
    val progress = item.getProgress(currentOdo)

    val (statusLabel, statusColor) = when (status) {
        MaintenanceStatus.OVERDUE -> "Overdue %,d km".format(remaining.absoluteValue.roundToInt()) to DangerRed
        MaintenanceStatus.DUE_SOON -> "Due in %,d km".format(remaining.roundToInt()) to WarningAmber
        MaintenanceStatus.GOOD -> "%,d km left".format(remaining.roundToInt()) to LeafGreen
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (status) {
                                MaintenanceStatus.OVERDUE -> Icons.Default.Warning
                                MaintenanceStatus.DUE_SOON -> Icons.Default.Build
                                MaintenanceStatus.GOOD -> Icons.Default.CheckCircle
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = item.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Every %,d km • Last: %,d km".format(
                                item.intervalKm.roundToInt(),
                                item.lastReplacedKm.roundToInt()
                            ),
                            fontSize = 11.sp,
                            color = TextSubtle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = onServiceClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = LeafGreen.copy(alpha = 0.12f),
                        contentColor = LeafGreen
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Replaced", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = statusColor,
                trackColor = CardBorderColor
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (status) {
                        MaintenanceStatus.OVERDUE -> "OVERDUE"
                        MaintenanceStatus.DUE_SOON -> "ATTENTION SOON"
                        MaintenanceStatus.GOOD -> "GOOD CONDITION"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = statusColor
                )

                Text(
                    text = statusLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            if (item.notes.isNotBlank()) {
                Surface(
                    color = LightScreenBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.notes,
                        fontSize = 11.sp,
                        color = TextSubtle,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}