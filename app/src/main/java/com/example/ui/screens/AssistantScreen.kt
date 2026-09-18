package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.JarvisTab
import com.example.ui.JarvisViewModel
import com.example.ui.components.ChatMessageBubble
import com.example.ui.components.HolographicArcReactor
import com.example.ui.components.LiveCallOverlay
import com.example.ui.components.QuoteOfTheDayCard
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
import com.example.util.VoiceManager

@Composable
fun AssistantScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val currentSpeakingId by viewModel.currentSpeakingId.collectAsState()
    val actionStatus by viewModel.actionStatus.collectAsState()
    val pendingConfirmationAction by viewModel.pendingConfirmationAction.collectAsState()

    // Quote of the Day State
    val quoteOfTheDay by viewModel.quoteOfTheDay.collectAsState()
    val isQuoteLoading by viewModel.isQuoteLoading.collectAsState()
    val isQuoteDismissed by viewModel.isQuoteDismissed.collectAsState()

    // Live Voice Call State
    val isLiveCallOpen by viewModel.isLiveCallOpen.collectAsState()
    val liveCallStatus by viewModel.liveCallStatus.collectAsState()
    val liveCallDurationSeconds by viewModel.liveCallDurationSeconds.collectAsState()
    val liveAudioRms by viewModel.liveAudioRms.collectAsState()
    val liveUserTranscript by viewModel.liveUserTranscript.collectAsState()
    val liveJarvisReply by viewModel.liveJarvisReply.collectAsState()
    val liveDetectedLanguage by viewModel.liveDetectedLanguage.collectAsState()
    val isLiveCallMuted by viewModel.isLiveCallMuted.collectAsState()
    val preferredLiveLanguage by viewModel.preferredLiveLanguage.collectAsState()

    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Permission launcher for Live Voice Call
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startLiveCall()
        } else {
            Toast.makeText(context, "Microphone access required for Live Voice Call, Boss", Toast.LENGTH_SHORT).show()
        }
    }

    // Start every session by listening for user's voice command
    LaunchedEffect(Unit) {
        val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        )
        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            viewModel.startLiveCall()
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Scroll to latest message
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Photo picker launcher (zero-permission Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onImageSelected(uri)
            Toast.makeText(context, "Image attached for analysis, Boss", Toast.LENGTH_SHORT).show()
        }
    }

    // Speech recognition launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenSpans = result.data?.getStringArrayListExtra(
                android.speech.RecognizerIntent.EXTRA_RESULTS
            )
            val spokenText = spokenSpans?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.onInputTextChanged(spokenText)
                viewModel.sendMessage(spokenText)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(JarvisVoidBlack, JarvisDeepNavy)
                    )
                )
                .navigationBarsPadding()
                .imePadding()
        ) {
            // TOP HUD BAR
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceNavy),
                border = BorderStroke(1.dp, JarvisBorderGlow)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HolographicArcReactor(
                        isThinking = isLoading,
                        isSpeaking = isSpeaking
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Prominent Live Call Voice Mode Trigger Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.horizontalGradient(listOf(JarvisCyanCore, Color(0xFF0077FF))))
                                .clickable {
                                    recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("start_live_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneInTalk,
                                contentDescription = "Start Live Call",
                                tint = JarvisVoidBlack,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE CALL",
                                color = JarvisVoidBlack,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // 🎬 "30s VIDEO" Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(JarvisGoldAccent.copy(alpha = 0.2f))
                                .border(1.dp, JarvisGoldAccent, RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.openCreate30sVideo()
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("assistant_create_30s_video_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Create 30s Video",
                                tint = JarvisGoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "30s VIDEO",
                                color = JarvisGoldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Clear chat button
                        IconButton(
                            onClick = { viewModel.clearChat() },
                            modifier = Modifier.size(36.dp).testTag("clear_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Chat",
                                tint = JarvisTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // ACTION STATUS BANNER (e.g. "Opening TikTok...", "Executing Android Back Action...", Confirmation)
            AnimatedVisibility(visible = !actionStatus.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("action_status_banner"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (pendingConfirmationAction != null) JarvisGoldAccent.copy(alpha = 0.18f) else JarvisCyanCore.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, if (pendingConfirmationAction != null) JarvisGoldAccent else JarvisCyanCore)
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
                            tint = if (pendingConfirmationAction != null) JarvisGoldAccent else JarvisCyanCore,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = actionStatus.orEmpty(),
                            color = if (pendingConfirmationAction != null) JarvisGoldAccent else JarvisCyanCore,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        if (pendingConfirmationAction != null) {
                            Button(
                                onClick = { viewModel.confirmSensitiveAction() },
                                colors = ButtonDefaults.buttonColors(containerColor = JarvisGreenSuccess, contentColor = JarvisVoidBlack),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp).testTag("action_confirm_button")
                            ) {
                                Text("CONFIRM", fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = { viewModel.cancelSensitiveAction() },
                                colors = ButtonDefaults.buttonColors(containerColor = JarvisRedAlert, contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp).testTag("action_cancel_button")
                            ) {
                                Text("CANCEL", fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }

            // VOICE HERO CARD: Large Mic, "JARVIS Listening...", Waveform, Recognized Command, Spoken Response, Settings
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .testTag("voice_hero_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceNavy),
                border = BorderStroke(1.dp, JarvisBorderGlow)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Large Hero Microphone Button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(
                                    if (liveCallStatus == com.example.util.LiveCallStatus.LISTENING)
                                        Brush.radialGradient(listOf(JarvisCyanCore, Color(0xFF0055AA)))
                                    else if (isSpeaking)
                                        Brush.radialGradient(listOf(JarvisGoldAccent, Color(0xFF996600)))
                                    else
                                        Brush.linearGradient(listOf(JarvisSurfaceElevated, JarvisDeepNavy))
                                )
                                .border(
                                    2.dp,
                                    if (liveCallStatus == com.example.util.LiveCallStatus.LISTENING) JarvisCyanCore
                                    else if (isSpeaking) JarvisGoldAccent
                                    else JarvisBorderGlow,
                                    CircleShape
                                )
                                .clickable {
                                    recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                                .testTag("large_hero_mic_button")
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.RecordVoiceOver else Icons.Default.Mic,
                                contentDescription = "Live Voice Mode",
                                tint = if (liveCallStatus == com.example.util.LiveCallStatus.LISTENING || isSpeaking) JarvisVoidBlack else JarvisCyanCore,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Center Status & Waveform
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (liveCallStatus == com.example.util.LiveCallStatus.LISTENING) JarvisGreenSuccess
                                            else if (isSpeaking) JarvisGoldAccent
                                            else JarvisCyanCore
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (liveCallStatus == com.example.util.LiveCallStatus.LISTENING) "JARVIS LISTENING…"
                                    else if (isSpeaking) "JARVIS SPEAKING…"
                                    else "LIVE VOICE • \"HI JARVIS\"",
                                    color = if (liveCallStatus == com.example.util.LiveCallStatus.LISTENING) JarvisGreenSuccess
                                    else if (isSpeaking) JarvisGoldAccent
                                    else JarvisCyanCore,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Animated Waveform Bars
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.height(16.dp)
                            ) {
                                val activeRms = if (liveCallStatus == com.example.util.LiveCallStatus.LISTENING) liveAudioRms else if (isSpeaking) 6f else 1f
                                for (barIdx in 0..14) {
                                    val dynamicHeight = (4 + ((barIdx * 7) % 12) * (activeRms / 8f).coerceIn(0.2f, 1.2f)).dp
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(dynamicHeight)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                if (isSpeaking) JarvisGoldAccent
                                                else if (liveCallStatus == com.example.util.LiveCallStatus.LISTENING) JarvisCyanCore
                                                else JarvisTextMuted.copy(alpha = 0.4f)
                                            )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Quick Settings Button for Language, Voice & Permissions
                        IconButton(
                            onClick = { viewModel.setTab(JarvisTab.SETTINGS) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(JarvisSurfaceElevated)
                                .testTag("quick_settings_shortcut")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Voice & Permissions Settings",
                                tint = JarvisCyanCore,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Recognized User Voice Command
                    if (liveUserTranscript.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Recognized: ",
                                color = JarvisTextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "\"$liveUserTranscript\"",
                                color = JarvisTextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                modifier = Modifier.testTag("hero_recognized_command_text")
                            )
                        }
                    }

                    // Spoken JARVIS Response
                    if (liveJarvisReply.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "JARVIS: ",
                                color = JarvisGoldAccent,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "\"$liveJarvisReply\"",
                                color = JarvisGoldAccent.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                modifier = Modifier.testTag("hero_spoken_reply_text")
                            )
                        }
                    }
                }
            }

            // PROMINENT QUOTE OF THE DAY FEATURE
            QuoteOfTheDayCard(
                quote = quoteOfTheDay,
                isLoading = isQuoteLoading,
                isDismissed = isQuoteDismissed,
                onToggleDismissed = { viewModel.toggleQuoteDismissed() },
                onRefresh = { viewModel.loadQuoteOfTheDay(forceRefresh = true) },
                onSpeak = { viewModel.speakQuote(it) },
                onToggleFavorite = { id, fav -> viewModel.toggleQuoteFavorite(id, fav) }
            )

            // QUICK CAPABILITY & PHONE ACTION CHIPS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // PHONE ACTIONS
                AssistChip(
                    onClick = { viewModel.sendMessage("JARVIS, open TikTok") },
                    label = { Text("📱 TikTok", fontSize = 12.sp, color = JarvisCyanCore) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = JarvisSurfaceElevated),
                    border = AssistChipDefaults.assistChipBorder(borderColor = JarvisCyanCore.copy(alpha = 0.5f), enabled = true),
                    modifier = Modifier.testTag("chip_open_tiktok")
                )
                AssistChip(
                    onClick = { viewModel.sendMessage("JARVIS, open YouTube") },
                    label = { Text("▶️ YouTube", fontSize = 12.sp, color = JarvisCyanCore) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = JarvisSurfaceElevated),
                    border = AssistChipDefaults.assistChipBorder(borderColor = JarvisCyanCore.copy(alpha = 0.5f), enabled = true),
                    modifier = Modifier.testTag("chip_open_youtube")
                )
                AssistChip(
                    onClick = { viewModel.sendMessage("JARVIS, open WhatsApp") },
                    label = { Text("💬 WhatsApp", fontSize = 12.sp, color = JarvisCyanCore) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = JarvisSurfaceElevated),
                    border = AssistChipDefaults.assistChipBorder(borderColor = JarvisCyanCore.copy(alpha = 0.5f), enabled = true),
                    modifier = Modifier.testTag("chip_open_whatsapp")
                )
                AssistChip(
                    onClick = { viewModel.sendMessage("JARVIS, open Settings") },
                    label = { Text("⚙️ Settings", fontSize = 12.sp, color = JarvisCyanCore) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = JarvisSurfaceElevated),
                    border = AssistChipDefaults.assistChipBorder(borderColor = JarvisCyanCore.copy(alpha = 0.5f), enabled = true),
                    modifier = Modifier.testTag("chip_open_settings")
                )
                AssistChip(
                    onClick = { viewModel.sendMessage("JARVIS, go back") },
                    label = { Text("◀️ Go Back", fontSize = 12.sp, color = JarvisGoldAccent) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = JarvisSurfaceElevated),
                    border = AssistChipDefaults.assistChipBorder(borderColor = JarvisGoldAccent.copy(alpha = 0.5f), enabled = true),
                    modifier = Modifier.testTag("chip_go_back")
                )
                AssistChip(
                    onClick = { viewModel.sendMessage("JARVIS, close this") },
                    label = { Text("🏠 Close App", fontSize = 12.sp, color = JarvisGoldAccent) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = JarvisSurfaceElevated),
                    border = AssistChipDefaults.assistChipBorder(borderColor = JarvisGoldAccent.copy(alpha = 0.5f), enabled = true),
                    modifier = Modifier.testTag("chip_close_this")
                )
                AssistChip(
                    onClick = { viewModel.openCreate30sVideo() },
                    label = { Text("🎬 30s Video", fontSize = 12.sp, color = JarvisGoldAccent) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = JarvisSurfaceElevated),
                    border = AssistChipDefaults.assistChipBorder(borderColor = JarvisGoldAccent.copy(alpha = 0.5f), enabled = true),
                    modifier = Modifier.testTag("chip_create_30s_video")
                )
                AssistChip(
                    onClick = {
                        viewModel.triggerQuickAction("What is the inspirational Quote of the Day, JARVIS?")
                    },
                    label = { Text("✨ Quote of the Day", fontSize = 12.sp, color = JarvisGoldAccent) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = JarvisSurfaceElevated),
                    border = AssistChipDefaults.assistChipBorder(borderColor = JarvisGoldAccent.copy(alpha = 0.5f), enabled = true),
                    modifier = Modifier.testTag("quick_quote_chip")
                )
            AssistChip(
                onClick = {
                    viewModel.triggerQuickAction("Video Prompt: Cyberpunk futuristic detective in rainy neon Tokyo with cinematic lighting")
                },
                label = { Text("🎬 Video Prompt", fontSize = 12.sp, color = JarvisCyanCore) },
                colors = AssistChipDefaults.assistChipColors(containerColor = JarvisSurfaceElevated),
                border = AssistChipDefaults.assistChipBorder(borderColor = JarvisBorderGlow, enabled = true),
                modifier = Modifier.testTag("quick_video_prompt_chip")
            )

            AssistChip(
                onClick = {
                    viewModel.triggerQuickAction("Automate daily YouTube video planning, script creation and social media task pipeline")
                },
                label = { Text("⚡ Automation Plan", fontSize = 12.sp, color = JarvisGoldAccent) },
                colors = AssistChipDefaults.assistChipColors(containerColor = JarvisSurfaceElevated),
                border = AssistChipDefaults.assistChipBorder(borderColor = JarvisBorderGlow, enabled = true),
                modifier = Modifier.testTag("quick_automation_chip")
            )

            AssistChip(
                onClick = {
                    viewModel.triggerQuickAction("YouTube titles, description, tags and full script for: The Future of AI Agents in 2026")
                },
                label = { Text("📹 YouTube Studio", fontSize = 12.sp, color = JarvisTextPrimary) },
                colors = AssistChipDefaults.assistChipColors(containerColor = JarvisSurfaceElevated),
                border = AssistChipDefaults.assistChipBorder(borderColor = JarvisBorderGlow, enabled = true)
            )

            AssistChip(
                onClick = {
                    viewModel.triggerQuickAction("السلام علیکم باس! مجھے آج کا مکمل شیڈول اور اہداف سمجھا دیں۔")
                },
                label = { Text("اردو میں گفتگو", fontSize = 12.sp, color = JarvisCyanCore) },
                colors = AssistChipDefaults.assistChipColors(containerColor = JarvisSurfaceElevated),
                border = AssistChipDefaults.assistChipBorder(borderColor = JarvisBorderGlow, enabled = true)
            )

            AssistChip(
                onClick = {
                    viewModel.triggerQuickAction("Salam Boss! Aaj ka video script aur tasks plan karein.")
                },
                label = { Text("Roman Urdu", fontSize = 12.sp, color = JarvisTextPrimary) },
                colors = AssistChipDefaults.assistChipColors(containerColor = JarvisSurfaceElevated),
                border = AssistChipDefaults.assistChipBorder(borderColor = JarvisBorderGlow, enabled = true)
            )

            AssistChip(
                onClick = {
                    viewModel.triggerQuickAction("سلام باس! ماته د یوټیوب او نوې پروژې په اړه لارښوونه وکړئ.")
                },
                label = { Text("پښتو مرسته", fontSize = 12.sp, color = JarvisCyanCore) },
                colors = AssistChipDefaults.assistChipColors(containerColor = JarvisSurfaceElevated),
                border = AssistChipDefaults.assistChipBorder(borderColor = JarvisBorderGlow, enabled = true)
            )
        }

        // CHAT CONVERSATION STREAM
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items = messages, key = { it.id }) { message ->
                ChatMessageBubble(
                    message = message,
                    isSpeaking = isSpeaking && currentSpeakingId == message.id,
                    onSpeakClick = { viewModel.speakMessage(message) }
                )
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .testTag("thinking_indicator"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = JarvisCyanCore,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "JARVIS is formulating telemetry & computing response...",
                            color = JarvisTextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // ATTACHED IMAGE PREVIEW
        AnimatedVisibility(
            visible = selectedImageUri != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (selectedImageUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceElevated),
                        border = BorderStroke(1.dp, JarvisCyanCore)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Attached thumbnail",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Asset Ready for Analysis",
                                    color = JarvisCyanCore,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "JARVIS will inspect visual telemetry",
                                    color = JarvisTextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            IconButton(
                                onClick = { viewModel.clearSelectedImage() },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove photo",
                                    tint = JarvisTextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // INPUT CONSOLE
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceNavy),
            border = BorderStroke(1.dp, JarvisBorderGlow)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Photo Attachment button (Android Photo Picker)
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.size(42.dp).testTag("attach_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Attach Photo",
                        tint = if (selectedImageUri != null) JarvisCyanCore else JarvisTextSecondary
                    )
                }

                // Voice input mic button
                IconButton(
                    onClick = {
                        try {
                            val intent = VoiceManager.createSpeechIntent()
                            speechLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Speech recognition not available on this device", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(42.dp).testTag("voice_mic_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Speak to JARVIS",
                        tint = if (isSpeaking) JarvisGoldAccent else JarvisTextSecondary
                    )
                }

                // Text Input field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.onInputTextChanged(it) },
                    placeholder = {
                        Text(
                            text = "Command JARVIS, Boss...",
                            color = JarvisTextMuted,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary,
                        cursorColor = JarvisCyanCore
                    ),
                    maxLines = 4
                )

                // Send button
                IconButton(
                    onClick = { viewModel.sendMessage() },
                    enabled = inputText.isNotBlank() || selectedImageUri != null,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (inputText.isNotBlank() || selectedImageUri != null)
                                Brush.linearGradient(listOf(JarvisCyanCore, Color(0xFF0077FF)))
                            else Brush.linearGradient(listOf(JarvisSurfaceElevated, JarvisSurfaceElevated)),
                            CircleShape
                        )
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Command",
                        tint = if (inputText.isNotBlank() || selectedImageUri != null)
                            JarvisVoidBlack else JarvisTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // FULL SCREEN LIVE VOICE CALL OVERLAY
    AnimatedVisibility(
        visible = isLiveCallOpen,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        LiveCallOverlay(
            callStatus = liveCallStatus,
            durationSeconds = liveCallDurationSeconds,
            audioRms = liveAudioRms,
            userTranscript = liveUserTranscript,
            jarvisReply = liveJarvisReply,
            detectedLanguage = liveDetectedLanguage,
            isMuted = isLiveCallMuted,
            preferredLanguage = preferredLiveLanguage,
            actionStatus = actionStatus,
            pendingConfirmation = pendingConfirmationAction,
            onConfirmAction = { viewModel.confirmSensitiveAction() },
            onCancelAction = { viewModel.cancelSensitiveAction() },
            onMuteToggle = { viewModel.toggleLiveCallMute() },
            onInterrupt = { viewModel.interruptLiveCall() },
            onEndCall = { viewModel.endLiveCall() },
            onSelectLanguage = { viewModel.setLiveCallLanguage(it) },
            onSendQuickVoiceCommand = { viewModel.liveCallManager.processSpokenInput(it) }
        )
    }
}
}
