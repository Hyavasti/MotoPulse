package com.example.motorapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.motorapp.data.ChatNotificationHelper
import com.example.motorapp.data.ChatRepository
import com.example.motorapp.data.MotorRepository
import com.example.motorapp.screens.AccountViewModel
import com.example.motorapp.screens.AuthScreen
import com.example.motorapp.screens.AuthUiState
import com.example.motorapp.screens.AuthViewModel
import com.example.motorapp.screens.DashboardViewModel
import com.example.motorapp.screens.FuelViewModel
import com.example.motorapp.screens.GarageViewModel
import com.example.motorapp.screens.MaintenanceViewModel
import com.example.motorapp.screens.MainScreen
import com.example.motorapp.ui.theme.MotorAppTheme
import kotlinx.coroutines.flow.catch
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize OSMDroid cache & User-Agent before any UI renders
        val prefs = getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
        Configuration.getInstance().load(this, prefs)
        Configuration.getInstance().userAgentValue = "${packageName}/1.0 (Android; MotoPulse Convoy Tracker)"

        enableEdgeToEdge()

        setContent {
            MotorAppTheme {
                val context = LocalContext.current
                val authState by authViewModel.uiState.collectAsState()

                // Request POST_NOTIFICATIONS permission on Android 13+ (API 33+)
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { /* Granted or dismissed */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val isGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!isGranted) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                when (val state = authState) {
                    is AuthUiState.Success -> {
                        val repo = remember(state.userId) { MotorRepository(userId = state.userId) }
                        val chatRepo = remember { ChatRepository() }

                        // Background listener: Fire notifications for incoming rider messages
                        LaunchedEffect(state.userId) {
                            var initialLoadSkipped = false
                            chatRepo.getAllIncomingMessagesStream()
                                .catch { /* Ignore stream errors silently */ }
                                .collect { messages ->
                                    if (!initialLoadSkipped) {
                                        // Skip notifications on first fetch of old messages
                                        initialLoadSkipped = true
                                    } else {
                                        val latest = messages.lastOrNull()
                                        if (latest != null && latest.senderId != state.userId) {
                                            ChatNotificationHelper.showMessageNotification(
                                                context = context,
                                                senderName = "Rider Message",
                                                messageText = latest.content
                                            )
                                        }
                                    }
                                }
                        }

                        val dashboardViewModel = remember(repo) { DashboardViewModel(repository = repo) }
                        val fuelViewModel = remember(repo) { FuelViewModel(repository = repo) }
                        val garageViewModel = remember(repo) { GarageViewModel(repository = repo) }
                        val maintenanceViewModel = remember(repo) { MaintenanceViewModel(repository = repo) }
                        val accountViewModel = remember(repo) { AccountViewModel(repository = repo) }

                        MainScreen(
                            dashboardViewModel = dashboardViewModel,
                            fuelViewModel = fuelViewModel,
                            garageViewModel = garageViewModel,
                            maintenanceViewModel = maintenanceViewModel,
                            accountViewModel = accountViewModel,
                            onLogout = { authViewModel.signOut() }
                        )
                    }
                    else -> {
                        AuthScreen(
                            authViewModel = authViewModel,
                            onAuthSuccess = { /* Handled by authState */ }
                        )
                    }
                }
            }
        }
    }
}