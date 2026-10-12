package com.example.motorapp.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motorapp.data.ChatRepository
import com.example.motorapp.data.ChatRoom
import com.example.motorapp.data.RoomType
import kotlinx.coroutines.launch

private val LeafGreen = Color(0xFF16A34A)
private val LightScreenBg = Color(0xFFF8FAFC)
private val CardBg = Color(0xFFFFFFFF)
private val TextDark = Color(0xFF0F172A)
private val TextSubtle = Color(0xFF64748B)
private val CardBorderColor = Color(0xFFE2E8F0)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoomListScreen(
    title: String,
    roomType: RoomType,
    repository: ChatRepository = remember { ChatRepository() },
    onOpenRoom: (roomId: String, roomTitle: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var rooms by remember { mutableStateOf<List<ChatRoom>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showNewChatDialog by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }

    var roomToDelete by remember { mutableStateOf<ChatRoom?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val roomMeta = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(roomType, refreshKey) {
        isLoading = true
        repository.getAvailableRooms(roomType).onSuccess { list ->
            rooms = list
            list.forEach { room ->
                when (room.type) {
                    RoomType.DIRECT -> {
                        val preview = repository.getDirectRoomPreview(room.id)
                        roomMeta[room.id] = when {
                            preview.lastMessageText == null -> "No messages yet"
                            preview.isLastFromMe -> "You: ${preview.lastMessageText}"
                            preview.unreadCount > 0 -> "${preview.unreadCount} unread message${if (preview.unreadCount > 1) "s" else ""}"
                            else -> preview.lastMessageText
                        }
                    }
                    RoomType.CONVOY -> {
                        val members = repository.getRoomMemberCount(room.id)
                        roomMeta[room.id] = "$members member${if (members > 1) "s" else ""}"
                    }
                    RoomType.REGIONAL -> {
                        roomMeta[room.id] = room.region ?: "Public board"
                    }
                }
            }
        }
        isLoading = false
    }

    val icon: ImageVector = when (roomType) {
        RoomType.DIRECT -> Icons.AutoMirrored.Filled.Chat
        RoomType.CONVOY -> Icons.Default.Group
        RoomType.REGIONAL -> Icons.Default.Public
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
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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

                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }

            // Body Area
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LeafGreen, strokeWidth = 3.dp)
                }
            } else if (rooms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(LeafGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = LeafGreen, modifier = Modifier.size(40.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (roomType == RoomType.DIRECT) "No Direct Messages" else "No Active Convoys",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the + button below to find riders",
                            fontSize = 13.sp,
                            color = TextSubtle
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rooms, key = { it.id }) { room ->
                        val subtitleText = roomMeta[room.id] ?: when (room.type) {
                            RoomType.DIRECT -> "Loading preview..."
                            RoomType.CONVOY -> "Loading members..."
                            RoomType.REGIONAL -> room.region ?: "Public"
                        }

                        val hasIncomingUnread = subtitleText.contains("unread", ignoreCase = true)

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = CardBg,
                            border = BorderStroke(1.dp, CardBorderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .combinedClickable(
                                    onClick = { onOpenRoom(room.id, room.name ?: "Chat") },
                                    onLongClick = {
                                        if (room.type != RoomType.REGIONAL) {
                                            roomToDelete = room
                                        }
                                    }
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(LeafGreen.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = LeafGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = room.name ?: "Untitled Room",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = subtitleText,
                                        fontSize = 12.sp,
                                        fontWeight = if (hasIncomingUnread) FontWeight.Bold else FontWeight.Normal,
                                        color = if (hasIncomingUnread) LeafGreen else TextSubtle,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom-Right Add FAB
        if (roomType != RoomType.REGIONAL) {
            FloatingActionButton(
                onClick = { showNewChatDialog = true },
                containerColor = LeafGreen,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Chat",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }

    // Confirmation Alert Dialog matching the light UI theme
    roomToDelete?.let { room ->
        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) roomToDelete = null
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            titleContentColor = TextDark,
            textContentColor = TextSubtle,
            title = {
                Text(
                    text = if (room.type == RoomType.CONVOY) "Delete Convoy Chat" else "Delete Chat",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextDark
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${room.name ?: "this chat"}\"? You will no longer be found in this chat room.",
                    fontSize = 14.sp,
                    color = TextSubtle,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetId = room.id
                        isDeleting = true
                        coroutineScope.launch {
                            repository.leaveOrDeleteRoom(targetId)
                            rooms = rooms.filterNot { it.id == targetId }
                            isDeleting = false
                            roomToDelete = null
                        }
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Delete",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { roomToDelete = null },
                    enabled = !isDeleting,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CardBorderColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSubtle
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Cancel",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        )
    }

    // Opens the rider selection / new chat dialog
    if (showNewChatDialog) {
        NewChatDialog(
            roomType = roomType,
            repository = repository,
            onDismiss = { showNewChatDialog = false },
            onChatCreated = { roomId, roomTitle ->
                showNewChatDialog = false
                refreshKey++
                onOpenRoom(roomId, roomTitle)
            }
        )
    }
}