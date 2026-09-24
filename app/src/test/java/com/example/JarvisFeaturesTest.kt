package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.JarvisRepository
import com.example.util.AndroidActionExecutor
import com.example.util.GeminiLiveWebSocketClient
import com.example.util.LiveCallManager
import com.example.util.LiveCallStatus
import com.example.util.VoiceManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JarvisFeaturesTest {

    private lateinit var context: Context
    private lateinit var repository: JarvisRepository
    private lateinit var actionExecutor: AndroidActionExecutor

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = JarvisRepository(context)
        actionExecutor = AndroidActionExecutor(context)
    }

    @Test
    fun testLanguageDetection() {
        // Urdu script
        val urduText = "آپ کیسے ہیں؟"
        assertEquals("ur", repository.detectLanguage(urduText))

        // Pashto script
        val pashtoText = "تاسو څنګه یاست؟"
        assertEquals("ps", repository.detectLanguage(pashtoText))

        // Roman Urdu
        val romanUrduText = "kya haal hai bhai jaan"
        assertEquals("roman_ur", repository.detectLanguage(romanUrduText))

        // English
        val englishText = "What is the current status of the project?"
        assertEquals("en", repository.detectLanguage(englishText))
    }

    @Test
    fun testPhoneActionExecutorOpenApps() {
        // TikTok
        val tiktokResult = actionExecutor.evaluateAndExecute("JARVIS, open TikTok", "en")
        assertNotNull(tiktokResult)
        assertTrue(tiktokResult!!.isAction)
        assertEquals("open_tiktok", tiktokResult.actionType)

        // YouTube
        val youtubeResult = actionExecutor.evaluateAndExecute("JARVIS, open YouTube", "en")
        assertNotNull(youtubeResult)
        assertTrue(youtubeResult!!.isAction)
        assertEquals("open_youtube", youtubeResult.actionType)

        // WhatsApp
        val whatsappResult = actionExecutor.evaluateAndExecute("JARVIS, open WhatsApp", "en")
        assertNotNull(whatsappResult)
        assertTrue(whatsappResult!!.isAction)
        assertEquals("open_whatsapp", whatsappResult.actionType)

        // Settings
        val settingsResult = actionExecutor.evaluateAndExecute("JARVIS, open Settings", "en")
        assertNotNull(settingsResult)
        assertTrue(settingsResult!!.isAction)
        assertEquals("open_settings", settingsResult.actionType)

        // Go Back
        val backResult = actionExecutor.evaluateAndExecute("JARVIS, go back", "en")
        assertNotNull(backResult)
        assertTrue(backResult!!.isAction)
        assertTrue(backResult.actionType.startsWith("go_back"))

        // Close app
        val closeResult = actionExecutor.evaluateAndExecute("JARVIS, close this", "en")
        assertNotNull(closeResult)
        assertTrue(closeResult!!.isAction)
        assertEquals("close_this", closeResult.actionType)
    }

    @Test
    fun testSensitiveActionRequiresConfirmation() {
        // Delete all tasks
        val result = actionExecutor.evaluateAndExecute("JARVIS, delete all tasks", "en")
        assertNotNull(result)
        assertTrue(result!!.isAction)
        assertTrue(result.requiresConfirmation)
        assertEquals("clear_tasks", result.pendingActionKey)
        assertTrue(actionExecutor.isPendingConfirmation())

        // Confirm
        var cleared = false
        val confirmResult = actionExecutor.handleConfirmation(
            isConfirmed = true,
            onClearTasks = { cleared = true },
            onClearChat = {}
        )
        assertTrue(cleared)
        assertTrue(confirmResult.success)
        assertFalse(actionExecutor.isPendingConfirmation())
    }

    @Test
    fun testLiveCallManagerKeywords() {
        // Stop keywords
        assertTrue(LiveCallManager.STOP_KEYWORDS.contains("stop"))
        assertTrue(LiveCallManager.STOP_KEYWORDS.contains("خاموش شه"))
        assertTrue(LiveCallManager.STOP_KEYWORDS.contains("bas"))
        assertTrue(LiveCallManager.STOP_KEYWORDS.contains("ruko"))

        // Wake keywords
        assertTrue(LiveCallManager.WAKE_KEYWORDS.contains("jarvis"))
        assertTrue(LiveCallManager.WAKE_KEYWORDS.contains("hi jarvis"))
        assertTrue(LiveCallManager.WAKE_KEYWORDS.contains("hey jarvis"))
    }

    @Test
    fun testGeminiLiveModelConstant() {
        assertEquals("models/gemini-2.5-flash-native-audio-preview-12-2025", GeminiLiveWebSocketClient.LIVE_MODEL)
    }

    @Test
    fun testTranslationFallback() = kotlinx.coroutines.runBlocking {
        val urduTranslation = repository.translateText("Hello", "en", "ur")
        assertEquals("السلام علیکم", urduTranslation)

        val pashtoTranslation = repository.translateText("Hello", "en", "ps")
        assertEquals("سلام", pashtoTranslation)

        val romanUrduTranslation = repository.translateText("Hello", "en", "roman_ur")
        assertEquals("Salam Boss", romanUrduTranslation)
    }
}
