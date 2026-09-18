package com.example

import com.example.data.api.QuoteApiClient
import com.example.util.LiveCallManager
import com.example.util.LiveCallStatus
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testQuoteCuratedArchiveHasRequiredCategories() {
    val quotes = QuoteApiClient.CURATED_DAILY_QUOTES
    assertTrue("Quotes archive should not be empty", quotes.isNotEmpty())

    val categories = quotes.map { it.category }.toSet()
    assertTrue("Quotes must include Inspirational", categories.contains("Inspirational"))
    assertTrue("Quotes must include Thought-Provoking", categories.contains("Thought-Provoking"))
    assertTrue("Quotes must include Humorous", categories.contains("Humorous"))

    // Each quote must have author and non-blank quote text
    quotes.forEach { q ->
      assertTrue("Quote text must not be blank", q.quote.isNotBlank())
      assertTrue("Author must not be blank", q.author.isNotBlank())
    }
  }

  @Test
  fun testQuoteDateFormatting() {
    val client = QuoteApiClient()
    val formatted = client.getTodayDateFormatted()
    assertTrue("Date should follow yyyy-MM-dd format", formatted.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
  }

  @Test
  fun testLiveCallKeywords() {
    val stopKeywords = LiveCallManager.STOP_KEYWORDS
    assertTrue("Must support English 'stop'", stopKeywords.contains("stop"))
    assertTrue("Must support English 'wait'", stopKeywords.contains("wait"))
    assertTrue("Must support Pashto 'خاموش شه'", stopKeywords.contains("خاموش شه"))
    assertTrue("Must support Roman Urdu / Urdu 'bas'", stopKeywords.contains("bas"))

    val wakeKeywords = LiveCallManager.WAKE_KEYWORDS
    assertTrue("Must support wake word 'jarvis'", wakeKeywords.contains("jarvis"))
  }

  @Test
  fun testLiveCallStatusEnum() {
    val statuses = LiveCallStatus.values().map { it.name }
    assertTrue(statuses.contains("LISTENING"))
    assertTrue(statuses.contains("SPEAKING"))
    assertTrue(statuses.contains("INTERRUPTED"))
    assertTrue(statuses.contains("ENDED"))
  }

  @Test
  fun testLanguageScriptRecognition() {
    val urduText = "آج کا دن کیسا ہے"
    val pashtoText = "څنګه یاست وروره مننه"
    val romanUrduText = "kya haal hai bhai mujhe task banao"
    val englishText = "What is the time right now?"

    // Verify script properties
    assertTrue(urduText.any { Character.UnicodeBlock.of(it) == Character.UnicodeBlock.ARABIC })
    assertTrue(pashtoText.contains("څنګه") || pashtoText.contains("مننه"))
    assertTrue(romanUrduText.contains("kya") && romanUrduText.contains("bhai"))
    assertTrue(englishText.startsWith("What"))
  }

  @Test
  fun testThirtySecondVideoEngineGeneratesSixScenesAndThirtySeconds() {
    val plan = com.example.util.ThirtySecondVideoEngine.generateLocalThirtySecondPlan(
      "Cyberpunk courier delivers glowing artifact",
      "9:16",
      "Cyberpunk Neon Noir"
    )

    assertEquals(30, plan.durationSeconds)
    assertEquals(6, plan.scenes.size)
    assertEquals("9:16", plan.aspectRatio)
    assertTrue(plan.characterContinuitySheet.isNotBlank())

    plan.scenes.forEachIndexed { index, scene ->
      assertEquals(index + 1, scene.sceneNumber)
      assertTrue("Scene action description must not be blank", scene.actionDescription.isNotBlank())
      assertTrue("Camera movement must not be blank", scene.cameraMovement.isNotBlank())
      assertTrue("Lighting must not be blank", scene.lighting.isNotBlank())
      assertTrue("Prompt must be non-empty", scene.veoKlingPrompt.isNotBlank())
    }
  }

  @Test
  fun testThirtySecondVideoSupportsAspectRatios() {
    val verticalPlan = com.example.util.ThirtySecondVideoEngine.generateLocalThirtySecondPlan(
      "Space exploration",
      "9:16",
      "Photorealistic 8K"
    )
    assertEquals("9:16", verticalPlan.aspectRatio)

    val landscapePlan = com.example.util.ThirtySecondVideoEngine.generateLocalThirtySecondPlan(
      "Space exploration",
      "16:9",
      "Photorealistic 8K"
    )
    assertEquals("16:9", landscapePlan.aspectRatio)
  }
}

