package com.example.motorapp.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.motorapp.data.RoomType
import kotlinx.coroutines.launch

private val LeafGreen = Color(0xFF16A34A)
private val TextDark = Color(0xFF0F172A)
private val TextSubtle = Color(0xFF64748B)
private val BottomNavBg = Color(0xFFFFFFFF)
private val ScreenBg = Color(0xFFF8FAFC)

@Composable
fun MainScreen(
    dashboardViewModel: DashboardViewModel,
    fuelViewModel: FuelViewModel,
    garageViewModel: GarageViewModel,
    maintenanceViewModel: MaintenanceViewModel,
    accountViewModel: AccountViewModel,
    onLogout: () -> Unit = {}
) {
    val items = listOf(
        Screen.Cockpit,
        Screen.Maintenance,
        Screen.Fuel,
        Screen.Garage,
        Screen.Account
    )

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Cockpit) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val maintenanceState by maintenanceViewModel.uiState.collectAsStateWithLifecycle()
    val garageState by garageViewModel.uiState.collectAsStateWithLifecycle()
    val dashboardState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val fuelState by fuelViewModel.uiState.collectAsStateWithLifecycle()
    val accountState by accountViewModel.uiState.collectAsStateWithLifecycle()

    val (isCurrentScreenLoading, loadingMessage) = when (currentScreen) {
        Screen.Cockpit -> Pair(dashboardState.isLoading, "Syncing cockpit...")
        Screen.Maintenance -> Pair(maintenanceState.isLoading, "Loading maintenance records...")
        Screen.Fuel -> Pair(fuelState.isLoading, "Retrieving fuel history...")
        Screen.Garage -> Pair(garageState.isLoading, "Loading your garage...")
        Screen.Account -> Pair(accountState.isLoading, "Loading profile...")
        else -> Pair(false, "")
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "MotoPulse Tools",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Text(
                    text = "Rider utilities & quick actions",
                    fontSize = 12.sp,
                    color = TextSubtle,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(16.dp))

                // 1. Ride Navigation
                NavigationDrawerItem(
                    label = { Text("Ride Navigation", fontWeight = FontWeight.SemiBold) },
                    selected = currentScreen == Screen.RideNavigation,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        currentScreen = Screen.RideNavigation
                    },
                    icon = { Icon(Icons.Default.Navigation, contentDescription = null) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = LeafGreen.copy(alpha = 0.12f),
                        selectedIconColor = LeafGreen,
                        selectedTextColor = LeafGreen,
                        unselectedIconColor = TextSubtle,
                        unselectedTextColor = TextDark
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // 2. Private Chat
                NavigationDrawerItem(
                    label = { Text("Private Chat", fontWeight = FontWeight.SemiBold) },
                    selected = currentScreen == Screen.PrivateChat,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        currentScreen = Screen.PrivateChat
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = LeafGreen.copy(alpha = 0.12f),
                        selectedIconColor = LeafGreen,
                        selectedTextColor = LeafGreen,
                        unselectedIconColor = TextSubtle,
                        unselectedTextColor = TextDark
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // 3. Group Chat
                NavigationDrawerItem(
                    label = { Text("Group Chat", fontWeight = FontWeight.SemiBold) },
                    selected = currentScreen == Screen.GroupChat,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        currentScreen = Screen.GroupChat
                    },
                    icon = { Icon(Icons.Default.Group, contentDescription = null) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = LeafGreen.copy(alpha = 0.12f),
                        selectedIconColor = LeafGreen,
                        selectedTextColor = LeafGreen,
                        unselectedIconColor = TextSubtle,
                        unselectedTextColor = TextDark
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // 4. Community Chat
                NavigationDrawerItem(
                    label = { Text("Community Chat", fontWeight = FontWeight.SemiBold) },
                    selected = currentScreen == Screen.CommunityChat,
                    onClick = {
                        coroutineScope.launch { drawerState.close() }
                        currentScreen = Screen.CommunityChat
                    },
                    icon = { Icon(Icons.Default.Public, contentDescription = null) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = LeafGreen.copy(alpha = 0.12f),
                        selectedIconColor = LeafGreen,
                        selectedTextColor = LeafGreen,
                        unselectedIconColor = TextSubtle,
                        unselectedTextColor = TextDark
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // 5. App Settings
                NavigationDrawerItem(
                    label = { Text("App Settings") },
                    selected = false,
                    onClick = { coroutineScope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedIconColor = TextSubtle,
                        unselectedTextColor = TextDark
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                val isChatSection = currentScreen in listOf(
                    Screen.PrivateChat,
                    Screen.GroupChat,
                    Screen.CommunityChat
                ) || currentScreen is Screen.Chat || currentScreen is Screen.ConvoyMap

                val showBottomBar = !isChatSection && currentScreen != Screen.RideNavigation

                if (showBottomBar) {
                    NavigationBar(
                        containerColor = BottomNavBg,
                        tonalElevation = 8.dp
                    ) {
                        items.forEach { screen ->
                            val selected = currentScreen == screen
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentScreen = screen },
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = LeafGreen,
                                    selectedTextColor = LeafGreen,
                                    indicatorColor = LeafGreen.copy(alpha = 0.12f),
                                    unselectedIconColor = TextSubtle,
                                    unselectedTextColor = TextSubtle
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (val screen = currentScreen) {
                    Screen.Cockpit -> DashboardScreen(
                        viewModel = dashboardViewModel,
                        onNavigateToGarage = { currentScreen = Screen.Garage },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                    Screen.Maintenance -> MaintenanceScreen(
                        viewModel = maintenanceViewModel,
                        onNavigateToGarage = { currentScreen = Screen.Garage },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                    Screen.Fuel -> FuelLogScreen(
                        viewModel = fuelViewModel,
                        onNavigateToGarage = { currentScreen = Screen.Garage },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                    Screen.Garage -> GarageScreen(
                        viewModel = garageViewModel,
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                    Screen.Account -> AccountScreen(
                        viewModel = accountViewModel,
                        onLogoutClick = onLogout
                    )
                    Screen.RideNavigation -> RiderRouteLauncherScreen(
                        onBackToCockpit = { currentScreen = Screen.Cockpit }
                    )
                    // Drawer Chat Views pass their origin to Screen.Chat
                    Screen.PrivateChat -> RoomListScreen(
                        title = "Private Chats",
                        roomType = RoomType.DIRECT,
                        onOpenRoom = { id, title ->
                            currentScreen = Screen.Chat(
                                roomId = id,
                                roomTitle = title,
                                returnScreen = Screen.PrivateChat
                            )
                        },
                        onNavigateBack = {
                            currentScreen = Screen.Cockpit
                            coroutineScope.launch { drawerState.open() }
                        }
                    )
                    Screen.GroupChat -> RoomListScreen(
                        title = "Group / Convoy Chats",
                        roomType = RoomType.CONVOY,
                        onOpenRoom = { id, title ->
                            currentScreen = Screen.Chat(
                                roomId = id,
                                roomTitle = title,
                                returnScreen = Screen.GroupChat
                            )
                        },
                        onNavigateBack = {
                            currentScreen = Screen.Cockpit
                            coroutineScope.launch { drawerState.open() }
                        }
                    )
                    Screen.CommunityChat -> RoomListScreen(
                        title = "Community Boards",
                        roomType = RoomType.REGIONAL,
                        onOpenRoom = { id, title ->
                            currentScreen = Screen.Chat(
                                roomId = id,
                                roomTitle = title,
                                returnScreen = Screen.CommunityChat
                            )
                        },
                        onNavigateBack = {
                            currentScreen = Screen.Cockpit
                            coroutineScope.launch { drawerState.open() }
                        }
                    )
                    is Screen.Chat -> {
                        val chatViewModel = remember(screen.roomId) {
                            ChatViewModel(roomId = screen.roomId)
                        }
                        ChatScreen(
                            roomTitle = screen.roomTitle,
                            viewModel = chatViewModel,
                            isGroupChat = screen.returnScreen == Screen.GroupChat,
                            onNavigateBack = {
                                currentScreen = screen.returnScreen
                            },
                            onOpenMap = { rideId, dest ->
                                currentScreen = Screen.ConvoyMap(
                                    rideId = rideId,
                                    destinationName = dest,
                                    roomId = screen.roomId,
                                    roomTitle = screen.roomTitle
                                )
                            }
                        )
                    }
                    is Screen.ConvoyMap -> {
                        ConvoyMapScreen(
                            rideId = screen.rideId,
                            destinationName = screen.destinationName,
                            onNavigateBack = {
                                currentScreen = Screen.Chat(
                                    roomId = screen.roomId,
                                    roomTitle = screen.roomTitle,
                                    returnScreen = Screen.GroupChat
                                )
                            }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isCurrentScreenLoading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ScreenBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = LeafGreen,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = loadingMessage,
                                fontSize = 13.sp,
                                color = TextSubtle,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}