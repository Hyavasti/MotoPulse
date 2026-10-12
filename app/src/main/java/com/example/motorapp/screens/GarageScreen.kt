package com.example.motorapp.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.motorapp.data.GarageBike
import com.example.motorapp.data.MotorcycleCatalogItem

private val LeafGreen = Color(0xFF16A34A)
private val LightScreenBg = Color(0xFFF8FAFC)
private val CardBg = Color(0xFFFFFFFF)
private val TextDark = Color(0xFF0F172A)
private val TextSubtle = Color(0xFF64748B)
private val CardBorderColor = Color(0xFFE2E8F0)
private val DestructiveRed = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageScreen(
    viewModel: GarageViewModel,
    onOpenDrawer: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var showCatalogSheet by remember { mutableStateOf(false) }
    var bikeToDelete by remember { mutableStateOf<GarageBike?>(null) }
    var fullScreenImageUrl by remember { mutableStateOf<String?>(null) }

    val hasNoBike = state.activeBike == null || state.userBikes.isEmpty()

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
            // Header with Drawer Toggle & Add Bike FAB
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
                            text = "Digital Garage",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "Specs & factory baselines",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSubtle
                        )
                    }
                }

                if (state.userBikes.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = { showCatalogSheet = true },
                        containerColor = LeafGreen,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Motorcycle",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Body
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
                            onClick = { showCatalogSheet = true },
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
                    // Bike Switcher Chips
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(state.userBikes) { bike ->
                                val isSelected = bike.id == state.activeBike?.id ||
                                        (bike.plateNumber == state.activeBike?.plateNumber && bike.bikeName == state.activeBike?.bikeName)

                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectBike(bike) },
                                    label = {
                                        Text(
                                            text = bike.bikeName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.TwoWheeler,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LeafGreen,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White,
                                        containerColor = CardBg,
                                        labelColor = TextDark
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    // Active Bike Card
                    item {
                        state.activeBike?.let { bike ->
                            ActiveBikeDisplayCard(
                                bike = bike,
                                onImageClick = {
                                    if (!bike.imageUrl.isNullOrBlank()) {
                                        fullScreenImageUrl = bike.imageUrl
                                    }
                                },
                                onDeleteClick = { bikeToDelete = bike }
                            )
                        }
                    }

                    // Quick Specs Grid
                    state.activeBike?.let { bike ->
                        item {
                            Text(
                                text = "QUICK SPEC SHEET",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = TextSubtle,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    SpecTile(
                                        icon = Icons.Default.Speed,
                                        title = "FRONT TIRE PSI",
                                        value = "${bike.frontPsi} PSI (Cold)",
                                        modifier = Modifier.weight(1f)
                                    )
                                    SpecTile(
                                        icon = Icons.Default.Speed,
                                        title = "REAR TIRE PSI",
                                        value = "${bike.rearPsi} PSI (Cold)",
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    SpecTile(
                                        icon = Icons.Default.Opacity,
                                        title = "ENGINE OIL",
                                        value = "${bike.oilCapacityMl} mL",
                                        modifier = Modifier.weight(1f)
                                    )
                                    SpecTile(
                                        icon = Icons.Default.ElectricBolt,
                                        title = "SPARK PLUG",
                                        value = bike.sparkPlug,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    SpecTile(
                                        icon = Icons.Default.Build,
                                        title = "DRIVETRAIN",
                                        value = bike.transmissionType,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Fullscreen Image Dialog
        if (fullScreenImageUrl != null) {
            FullScreenImageDialog(
                imageUrl = fullScreenImageUrl!!,
                onDismiss = { fullScreenImageUrl = null }
            )
        }

        // Delete Confirmation Dialog
        if (bikeToDelete != null) {
            AlertDialog(
                onDismissRequest = { bikeToDelete = null },
                icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = DestructiveRed) },
                title = { Text("Delete Motorcycle", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to remove \"${bikeToDelete?.bikeName}\" from your garage?") },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed),
                        onClick = {
                            bikeToDelete?.let { viewModel.deleteBike(it) }
                            bikeToDelete = null
                        }
                    ) {
                        Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { bikeToDelete = null }) {
                        Text("Cancel", color = TextDark)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Bottom Sheet
        if (showCatalogSheet) {
            SelectMotorcycleSheet(
                catalog = state.catalog,
                onDismiss = { showCatalogSheet = false },
                onSelectModel = { selected, nickname, plate, color ->
                    viewModel.addBikeFromCatalog(selected, nickname, plate, color)
                    showCatalogSheet = false
                }
            )
        }
    }
}

@Composable
private fun ActiveBikeDisplayCard(
    bike: GarageBike,
    onImageClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .border(1.dp, CardBorderColor, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Whole Picture Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF1F5F9))
                    .border(BorderStroke(1.dp, CardBorderColor), RoundedCornerShape(14.dp))
                    .clickable(enabled = !bike.imageUrl.isNullOrBlank()) { onImageClick() },
                contentAlignment = Alignment.Center
            ) {
                val bikeImageUrl = bike.imageUrl
                if (!bikeImageUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = bikeImageUrl,
                        contentDescription = bike.bikeName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        loading = {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = LeafGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        error = {
                            Icon(
                                imageVector = Icons.Default.TwoWheeler,
                                contentDescription = bike.bikeName,
                                tint = LeafGreen,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.TwoWheeler,
                        contentDescription = bike.bikeName,
                        tint = LeafGreen,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = bike.bikeName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    val subtitleParts = buildList {
                        if (bike.brand.isNotBlank()) add(bike.brand)
                        if (bike.version.isNotBlank() && !bike.version.equals("Standard", ignoreCase = true)) add(bike.version)
                        if (bike.plateNumber.isNotBlank()) add(bike.plateNumber)
                    }

                    Text(
                        text = if (subtitleParts.isNotEmpty()) subtitleParts.joinToString(" • ") else "Factory Spec",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSubtle
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = LeafGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = LeafGreen,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Motorcycle",
                            tint = DestructiveRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenImageDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = "Full Size Motorcycle Photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 20.dp)
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Fullscreen",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun SpecTile(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = LeafGreen, modifier = Modifier.size(20.dp))
            Column {
                Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSubtle)
                Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectMotorcycleSheet(
    catalog: List<MotorcycleCatalogItem>,
    onDismiss: () -> Unit,
    onSelectModel: (MotorcycleCatalogItem, String, String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { targetValue ->
            targetValue != SheetValue.Hidden
        }
    )
    val scrollState = rememberScrollState()

    var step by remember { mutableIntStateOf(1) }
    var searchQuery by remember { mutableStateOf("") }
    var isCustomMode by remember { mutableStateOf(false) }

    var selectedBrand by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedVariant by remember { mutableStateOf<MotorcycleCatalogItem?>(null) }
    var selectedColor by remember { mutableStateOf("Standard") }
    var customNickname by remember { mutableStateOf("") }
    var plateNumber by remember { mutableStateOf("") }

    // Custom Mode inputs
    var customBrand by remember { mutableStateOf("") }
    var customModel by remember { mutableStateOf("") }
    var customCategory by remember { mutableStateOf("Motorcycle") }
    var customVersion by remember { mutableStateOf("Standard") }
    var customColor by remember { mutableStateOf("Standard") }
    var customOilMl by remember { mutableStateOf("800") }
    var customFrontPsi by remember { mutableStateOf("29") }
    var customRearPsi by remember { mutableStateOf("33") }
    var customSparkPlug by remember { mutableStateOf("CPR9EA-9") }

    val availableBrands = remember(catalog, searchQuery) {
        val brands = catalog.map { it.brand }.filter { it.isNotBlank() }.distinct().sorted()
        if (searchQuery.isBlank() || step != 1) brands
        else brands.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    val availableCategories = remember(catalog, selectedBrand) {
        catalog
            .filter { it.brand.equals(selectedBrand, ignoreCase = true) }
            .map { it.category }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val availableModelsAndVersions = remember(catalog, selectedBrand, selectedCategory, searchQuery) {
        val filtered = catalog.filter {
            it.brand.equals(selectedBrand, ignoreCase = true) &&
                    it.category.equals(selectedCategory, ignoreCase = true)
        }
        if (searchQuery.isBlank() || step != 3) filtered
        else filtered.filter {
            it.modelName.contains(searchQuery, ignoreCase = true) ||
                    it.version.contains(searchQuery, ignoreCase = true)
        }
    }

    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextDark,
        unfocusedTextColor = TextDark,
        focusedBorderColor = LeafGreen,
        unfocusedBorderColor = Color(0xFF475569),
        focusedLabelColor = LeafGreen,
        unfocusedLabelColor = Color(0xFF1E293B),
        cursorColor = LeafGreen,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White
    )

    ModalBottomSheet(
        onDismissRequest = {
            if (step == 1 && !isCustomMode) {
                onDismiss()
            }
        },
        sheetState = sheetState,
        dragHandle = null,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (step > 1 || isCustomMode) {
                        IconButton(
                            onClick = {
                                if (isCustomMode) {
                                    isCustomMode = false
                                } else when (step) {
                                    4 -> step = 3
                                    3 -> {
                                        searchQuery = ""
                                        step = 2
                                    }
                                    2 -> {
                                        selectedCategory = null
                                        step = 1
                                    }
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = TextDark
                            )
                        }
                    }

                    Column {
                        Text(
                            text = when {
                                isCustomMode -> "Custom Motorcycle"
                                step == 1 -> "Select Brand"
                                step == 2 -> "${selectedBrand.orEmpty()} Categories"
                                step == 3 -> "Select Model"
                                else -> "Confirm Specs"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = when {
                                isCustomMode -> "Enter custom specifications"
                                step == 1 -> "Step 1 of 4 • Choose motorcycle make"
                                step == 2 -> "Step 2 of 4 • Choose motorcycle type"
                                step == 3 -> "Step 3 of 4 • Choose model line & version"
                                else -> "Step 4 of 4 • Review specs & details"
                            },
                            fontSize = 12.sp,
                            color = TextSubtle
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSubtle
                    )
                }
            }

            // STEP 1: SELECT BRAND
            if (step == 1 && !isCustomMode) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search brand (e.g. Honda, Yamaha, Suzuki)...", color = TextSubtle) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextDark) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = inputColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    availableBrands.forEach { brand ->
                        val bikeCount = catalog.count { it.brand.equals(brand, ignoreCase = true) }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedBrand = brand
                                    searchQuery = ""
                                    step = 2
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = LightScreenBg,
                            border = BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(LeafGreen.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TwoWheeler,
                                            contentDescription = null,
                                            tint = LeafGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = brand,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = TextDark
                                        )
                                        Text(
                                            text = "$bikeCount models available",
                                            fontSize = 12.sp,
                                            color = TextSubtle
                                        )
                                    }
                                }
                                Text("Select →", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LeafGreen)
                            }
                        }
                    }
                }

                TextButton(
                    onClick = { isCustomMode = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Bike not in list? Setup custom specs →",
                        color = LeafGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            // STEP 2: SELECT CATEGORY
            if (step == 2 && !isCustomMode) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    availableCategories.forEach { category ->
                        val count = catalog.count {
                            it.brand.equals(selectedBrand, ignoreCase = true) &&
                                    it.category.equals(category, ignoreCase = true)
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCategory = category
                                    searchQuery = ""
                                    step = 3
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = LightScreenBg,
                            border = BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = category,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "$count variant${if (count > 1) "s" else ""}",
                                        fontSize = 12.sp,
                                        color = TextSubtle
                                    )
                                }
                                Text("Choose →", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LeafGreen)
                            }
                        }
                    }
                }
            }

            // STEP 3: MODEL & SUB-LINE VERSION DISPLAY
            if (step == 3 && !isCustomMode) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter model / version...", color = TextSubtle) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextDark) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = inputColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    availableModelsAndVersions.forEach { variant ->
                        val hasCustomVersion = variant.version.isNotBlank() &&
                                !variant.version.equals("Standard", ignoreCase = true)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedVariant = variant
                                    selectedColor = variant.availableColors.firstOrNull() ?: "Standard"
                                    customNickname = if (hasCustomVersion) {
                                        "${variant.modelName} ${variant.version}".trim()
                                    } else {
                                        variant.modelName
                                    }
                                    step = 4
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = LightScreenBg,
                            border = BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 12.dp)
                                ) {
                                    Text(
                                        text = variant.modelName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TextDark
                                    )

                                    if (hasCustomVersion) {
                                        Text(
                                            text = variant.version,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = TextDark.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Text(
                                        text = "${variant.oilCapacityMl} mL Oil • ${variant.transmissionType}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextSubtle
                                    )
                                }

                                Text(
                                    text = "Select →",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LeafGreen
                                )
                            }
                        }
                    }
                }
            }

            // STEP 4: REVIEW SPECS & SAVE
            if (step == 4 && selectedVariant != null && !isCustomMode) {
                val variant = selectedVariant!!

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = LightScreenBg,
                    border = BorderStroke(1.5.dp, LeafGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardBg)
                                .border(BorderStroke(1.dp, CardBorderColor), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TwoWheeler,
                                contentDescription = null,
                                tint = LeafGreen,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            ) {
                                Text(
                                    text = if (variant.version.isNotBlank() && !variant.version.equals("Standard", ignoreCase = true)) {
                                        "${variant.modelName} (${variant.version})"
                                    } else {
                                        variant.modelName
                                    },
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = "${variant.brand} • ${variant.category} • ${variant.transmissionType}",
                                    fontSize = 12.sp,
                                    color = TextSubtle
                                )
                            }
                            Surface(
                                color = LeafGreen,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "SELECTED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = CardBorderColor, thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("TIRE PRESSURE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSubtle)
                                Text("${variant.frontPsi}F / ${variant.rearPsi}R PSI", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            }
                            Column {
                                Text("OIL CAPACITY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSubtle)
                                Text("${variant.oilCapacityMl} mL", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            }
                            Column {
                                Text("SPARK PLUG", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSubtle)
                                Text(variant.sparkPlug, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            }
                        }
                    }
                }

                if (variant.availableColors.isNotEmpty()) {
                    Text(
                        text = "FACTORY COLORWAY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSubtle,
                        letterSpacing = 1.sp
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(variant.availableColors) { colorName ->
                            FilterChip(
                                selected = selectedColor == colorName,
                                onClick = { selectedColor = colorName },
                                label = { Text(colorName, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LeafGreen,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = customNickname,
                    onValueChange = { customNickname = it },
                    label = { Text("Bike Nickname (Optional)", fontWeight = FontWeight.SemiBold) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = inputColors,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = plateNumber,
                    onValueChange = { plateNumber = it.uppercase() },
                    label = { Text("Plate Number (Optional)", fontWeight = FontWeight.SemiBold) },
                    placeholder = { Text("e.g., 123ABC", color = TextSubtle) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = inputColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onSelectModel(variant, customNickname, plateNumber, selectedColor) },
                    colors = ButtonDefaults.buttonColors(containerColor = LeafGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Save to Garage", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                }
            }

            // CUSTOM MOTORCYCLE FALLBACK
            if (isCustomMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customBrand,
                        onValueChange = { customBrand = it },
                        label = { Text("Brand (e.g., KTM)") },
                        colors = inputColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customModel,
                        onValueChange = { customModel = it },
                        label = { Text("Model Name") },
                        colors = inputColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customCategory,
                        onValueChange = { customCategory = it },
                        label = { Text("Category") },
                        colors = inputColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customVersion,
                        onValueChange = { customVersion = it },
                        label = { Text("Version / Year") },
                        colors = inputColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customOilMl,
                        onValueChange = { customOilMl = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Oil (mL)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = inputColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customSparkPlug,
                        onValueChange = { customSparkPlug = it },
                        label = { Text("Spark Plug") },
                        colors = inputColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customFrontPsi,
                        onValueChange = { customFrontPsi = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Front PSI") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = inputColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customRearPsi,
                        onValueChange = { customRearPsi = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Rear PSI") },
                        colors = inputColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = customColor,
                    onValueChange = { customColor = it },
                    label = { Text("Color") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = inputColors,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = plateNumber,
                    onValueChange = { plateNumber = it.uppercase() },
                    label = { Text("Plate Number (Optional)") },
                    placeholder = { Text("e.g., 123ABC", color = TextSubtle) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = inputColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val customItem = MotorcycleCatalogItem(
                            id = "custom_${System.currentTimeMillis()}",
                            brand = customBrand.ifBlank { "Custom" },
                            category = customCategory.ifBlank { "Motorcycle" },
                            modelName = customModel.ifBlank { "Custom Motorcycle" },
                            version = customVersion,
                            oilCapacityMl = customOilMl.toIntOrNull() ?: 800,
                            frontPsi = customFrontPsi.toIntOrNull() ?: 29,
                            rearPsi = customRearPsi.toIntOrNull() ?: 33,
                            sparkPlug = customSparkPlug,
                            transmissionType = "Manual",
                            availableColors = listOf(customColor)
                        )
                        onSelectModel(customItem, customModel, plateNumber, customColor)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LeafGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Save Custom Ride", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                }
            }
        }
    }
}