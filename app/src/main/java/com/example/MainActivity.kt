package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.JarvisAssistantService
import com.example.ui.ActiveTab
import com.example.ui.JarvisViewModel
import com.example.ui.components.ConfirmationDialog
import com.example.ui.screens.CodeStudioScreen
import com.example.ui.screens.HudScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TerminalScreen
import com.example.ui.screens.ToolsAndProjectsScreen
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == JarvisAssistantService.ACTION_TRIGGER_VOICE) {
            viewModel.toggleVoiceListening()
        }
    }
}

@Composable
fun MainAppContent(viewModel: JarvisViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val pendingConfirm by viewModel.pendingConfirmation.collectAsState()

    var showPermissionsScreen by remember { mutableStateOf(false) }

    // Back handler: return to HUD if on another tab or screen
    BackHandler(enabled = showPermissionsScreen || currentTab != ActiveTab.HUD) {
        if (showPermissionsScreen) {
            showPermissionsScreen = false
        } else {
            viewModel.setTab(ActiveTab.HUD)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = JarvisBackground,
        bottomBar = {
            if (!showPermissionsScreen) {
                NavigationBar(
                    containerColor = JarvisSurface,
                    contentColor = JarvisCyan,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    NavigationBarItem(
                        selected = currentTab == ActiveTab.HUD,
                        onClick = { viewModel.setTab(ActiveTab.HUD) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "HUD",
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                "HUD",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = navBarColors()
                    )

                    NavigationBarItem(
                        selected = currentTab == ActiveTab.TERMINAL,
                        onClick = { viewModel.setTab(ActiveTab.TERMINAL) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Terminal",
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                "CHAT",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = navBarColors()
                    )

                    NavigationBarItem(
                        selected = currentTab == ActiveTab.CODE_STUDIO,
                        onClick = { viewModel.setTab(ActiveTab.CODE_STUDIO) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "Code",
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                "STUDIO",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = navBarColors()
                    )

                    NavigationBarItem(
                        selected = currentTab == ActiveTab.PROJECTS_AND_TOOLS,
                        onClick = { viewModel.setTab(ActiveTab.PROJECTS_AND_TOOLS) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Tools",
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                "TOOLS",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = navBarColors()
                    )

                    NavigationBarItem(
                        selected = currentTab == ActiveTab.SETTINGS,
                        onClick = { viewModel.setTab(ActiveTab.SETTINGS) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                "CONFIG",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = navBarColors()
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(JarvisBackground)
        ) {
            if (showPermissionsScreen) {
                PermissionsScreen(onBack = { showPermissionsScreen = false })
            } else {
                when (currentTab) {
                    ActiveTab.HUD -> HudScreen(
                        viewModel = viewModel,
                        onNavigateToPermissions = { showPermissionsScreen = true }
                    )
                    ActiveTab.TERMINAL -> TerminalScreen(viewModel = viewModel)
                    ActiveTab.CODE_STUDIO -> CodeStudioScreen(viewModel = viewModel)
                    ActiveTab.PROJECTS_AND_TOOLS -> ToolsAndProjectsScreen(viewModel = viewModel)
                    ActiveTab.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToPermissions = { showPermissionsScreen = true }
                    )
                }
            }

            // Pending Confirmation Dialog (for Play Store install or High Risk actions)
            pendingConfirm?.let { pending ->
                ConfirmationDialog(
                    title = pending.title,
                    message = pending.message,
                    confirmButtonText = if (pending.isPlayStoreOffer) "Open Play Store" else "Confirm",
                    cancelButtonText = "Cancel",
                    onConfirm = { viewModel.confirmPendingAction() },
                    onDismiss = { viewModel.dismissPendingAction() }
                )
            }
        }
    }
}

@Composable
fun navBarColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Color(0xFF001F29),
    selectedTextColor = JarvisCyan,
    indicatorColor = JarvisCyan,
    unselectedIconColor = JarvisTextSecondary,
    unselectedTextColor = JarvisTextSecondary
)
