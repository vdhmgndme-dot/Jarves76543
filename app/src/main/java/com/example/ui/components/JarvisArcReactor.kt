package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanGlow
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisNeonTeal
import kotlin.math.cos
import kotlin.math.sin

enum class AssistantCoreState {
    IDLE,
    LISTENING,
    THINKING,
    EXECUTING,
    SPEAKING,
    ERROR
}

@Composable
fun JarvisArcReactor(
    state: AssistantCoreState,
    audioAmplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "reactor_rotations")

    // Outer ring rotation
    val outerAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rotation"
    )

    // Inner counter-rotation (speeds up when thinking)
    val innerSpeed = if (state == AssistantCoreState.THINKING) 2000 else 8000
    val innerAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(innerSpeed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rotation"
    )

    // Gentle pulse
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val primaryColor = when (state) {
        AssistantCoreState.IDLE -> JarvisCyan
        AssistantCoreState.LISTENING -> JarvisNeonTeal
        AssistantCoreState.THINKING -> JarvisElectricBlue
        AssistantCoreState.EXECUTING -> JarvisAmber
        AssistantCoreState.SPEAKING -> JarvisCyan
        AssistantCoreState.ERROR -> JarvisCrimson
    }

    val stateText = when (state) {
        AssistantCoreState.IDLE -> "JARVIS // IDLE"
        AssistantCoreState.LISTENING -> "LISTENING"
        AssistantCoreState.THINKING -> "THINKING"
        AssistantCoreState.EXECUTING -> "EXECUTING"
        AssistantCoreState.SPEAKING -> "SPEAKING"
        AssistantCoreState.ERROR -> "SYSTEM ALERT"
    }

    Box(
        modifier = modifier
            .size(240.dp)
            .testTag("jarvis_arc_reactor")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) - 16.dp.toPx()

            // 1. Hologram Glow Background
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f + audioAmplitude * 0.3f),
                        primaryColor.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.2f
                ),
                radius = baseRadius * 1.2f,
                center = center
            )

            // 2. Outermost Static HUD Ring
            drawCircle(
                color = primaryColor.copy(alpha = 0.25f),
                radius = baseRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 3. Segmented Rotating Outer Ring (Dashed arcs)
            val outerDashSegments = 8
            for (i in 0 until outerDashSegments) {
                val startAngle = outerAngle + (i * 45f)
                drawArc(
                    color = primaryColor.copy(alpha = 0.6f),
                    startAngle = startAngle,
                    sweepAngle = 28f,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius + 8.dp.toPx(), center.y - baseRadius + 8.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size((baseRadius - 8.dp.toPx()) * 2, (baseRadius - 8.dp.toPx()) * 2),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 4. Middle Arc Reactor Track (Counter-rotating)
            val midRadius = baseRadius * 0.72f
            drawCircle(
                color = primaryColor.copy(alpha = 0.2f),
                radius = midRadius,
                center = center,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), innerAngle)
                )
            )

            // 5. Reactive Waveform Nodes (react to microphone amplitude or speaking)
            val effectiveAmp = if (state == AssistantCoreState.LISTENING || state == AssistantCoreState.SPEAKING) {
                audioAmplitude.coerceIn(0.1f, 1f)
            } else {
                0.08f
            }

            val nodeCount = 12
            for (i in 0 until nodeCount) {
                val angleDeg = (i * (360f / nodeCount)) + innerAngle
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val nodeOffsetDist = midRadius + (sin(angleRad * 2 + outerAngle * 0.05).toFloat() * 10.dp.toPx() * effectiveAmp)
                val nodePos = Offset(
                    center.x + (nodeOffsetDist * cos(angleRad)).toFloat(),
                    center.y + (nodeOffsetDist * sin(angleRad)).toFloat()
                )

                drawCircle(
                    color = primaryColor,
                    radius = 3.dp.toPx() + (effectiveAmp * 3.dp.toPx()),
                    center = nodePos
                )
            }

            // 6. Core Inner Glow Arc Reactor
            val coreRadius = baseRadius * 0.42f * pulseScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        primaryColor,
                        primaryColor.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )

            drawCircle(
                color = primaryColor,
                radius = coreRadius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Center HUD State Label
        Text(
            text = stateText,
            color = primaryColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
