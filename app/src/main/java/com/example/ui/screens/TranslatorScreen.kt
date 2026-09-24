package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import com.example.util.VoiceManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

data class SupportedLanguage(val code: String, val name: String, val nativeName: String)

val AVAILABLE_TRANSLATE_LANGUAGES = listOf(
    SupportedLanguage("en", "English", "English"),
    SupportedLanguage("ur", "Urdu", "اردو"),
    SupportedLanguage("roman_ur", "Roman Urdu", "Roman Urdu"),
    SupportedLanguage("ps", "Pashto", "پښتو"),
    SupportedLanguage("ar", "Arabic", "العربية"),
    SupportedLanguage("es", "Spanish", "Español"),
    SupportedLanguage("fr", "French", "Français"),
    SupportedLanguage("de", "German", "Deutsch"),
    SupportedLanguage("zh", "Chinese", "中文"),
    SupportedLanguage("hi", "Hindi", "हिन्दी")
)

@Composable
fun TranslatorScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var sourceText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var sourceLang by remember { mutableStateOf(AVAILABLE_TRANSLATE_LANGUAGES[0]) } // English
    var targetLang by remember { mutableStateOf(AVAILABLE_TRANSLATE_LANGUAGES[1]) } // Urdu
    var isTranslating by remember { mutableStateOf(false) }

    val isSpeaking by viewModel.voiceManager.isSpeaking.collectAsState()

    // Speech recognizer launcher for dictating text to translate
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenSpans = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognized = spokenSpans?.firstOrNull()
            if (!recognized.isNullOrBlank()) {
                sourceText = recognized
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(JarvisVoidBlack, JarvisDeepNavy)
                )
            )
            .navigationBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // TOP HUD HEADER
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceNavy),
            border = BorderStroke(1.dp, JarvisBorderGlow),
            modifier = Modifier.fillMaxWidth().testTag("translator_header_card")
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(JarvisCyanCore.copy(alpha = 0.15f))
                        .border(1.dp, JarvisCyanCore, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "JARVIS Neural Translator",
                        tint = JarvisCyanCore,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "JARVIS NEURAL TRANSLATOR",
                        color = JarvisCyanCore,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Real-time multilingual translation across English, Urdu, Roman Urdu & Pashto",
                        color = JarvisTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // LANGUAGE SELECTOR BAR
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceNavy),
            border = BorderStroke(1.dp, JarvisBorderGlow),
            modifier = Modifier.fillMaxWidth().testTag("language_selector_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Source Language Label
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FROM",
                        color = JarvisTextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${sourceLang.name} (${sourceLang.nativeName})",
                        color = JarvisCyanCore,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Swap Button
                IconButton(
                    onClick = {
                        val temp = sourceLang
                        sourceLang = targetLang
                        targetLang = temp
                        val tempText = sourceText
                        sourceText = translatedText
                        translatedText = tempText
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(JarvisSurfaceElevated)
                        .testTag("swap_languages_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Swap Languages",
                        tint = JarvisGoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Target Language Label
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "TO",
                        color = JarvisTextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${targetLang.name} (${targetLang.nativeName})",
                        color = JarvisGoldAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // QUICK TARGET LANGUAGE CHIPS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AVAILABLE_TRANSLATE_LANGUAGES.forEach { lang ->
                val isSelected = targetLang.code == lang.code
                FilterChip(
                    selected = isSelected,
                    onClick = { targetLang = lang },
                    label = { Text("${lang.name} • ${lang.nativeName}", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = JarvisGoldAccent,
                        selectedLabelColor = JarvisVoidBlack,
                        containerColor = JarvisSurfaceElevated,
                        labelColor = JarvisTextPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) JarvisGoldAccent else JarvisBorderGlow,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SOURCE TEXT INPUT CARD
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceNavy),
            border = BorderStroke(1.dp, JarvisBorderGlow),
            modifier = Modifier.fillMaxWidth().testTag("source_input_card")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ENTER TEXT OR SPEAK",
                        color = JarvisTextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Row {
                        IconButton(
                            onClick = {
                                try {
                                    val intent = VoiceManager.createSpeechIntent()
                                    speechLauncher.launch(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Voice input not available", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(32.dp).testTag("translator_mic_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Speak Text",
                                tint = JarvisCyanCore,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (sourceText.isNotBlank()) {
                            IconButton(
                                onClick = { sourceText = "" },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Text",
                                    tint = JarvisTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = sourceText,
                    onValueChange = { sourceText = it },
                    placeholder = {
                        Text(
                            text = "Type or dictate text here (e.g., 'What are today's goals?' or 'آج کا کیا شیڈول ہے؟')...",
                            color = JarvisTextMuted,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("source_text_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyanCore,
                        unfocusedBorderColor = JarvisBorderGlow,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (sourceText.isBlank()) {
                            Toast.makeText(context, "Please enter or speak text to translate, Boss", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isTranslating = true
                        viewModel.translateText(
                            text = sourceText,
                            fromLang = sourceLang.code,
                            toLang = targetLang.code
                        ) { result ->
                            isTranslating = false
                            translatedText = result
                        }
                    },
                    enabled = !isTranslating && sourceText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JarvisCyanCore,
                        contentColor = JarvisVoidBlack
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("execute_translate_button")
                ) {
                    if (isTranslating) {
                        CircularProgressIndicator(
                            color = JarvisVoidBlack,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TRANSLATING...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TRANSLATE TO ${targetLang.name.uppercase()}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // TRANSLATED RESULT CARD
        AnimatedVisibility(visible = translatedText.isNotBlank() || isTranslating) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceNavy),
                border = BorderStroke(1.dp, JarvisGoldAccent),
                modifier = Modifier.fillMaxWidth().testTag("translation_result_card")
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
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(JarvisGreenSuccess)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "RESULT (${targetLang.name.uppercase()})",
                                color = JarvisGoldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row {
                            // Pronounce / Speak Translation
                            IconButton(
                                onClick = {
                                    if (translatedText.isNotBlank()) {
                                        viewModel.voiceManager.speak(translatedText)
                                    }
                                },
                                modifier = Modifier.size(32.dp).testTag("speak_translation_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Speak Translation",
                                    tint = if (isSpeaking) JarvisGoldAccent else JarvisCyanCore,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Copy Translation to Clipboard
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("JARVIS Translation", translatedText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Translation copied to clipboard, Boss", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp).testTag("copy_translation_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Translation",
                                    tint = JarvisTextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = translatedText,
                        color = JarvisTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp,
                        modifier = Modifier.testTag("translated_result_text")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // QUICK PHRASES CARD
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = JarvisSurfaceNavy),
            border = BorderStroke(1.dp, JarvisBorderGlow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "QUICK CONVERSATION SHORTCUTS",
                    color = JarvisCyanCore,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                val samplePhrases = listOf(
                    "Hello Boss, how can I help you today?",
                    "What are the highest priority tasks on the agenda?",
                    "Could you summarize the recent developments?",
                    "At your command, Boss."
                )

                samplePhrases.forEach { phrase ->
                    Text(
                        text = "• \"$phrase\"",
                        color = JarvisTextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                sourceText = phrase
                                sourceLang = AVAILABLE_TRANSLATE_LANGUAGES[0] // English
                                targetLang = AVAILABLE_TRANSLATE_LANGUAGES[1] // Urdu
                            }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
