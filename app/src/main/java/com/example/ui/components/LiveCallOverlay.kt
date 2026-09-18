package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.LiveCallStatus
import com.example.ui.theme.JarvisBorderGlow
import com.example.ui.theme.JarvisCyanCore
import com.example.ui.theme.JarvisDeepNavy
import com.example.ui.theme.JarvisGoldAccent
import com.example.ui.theme.JarvisGreenSuccess
import com.example.ui.theme.JarvisRedAlert
import com.example.ui.theme.JarvisSurfaceElevated
import com.example.ui.theme.JarvisSurfaceNavy
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.ui.theme.JarvisVoidBlack

@Composable
fun LiveCallOverlay(
    callStatus: LiveCallStatus,
    durationSeconds: Long,
    audioRms: Float,
    userTranscript: String,
    jarvisReply: String,
    detectedLanguage: String,
    isMuted: Boolean,
    preferredLanguage: String,
    actionStatus: String? = null,
    pendingConfirmation: String? = null,
    onConfirmAction: () -> Unit = {},
    onCancelAction: () -> Unit = {},
    onMuteToggle: () -> Unit,
    onInterrupt: () -> Unit,
    onEndCall: () -> Unit,
    onSelectLanguage: (String) -> Unit,
    onSendQuickVoiceCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "live_call_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "call_pulse_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hud_glow_alpha"
    )

    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val formattedDuration = String.format("%02d:%02d", minutes, seconds)

    val languageDisplayName = when (detectedLanguage) {
        "ur" -> "اردو (Urdu)"
        "ps" -> "پښتو (Pashto)"
        "roman_ur" -> "Roman Urdu"
        else -> "English"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        JarvisSurfaceNavy,
                        JarvisDeepNavy,
                        JarvisVoidBlack
                    )
                )
            )
            .testTag("live_call_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP BAR: STATUS, CALL TIMER & LANGUAGE PILL
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Neural Voice Link badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(JarvisGreenSuccess.copy(alpha = 0.15f))
                            .borderBadge(JarvisGreenSuccess)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (callStatus == LiveCallStatus.SPEAKING) JarvisGoldAccent else JarvisGreenSuccess)
                                .scale(pulseScale)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE NEURAL CALL",
                            color = JarvisGreenSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Call duration timer
                    Text(
                        text = formattedDuration,
                        color = JarvisTextPrimary,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("live_call_timer")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Language indicator pill with quick selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(JarvisSurfaceElevated)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Language",
                        tint = JarvisCyanCore,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Auto-Language: $languageDisplayName",
                        color = JarvisCyanCore,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. CENTER SECTION: HOLOGRAPHIC REACTOR + DYNAMIC SOUND WAVE EQUALIZER
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Arc Reactor Visualizer
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(170.dp)
                        .scale(if (callStatus == LiveCallStatus.SPEAKING) pulseScale else 1f)
                ) {
                    HolographicArcReactor(
                        isThinking = callStatus == LiveCallStatus.THINKING,
                        isSpeaking = callStatus == LiveCallStatus.SPEAKING,
                        modifier = Modifier.size(170.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Sound Wave Equalizer Bars
                AudioWaveVisualizer(
                    callStatus = callStatus,
                    audioRms = audioRms,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(38.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Status Label
                val (statusText, statusColor) = when (callStatus) {
                    LiveCallStatus.IDLE -> "Standby" to JarvisTextMuted
                    LiveCallStatus.CONNECTING -> "⚡ Initializing Neural Voice Link..." to JarvisTextSecondary
                    LiveCallStatus.RECONNECTING -> "🔄 Reconnecting to Gemini Live Link..." to JarvisGoldAccent
                    LiveCallStatus.LISTENING -> "🎙️ Listening... (Say 'JARVIS', ask or command)" to JarvisCyanCore
                    LiveCallStatus.THINKING -> "⚡ JARVIS Telemetry Processing..." to JarvisGoldAccent
                    LiveCallStatus.SPEAKING -> "🔊 JARVIS Speaking (Tap Stop or say 'خاموش شه')" to JarvisGreenSuccess
                    LiveCallStatus.INTERRUPTED -> "⏸️ Interrupted — Standing by for you, Boss" to JarvisGoldAccent
                    LiveCallStatus.ERROR -> "⚠️ Connection Error — Tap Retry or Reconnect" to JarvisRedAlert
                    LiveCallStatus.ENDED -> "Call Ended" to JarvisTextMuted
                }

                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("live_call_status_text")
                )
            }

            // ACTION STATUS BANNER (e.g. Opening TikTok..., Go Back, Confirmation)
            AnimatedVisibility(visible = !actionStatus.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .testTag("live_call_action_status_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (pendingConfirmation != null) JarvisGoldAccent.copy(alpha = 0.15f)
                        else JarvisCyanCore.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (pendingConfirmation != null) JarvisGoldAccent else JarvisCyanCore
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (pendingConfirmation != null) JarvisGoldAccent else JarvisCyanCore,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = actionStatus.orEmpty(),
                            color = if (pendingConfirmation != null) JarvisGoldAccent else JarvisCyanCore,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        if (pendingConfirmation != null) {
                            Button(
                                onClick = onConfirmAction,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = JarvisGreenSuccess,
                                    contentColor = JarvisVoidBlack
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp).testTag("live_call_confirm_button")
                            ) {
                                Text("CONFIRM", fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = onCancelAction,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = JarvisRedAlert,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp).testTag("live_call_cancel_button")
                            ) {
                                Text("CANCEL", fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. LIVE TRANSCRIPTION HUD CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_call_transcript_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceElevated.copy(alpha = 0.9f)),
                border = BorderStroke(1.dp, JarvisBorderGlow)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Boss voice transcript
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "User",
                            tint = JarvisCyanCore,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (userTranscript.isNotBlank()) userTranscript else "Speak anytime in English, Urdu, Pashto, or Roman Urdu...",
                            color = if (userTranscript.isNotBlank()) JarvisTextPrimary else JarvisTextMuted,
                            fontSize = 13.sp,
                            fontWeight = if (userTranscript.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.testTag("user_transcript_text")
                        )
                    }

                    if (jarvisReply.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "JARVIS",
                                tint = JarvisGoldAccent,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = jarvisReply,
                                color = JarvisGoldAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 18.sp,
                                modifier = Modifier.testTag("jarvis_reply_transcript")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. QUICK VOICE ACTION SHORTCUT CHIPS
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "VOICE SHORTCUTS",
                    color = JarvisTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickCommands = listOf(
                        "📱 Open TikTok" to "JARVIS, open TikTok",
                        "▶️ Open YouTube" to "JARVIS, open YouTube",
                        "💬 Open WhatsApp" to "JARVIS, open WhatsApp",
                        "⚙️ Settings" to "JARVIS, open Settings",
                        "◀️ Go Back" to "JARVIS, go back",
                        "🏠 Close App" to "JARVIS, close this",
                        "📷 Camera" to "JARVIS, open camera",
                        "✨ Quote of the day" to "Tell me the quote of the day",
                        "📝 Add task" to "Add task: Review project telemetry",
                        "🇵🇰 اردو میں بولو" to "اردو میں بات کرو",
                        "🇦🇫 پښتو کې وغږېږه" to "پښتو کې خبرې وکړه",
                        "⏸️ Stop / خاموش شه" to "Stop"
                    )

                    items(quickCommands) { (label, command) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(JarvisSurfaceElevated)
                                .borderBadge(JarvisBorderGlow)
                                .clickable { onSendQuickVoiceCommand(command) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("quick_voice_chip_$label")
                        ) {
                            Text(
                                text = label,
                                color = JarvisCyanCore,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. BOTTOM CALL CONTROLS: MUTE, INTERRUPT, AND END CALL
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute / Unmute Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onMuteToggle,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) JarvisRedAlert.copy(alpha = 0.2f) else JarvisSurfaceElevated)
                            .borderBadge(if (isMuted) JarvisRedAlert else JarvisBorderGlow)
                            .testTag("toggle_mute_call_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isMuted) "Unmute Microphone" else "Mute Microphone",
                            tint = if (isMuted) JarvisRedAlert else JarvisTextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isMuted) "Unmute" else "Mute",
                        color = JarvisTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Immediate Interruption Button (e.g. Stop / خاموش شه)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onInterrupt,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(JarvisSurfaceElevated)
                            .borderBadge(JarvisGoldAccent)
                            .testTag("interrupt_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Interrupt / Stop JARVIS",
                            tint = JarvisGoldAccent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Interrupt",
                        color = JarvisGoldAccent,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // End Call Button (Big Red FAB)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onEndCall,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(JarvisRedAlert)
                            .testTag("end_live_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Live Call",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "End Call",
                        color = JarvisRedAlert,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioWaveVisualizer(
    callStatus: LiveCallStatus,
    audioRms: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_bars")
    val waveAnimation by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_anim"
    )

    val isVisualizing = callStatus == LiveCallStatus.LISTENING || callStatus == LiveCallStatus.SPEAKING
    val baseBarColor = if (callStatus == LiveCallStatus.SPEAKING) JarvisGoldAccent else JarvisCyanCore

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val barCount = 12
        for (i in 0 until barCount) {
            val factor = ((i * 3 + 1) % 5 + 1) / 5f
            val heightFraction = if (isVisualizing) {
                // Multiply RMS or animation wave
                val dynamicHeight = (audioRms * factor * 1.5f).coerceIn(0.15f, 1.0f)
                dynamicHeight * waveAnimation
            } else {
                0.12f
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((34 * heightFraction).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(baseBarColor.copy(alpha = if (isVisualizing) 0.9f else 0.3f))
            )
        }
    }
}

private fun Modifier.borderBadge(color: Color): Modifier = this.then(
    Modifier.background(
        Brush.horizontalGradient(listOf(color.copy(alpha = 0.3f), color.copy(alpha = 0.1f))),
        RoundedCornerShape(20.dp)
    )
)
