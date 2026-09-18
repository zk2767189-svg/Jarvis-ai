package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.ThirtySecondVideoPlan
import com.example.data.model.VideoScene
import com.example.ui.JarvisViewModel
import com.example.ui.theme.JarvisBorderGlow
import com.example.ui.theme.JarvisCyanCore
import com.example.ui.theme.JarvisDeepNavy
import com.example.ui.theme.JarvisGoldAccent
import com.example.ui.theme.JarvisGreenSuccess
import com.example.ui.theme.JarvisSurfaceElevated
import com.example.ui.theme.JarvisSurfaceNavy
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.ui.theme.JarvisVoidBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateThirtySecondVideoModal(
    viewModel: JarvisViewModel,
    onDismiss: () -> Unit
) {
    val storyIdea by viewModel.storyIdea.collectAsState()
    val aspectRatio by viewModel.videoAspectRatio.collectAsState()
    val visualStyle by viewModel.videoVisualStyle.collectAsState()
    val isGenerating by viewModel.isVideoGenerating.collectAsState()
    val progress by viewModel.videoGenerationProgress.collectAsState()
    val stepMessage by viewModel.videoGenerationStepMessage.collectAsState()
    val currentPlan by viewModel.currentVideoPlan.collectAsState()
    val activeSceneIndex by viewModel.activeSceneIndex.collectAsState()
    val isPlaying by viewModel.isVideoPlaying.collectAsState()

    val context = LocalContext.current

    Dialog(
        onDismissRequest = {
            if (!isGenerating) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isGenerating,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = JarvisVoidBlack
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(JarvisVoidBlack, JarvisDeepNavy, JarvisVoidBlack)
                        )
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // TOP BAR
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(JarvisCyanCore.copy(alpha = 0.15f))
                                    .border(1.dp, JarvisCyanCore.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Video Generator",
                                    tint = JarvisCyanCore,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "CREATE 30s VIDEO",
                                        color = JarvisTextPrimary,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(JarvisGoldAccent.copy(alpha = 0.2f))
                                            .border(1.dp, JarvisGoldAccent.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "30s AI",
                                            color = JarvisGoldAccent,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = "6-Scene Storyboard • Character Continuity • Kling & Veo",
                                    color = JarvisTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            enabled = !isGenerating,
                            modifier = Modifier.testTag("close_video_modal_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = JarvisTextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, JarvisBorderGlow, Color.Transparent)
                                )
                            )
                    )

                    // CONTENT SWITCHER: GENERATING vs PREVIEW vs INPUT
                    if (isGenerating) {
                        VideoGenerationProgressView(
                            progress = progress,
                            stepMessage = stepMessage
                        )
                    } else if (currentPlan != null) {
                        VideoPreviewAndPlanView(
                            plan = currentPlan!!,
                            activeSceneIndex = activeSceneIndex,
                            isPlaying = isPlaying,
                            onTogglePlay = { viewModel.toggleVideoPlayback() },
                            onSelectScene = { idx -> viewModel.setActiveSceneIndex(idx) },
                            onRegenerate = { viewModel.generateThirtySecondVideo(isRegenerate = true) },
                            onEditPrompt = { viewModel.editVideoPrompt() },
                            onCreateAgain = { viewModel.createAnotherVideo() },
                            context = context
                        )
                    } else {
                        VideoInputFormView(
                            storyIdea = storyIdea,
                            aspectRatio = aspectRatio,
                            visualStyle = visualStyle,
                            onStoryIdeaChange = { viewModel.setThirtySecondStoryIdea(it) },
                            onAspectRatioChange = { viewModel.setThirtySecondAspectRatio(it) },
                            onVisualStyleChange = { viewModel.setThirtySecondVisualStyle(it) },
                            onGenerate = { viewModel.generateThirtySecondVideo() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoInputFormView(
    storyIdea: String,
    aspectRatio: String,
    visualStyle: String,
    onStoryIdeaChange: (String) -> Unit,
    onAspectRatioChange: (String) -> Unit,
    onVisualStyleChange: (String) -> Unit,
    onGenerate: () -> Unit
) {
    val sampleIdeas = listOf(
        "Cyberpunk detective tracking a rogue AI in rainy Neo-Tokyo",
        "Ancient mythical dragon awakening on a snowy mountain summit",
        "Interstellar deep-space astronaut discovering a glowing portal",
        "Coffee artisan brewing magical glowing elixir in hidden alley"
    )

    val styles = listOf(
        "Cinematic 8K Photorealistic",
        "Cyberpunk Neon Noir",
        "Anime Fantasy Makoto Shinkai",
        "IMAX 70mm Documentarian",
        "Hyper-real Unreal Engine 5"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // STORY / IDEA INPUT
        Text(
            text = "STORY OR VIDEO CONCEPT",
            color = JarvisCyanCore,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = storyIdea,
            onValueChange = onStoryIdeaChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("video_story_input"),
            placeholder = {
                Text(
                    text = "Describe your 30s video idea, protagonist, and action...",
                    color = JarvisTextMuted,
                    fontSize = 13.sp
                )
            },
            minLines = 3,
            maxLines = 5,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = JarvisSurfaceNavy,
                unfocusedContainerColor = JarvisSurfaceNavy,
                focusedBorderColor = JarvisCyanCore,
                unfocusedBorderColor = JarvisBorderGlow,
                focusedTextColor = JarvisTextPrimary,
                unfocusedTextColor = JarvisTextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // IDEA INSPIRATION CHIPS
        Text(
            text = "Quick Inspiration Concepts:",
            color = JarvisTextMuted,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sampleIdeas.forEach { idea ->
                Card(
                    onClick = { onStoryIdeaChange(idea) },
                    colors = CardDefaults.cardColors(containerColor = JarvisSurfaceElevated),
                    border = BorderStroke(1.dp, JarvisBorderGlow.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = idea,
                        color = JarvisTextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ASPECT RATIO SELECTOR: 9:16 vs 16:9
        Text(
            text = "ASPECT RATIO (PLATFORM OPTIMIZED)",
            color = JarvisCyanCore,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 9:16 Card
            val isVertical = aspectRatio == "9:16"
            Card(
                onClick = { onAspectRatioChange("9:16") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("aspect_ratio_9_16"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isVertical) JarvisCyanCore.copy(alpha = 0.15f) else JarvisSurfaceNavy
                ),
                border = BorderStroke(
                    width = if (isVertical) 1.5.dp else 1.dp,
                    color = if (isVertical) JarvisCyanCore else JarvisBorderGlow
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isVertical) JarvisCyanCore else JarvisSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = "9:16 Shorts/TikTok",
                            tint = if (isVertical) JarvisVoidBlack else JarvisTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "9:16 Vertical",
                        color = if (isVertical) JarvisCyanCore else JarvisTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Shorts • TikTok • Reels",
                        color = JarvisTextMuted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                    if (isVertical) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "SELECTED",
                            color = JarvisCyanCore,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // 16:9 Card
            val isWidescreen = aspectRatio == "16:9"
            Card(
                onClick = { onAspectRatioChange("16:9") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("aspect_ratio_16_9"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isWidescreen) JarvisCyanCore.copy(alpha = 0.15f) else JarvisSurfaceNavy
                ),
                border = BorderStroke(
                    width = if (isWidescreen) 1.5.dp else 1.dp,
                    color = if (isWidescreen) JarvisCyanCore else JarvisBorderGlow
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isWidescreen) JarvisCyanCore else JarvisSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = "16:9 YouTube",
                            tint = if (isWidescreen) JarvisVoidBlack else JarvisTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "16:9 Landscape",
                        color = if (isWidescreen) JarvisCyanCore else JarvisTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "YouTube • Cinematic TV",
                        color = JarvisTextMuted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                    if (isWidescreen) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "SELECTED",
                            color = JarvisCyanCore,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // CINEMATIC STYLE SELECTOR
        Text(
            text = "CINEMATIC VISUAL STYLE",
            color = JarvisCyanCore,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            styles.forEach { style ->
                FilterChip(
                    selected = visualStyle == style,
                    onClick = { onVisualStyleChange(style) },
                    label = { Text(style, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = JarvisCyanCore.copy(alpha = 0.2f),
                        selectedLabelColor = JarvisCyanCore,
                        containerColor = JarvisSurfaceElevated,
                        labelColor = JarvisTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = visualStyle == style,
                        borderColor = if (visualStyle == style) JarvisCyanCore else JarvisBorderGlow
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // SPECIFICATION SUMMARY BADGE
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceElevated),
            border = BorderStroke(1.dp, JarvisGoldAccent.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = JarvisGoldAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "30-Second AI Video Director Engine",
                        color = JarvisGoldAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Generates 6 continuous 5-second scenes, synchronized character anchors, camera physics, lighting, dialogue, and Kling/Veo prompt pipeline.",
                        color = JarvisTextMuted,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SUBMIT GENERATE BUTTON
        Button(
            onClick = onGenerate,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("generate_30s_video_button"),
            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyanCore),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                tint = JarvisVoidBlack,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "⚡ GENERATE 30s VIDEO",
                color = JarvisVoidBlack,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun VideoGenerationProgressView(
    progress: Float,
    stepMessage: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Holographic Reactor Ring
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(JarvisCyanCore.copy(alpha = 0.08f))
                .border(2.dp, JarvisCyanCore.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(110.dp),
                color = JarvisCyanCore,
                strokeWidth = 5.dp,
                trackColor = JarvisSurfaceElevated
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = JarvisCyanCore,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "DIRECTING",
                    color = JarvisTextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "SYNTHESIZING 30s AI VIDEO",
            color = JarvisTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stepMessage.ifBlank { "Directing scenes, lighting & character continuity..." },
            color = JarvisGoldAccent,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = JarvisCyanCore,
            trackColor = JarvisSurfaceElevated
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Progression stages checklist
        val stages = listOf(
            "Analyzing story arc & narrative tension" to (progress >= 0.15f),
            "Locking character continuity anchors" to (progress >= 0.35f),
            "Directing 6-scene 30s temporal pacing" to (progress >= 0.60f),
            "Synthesizing camera physics, lighting & dialogue" to (progress >= 0.80f),
            "Rendering master 30s video preview" to (progress >= 0.95f)
        )

        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceNavy),
            border = BorderStroke(1.dp, JarvisBorderGlow),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                stages.forEach { (label, isPassed) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPassed) Icons.Default.CheckCircle else Icons.Default.Movie,
                            contentDescription = null,
                            tint = if (isPassed) JarvisGreenSuccess else JarvisTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = label,
                            color = if (isPassed) JarvisTextPrimary else JarvisTextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (isPassed) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoPreviewAndPlanView(
    plan: ThirtySecondVideoPlan,
    activeSceneIndex: Int,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onSelectScene: (Int) -> Unit,
    onRegenerate: () -> Unit,
    onEditPrompt: () -> Unit,
    onCreateAgain: () -> Unit,
    context: Context
) {
    val activeScene = plan.scenes.getOrElse(activeSceneIndex) { plan.scenes.first() }
    val isVertical = plan.aspectRatio == "9:16"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // VIDEO PREVIEW PLAYER CONTAINER
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("video_preview_player_card"),
                colors = CardDefaults.cardColors(containerColor = JarvisVoidBlack),
                border = BorderStroke(1.5.dp, JarvisCyanCore.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Frame container with proper aspect ratio
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isVertical) {
                                    Modifier
                                        .height(360.dp)
                                        .background(JarvisVoidBlack)
                                } else {
                                    Modifier
                                        .aspectRatio(16f / 9f)
                                        .background(JarvisVoidBlack)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background visual asset
                        val drawableRes = if (isVertical) R.drawable.video_preview_vertical else R.drawable.video_preview_cinematic
                        Image(
                            painter = painterResource(id = drawableRes),
                            contentDescription = "30s Video Preview Frame",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                            contentScale = ContentScale.Crop
                        )

                        // Subtle cinematic vignette
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.5f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.75f)
                                        )
                                    )
                                )
                        )

                        // TOP HUD IN PLAYER
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(JarvisCyanCore)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "SCENE ${activeScene.sceneNumber} / 6",
                                        color = JarvisVoidBlack,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = activeScene.timeRange,
                                        color = JarvisGoldAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = plan.aspectRatio,
                                    color = JarvisTextPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // CENTER PLAY/PAUSE BUTTON OVERLAY
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(JarvisVoidBlack.copy(alpha = 0.65f))
                                .border(1.5.dp, JarvisCyanCore, CircleShape)
                                .clickable { onTogglePlay() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = JarvisCyanCore,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        // BOTTOM PLAYER HUD: DIALOGUE SUBTITLE & CAMERA BADGE
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(12.dp)
                        ) {
                            if (activeScene.dialogue.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.75f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = activeScene.dialogue,
                                        color = JarvisGoldAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeScene.title,
                                    color = JarvisTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "🎥 ${activeScene.cameraMovement}",
                                    color = JarvisCyanCore,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // 6-SCENE PROGRESS SCRUBBER TIMELINE
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(JarvisSurfaceNavy)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            plan.scenes.forEachIndexed { idx, scene ->
                                val isCurrent = idx == activeSceneIndex
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            if (isCurrent) JarvisCyanCore else JarvisSurfaceElevated
                                        )
                                        .clickable { onSelectScene(idx) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // PLAYBACK CONTROLS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        val prev = if (activeSceneIndex > 0) activeSceneIndex - 1 else 5
                                        onSelectScene(prev)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipPrevious,
                                        contentDescription = "Previous Scene",
                                        tint = JarvisTextSecondary
                                    )
                                }

                                IconButton(
                                    onClick = onTogglePlay
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = JarvisCyanCore
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val next = (activeSceneIndex + 1) % 6
                                        onSelectScene(next)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "Next Scene",
                                        tint = JarvisTextSecondary
                                    )
                                }
                            }

                            Text(
                                text = "00:${(activeSceneIndex * 5).toString().padStart(2, '0')} / 00:30",
                                color = JarvisTextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // CHARACTER CONTINUITY ANCHOR SHEET CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceElevated),
                border = BorderStroke(1.dp, JarvisBorderGlow),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = JarvisGreenSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CHARACTER & STORY CONTINUITY",
                                color = JarvisGoldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "LOCKED",
                            color = JarvisGreenSuccess,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = plan.characterContinuitySheet,
                        color = JarvisTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // SCENE BREAKDOWN HEADER
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "30s SCENE-BY-SCENE DIRECTOR PLAN",
                    color = JarvisCyanCore,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "6 Scenes • 5s each",
                    color = JarvisTextMuted,
                    fontSize = 11.sp
                )
            }
        }

        // SCENE CARDS (SCENES 1 TO 6)
        items(plan.scenes) { scene ->
            val isSelected = scene.sceneNumber - 1 == activeSceneIndex
            Card(
                onClick = { onSelectScene(scene.sceneNumber - 1) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) JarvisSurfaceNavy else JarvisSurfaceElevated
                ),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) JarvisCyanCore else JarvisBorderGlow.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) JarvisCyanCore else JarvisSurfaceElevated)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "SCENE ${scene.sceneNumber}",
                                    color = if (isSelected) JarvisVoidBlack else JarvisCyanCore,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = scene.timeRange,
                                color = JarvisGoldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Veo/Kling Prompt", scene.veoKlingPrompt)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Scene ${scene.sceneNumber} prompt copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Prompt",
                                tint = JarvisTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = scene.title,
                        color = JarvisTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = scene.actionDescription,
                        color = JarvisTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // TECHNICAL CINEMATOGRAPHY DETAILS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(JarvisVoidBlack)
                                .padding(6.dp)
                        ) {
                            Column {
                                Text("CAMERA", color = JarvisTextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text(scene.cameraMovement, color = JarvisCyanCore, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(JarvisVoidBlack)
                                .padding(6.dp)
                        ) {
                            Column {
                                Text("LIGHTING", color = JarvisTextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Text(scene.lighting, color = JarvisGoldAccent, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    if (scene.dialogue.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "💬 Dialogue: \"${scene.dialogue}\"",
                            color = JarvisTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // MANDATORY ACTION OPTIONS: REGENERATE, EDIT PROMPT, CREATE AGAIN, COPY ALL
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "VIDEO OPTIONS & ACTIONS",
                    color = JarvisCyanCore,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Regenerate Option
                    Button(
                        onClick = onRegenerate,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("video_regenerate_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyanCore),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = JarvisVoidBlack,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Regenerate",
                            color = JarvisVoidBlack,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Edit Prompt Option
                    OutlinedButton(
                        onClick = onEditPrompt,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("video_edit_prompt_button"),
                        border = BorderStroke(1.dp, JarvisCyanCore),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = JarvisCyanCore,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Edit Prompt",
                            color = JarvisCyanCore,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Create Again Option
                    OutlinedButton(
                        onClick = onCreateAgain,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("video_create_again_button"),
                        border = BorderStroke(1.dp, JarvisGoldAccent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = JarvisGoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Create Again",
                            color = JarvisGoldAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Copy All Prompts
                    OutlinedButton(
                        onClick = {
                            val allPrompts = buildString {
                                appendLine("=== 30s AI VIDEO MASTER PLAN ===")
                                appendLine("Title: ${plan.title}")
                                appendLine("Aspect Ratio: ${plan.aspectRatio}")
                                appendLine("Character Anchor: ${plan.characterContinuitySheet}")
                                appendLine()
                                plan.scenes.forEach { s ->
                                    appendLine("--- SCENE ${s.sceneNumber} (${s.timeRange}) ---")
                                    appendLine(s.veoKlingPrompt)
                                    appendLine()
                                }
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("30s Video Master Prompts", allPrompts)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "All 6 scene prompts copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("video_copy_all_button"),
                        border = BorderStroke(1.dp, JarvisBorderGlow),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = JarvisTextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Copy Plan",
                            color = JarvisTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
