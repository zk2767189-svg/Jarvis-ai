package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.AutomationHubScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.screens.TranslatorScreen
import com.example.ui.screens.VideoPromptStudioScreen
import com.example.ui.components.CreateThirtySecondVideoModal
import com.example.ui.theme.JarvisCyanCore
import com.example.ui.theme.JarvisDeepNavy
import com.example.ui.theme.JarvisSurfaceElevated
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.ui.theme.JarvisVoidBlack

@Composable
fun JarvisApp(
    viewModel: JarvisViewModel = viewModel()
) {
    val currentTab by viewModel.currentTab.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_navigation_bar"),
                containerColor = JarvisDeepNavy,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == JarvisTab.ASSISTANT,
                    onClick = { viewModel.setTab(JarvisTab.ASSISTANT) },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "JARVIS Chat") },
                    label = { Text("JARVIS", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JarvisVoidBlack,
                        selectedTextColor = JarvisCyanCore,
                        indicatorColor = JarvisCyanCore,
                        unselectedIconColor = JarvisTextSecondary,
                        unselectedTextColor = JarvisTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_assistant")
                )

                NavigationBarItem(
                    selected = currentTab == JarvisTab.VIDEO_STUDIO,
                    onClick = { viewModel.setTab(JarvisTab.VIDEO_STUDIO) },
                    icon = { Icon(Icons.Default.Videocam, contentDescription = "Video Studio") },
                    label = { Text("Video", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JarvisVoidBlack,
                        selectedTextColor = JarvisCyanCore,
                        indicatorColor = JarvisCyanCore,
                        unselectedIconColor = JarvisTextSecondary,
                        unselectedTextColor = JarvisTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_video")
                )

                NavigationBarItem(
                    selected = currentTab == JarvisTab.AUTOMATION,
                    onClick = { viewModel.setTab(JarvisTab.AUTOMATION) },
                    icon = { Icon(Icons.Default.FlashOn, contentDescription = "Automation") },
                    label = { Text("Auto", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JarvisVoidBlack,
                        selectedTextColor = JarvisCyanCore,
                        indicatorColor = JarvisCyanCore,
                        unselectedIconColor = JarvisTextSecondary,
                        unselectedTextColor = JarvisTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_automation")
                )

                NavigationBarItem(
                    selected = currentTab == JarvisTab.TRANSLATOR,
                    onClick = { viewModel.setTab(JarvisTab.TRANSLATOR) },
                    icon = { Icon(Icons.Default.Translate, contentDescription = "Translator") },
                    label = { Text("Translate", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JarvisVoidBlack,
                        selectedTextColor = JarvisCyanCore,
                        indicatorColor = JarvisCyanCore,
                        unselectedIconColor = JarvisTextSecondary,
                        unselectedTextColor = JarvisTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_translator")
                )

                NavigationBarItem(
                    selected = currentTab == JarvisTab.TASKS,
                    onClick = { viewModel.setTab(JarvisTab.TASKS) },
                    icon = { Icon(Icons.Default.FolderSpecial, contentDescription = "Tasks") },
                    label = { Text("Tasks", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JarvisVoidBlack,
                        selectedTextColor = JarvisCyanCore,
                        indicatorColor = JarvisCyanCore,
                        unselectedIconColor = JarvisTextSecondary,
                        unselectedTextColor = JarvisTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_tasks")
                )

                NavigationBarItem(
                    selected = currentTab == JarvisTab.SETTINGS,
                    onClick = { viewModel.setTab(JarvisTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Config", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JarvisVoidBlack,
                        selectedTextColor = JarvisCyanCore,
                        indicatorColor = JarvisCyanCore,
                        unselectedIconColor = JarvisTextSecondary,
                        unselectedTextColor = JarvisTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentTab, label = "TabSwitch") { tab ->
                when (tab) {
                    JarvisTab.ASSISTANT -> AssistantScreen(viewModel = viewModel)
                    JarvisTab.VIDEO_STUDIO -> VideoPromptStudioScreen(viewModel = viewModel)
                    JarvisTab.AUTOMATION -> AutomationHubScreen(viewModel = viewModel)
                    JarvisTab.TRANSLATOR -> TranslatorScreen(viewModel = viewModel)
                    JarvisTab.TASKS -> TasksScreen(viewModel = viewModel)
                    JarvisTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }

            val isCreate30sVideoOpen by viewModel.isCreate30sVideoOpen.collectAsState()
            if (isCreate30sVideoOpen) {
                CreateThirtySecondVideoModal(
                    viewModel = viewModel,
                    onDismiss = { viewModel.closeCreate30sVideo() }
                )
            }
        }
    }
}
