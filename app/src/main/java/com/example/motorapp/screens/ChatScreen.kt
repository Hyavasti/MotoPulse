package com.example.motorapp.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.motorapp.data.ChatMessage
import com.example.motorapp.data.ChatRepository
import com.example.motorapp.data.ConvoyLocationService
import com.example.motorapp.data.ConvoyRide
import com.example.motorapp.data.UserProfile
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private val LeafGreen = Color(0xFF16A34A)
private val LightScreenBg = Color(0xFFF8FAFC)
private val CardBg = Color(0xFFFFFFFF)
private val TextDark = Color(0xFF0F172A)
private val CardBorderColor = Color(0xFFBBF7D0)
private val IncomingBubbleBg = Color(0xFFF0FDF4)
private val FieldBorderDark = Color(0xFF86EFAC)
private val WarningBannerBg = Color(0xFFFEF3C7)
private val WarningBannerBorder = Color(0xFFFDE68A)
private val WarningText = Color(0xFF92400E)
private val GreenTintBg = Color(0xFFF0FDF4)
private val BorderGreen = Color(0xFFBBF7D0)
private val DangerRed = Color(0xFFDC2626)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    roomTitle: String,
    viewModel: ChatViewModel,
    repository: ChatRepository = remember { ChatRepository() },
    isGroupChat: Boolean = false,
    onNavigateBack: () -> Unit,
    onOpenMap: (rideId: String, destinationName: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    var inputMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showMenu by remember { mutableStateOf(false) }

    // Admin & Ride States
    var isAdmin by remember { mutableStateOf(false) }
    var activeRide by remember { mutableStateOf<ConvoyRide?>(null) }
    var hasJoinedRide by remember { mutableStateOf(false) }
    var showCreateRideDialog by remember { mutableStateOf(false) }
    var showEndRideConfirmDialog by remember { mutableStateOf(false) }

    // Group members modal state
    var showMembersSheet by remember { mutableStateOf(false) }
    var groupMembers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoadingMembers by remember { mutableStateOf(false) }

    val currentRoomId = viewModel.roomId
    val myUserId = repository.currentUserId

    // 1. Check if current user is leader/creator
    LaunchedEffect(currentRoomId, isGroupChat) {
        if (isGroupChat) {
            isAdmin = repository.isRoomAdmin(currentRoomId)
        }
    }

    // Permission launcher for Location tracking
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted && activeRide != null) {
            val ride = activeRide!!
            coroutineScope.launch {
                repository.joinConvoyRide(ride.id)
                    .onSuccess {
                        hasJoinedRide = true
                        ConvoyLocationService.startService(context, ride.id, ride.destinationName)
                        Toast.makeText(context, "Joined ride! Live location sharing active.", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure {
                        Toast.makeText(context, "Could not join: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        } else if (!granted) {
            Toast.makeText(context, "Location permission is required to share coordinates in a convoy.", Toast.LENGTH_LONG).show()
        }
    }

    // 2. Fetch active ride immediately + listen to realtime stream
    LaunchedEffect(currentRoomId, isGroupChat) {
        if (isGroupChat) {
            // Instant initial fetch
            val initialRide = repository.getActiveRide(currentRoomId)
            if (initialRide != null) {
                activeRide = initialRide
            }

            // Realtime background listener
            repository.getActiveRideStream(currentRoomId)
                .catch { /* ignore */ }
                .collect { ride ->
                    activeRide = ride
                }
        }
    }

    // 3. Persistent join check
    LaunchedEffect(activeRide?.id, myUserId) {
        val rideId = activeRide?.id
        if (rideId != null && myUserId != null) {
            hasJoinedRide = repository.checkHasJoinedRide(rideId)

            repository.getJoinedRidersStream(rideId)
                .catch { /* ignore */ }
                .collect { joinedRiders ->
                    hasJoinedRide = joinedRiders.any { it.userId == myUserId && it.hasJoined }
                }
        } else {
            hasJoinedRide = false
        }
    }

    val isKeyboardOpen = WindowInsets.isImeVisible

    LaunchedEffect(uiState.messages.size, isKeyboardOpen) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LightScreenBg)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = CardBg,
                shape = CircleShape,
                border = BorderStroke(1.dp, CardBorderColor),
                modifier = Modifier
                    .size(42.dp)
                    .clickable { onNavigateBack() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Profile or Group Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(LeafGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGroupChat) Icons.Default.Group else Icons.Default.Person,
                    contentDescription = null,
                    tint = LeafGreen,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Title & Status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = roomTitle,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isLoading) TextDark.copy(alpha = 0.4f) else LeafGreen)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (uiState.isLoading) "Connecting..." else "Active now",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (uiState.isLoading) TextDark.copy(alpha = 0.6f) else LeafGreen
                    )
                }
            }

            // 3-Dots Overflow Menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = TextDark
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(CardBg)
                ) {
                    if (isGroupChat) {
                        if (isAdmin && activeRide == null) {
                            DropdownMenuItem(
                                text = { Text("Start Convoy Ride", color = LeafGreen, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = LeafGreen) },
                                onClick = {
                                    showMenu = false
                                    showCreateRideDialog = true
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = { Text("See Members", color = TextDark, fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Outlined.Group, contentDescription = null, tint = TextDark) },
                            onClick = {
                                showMenu = false
                                showMembersSheet = true
                                coroutineScope.launch {
                                    isLoadingMembers = true
                                    repository.getGroupMembers(currentRoomId)
                                        .onSuccess { members ->
                                            groupMembers = members
                                        }
                                        .onFailure { error ->
                                            Toast.makeText(context, "Error fetching members: ${error.message}", Toast.LENGTH_LONG).show()
                                        }
                                    isLoadingMembers = false
                                }
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("View Profile", color = TextDark, fontWeight = FontWeight.Medium) },
                            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = TextDark) },
                            onClick = {
                                showMenu = false
                                Toast.makeText(context, "Opening profile for $roomTitle", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text(if (isGroupChat) "Report Convoy" else "Report Rider", color = DangerRed, fontWeight = FontWeight.Medium) },
                        leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null, tint = DangerRed) },
                        onClick = {
                            showMenu = false
                            Toast.makeText(context, "Report submitted for moderation review", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        }

        // Security Notice Banner
        Surface(
            color = WarningBannerBg,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, WarningBannerBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = "Security Note",
                    tint = WarningText,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Security Notice: For your safety, do not share personal or financial information.",
                    color = WarningText,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Active Convoy Ride Banner
        if (isGroupChat && activeRide != null) {
            val ride = activeRide!!
            Surface(
                color = CardBg,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, LeafGreen),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(GreenTintBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            tint = LeafGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Destination: ${ride.destinationName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (hasJoinedRide) "Sharing location with convoy" else "Join ride to share & see live pins",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (hasJoinedRide) LeafGreen else TextDark.copy(alpha = 0.6f)
                        )
                    }

                    if (!hasJoinedRide) {
                        Button(
                            onClick = {
                                val hasFine = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasFine) {
                                    coroutineScope.launch {
                                        repository.joinConvoyRide(ride.id)
                                            .onSuccess {
                                                hasJoinedRide = true
                                                ConvoyLocationService.startService(context, ride.id, ride.destinationName)
                                                Toast.makeText(context, "Joined ride! Live location sharing active.", Toast.LENGTH_SHORT).show()
                                            }
                                            .onFailure {
                                                Toast.makeText(context, "Could not join: ${it.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LeafGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = "Join Ride",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Map Button
                            Button(
                                onClick = { onOpenMap(ride.id, ride.destinationName) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LeafGreen,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Map",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            val isRideCreator = ride.creatorId == myUserId || isAdmin

                            if (isRideCreator) {
                                Button(
                                    onClick = { showEndRideConfirmDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DangerRed,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(
                                        text = "End Ride",
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.leaveConvoyRide(ride.id)
                                                .onSuccess {
                                                    hasJoinedRide = false
                                                    ConvoyLocationService.stopService(context)
                                                    Toast.makeText(context, "Stopped sharing location.", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                    },
                                    border = BorderStroke(1.5.dp, DangerRed),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(
                                        text = "Leave",
                                        fontSize = 12.sp,
                                        color = DangerRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Messages List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (uiState.isLoading && uiState.messages.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = LeafGreen,
                    strokeWidth = 3.dp
                )
            } else if (uiState.messages.isEmpty()) {
                Text(
                    text = "No messages yet. Say hi!",
                    color = TextDark.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        val isMe = message.senderId == viewModel.currentUserId
                        val isLatestFromMe = isMe && message.id == uiState.messages.lastOrNull { it.senderId == viewModel.currentUserId }?.id

                        ChatBubble(
                            message = message,
                            isFromCurrentUser = isMe,
                            showStatus = isLatestFromMe
                        )
                    }
                }
            }
        }

        // Input Field
        Surface(
            color = CardBg,
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    placeholder = {
                        Text("Message riders...", color = TextDark.copy(alpha = 0.5f), fontSize = 14.sp)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextDark,
                        unfocusedTextColor = TextDark,
                        focusedBorderColor = LeafGreen,
                        unfocusedBorderColor = FieldBorderDark
                    )
                )

                IconButton(
                    onClick = {
                        if (inputMessage.isNotBlank()) {
                            viewModel.onSendMessage(inputMessage)
                            inputMessage = ""
                        }
                    },
                    enabled = inputMessage.isNotBlank(),
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = if (inputMessage.isNotBlank()) LeafGreen else FieldBorderDark,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
                }
            }
        }
    }

    // Start Ride Dialog (Admin Only)
    if (showCreateRideDialog) {
        var destination by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showCreateRideDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = "Start Convoy Ride",
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = TextDark
                )
            },
            text = {
                Column {
                    Text(
                        text = "Set destination. Group riders will be asked to join before their GPS is visible.",
                        fontSize = 13.sp,
                        color = TextDark,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        placeholder = {
                            Text(
                                text = "e.g. Laguna Loop",
                                color = TextDark.copy(alpha = 0.4f)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark,
                            focusedBorderColor = LeafGreen,
                            unfocusedBorderColor = LeafGreen.copy(alpha = 0.5f),
                            cursorColor = LeafGreen,
                            focusedContainerColor = GreenTintBg,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (destination.isNotBlank()) {
                            isSubmitting = true
                            coroutineScope.launch {
                                repository.createConvoyRide(
                                    roomId = currentRoomId,
                                    destinationName = destination.trim(),
                                    destinationLat = 14.1153,
                                    destinationLng = 120.9621
                                ).onSuccess { newRide ->
                                    // Instantly render banner without waiting for Supabase Realtime echo
                                    activeRide = newRide
                                    isSubmitting = false
                                    showCreateRideDialog = false
                                    Toast.makeText(context, "Ride started! Riders can now join.", Toast.LENGTH_SHORT).show()
                                }.onFailure {
                                    isSubmitting = false
                                    Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = destination.isNotBlank() && !isSubmitting,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LeafGreen,
                        contentColor = Color.White,
                        disabledContainerColor = LeafGreen.copy(alpha = 0.35f),
                        disabledContentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    Text(
                        text = if (isSubmitting) "Starting..." else "Start Ride",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateRideDialog = false },
                    enabled = !isSubmitting
                ) {
                    Text(
                        text = "Cancel",
                        color = TextDark,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }

    // Admin End Ride Confirmation Dialog
    if (showEndRideConfirmDialog && activeRide != null) {
        val rideToEnd = activeRide!!
        var isEndingRide by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isEndingRide) showEndRideConfirmDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = "End Convoy Ride?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextDark
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to end \"${rideToEnd.destinationName}\"? This will terminate the ride and stop live tracking for all members in the convoy.",
                    fontSize = 13.sp,
                    color = TextDark.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isEndingRide = true
                        // Instantly clear banner from UI
                        activeRide = null
                        hasJoinedRide = false
                        ConvoyLocationService.stopService(context)
                        showEndRideConfirmDialog = false

                        coroutineScope.launch {
                            repository.endConvoyRide(rideToEnd.id)
                                .onSuccess {
                                    isEndingRide = false
                                    Toast.makeText(context, "Convoy ride ended for all riders.", Toast.LENGTH_SHORT).show()
                                }
                                .onFailure { error ->
                                    isEndingRide = false
                                    Toast.makeText(context, "Failed to end ride: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    },
                    enabled = !isEndingRide,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DangerRed,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (isEndingRide) "Ending..." else "End Ride for All",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEndRideConfirmDialog = false },
                    enabled = !isEndingRide
                ) {
                    Text(
                        text = "Cancel",
                        color = TextDark,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }

    // Bottom Sheet for "See Members"
    if (showMembersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMembersSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Convoy Members (${groupMembers.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (isLoadingMembers) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LeafGreen, strokeWidth = 3.dp)
                    }
                } else if (groupMembers.isEmpty()) {
                    Text("No member details available", color = TextDark.copy(alpha = 0.6f), fontSize = 14.sp)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(groupMembers, key = { it.id }) { member ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = GreenTintBg,
                                border = BorderStroke(1.dp, BorderGreen),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(LeafGreen.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = LeafGreen)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = member.username ?: "Rider",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextDark
                                        )
                                        member.bikeName?.let {
                                            Text(text = it, fontSize = 12.sp, color = LeafGreen)
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
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    isFromCurrentUser: Boolean,
    showStatus: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bubbleColor = if (isFromCurrentUser) LeafGreen else IncomingBubbleBg
    val textColor = if (isFromCurrentUser) Color.White else TextDark
    val timestampColor = if (isFromCurrentUser) Color.White.copy(alpha = 0.85f) else LeafGreen.copy(alpha = 0.85f)

    val bubbleShape = if (isFromCurrentUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = bubbleShape,
            border = if (!isFromCurrentUser) BorderStroke(1.dp, BorderGreen) else null,
            tonalElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(
                    text = message.content,
                    color = textColor,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    style = MaterialTheme.typography.bodyMedium
                )

                message.createdAt?.let { timestamp ->
                    Text(
                        text = formatTime(timestamp),
                        color = timestampColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 2.dp)
                    )
                }
            }
        }

        if (isFromCurrentUser && showStatus) {
            Text(
                text = if (message.isRead) "Seen ✓✓" else "Sent ✓",
                fontSize = 10.sp,
                color = LeafGreen,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp, end = 4.dp)
            )
        }
    }
}

private fun formatTime(isoTimestamp: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = parser.parse(isoTimestamp.substringBefore("."))
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        if (date != null) formatter.format(date) else ""
    } catch (_: Exception) {
        ""
    }
}