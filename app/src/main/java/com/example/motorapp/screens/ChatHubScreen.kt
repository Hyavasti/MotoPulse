package com.example.motorapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motorapp.data.ChatRoom
import com.example.motorapp.data.RoomType
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

private val LeafGreen = Color(0xFF16A34A)
private val TextDark = Color(0xFF0F172A)
private val TextSubtle = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHubScreen(
    onOpenRoom: (roomId: String, roomTitle: String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Community", "Groups / Convoys", "Direct")

    // Starter channels until loaded from Supabase
    val communityRooms = listOf(
        ChatRoom(id = "comm_general", type = RoomType.REGIONAL, name = "General Rider Lounge", region = "Philippines"),
        ChatRoom(id = "comm_calabarzon", type = RoomType.REGIONAL, name = "CALABARZON Riders", region = "Region IV-A"),
        ChatRoom(id = "comm_tech", type = RoomType.REGIONAL, name = "Maintenance & Tuning Help", region = "All")
    )

    val convoyRooms = listOf(
        ChatRoom(id = "group_weekend_loop", type = RoomType.CONVOY, name = "Sunday Tagaytay Loop Ride"),
        ChatRoom(id = "group_marilaque", type = RoomType.CONVOY, name = "Marilaque Breakfast Run")
    )

    val directMessages = listOf(
        ChatRoom(id = "dm_rider_sample", type = RoomType.DIRECT, name = "Rider @JuanD")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Rider Messages & Hub",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8FAFC))
        ) {
            // Scope Switcher Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = LeafGreen,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = LeafGreen
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == index) LeafGreen else TextSubtle
                            )
                        }
                    )
                }
            }

            // Room Lists
            val activeList = when (selectedTab) {
                0 -> communityRooms
                1 -> convoyRooms
                else -> directMessages
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(activeList, key = { it.id }) { room ->
                    ChatRoomCard(
                        room = room,
                        onClick = {
                            onOpenRoom(room.id, room.name ?: "Chat Room")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatRoomCard(
    room: ChatRoom,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = when (room.type) {
                            RoomType.REGIONAL -> LeafGreen.copy(alpha = 0.12f)
                            RoomType.CONVOY -> Color(0xFF2563EB).copy(alpha = 0.12f)
                            RoomType.DIRECT -> Color(0xFF7C3AED).copy(alpha = 0.12f)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (room.type) {
                        RoomType.REGIONAL -> Icons.Default.Public
                        RoomType.CONVOY -> Icons.Default.Group
                        RoomType.DIRECT -> Icons.AutoMirrored.Filled.Chat
                    },
                    contentDescription = null,
                    tint = when (room.type) {
                        RoomType.REGIONAL -> LeafGreen
                        RoomType.CONVOY -> Color(0xFF2563EB)
                        RoomType.DIRECT -> Color(0xFF7C3AED)
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.name ?: "Untitled Chat",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextDark
                )

                val subtitle = when (room.type) {
                    RoomType.REGIONAL -> "Regional Board • ${room.region ?: "All Riders"}"
                    RoomType.CONVOY -> "Convoy Room • Live ride chat"
                    RoomType.DIRECT -> "Direct Message"
                }

                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextSubtle
                )
            }
        }
    }
}