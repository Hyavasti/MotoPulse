package com.example.motorapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.motorapp.data.GarageBike
import com.example.motorapp.data.MaintenanceRecord

private val LeafGreen = Color(0xFF16A34A)
private val CardBg = Color(0xFFF8FAFC)
private val TextDark = Color(0xFF0F172A)
private val TextSubtle = Color(0xFF64748B)
private val DangerRed = Color(0xFFDC2626)
private val BadgeBg = Color(0xFFDCFCE7)

@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    onLogoutClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showMaintenanceHistoryDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = LeafGreen)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Account",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }

        // Profile Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(LeafGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = LeafGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.username.ifBlank { "Rider" },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = uiState.email.ifBlank { "No email associated" },
                            fontSize = 13.sp,
                            color = TextSubtle
                        )
                        if (uiState.bikeName.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Bike: ${uiState.bikeName}",
                                fontSize = 12.sp,
                                color = LeafGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            editNameInput = uiState.username
                            showEditProfileDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = LeafGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Vehicle Records Section
        item {
            Text(
                text = "Vehicle Records",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSubtle
            )
        }

        // Clean Navigation Row to Maintenance History (Subtitle removed)
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.refreshMaintenanceHistory()
                        showMaintenanceHistoryDialog = true
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Maintenance History",
                        tint = LeafGreen,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = "Maintenance History",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDark,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = TextSubtle,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Preferences & Security
        item {
            Text(
                text = "Preferences & Security",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSubtle,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            AccountOptionRow(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                onClick = { /* Notification actions */ }
            )
        }

        item {
            AccountOptionRow(
                icon = Icons.Default.Lock,
                title = "Password & Security",
                onClick = { /* Security actions */ }
            )
        }

        item {
            AccountOptionRow(
                icon = Icons.Default.Info,
                title = "About MotoApp",
                onClick = { /* Info actions */ }
            )
        }

        // Logout Button
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DangerRed.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLogoutDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Log Out",
                        tint = DangerRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Log Out",
                        color = DangerRed,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Maintenance History Full View Modal
    if (showMaintenanceHistoryDialog) {
        Dialog(
            onDismissRequest = { showMaintenanceHistoryDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            MaintenanceHistoryScreen(
                records = uiState.maintenanceHistory,
                bikes = uiState.garageBikes,
                isRefreshing = uiState.isRefreshingMaintenance,
                onRefresh = { viewModel.refreshMaintenanceHistory() },
                onClose = { showMaintenanceHistoryDialog = false }
            )
        }
    }

    // Edit Name Dialog
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Display Name", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editNameInput,
                    onValueChange = { editNameInput = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateUsername(editNameInput.trim())
                        showEditProfileDialog = false
                    }
                ) {
                    Text("Save", color = LeafGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = TextSubtle)
                }
            }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    }
                ) {
                    Text("Log Out", color = DangerRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextSubtle)
                }
            }
        )
    }
}

@Composable
fun MaintenanceHistoryScreen(
    records: List<MaintenanceRecord>,
    bikes: List<GarageBike>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onClose: () -> Unit
) {
    var selectedBikeId by remember { mutableStateOf<String?>("All") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedPart by remember { mutableStateOf("All") }

    LaunchedEffect(Unit) {
        onRefresh()
    }

    val bikeMap = remember(bikes) {
        bikes.associate { it.id to it.bikeName.ifBlank { "Motorcycle" } }
    }

    // 1. Records filtered by chosen motorcycle first
    val recordsForBike = remember(records, selectedBikeId) {
        if (selectedBikeId == "All" || selectedBikeId == null) {
            records
        } else {
            records.filter { it.bikeId == selectedBikeId }
        }
    }

    // 2. Categories under this chosen motorcycle
    val categories = remember(recordsForBike) {
        listOf("All") + recordsForBike
            .map { it.category }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    LaunchedEffect(selectedBikeId) {
        selectedCategory = "All"
        selectedPart = "All"
    }

    // 3. Parts under the selected category & selected bike
    val availablePartsForCategory = remember(recordsForBike, selectedCategory) {
        if (selectedCategory == "All") {
            emptyList()
        } else {
            listOf("All") + recordsForBike
                .filter { it.category.equals(selectedCategory, ignoreCase = true) }
                .map { it.partName }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }
    }

    LaunchedEffect(selectedCategory) {
        selectedPart = "All"
    }

    // 4. Final filtered records sorted latest-first
    val filteredRecords = remember(recordsForBike, selectedCategory, selectedPart) {
        recordsForBike
            .filter { record ->
                val matchesCategory = selectedCategory == "All" || record.category.equals(selectedCategory, ignoreCase = true)
                val matchesPart = selectedPart == "All" || record.partName.equals(selectedPart, ignoreCase = true)
                matchesCategory && matchesPart
            }
            .sortedWith(
                compareByDescending<MaintenanceRecord> { it.createdAt ?: it.serviceDate.orEmpty() }
                    .thenByDescending { it.servicedOdometerKm }
            )
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDark
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Maintenance History",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = LeafGreen
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = LeafGreen
                            )
                        }
                    }
                }

                // Level 0: Motorcycle Selection Chips
                if (bikes.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = (selectedBikeId == "All"),
                            onClick = { selectedBikeId = "All" },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.TwoWheeler,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("All Bikes") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LeafGreen,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )

                        bikes.forEach { bike ->
                            val isSelected = selectedBikeId == bike.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedBikeId = bike.id },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.TwoWheeler,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(bike.bikeName.ifBlank { "Bike" }) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LeafGreen,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Level 1: Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = (selectedCategory == cat),
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LeafGreen.copy(alpha = 0.85f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Level 2: Specific Parts under selected category
                if (availablePartsForCategory.size > 2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availablePartsForCategory.forEach { part ->
                            FilterChip(
                                selected = (selectedPart == part),
                                onClick = { selectedPart = part },
                                label = { Text(part, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LeafGreen.copy(alpha = 0.15f),
                                    selectedLabelColor = LeafGreen
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
            }
        }
    ) { innerPadding ->
        if (filteredRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        tint = TextSubtle,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isRefreshing) "Loading maintenance logs..." else "No records found for this selection",
                        fontSize = 14.sp,
                        color = TextSubtle
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredRecords, key = { it.id ?: it.hashCode().toString() }) { record ->
                    val bikeLabel = record.bikeId?.let { bikeMap[it] }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CardBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(LeafGreen.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = LeafGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = record.partName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextDark,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (record.category.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = BadgeBg
                                        ) {
                                            Text(
                                                text = record.category,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = LeafGreen,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                if (selectedBikeId == "All" && !bikeLabel.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Bike: $bikeLabel",
                                        fontSize = 11.sp,
                                        color = LeafGreen,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = TextSubtle,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "%,.0f km".format(record.servicedOdometerKm),
                                        fontSize = 12.sp,
                                        color = TextSubtle
                                    )

                                    record.serviceDate?.let { dateStr ->
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = TextSubtle,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = dateStr,
                                            fontSize = 12.sp,
                                            color = TextSubtle
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountOptionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CardBg,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = TextDark,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = TextSubtle,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}