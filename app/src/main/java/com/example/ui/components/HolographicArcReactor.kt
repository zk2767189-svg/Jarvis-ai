package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisCyanCore
import com.example.ui.theme.JarvisGoldAccent
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HolographicArcReactor(
    modifier: Modifier = Modifier,
    isThinking: Boolean = false,
    isSpeaking: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "ArcReactorInfinite")

    // Rotation angle
    val rotationAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isThinking) 3000 else 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    // Reverse rotation angle
    val counterRotationAngle by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isThinking) 4000 else 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CounterRotation"
    )

    // Pulse size
    val pulseScale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 600 else 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Row(
        modifier = modifier.testTag("holographic_arc_reactor"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(52.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val baseRadius = size.minDimension / 2 - 4.dp.toPx()

                // Glow radial background
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            JarvisCyanCore.copy(alpha = if (isSpeaking || isThinking) 0.45f else 0.25f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * 1.3f
                    ),
                    radius = baseRadius * 1.3f
                )

                // Outer tick ring
                rotate(rotationAngle, pivot = center) {
                    drawCircle(
                        color = JarvisCyanCore.copy(alpha = 0.4f),
                        radius = baseRadius,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Draw 8 outer technological tick markers
                    for (i in 0 until 8) {
                        val angle = (i * 45) * (PI / 180)
                        val r1 = baseRadius - 2.dp.toPx()
                        val r2 = baseRadius + 2.dp.toPx()
                        val start = Offset(
                            (center.x + r1 * cos(angle)).toFloat(),
                            (center.y + r1 * sin(angle)).toFloat()
                        )
                        val end = Offset(
                            (center.x + r2 * cos(angle)).toFloat(),
                            (center.y + r2 * sin(angle)).toFloat()
                        )
                        drawLine(
                            color = JarvisCyanCore,
                            start = start,
                            end = end,
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }

                // Inner counter-rotating segmented ring
                rotate(counterRotationAngle, pivot = center) {
                    drawCircle(
                        color = JarvisCyanCore.copy(alpha = 0.7f),
                        radius = baseRadius * 0.68f,
                        style = Stroke(
                            width = 2.5.dp.toPx()
                        )
                    )
                }

                // Glowing pulsing core
                drawCircle(
                    color = if (isSpeaking) JarvisGoldAccent else JarvisCyanCore,
                    radius = (baseRadius * 0.38f) * pulseScale
                )

                // White energy center
                drawCircle(
                    color = Color.White,
                    radius = baseRadius * 0.18f
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "J.A.R.V.I.S.",
                    color = JarvisTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Status pill
                Box(
                    modifier = Modifier
                        .size(8.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = if (isThinking) JarvisGoldAccent else if (isSpeaking) JarvisCyanCore else Color(0xFF00F5A0)
                        )
                    }
                }
            }
            Text(
                text = when {
                    isThinking -> "PROCESSING TELEMETRY..."
                    isSpeaking -> "AUDIO SYNTHESIS ACTIVE"
                    else -> "SYSTEMS NOMINAL // AT YOUR SERVICE"
                },
                color = JarvisTextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.8.sp
            )
        }
    }
}
