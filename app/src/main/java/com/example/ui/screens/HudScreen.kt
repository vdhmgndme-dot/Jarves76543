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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.JarvisViewModel
import com.example.ui.components.AssistantCoreState
import com.example.ui.components.JarvisArcReactor
import com.example.ui.components.JarvisTelemetryBar
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisNeonTeal
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun HudScreen(
    viewModel: JarvisViewModel,
    onNavigateToPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val telemetry by viewModel.telemetry.collectAsState()
    val coreState by viewModel.coreState.collectAsState()
    val audioAmp by viewModel.audioAmplitude.collectAsState()
    val lastStatus by viewModel.lastSpokenStatus.collectAsState()
    val activeModel by viewModel.selectedModel.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val quickCommands = listOf(
        "WhatsApp চালু করো",
        "YouTube খুলে Minecraft search করো",
        "Chrome খুলে HTML compiler খুঁজে দাও",
        "একটা Ludo game বানাও",
        "আমার আগের project খুলে দাও",
        "Telegram আছে?",
        "YouTube বন্ধ করো"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top HUD Telemetry
        JarvisTelemetryBar(
            telemetry = telemetry,
            activeModel = activeModel,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Futuristic Arc Reactor Core
        JarvisArcReactor(
            state = coreState,
            audioAmplitude = audioAmp,
            onClick = {
                viewModel.toggleVoiceListening()
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // State indicator & instruction
        Text(
            text = when (coreState) {
                AssistantCoreState.IDLE -> "TAP REACTOR OR MIC TO SPEAK"
                AssistantCoreState.LISTENING -> "LISTENING TO VOICE..."
                AssistantCoreState.THINKING -> "PROCESSING COMMAND // GEMINI"
                AssistantCoreState.EXECUTING -> "EXECUTING SYSTEM ACTION..."
                AssistantCoreState.SPEAKING -> "TRANSMITTING VOCAL CONFIRMATION"
                AssistantCoreState.ERROR -> "SYSTEM NOTICE // CHECK SETTINGS"
            },
            color = when (coreState) {
                AssistantCoreState.IDLE -> JarvisTextSecondary
                AssistantCoreState.LISTENING -> JarvisNeonTeal
                AssistantCoreState.THINKING -> JarvisCyan
                AssistantCoreState.EXECUTING -> JarvisAmber
                AssistantCoreState.SPEAKING -> JarvisCyan
                AssistantCoreState.ERROR -> JarvisCrimson
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Audio Waveform Visualizer
        WaveformVisualizer(
            amplitude = audioAmp,
            isSpeakingOrListening = coreState == AssistantCoreState.LISTENING || coreState == AssistantCoreState.SPEAKING
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Spoken Status Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(JarvisSurface)
                .border(1.dp, JarvisCardBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LAST SYSTEM TRANSMISSION",
                        color = JarvisCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = lastStatus,
                        color = JarvisTextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
                IconButton(
                    onClick = { viewModel.speakResponse(lastStatus) },
                    modifier = Modifier.testTag("hud_speak_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Speak status",
                        tint = JarvisCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Suggestion Chips
        Text(
            text = "QUICK DIRECTIVES (BENGALI / ENGLISH)",
            color = JarvisTextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickCommands) { cmd ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(JarvisSurfaceVariant)
                        .border(1.dp, JarvisCyan.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .clickable { viewModel.submitUserCommand(cmd) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = cmd,
                        color = JarvisCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Text & Voice Command Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = {
                    Text(
                        "Command JARVIS (Voice or Text)...",
                        color = JarvisTextSecondary.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_input_field"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = JarvisCyan,
                    unfocusedBorderColor = JarvisCardBorder,
                    focusedTextColor = JarvisTextPrimary,
                    unfocusedTextColor = JarvisTextPrimary,
                    cursorColor = JarvisCyan,
                    focusedContainerColor = JarvisSurface,
                    unfocusedContainerColor = JarvisSurface
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (textInput.isNotBlank()) {
                        viewModel.submitUserCommand(textInput)
                        textInput = ""
                    }
                })
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Push-to-Talk Mic Button
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(if (coreState == AssistantCoreState.LISTENING) JarvisCrimson else JarvisCyan)
                    .clickable { viewModel.toggleVoiceListening() }
                    .testTag("mic_toggle_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (coreState == AssistantCoreState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Microphone",
                    tint = Color(0xFF001F29),
                    modifier = Modifier.size(24.dp)
                )
            }

            if (textInput.isNotBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = {
                        viewModel.submitUserCommand(textInput)
                        textInput = ""
                    },
                    modifier = Modifier.testTag("send_command_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = JarvisCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
