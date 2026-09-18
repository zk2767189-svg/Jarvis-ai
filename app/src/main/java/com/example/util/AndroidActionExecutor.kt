package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log

data class ActionExecutionResult(
    val isAction: Boolean,
    val actionType: String = "",
    val success: Boolean = false,
    val statusDisplay: String = "", // e.g. "Opening TikTok..."
    val spokenResponse: String = "", // Spoken reply in user language
    val requiresPermission: String? = null,
    val permissionSettingsIntent: Intent? = null,
    val requiresConfirmation: Boolean = false,
    val confirmationPrompt: String? = null,
    val pendingActionKey: String? = null
)

class AndroidActionExecutor(
    private val context: Context,
    private val activityProvider: (() -> Activity?)? = null
) {
    companion object {
        private const val TAG = "ActionExecutor"
        val TIKTOK_PACKAGES = listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")
        val YOUTUBE_PACKAGES = listOf("com.google.android.youtube")
        val WHATSAPP_PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")
    }

    private var pendingSensitiveAction: String? = null

    fun isPendingConfirmation(): Boolean = pendingSensitiveAction != null

    fun handleConfirmation(isConfirmed: Boolean, onClearTasks: () -> Unit, onClearChat: () -> Unit): ActionExecutionResult {
        val action = pendingSensitiveAction
        pendingSensitiveAction = null

        if (!isConfirmed) {
            return ActionExecutionResult(
                isAction = true,
                actionType = "cancel_sensitive",
                success = true,
                statusDisplay = "Action Cancelled",
                spokenResponse = "Understood Boss. Action has been cancelled safely."
            )
        }

        return when (action) {
            "clear_tasks" -> {
                onClearTasks()
                ActionExecutionResult(
                    isAction = true,
                    actionType = "clear_tasks",
                    success = true,
                    statusDisplay = "Tasks Cleared",
                    spokenResponse = "Confirmed: All tasks have been cleared as requested, Boss."
                )
            }
            "clear_chat" -> {
                onClearChat()
                ActionExecutionResult(
                    isAction = true,
                    actionType = "clear_chat",
                    success = true,
                    statusDisplay = "Chat History Cleared",
                    spokenResponse = "Confirmed: Chat history cleared, Boss."
                )
            }
            else -> {
                ActionExecutionResult(
                    isAction = true,
                    actionType = "unknown",
                    success = false,
                    statusDisplay = "No pending action",
                    spokenResponse = "No pending action was awaiting confirmation, Boss."
                )
            }
        }
    }

    /**
     * Attempts to interpret and execute the spoken text as an Android Phone Action.
     * Returns result if recognized as an action command, or null if it's general AI conversation.
     */
    fun evaluateAndExecute(text: String, detectedLang: String = "en"): ActionExecutionResult? {
        val lower = text.lowercase().trim()

        // Confirmation responses
        if (pendingSensitiveAction != null) {
            if (lower.contains("yes") || lower.contains("confirm") || lower.contains("ہاں") || lower.contains("جی ہاں") || lower.contains("هو") || lower.contains("theek hai")) {
                // Handled in ViewModel with callback
                return ActionExecutionResult(
                    isAction = true,
                    actionType = "confirm_execution",
                    pendingActionKey = pendingSensitiveAction
                )
            } else if (lower.contains("no") || lower.contains("cancel") || lower.contains("نہیں") || lower.contains("نه") || lower.contains("ruko")) {
                pendingSensitiveAction = null
                return ActionExecutionResult(
                    isAction = true,
                    actionType = "cancel_execution",
                    success = true,
                    statusDisplay = "Cancelled",
                    spokenResponse = when (detectedLang) {
                        "ur" -> "کارروائی منسوخ کر دی گئی ہے، باس۔"
                        "ps" -> "عمل لغوه شو، باس."
                        "roman_ur" -> "Action cancel kar diya gaya hai, Boss."
                        else -> "Action cancelled, Boss."
                    }
                )
            }
        }

        // 1. Sensitive Irreversible Actions check
        if (lower.contains("delete all tasks") || lower.contains("clear all tasks") || lower.contains("تمام ٹاسک ختم کرو") || lower.contains("ټول کارونه پاک کړه") || lower.contains("sare tasks delete karo")) {
            pendingSensitiveAction = "clear_tasks"
            val prompt = when (detectedLang) {
                "ur" -> "کیا آپ واقعی تمام ٹاسک حذف کرنا چاہتے ہیں، باس؟ تصدیق کے لیے 'ہاں' کہیں۔"
                "ps" -> "ایا تاسو واقعیا ټول کارونه پاکول غواړئ، باس؟ د تایید لپاره 'هو' ووایاست."
                "roman_ur" -> "Kya aap waqai tamam tasks delete karna chahte hain, Boss? Confirm karne ke liye 'Yes' kahein."
                else -> "Are you sure you want to delete all tasks, Boss? Please confirm with 'Yes'."
            }
            return ActionExecutionResult(
                isAction = true,
                actionType = "request_confirmation",
                requiresConfirmation = true,
                confirmationPrompt = prompt,
                statusDisplay = "Waiting for Confirmation...",
                spokenResponse = prompt,
                pendingActionKey = "clear_tasks"
            )
        }

        if (lower.contains("clear chat history") || lower.contains("delete all messages") || lower.contains("تمام چیٹ صاف کرو") || lower.contains("ټولې خبرې پاکې کړه")) {
            pendingSensitiveAction = "clear_chat"
            val prompt = when (detectedLang) {
                "ur" -> "کیا آپ چیٹ کی تمام ہسٹری صاف کرنا چاہتے ہیں؟ تصدیق کے لیے 'ہاں' کہیں۔"
                "ps" -> "ایا تاسو غواړئ د خبرو تاریخچه پاکه کړئ؟ د تایید لپاره 'هو' ووایاست."
                "roman_ur" -> "Kya aap chat history clear karna chahte hain? Confirm karne ke liye 'Yes' kahein."
                else -> "Are you sure you want to clear the entire chat history? Please confirm with 'Yes'."
            }
            return ActionExecutionResult(
                isAction = true,
                actionType = "request_confirmation",
                requiresConfirmation = true,
                confirmationPrompt = prompt,
                statusDisplay = "Waiting for Confirmation...",
                spokenResponse = prompt,
                pendingActionKey = "clear_chat"
            )
        }

        // 2. Open TikTok
        if (lower.contains("open tiktok") || lower.contains("launch tiktok") || lower.contains("ٹک ٹاک کھولو") || lower.contains("ٹک ٹاک واز کړه") || lower.contains("tiktok kholo") || lower.contains("tiktok open")) {
            return launchApp(
                targetName = "TikTok",
                packageNames = TIKTOK_PACKAGES,
                fallbackUrl = "https://www.tiktok.com",
                detectedLang = detectedLang
            )
        }

        // 3. Open YouTube
        if (lower.contains("open youtube") || lower.contains("launch youtube") || lower.contains("یوٹیوب کھولو") || lower.contains("یوټیوب واز کړه") || lower.contains("youtube kholo") || lower.contains("youtube open")) {
            return launchApp(
                targetName = "YouTube",
                packageNames = YOUTUBE_PACKAGES,
                fallbackUrl = "https://www.youtube.com",
                detectedLang = detectedLang
            )
        }

        // 4. Open WhatsApp
        if (lower.contains("open whatsapp") || lower.contains("launch whatsapp") || lower.contains("واٹس ایپ کھولو") || lower.contains("واټس اپ واز کړه") || lower.contains("whatsapp kholo") || lower.contains("whatsapp open")) {
            return launchApp(
                targetName = "WhatsApp",
                packageNames = WHATSAPP_PACKAGES,
                fallbackUrl = null,
                detectedLang = detectedLang
            )
        }

        // 5. Open Settings
        if (lower.contains("open settings") || lower.contains("launch settings") || lower.contains("سیٹنگز کھولو") || lower.contains("ترتیبات واز کړه") || lower.contains("settings kholo") || lower.contains("settings open")) {
            return try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                val reply = when (detectedLang) {
                    "ur" -> "اینڈرائیڈ سیٹنگز کھولی جا رہی ہیں، باس۔"
                    "ps" -> "د انډرایډ ترتیبات پرانیستل کیږي، باس."
                    "roman_ur" -> "Android Settings open ki ja rahi hain, Boss."
                    else -> "Opening Android Settings now, Boss."
                }
                ActionExecutionResult(
                    isAction = true,
                    actionType = "open_settings",
                    success = true,
                    statusDisplay = "Opening Android Settings...",
                    spokenResponse = reply
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open settings", e)
                ActionExecutionResult(
                    isAction = true,
                    actionType = "open_settings",
                    success = false,
                    statusDisplay = "Error Opening Settings",
                    spokenResponse = "Could not launch Android Settings directly, Boss."
                )
            }
        }

        // 6. Go Back action
        if (lower == "go back" || lower == "back" || lower.contains("پیچھے جاؤ") || lower.contains("شاته شه") || lower.contains("back jao") || lower.contains("واپس جاؤ")) {
            val a11y = JarvisAccessibilityService.instance
            if (a11y != null) {
                val performed = a11y.performBackAction()
                val reply = when (detectedLang) {
                    "ur" -> "بیک ایکشن انجام دے دیا گیا ہے۔"
                    "ps" -> "شاته تګ ترسره شو."
                    "roman_ur" -> "Back action perform kar diya gaya hai, Boss."
                    else -> "Back action executed, Boss."
                }
                return ActionExecutionResult(
                    isAction = true,
                    actionType = "go_back",
                    success = performed,
                    statusDisplay = "Executing Android Back Action...",
                    spokenResponse = reply
                )
            } else {
                // Try activity back or instruct user about accessibility permission
                val activity = activityProvider?.invoke()
                if (activity != null) {
                    activity.runOnUiThread {
                        if (activity is androidx.activity.ComponentActivity) {
                            activity.onBackPressedDispatcher.onBackPressed()
                        } else {
                            @Suppress("DEPRECATION")
                            activity.onBackPressed()
                        }
                    }
                    val reply = when (detectedLang) {
                        "ur" -> "واپس جا رہا ہوں، باس۔"
                        "ps" -> "شاته ستنېږم، باس."
                        "roman_ur" -> "Back ja raha hoon, Boss."
                        else -> "Going back, Boss."
                    }
                    return ActionExecutionResult(
                        isAction = true,
                        actionType = "go_back",
                        success = true,
                        statusDisplay = "Going Back...",
                        spokenResponse = reply
                    )
                } else {
                    val reply = when (detectedLang) {
                        "ur" -> "سسٹم وائڈ بیک کے لیے ایکسیبلٹی سروس کی اجازت درکار ہے۔ براہ کرم سیٹنگز میں جاری رکھیں۔"
                        "ps" -> "د سیسټم شاته تګ لپاره د لاسرسي اجازې ته اړتیا ده. مهرباني وکړئ په ترتیباتو کې فعاله کړئ."
                        "roman_ur" -> "System-wide Back action ke liye Accessibility permission required hai. Please Android settings me allow karein."
                        else -> "Accessibility permission is required for global Android Back actions. Please enable JARVIS in Accessibility Settings."
                    }
                    return ActionExecutionResult(
                        isAction = true,
                        actionType = "go_back_permission_needed",
                        success = false,
                        statusDisplay = "Accessibility Permission Required",
                        spokenResponse = reply,
                        requiresPermission = "Accessibility Service",
                        permissionSettingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
            }
        }

        // 7. Close this / Minimize / Go Home
        if (lower == "close this" || lower == "close app" || lower == "minimize" || lower.contains("بند کرو") || lower.contains("بند کړه") || lower.contains("band karo") || lower.contains("go home")) {
            val a11y = JarvisAccessibilityService.instance
            if (a11y != null) {
                a11y.performHomeAction()
            }
            try {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(homeIntent)
                val reply = when (detectedLang) {
                    "ur" -> "ایپ کو منیمائز کر دیا گیا ہے، باس۔"
                    "ps" -> "اپلیکیشن کښته او بند شو، باس."
                    "roman_ur" -> "App minimize kar di gayi hai, Boss."
                    else -> "Minimizing to Home screen, Boss."
                }
                return ActionExecutionResult(
                    isAction = true,
                    actionType = "close_this",
                    success = true,
                    statusDisplay = "Minimizing to Home Screen...",
                    spokenResponse = reply
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed home minimization", e)
            }
        }

        // 8. Open Camera
        if (lower.contains("open camera") || lower.contains("launch camera") || lower.contains("کیمرہ کھولو") || lower.contains("کیمره واز کړه") || lower.contains("camera kholo")) {
            return try {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                val reply = when (detectedLang) {
                    "ur" -> "کیمرہ کھول دیا گیا ہے، باس۔"
                    "ps" -> "کیمره پرانیستل شوه، باس."
                    "roman_ur" -> "Camera open ho chuka hai, Boss."
                    else -> "Camera opened, Boss."
                }
                ActionExecutionResult(
                    isAction = true,
                    actionType = "open_camera",
                    success = true,
                    statusDisplay = "Opening Camera...",
                    spokenResponse = reply
                )
            } catch (e: Exception) {
                ActionExecutionResult(
                    isAction = true,
                    actionType = "open_camera",
                    success = false,
                    statusDisplay = "Camera Unavailable",
                    spokenResponse = "Could not open camera, Boss."
                )
            }
        }

        return null
    }

    private fun launchApp(
        targetName: String,
        packageNames: List<String>,
        fallbackUrl: String?,
        detectedLang: String
    ): ActionExecutionResult {
        val pm = context.packageManager
        for (pkg in packageNames) {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(launchIntent)
                    val reply = when (detectedLang) {
                        "ur" -> "$targetName کھول دی گئی ہے، باس۔"
                        "ps" -> "$targetName پرانیستل شوه، باس."
                        "roman_ur" -> "$targetName open kar di gayi hai, Boss."
                        else -> "Opening $targetName now, Boss."
                    }
                    return ActionExecutionResult(
                        isAction = true,
                        actionType = "launch_${targetName.lowercase()}",
                        success = true,
                        statusDisplay = "Opening $targetName...",
                        spokenResponse = reply
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error launching $pkg", e)
                }
            }
        }

        // App not installed on device: Do not fake actions!
        if (fallbackUrl != null) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                val reply = when (detectedLang) {
                    "ur" -> "$targetName ایپ انسٹال نہیں تھی، اس لیے میں نے ویب لنک کھول دیا ہے، باس۔"
                    "ps" -> "د $targetName اپلیکیشن نه و لګول شوی، ویب پاڼه پرانیستل شوه، باس."
                    "roman_ur" -> "$targetName app installed nahi thi, maine web link open kar diya hai, Boss."
                    else -> "$targetName app is not installed on this device. Opening the web link for you, Boss."
                }
                return ActionExecutionResult(
                    isAction = true,
                    actionType = "launch_${targetName.lowercase()}_web",
                    success = true,
                    statusDisplay = "Opening $targetName (Web)...",
                    spokenResponse = reply
                )
            } catch (e: Exception) {
                Log.e(TAG, "Fallback web intent failed", e)
            }
        }

        // Truthful reporting without faking
        val notInstalledReply = when (detectedLang) {
            "ur" -> "$targetName اس ڈیوائس پر انسٹال نہیں ہے۔ براہ کرم گوگل پلے اسٹور سے انسٹال کریں۔"
            "ps" -> "$targetName په دې وسیله کې نه دی لګول شوی. مهرباني وکړئ له پلې سټور څخه یې نصب کړئ."
            "roman_ur" -> "$targetName is device par installed nahi hai. Play Store se install karein."
            else -> "$targetName is not installed on this device. Please install it from the Google Play Store."
        }
        return ActionExecutionResult(
            isAction = true,
            actionType = "app_not_installed",
            success = false,
            statusDisplay = "$targetName Not Installed",
            spokenResponse = notInstalledReply
        )
    }
}
