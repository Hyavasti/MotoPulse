package com.example.motorapp.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Cockpit : Screen("cockpit", "Cockpit", Icons.Default.Speed)
    data object Maintenance : Screen("maintenance", "PMS", Icons.Default.Build)
    data object Fuel : Screen("fuel", "Fuel Log", Icons.Default.LocalGasStation)
    data object Garage : Screen("garage", "Garage", Icons.Default.TwoWheeler)
    data object Account : Screen("account", "Account", Icons.Default.Person)
    data object RideNavigation : Screen("navigation", "Ride Navigation", Icons.Default.Navigation)

    // Drawer Chat Scopes
    data object PrivateChat : Screen("private_chat", "Private Chat", Icons.AutoMirrored.Filled.Chat)
    data object GroupChat : Screen("group_chat", "Group Chat", Icons.Default.Group)
    data object CommunityChat : Screen("community_chat", "Community Chat", Icons.Default.Public)

    // Active Chat Destination (records returnScreen for correct back navigation)
    data class Chat(
        val roomId: String,
        val roomTitle: String,
        val returnScreen: Screen = Screen.Cockpit
    ) : Screen("chat", "Chat", Icons.AutoMirrored.Filled.Chat)

    // Live Convoy Map Screen
    data class ConvoyMap(
        val rideId: String,
        val destinationName: String,
        val roomId: String,
        val roomTitle: String
    ) : Screen("convoy_map", "Convoy Map", Icons.Default.Navigation)
}