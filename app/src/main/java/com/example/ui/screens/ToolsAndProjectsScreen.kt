package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HistoryEntity
import com.example.data.ProjectEntity
import com.example.ui.ActiveTab
import com.example.ui.JarvisViewModel
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisSuccess
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ToolsTab {
    APP_DIRECTOR,
    PROJECT_ARCHIVE,
    AUDIT_LOG
}

@Composable
fun ToolsAndProjectsScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ToolsTab.APP_DIRECTOR) }

    val allProjects by viewModel.projects.collectAsState()
    val historyLogs by viewModel.historyLogs.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = JarvisSurface,
            contentColor = JarvisCyan,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                    color = JarvisCyan
                )
            }
        ) {
            ToolsTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        Text(
                            text = when (tab) {
                                ToolsTab.APP_DIRECTOR -> "APPS"
                                ToolsTab.PROJECT_ARCHIVE -> "PROJECTS"
                                ToolsTab.AUDIT_LOG -> "HISTORY"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (selectedTab == tab) JarvisCyan else JarvisTextSecondary
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            ToolsTab.APP_DIRECTOR -> AppDirectorSection(viewModel)
            ToolsTab.PROJECT_ARCHIVE -> ProjectArchiveSection(viewModel, allProjects)
            ToolsTab.AUDIT_LOG -> AuditLogSection(viewModel, historyLogs)
        }
    }
}

@Composable
fun AppDirectorSection(viewModel: JarvisViewModel) {
    var searchAppText by remember { mutableStateOf("") }

    val popularApps = listOf(
        Triple("WhatsApp", "com.whatsapp", "Messaging & Calls"),
        Triple("YouTube", "com.google.android.youtube", "Video Streaming"),
        Triple("Google Chrome", "com.android.chrome", "Web Browser"),
        Triple("Telegram", "org.telegram.messenger", "Cloud Messaging"),
        Triple("Google Maps", "com.google.android.apps.maps", "Navigation & GPS"),
        Triple("Spotify", "com.spotify.music", "Music Streaming")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "APP STATUS & PLAY STORE ASSISTANT",
            color = JarvisCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchAppText,
                onValueChange = { searchAppText = it },
                placeholder = { Text("Enter app name (e.g. Netflix, Telegram)...", fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JarvisCyan,
                    unfocusedBorderColor = JarvisCardBorder,
                    focusedTextColor = JarvisTextPrimary,
                    unfocusedTextColor = JarvisTextPrimary,
                    cursorColor = JarvisCyan,
                    focusedContainerColor = JarvisSurface,
                    unfocusedContainerColor = JarvisSurface
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (searchAppText.isNotBlank()) {
                        viewModel.submitUserCommand("$searchAppText চালু করো")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan)
            ) {
                Text("Check", color = Color(0xFF001F29), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(popularApps) { (name, pkg, desc) ->
                val isInstalled = viewModel.toolManager.isPackageInstalled(pkg)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(JarvisSurface)
                        .border(1.dp, JarvisCardBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = name,
                                    color = JarvisTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isInstalled) JarvisSuccess.copy(alpha = 0.2f) else JarvisCrimson.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isInstalled) "INSTALLED" else "NOT INSTALLED",
                                        color = if (isInstalled) JarvisSuccess else JarvisCrimson,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = desc,
                                color = JarvisTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        if (isInstalled) {
                            Button(
                                onClick = { viewModel.toolManager.openApp(name, pkg) },
                                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Launch,
                                    contentDescription = "Open",
                                    tint = Color(0xFF001F29),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open", color = Color(0xFF001F29), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.toolManager.openPlayStore(name, pkg) },
                                colors = ButtonDefaults.buttonColors(containerColor = JarvisAmber)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shop,
                                    contentDescription = "Get",
                                    tint = Color(0xFF001F29),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Get", color = Color(0xFF001F29), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectArchiveSection(
    viewModel: JarvisViewModel,
    projects: List<ProjectEntity>
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SAVED WEB PROJECTS (${projects.size})",
                color = JarvisCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            Button(
                onClick = {
                    viewModel.createNewProject("Project ${System.currentTimeMillis() % 1000}")
                    viewModel.setTab(ActiveTab.CODE_STUDIO)
                },
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New",
                    tint = Color(0xFF001F29),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("New", color = Color(0xFF001F29), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (projects.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No saved projects yet. Ask JARVIS: 'একটা Ludo game বানাও'",
                    color = JarvisTextSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(projects) { proj ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(JarvisSurface)
                            .border(1.dp, JarvisCardBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.setActiveProject(proj)
                                viewModel.setTab(ActiveTab.CODE_STUDIO)
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = proj.name,
                                    color = JarvisCyan,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = proj.description,
                                    color = JarvisTextSecondary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "HTML: ${proj.htmlCode.length} chars | CSS: ${proj.cssCode.length} | JS: ${proj.jsCode.length}",
                                    color = JarvisTextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        viewModel.setActiveProject(proj)
                                        viewModel.setTab(ActiveTab.CODE_STUDIO)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Run",
                                        tint = JarvisSuccess
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteProject(proj) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = JarvisCrimson
                                    )
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
fun AuditLogSection(
    viewModel: JarvisViewModel,
    historyLogs: List<HistoryEntity>
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "COMMAND AUDIT LOG",
                color = JarvisCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            IconButton(onClick = { viewModel.clearAllCommandHistory() }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear History",
                    tint = JarvisTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (historyLogs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Audit log is empty.",
                    color = JarvisTextSecondary,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyLogs) { log ->
                    val timeStr = remember(log.timestamp) {
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(JarvisSurface)
                            .border(1.dp, JarvisCardBorder, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = log.userInput,
                                    color = JarvisTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = timeStr,
                                    color = JarvisTextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(JarvisCardBorder)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = log.intent,
                                            color = JarvisAmber,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (log.status == "SUCCESS") JarvisSuccess.copy(alpha = 0.2f) else JarvisCrimson.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = log.status,
                                            color = if (log.status == "SUCCESS") JarvisSuccess else JarvisCrimson,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Text(
                                    text = "${log.executionTimeMs}ms",
                                    color = JarvisTextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
