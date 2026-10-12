package com.example.motorapp.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.motorapp.data.ChatRepository
import com.example.motorapp.data.RoomType
import com.example.motorapp.data.UserProfile
import kotlinx.coroutines.launch

private val LeafGreen = Color(0xFF16A34A)
private val DarkSlate = Color(0xFF0F172A)
private val GreenTintBg = Color(0xFFF0FDF4)
private val CardBorderGreen = Color(0xFFBBF7D0)

@Composable
fun NewChatDialog(
    roomType: RoomType,
    repository: ChatRepository = remember { ChatRepository() },
    onDismiss: () -> Unit,
    onChatCreated: (roomId: String, roomTitle: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    val selectedUsers = remember { mutableStateListOf<UserProfile>() }
    var isSearching by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(1.5.dp, LeafGreen.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (roomType == RoomType.DIRECT) "New Direct Message" else "Create Group / Convoy",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkSlate
                    )
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isCreating
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = DarkSlate
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (roomType == RoomType.CONVOY) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group / Convoy Name", color = DarkSlate) },
                        placeholder = { Text("e.g. Tagaytay Sunday Loop", color = DarkSlate.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        enabled = !isCreating,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = DarkSlate,
                            unfocusedTextColor = DarkSlate,
                            focusedBorderColor = LeafGreen,
                            unfocusedBorderColor = LeafGreen.copy(alpha = 0.4f),
                            focusedLabelColor = LeafGreen,
                            unfocusedLabelColor = DarkSlate
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Selected Riders Chips (All Green + Dark Text)
                    if (selectedUsers.isNotEmpty()) {
                        Text(
                            text = "SELECTED RIDERS (${selectedUsers.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = LeafGreen,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(selectedUsers, key = { it.id }) { user ->
                                InputChip(
                                    selected = true,
                                    onClick = { selectedUsers.remove(user) },
                                    label = {
                                        Text(
                                            user.username ?: "Rider",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = GreenTintBg,
                                        selectedLabelColor = LeafGreen,
                                        selectedTrailingIconColor = LeafGreen
                                    ),
                                    border = BorderStroke(1.dp, CardBorderGreen)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Search Bar (White background, green border, dark text)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        if (query.trim().length >= 2) {
                            coroutineScope.launch {
                                isSearching = true
                                repository.searchUsers(query.trim())
                                    .onSuccess { searchResults = it }
                                    .onFailure { err ->
                                        Log.e("ChatSearch", "Search error: ${err.message}", err)
                                    }
                                isSearching = false
                            }
                        } else {
                            searchResults = emptyList()
                        }
                    },
                    label = { Text("Search rider username...", color = DarkSlate) },
                    placeholder = { Text("Type name to find riders...", color = DarkSlate.copy(alpha = 0.5f)) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = LeafGreen
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = !isCreating,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DarkSlate,
                        unfocusedTextColor = DarkSlate,
                        focusedBorderColor = LeafGreen,
                        unfocusedBorderColor = LeafGreen.copy(alpha = 0.4f),
                        focusedLabelColor = LeafGreen,
                        unfocusedLabelColor = DarkSlate
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Results list
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(32.dp),
                            color = LeafGreen,
                            strokeWidth = 3.dp
                        )
                    } else if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
                        Text(
                            text = "No riders found with '$searchQuery'",
                            color = DarkSlate,
                            modifier = Modifier.align(Alignment.Center),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(searchResults, key = { it.id }) { user ->
                                val isSelected = selectedUsers.any { it.id == user.id }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) GreenTintBg else Color.White,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) LeafGreen else CardBorderGreen
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !isCreating) {
                                            if (roomType == RoomType.DIRECT) {
                                                isCreating = true
                                                coroutineScope.launch {
                                                    repository.createOrGetDirectChat(user)
                                                        .onSuccess { room ->
                                                            isCreating = false
                                                            onChatCreated(
                                                                room.id,
                                                                room.name ?: (user.username ?: "Direct Chat")
                                                            )
                                                        }
                                                        .onFailure { err ->
                                                            isCreating = false
                                                            Toast.makeText(
                                                                context,
                                                                "Error: ${err.message}",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                }
                                            } else {
                                                if (isSelected) {
                                                    selectedUsers.removeAll { it.id == user.id }
                                                } else {
                                                    selectedUsers.add(user)
                                                }
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(GreenTintBg, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = LeafGreen
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = user.username ?: "Unnamed Rider",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = DarkSlate
                                            )
                                            user.bikeName?.let {
                                                Text(
                                                    text = it,
                                                    fontSize = 12.sp,
                                                    color = LeafGreen,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        if (roomType == RoomType.CONVOY && isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = LeafGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isCreating) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.White.copy(alpha = 0.85f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = LeafGreen, strokeWidth = 3.dp)
                            }
                        }
                    }
                }

                // Create Group Action Button
                if (roomType == RoomType.CONVOY) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            if (groupName.isNotBlank() && selectedUsers.isNotEmpty()) {
                                isCreating = true
                                coroutineScope.launch {
                                    repository.createGroupChat(
                                        groupName = groupName,
                                        selectedUserIds = selectedUsers.map { it.id }
                                    ).onSuccess { room ->
                                        isCreating = false
                                        onChatCreated(room.id, room.name ?: groupName)
                                    }.onFailure { err ->
                                        isCreating = false
                                        Toast.makeText(
                                            context,
                                            "Error: ${err.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        },
                        enabled = groupName.isNotBlank() && selectedUsers.isNotEmpty() && !isCreating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LeafGreen,
                            contentColor = Color.White,
                            disabledContainerColor = LeafGreen.copy(alpha = 0.35f),
                            disabledContentColor = Color.White.copy(alpha = 0.7f)
                        )
                    ) {
                        Text(
                            text = "Create Group (${selectedUsers.size} riders)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}