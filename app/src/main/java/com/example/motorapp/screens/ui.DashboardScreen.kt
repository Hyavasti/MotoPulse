package com.example.motorapp.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.motorapp.R
import com.example.motorapp.data.MaintenancePart
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val LeafGreen = Color(0xFF16A34A)
private val LightScreenBg = Color(0xFFF8FAFC)
private val CardBg = Color(0xFFFFFFFF)
private val TextDark = Color(0xFF0F172A)
private val TextSubtle = Color(0xFF64748B)
private val CardBorderColor = Color(0xFFE2E8F0)
private val WarningAmber = Color(0xFFD97706)
private val DangerRed = Color(0xFFDC2626)

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToGarage: () -> Unit = {},
    onOpenDrawer: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var showOdoDialog by remember { mutableStateOf(false) }

    val activeBike = state.activeBike
    val activeOdometer = activeBike?.currentOdometerKm ?: state.profile.currentOdometerKm
    val hasNoBike = activeBike == null || state.userBikes.isEmpty()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isUploadingPhoto by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && activeBike != null && activeBike.id.isNotBlank()) {
            coroutineScope.launch {
                isUploadingPhoto = true
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use {
                        it.readBytes()
                    }
                    if (bytes != null) {
                        viewModel.uploadActiveBikePhoto(activeBike.id, bytes)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isUploadingPhoto = false
                }
            }
        }
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
            // --- 1. TOP BAR (WITH DRAWER MENU BUTTON) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Menu Trigger Button
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
                                contentDescription = "Open Navigation Menu",
                                tint = TextDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Image(
                        painter = painterResource(id = R.drawable.motopulse),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val greetingName = state.displayName.ifBlank {
                                state.profile.username?.ifBlank { null } ?: "Rider"
                            }

                            Text(
                                text = "Hello $greetingName",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(text = "🏍️", fontSize = 14.sp)
                        }
                        Text(
                            text = if (!hasNoBike && activeBike != null) {
                                "${activeBike.bikeName} • Active"
                            } else {
                                "Ready to Ride"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LeafGreen
                        )
                    }
                }

                Surface(
                    color = CardBg,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = "Alerts",
                            tint = TextDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // --- 2. BODY CONTENT ---
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
                    // Showcase & Odometer Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = CardBg,
                            border = BorderStroke(1.dp, CardBorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                if (state.userBikes.size > 1) {
                                    val sortedBikes = remember(state.userBikes) {
                                        state.userBikes.sortedBy { it.bikeName }
                                    }

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(sortedBikes, key = { it.id }) { bike ->
                                            val isSelected = bike.id == activeBike.id
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { viewModel.selectBike(bike) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.TwoWheeler,
                                                        contentDescription = null,
                                                        tint = if (isSelected) Color.White else LeafGreen,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = bike.bikeName,
                                                        fontSize = 11.sp,
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
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Bike Photo Box
                                    Box(
                                        modifier = Modifier
                                            .size(94.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(LightScreenBg)
                                            .border(BorderStroke(1.dp, CardBorderColor), RoundedCornerShape(14.dp))
                                            .clickable(enabled = !isUploadingPhoto) {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val bikeImageUrl = activeBike.imageUrl
                                        if (!bikeImageUrl.isNullOrBlank()) {
                                            SubcomposeAsyncImage(
                                                model = bikeImageUrl,
                                                contentDescription = activeBike.bikeName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                                loading = {
                                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                        CircularProgressIndicator(
                                                            strokeWidth = 2.dp,
                                                            color = LeafGreen,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                },
                                                error = {
                                                    Icon(
                                                        imageVector = Icons.Default.TwoWheeler,
                                                        contentDescription = null,
                                                        tint = LeafGreen,
                                                        modifier = Modifier.size(44.dp)
                                                    )
                                                }
                                            )
                                        } else {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.TwoWheeler,
                                                    contentDescription = null,
                                                    tint = LeafGreen,
                                                    modifier = Modifier.size(40.dp)
                                                )
                                                Text(
                                                    text = "Add Photo",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextSubtle
                                                )
                                            }
                                        }

                                        if (!isUploadingPhoto) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(4.dp)
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(CardBg),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AddAPhoto,
                                                    contentDescription = "Upload Bike Photo",
                                                    tint = LeafGreen,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }

                                        if (isUploadingPhoto) {
                                            Surface(
                                                color = CardBg.copy(alpha = 0.75f),
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(
                                                        strokeWidth = 2.dp,
                                                        color = LeafGreen,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = activeBike.bikeName,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = LightScreenBg,
                                                border = BorderStroke(1.dp, CardBorderColor)
                                            ) {
                                                Text(
                                                    text = activeBike.plateNumber.ifBlank { "NO PLATE" },
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextDark,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = "%,d km".format(activeOdometer.roundToInt()),
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = TextDark
                                                )
                                                Text(
                                                    text = "Total Distance",
                                                    fontSize = 11.sp,
                                                    color = TextSubtle
                                                )
                                            }

                                            Surface(
                                                shape = CircleShape,
                                                color = LeafGreen.copy(alpha = 0.12f),
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clickable { showOdoDialog = true }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit Odometer",
                                                        tint = LeafGreen,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Component Lifecycle Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "COMPONENT LIFECYCLE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = TextSubtle
                            )
                            Text(
                                text = "${state.parts.size} Monitored",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = LeafGreen
                            )
                        }
                    }

                    // Maintenance Part Cards
                    items(state.parts, key = { it.id }) { part ->
                        MaintenanceStylePartCard(
                            part = part,
                            currentOdo = activeOdometer,
                            onServiced = { viewModel.resetPartService(part.id) }
                        )
                    }
                }
            }
        }
    }

    if (showOdoDialog && activeBike != null) {
        UpdateOdometerDialog(
            bikeName = activeBike.bikeName,
            currentOdo = activeOdometer,
            onDismiss = { showOdoDialog = false },
            onConfirm = { newOdo ->
                viewModel.updateOdometer(newOdo)
                showOdoDialog = false
            }
        )
    }
}

@Composable
fun MaintenanceStylePartCard(
    part: MaintenancePart,
    currentOdo: Double,
    onServiced: () -> Unit
) {
    val health = part.healthPercentage(currentOdo)
    val remaining = part.remainingKm(currentOdo)
    val isOverdue = remaining <= 0

    val (statusColor, statusIcon, statusLabel) = when {
        isOverdue -> Triple(DangerRed, Icons.Default.Warning, "OVERDUE")
        health <= 25f -> Triple(WarningAmber, Icons.Default.Build, "ATTENTION SOON")
        else -> Triple(LeafGreen, Icons.Default.CheckCircle, "GOOD CONDITION")
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
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = part.partName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = LightScreenBg,
                                border = BorderStroke(1.dp, CardBorderColor)
                            ) {
                                Text(
                                    text = part.category.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSubtle,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Text(
                            text = if (isOverdue) "Overdue by %,d km".format((-remaining).roundToInt())
                            else "%,d km remaining (${health.roundToInt()}%)".format(remaining.roundToInt()),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isOverdue) DangerRed else TextSubtle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalButton(
                    onClick = onServiced,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = LeafGreen.copy(alpha = 0.12f),
                        contentColor = LeafGreen
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Service", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            LinearProgressIndicator(
                progress = { (health / 100f).coerceIn(0f, 1f) },
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
                    text = statusLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = statusColor
                )

                Text(
                    text = "${health.roundToInt()}% Health",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
fun UpdateOdometerDialog(
    bikeName: String,
    currentOdo: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val initialText = if (currentOdo <= 0.0) "" else currentOdo.roundToInt().toString()
    var textValue by remember { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Update Odometer",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = "Setting mileage for $bikeName",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSubtle
                )
            }
        },
        text = {
            OutlinedTextField(
                value = textValue,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }
                    textValue = if (digits.length > 1 && digits.startsWith("0")) {
                        digits.trimStart('0')
                    } else {
                        digits
                    }
                },
                placeholder = { Text("e.g. 10000", color = TextSubtle.copy(alpha = 0.5f)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = LeafGreen
                    )
                },
                trailingIcon = {
                    Text(
                        text = "KM",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = LeafGreen,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
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
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = textValue.toDoubleOrNull() ?: 0.0
                    onConfirm(parsed)
                },
                colors = ButtonDefaults.buttonColors(containerColor = LeafGreen),
                shape = RoundedCornerShape(10.dp),
                enabled = textValue.isNotBlank()
            ) {
                Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextDark, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}