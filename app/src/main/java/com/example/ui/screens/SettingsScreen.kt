package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.data.ModelConfig
import com.example.data.SpeechLanguage
import com.example.ui.JarvisViewModel
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun SettingsScreen(
    viewModel: JarvisViewModel,
    onNavigateToPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedModel by viewModel.selectedModel.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val speechRate by viewModel.speechRate.collectAsState()
    val developerMode by viewModel.developerMode.collectAsState()
    val apiKeyInput by viewModel.apiKeyInput.collectAsState()
    val fgServiceEnabled by viewModel.foregroundServiceEnabled.collectAsState()

    var modelMenuExpanded by remember { mutableStateOf(false) }
    var langMenuExpanded by remember { mutableStateOf(false) }
    var tempApiKey by remember { mutableStateOf(apiKeyInput) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "SYSTEM CONFIGURATION // SETTINGS",
            color = JarvisCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 1. AI Model Selection
        SettingCard(title = "AI BRAIN MODEL") {
            Box {
                Button(
                    onClick = { modelMenuExpanded = true },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceVariant),
                    modifier = Modifier.fillMaxWidth().testTag("model_select_btn")
                ) {
                    Text(
                        text = ModelConfig.AVAILABLE_MODELS.find { it.id == selectedModel }?.displayName ?: selectedModel,
                        color = JarvisCyan,
                        fontSize = 13.sp
                    )
                }

                DropdownMenu(
                    expanded = modelMenuExpanded,
                    onDismissRequest = { modelMenuExpanded = false }
                ) {
                    ModelConfig.AVAILABLE_MODELS.forEach { opt ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(opt.displayName, fontWeight = FontWeight.Bold)
                                    Text(opt.description, fontSize = 11.sp, color = JarvisTextSecondary)
                                }
                            },
                            onClick = {
                                viewModel.setSelectedModel(opt.id)
                                modelMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Custom Gemini API Key
        SettingCard(title = "GEMINI API KEY CONFIGURATION") {
            Text(
                text = "Keys are securely injected via Secrets panel. You can also override here for testing:",
                color = JarvisTextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = tempApiKey,
                    onValueChange = { tempApiKey = it },
                    placeholder = { Text("Paste custom API key...", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f).testTag("api_key_input"),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisCardBorder,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary,
                        focusedContainerColor = JarvisSurfaceVariant,
                        unfocusedContainerColor = JarvisSurfaceVariant
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Button(
                    onClick = { viewModel.setCustomApiKey(tempApiKey) },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                    modifier = Modifier.testTag("save_api_key_btn")
                ) {
                    Text("Save", color = Color(0xFF001F29), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Voice Language & TTS
        SettingCard(title = "VOICE ENGINE & LOCALIZATION") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Voice Language", color = JarvisTextPrimary, fontSize = 13.sp)
                Box {
                    Button(
                        onClick = { langMenuExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceVariant)
                    ) {
                        Text(selectedLanguage.label, color = JarvisCyan, fontSize = 12.sp)
                    }
                    DropdownMenu(
                        expanded = langMenuExpanded,
                        onDismissRequest = { langMenuExpanded = false }
                    ) {
                        SpeechLanguage.values().forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang.label) },
                                onClick = {
                                    viewModel.setSelectedLanguage(lang)
                                    langMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Speech Speed Rate", color = JarvisTextPrimary, fontSize = 13.sp)
                    Text(
                        "${String.format("%.1f", speechRate)}x",
                        color = JarvisCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
                Slider(
                    value = speechRate,
                    onValueChange = { viewModel.setSpeechRate(it) },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = JarvisCyan,
                        activeTrackColor = JarvisCyan,
                        inactiveTrackColor = JarvisCardBorder
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Background Assistant Service
        SettingCard(title = "BACKGROUND ARCHITECTURE") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Foreground Core Service", color = JarvisTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Maintains a persistent notification to keep JARVIS ready for screen-off voice calls without draining battery.",
                        color = JarvisTextSecondary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = fgServiceEnabled,
                    onCheckedChange = { viewModel.toggleForegroundService(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyan)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Developer Mode & Diagnostics
        SettingCard(title = "DEVELOPER MODE & TELEMETRY") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Raw JSON & Diagnostic Mode", color = JarvisTextPrimary, fontSize = 13.sp)
                    Text("Displays raw structured actions and intent timing in conversation.", color = JarvisTextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = developerMode,
                    onCheckedChange = { viewModel.setDeveloperMode(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = JarvisCyan)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onNavigateToPermissions,
                colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Manage System Permissions & Capabilities", color = JarvisCyan)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 6. Privacy & Security Assurance
        SettingCard(title = "PRIVACY & LOCAL STORAGE") {
            Text(
                text = "• Zero Stealth Tracking: Microphone is accessed only when initiated by tap or push-to-talk.\n" +
                        "• Android Security Compliance: Third-party apps are launched legitimately and never force-stopped without system mediation.\n" +
                        "• All coding files are stored locally in Room database on your device.",
                color = JarvisTextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.clearChatHistory() },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisSurfaceVariant)
                ) {
                    Text("Clear Chat Memory", color = JarvisTextPrimary, fontSize = 11.sp)
                }
                Button(
                    onClick = { viewModel.clearAllCommandHistory() },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCrimson.copy(alpha = 0.3f))
                ) {
                    Text("Clear All Logs", color = JarvisCrimson, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SettingCard(
    title: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(JarvisSurface)
            .border(1.dp, JarvisCardBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = title,
                color = JarvisAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
