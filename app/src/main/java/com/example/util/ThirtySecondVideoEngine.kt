package com.example.util

import com.example.data.model.ThirtySecondVideoPlan
import com.example.data.model.VideoScene
import org.json.JSONArray
import org.json.JSONObject

object ThirtySecondVideoEngine {

    fun scenesToJson(scenes: List<VideoScene>): String {
        val array = JSONArray()
        for (scene in scenes) {
            val obj = JSONObject()
            obj.put("sceneNumber", scene.sceneNumber)
            obj.put("timeRange", scene.timeRange)
            obj.put("title", scene.title)
            obj.put("characterDetails", scene.characterDetails)
            obj.put("actionDescription", scene.actionDescription)
            obj.put("cameraMovement", scene.cameraMovement)
            obj.put("lighting", scene.lighting)
            obj.put("dialogue", scene.dialogue)
            obj.put("soundFx", scene.soundFx)
            obj.put("veoKlingPrompt", scene.veoKlingPrompt)
            obj.put("visualColorAccent", scene.visualColorAccent)
            array.put(obj)
        }
        return array.toString()
    }

    fun jsonToScenes(jsonStr: String): List<VideoScene> {
        val list = mutableListOf<VideoScene>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    VideoScene(
                        sceneNumber = obj.optInt("sceneNumber", i + 1),
                        timeRange = obj.optString("timeRange", String.format("%02d:00 - %02d:05", i * 5, (i + 1) * 5)),
                        title = obj.optString("title", "Scene ${i + 1}"),
                        characterDetails = obj.optString("characterDetails", ""),
                        actionDescription = obj.optString("actionDescription", ""),
                        cameraMovement = obj.optString("cameraMovement", "Cinematic smooth tracking"),
                        lighting = obj.optString("lighting", "Volumetric atmospheric rim-light"),
                        dialogue = obj.optString("dialogue", ""),
                        soundFx = obj.optString("soundFx", "Subtle ambient hum"),
                        veoKlingPrompt = obj.optString("veoKlingPrompt", ""),
                        visualColorAccent = obj.optLong("visualColorAccent", 0xFF00E5FF)
                    )
                )
            }
        } catch (e: Exception) {
            // fallback
        }
        return list
    }

    /**
     * Parses Gemini API JSON output or constructs intelligent 30s plan
     */
    fun parseGeminiPlanResponse(
        rawResponse: String,
        storyIdea: String,
        aspectRatio: String,
        visualStyle: String
    ): ThirtySecondVideoPlan {
        try {
            // Find JSON block if Gemini wrapped in ```json ... ```
            val cleanedJson = if (rawResponse.contains("{") && rawResponse.contains("}")) {
                val start = rawResponse.indexOf('{')
                val end = rawResponse.lastIndexOf('}')
                rawResponse.substring(start, end + 1)
            } else {
                rawResponse
            }

            val root = JSONObject(cleanedJson)
            val title = root.optString("title", generateVideoTitle(storyIdea))
            val charSheet = root.optString("characterContinuitySheet", buildContinuitySheet(storyIdea))
            val scenesArray = root.optJSONArray("scenes")

            val scenes = mutableListOf<VideoScene>()
            if (scenesArray != null && scenesArray.length() > 0) {
                for (i in 0 until scenesArray.length()) {
                    val sObj = scenesArray.getJSONObject(i)
                    val sNum = sObj.optInt("sceneNumber", i + 1)
                    val startSec = (sNum - 1) * 5
                    val endSec = sNum * 5
                    val timeRange = sObj.optString("timeRange", String.format("00:%02d - 00:%02d", startSec, endSec))

                    scenes.add(
                        VideoScene(
                            sceneNumber = sNum,
                            timeRange = timeRange,
                            title = sObj.optString("title", "Scene $sNum"),
                            characterDetails = sObj.optString("characterDetails", charSheet),
                            actionDescription = sObj.optString("actionDescription", ""),
                            cameraMovement = sObj.optString("cameraMovement", "Cinematic steady push-in"),
                            lighting = sObj.optString("lighting", "Atmospheric high-contrast lighting"),
                            dialogue = sObj.optString("dialogue", ""),
                            soundFx = sObj.optString("soundFx", "Stereo soundscapes"),
                            veoKlingPrompt = sObj.optString("veoKlingPrompt", sObj.optString("actionDescription", "")),
                            visualColorAccent = getSceneAccentColor(sNum)
                        )
                    )
                }
            }

            if (scenes.size >= 4) {
                return ThirtySecondVideoPlan(
                    title = title,
                    storyIdea = storyIdea,
                    aspectRatio = aspectRatio,
                    visualStyle = visualStyle,
                    characterContinuitySheet = charSheet,
                    scenes = scenes,
                    durationSeconds = 30,
                    isComplete = true,
                    completionSummary = "Completed 30-second AI video plan with ${scenes.size} scenes in $aspectRatio."
                )
            }
        } catch (e: Exception) {
            // fallback to local generator
        }

        return generateLocalThirtySecondPlan(storyIdea, aspectRatio, visualStyle)
    }

    /**
     * Deterministic high-quality 6-scene 30-second video plan with consistent character continuity
     */
    fun generateLocalThirtySecondPlan(
        storyIdea: String,
        aspectRatio: String,
        visualStyle: String
    ): ThirtySecondVideoPlan {
        val title = generateVideoTitle(storyIdea)
        val charSheet = buildContinuitySheet(storyIdea)
        val cleanIdea = storyIdea.trim().ifBlank { "Futuristic traveler uncovering ancient secrets" }

        val scenes = listOf(
            VideoScene(
                sceneNumber = 1,
                timeRange = "00:00 - 00:05",
                title = "The Hook & Establishing Shot",
                characterDetails = "$charSheet Standing silently, silhouette framed against the expansive environment.",
                actionDescription = "The camera opens with an epic wide perspective showing $cleanIdea. Wind ripples through clothes as a sudden pulse of light illuminates the surroundings.",
                cameraMovement = "Sweeping 70mm crane shot descending from high-angle wide to eye-level dolly push.",
                lighting = "Moody atmospheric dawn illumination with deep cyan shadows and amber volumetric highlights.",
                dialogue = "\"They said it couldn't be found. They were wrong.\"",
                soundFx = "Low rumbling sub-bass chord fading into atmospheric gust of wind.",
                veoKlingPrompt = "Cinematic 8K, $aspectRatio, $visualStyle. Scene 1: Establishing wide angle. $cleanIdea. $charSheet. Volumetric lighting, 70mm anamorphic lens, photorealistic.",
                visualColorAccent = 0xFF00E5FF
            ),
            VideoScene(
                sceneNumber = 2,
                timeRange = "00:05 - 00:10",
                title = "Character Intro & First Step",
                characterDetails = "$charSheet Facial determination evident; sharp gaze focusing forward.",
                actionDescription = "The character steps forward into the primary focal area, reaching toward the central objective as energy particles begin swirling across the ground.",
                cameraMovement = "Low-angle tracking shot pacing smoothly alongside the protagonist's footsteps.",
                lighting = "Directional rim lighting emphasizing physical details, metallic textures, and fabric weave.",
                dialogue = "\"Everything begins right here.\"",
                soundFx = "Rhythmic mechanical gear clicks and accelerating energy hum.",
                veoKlingPrompt = "Cinematic 8K, $aspectRatio, $visualStyle. Scene 2: Medium tracking shot. $charSheet moving forward deliberately with focused expression, particle physics, shallow depth of field.",
                visualColorAccent = 0xFF00B0FF
            ),
            VideoScene(
                sceneNumber = 3,
                timeRange = "00:10 - 00:15",
                title = "Rising Tension & Discovery",
                characterDetails = "$charSheet Eyebrows raised in intense realization; hands reacting to the awakening catalyst.",
                actionDescription = "A sudden activation reaction unfolds. Geometric light fractures across the central artifact or environment, illuminating the protagonist's face in vivid neon reflections.",
                cameraMovement = "Medium close-up 180-degree orbital sweep maintaining character focus amidst dynamic background motion.",
                lighting = "Bioluminescent glow cast directly onto the character's facial features and signature outfit.",
                dialogue = "\"Look closely. It is awakening.\"",
                soundFx = "Crystalline frequency pulse resonating with rising orchestral crescendo.",
                veoKlingPrompt = "Cinematic 8K, $aspectRatio, $visualStyle. Scene 3: Dynamic orbital 180-degree sweep. $charSheet bathed in pulsating neon luminescence, hyper-detailed reflection in pupils.",
                visualColorAccent = 0xFF76FF03
            ),
            VideoScene(
                sceneNumber = 4,
                timeRange = "00:15 - 00:20",
                title = "The Climax & Peak Action",
                characterDetails = "$charSheet Unwavering stance, hair and coat catching turbulent atmospheric draft.",
                actionDescription = "The core transformation happens. An explosive surge of cinematic energy bursts outward in slow motion. The protagonist initiates the pivotal gesture that seals their objective.",
                cameraMovement = "High-speed 120fps slow-motion crash zoom entering directly into an extreme close-up.",
                lighting = "Dual-tone hyper-contrast strobe: blazing warm gold flash clashing with deep midnight blue.",
                dialogue = "\"No turning back now!\"",
                soundFx = "Thunderous sonic boom with high-velocity shockwave whoosh.",
                veoKlingPrompt = "Cinematic 8K, $aspectRatio, $visualStyle. Scene 4: Slow-motion 120fps climax. $charSheet executing pivotal action amid shockwaves and cinematic light bursts, blockbuster grade.",
                visualColorAccent = 0xFFFFD700
            ),
            VideoScene(
                sceneNumber = 5,
                timeRange = "00:20 - 00:25",
                title = "The Emotional Turning Point",
                characterDetails = "$charSheet Breathing calmly; solemn, victorious expression as the air settles.",
                actionDescription = "The storm of energy crystallizes into an awe-inspiring spectacle. The protagonist lowers their hand, looking toward the newly altered horizon with quiet reverence.",
                cameraMovement = "Slow backward tracking dolly zoom revealing the full breathtaking aftermath.",
                lighting = "Golden hour twilight rays breaking through dissipating vapor and smoke.",
                dialogue = "\"The world will never be the same.\"",
                soundFx = "Soulful cello melody beneath distant resonant chime echoes.",
                veoKlingPrompt = "Cinematic 8K, $aspectRatio, $visualStyle. Scene 5: Emotional wide pull-back. $charSheet looking out at transformed landscape in peaceful triumph, volumetric god rays.",
                visualColorAccent = 0xFFFF9100
            ),
            VideoScene(
                sceneNumber = 6,
                timeRange = "00:25 - 00:30",
                title = "Resolution & Dramatic Outro",
                characterDetails = "$charSheet Turned slightly over shoulder, giving a memorable parting glance.",
                actionDescription = "A master cinematic final tableau. The protagonist walks into the distance as subtle digital title graphics materialize in the negative space.",
                cameraMovement = "Slow vertical tilt upward into the starry infinite sky or dramatic skyline.",
                lighting = "Silhouetted contrast against a radiant, luminous horizon.",
                dialogue = "\"JARVIS: Sequence complete.\"",
                soundFx = "Final orchestral bass drop resonating into clean, lingering stillness.",
                veoKlingPrompt = "Cinematic 8K, $aspectRatio, $visualStyle. Scene 6: Outro wide silhouette. $charSheet walking toward glorious horizon, majestic composition, film grain, final title hook.",
                visualColorAccent = 0xFFFF3D00
            )
        )

        return ThirtySecondVideoPlan(
            title = title,
            storyIdea = storyIdea,
            aspectRatio = aspectRatio,
            visualStyle = visualStyle,
            characterContinuitySheet = charSheet,
            scenes = scenes,
            durationSeconds = 30,
            isComplete = true,
            completionSummary = "30-Second AI Video Plan successfully generated with 6 continuous scenes in $aspectRatio."
        )
    }

    private fun generateVideoTitle(idea: String): String {
        val trimmed = idea.trim()
        if (trimmed.isBlank()) return "Chronicles of Horizon: 30s Odyssey"
        val words = trimmed.split(" ").filter { it.length > 2 }.take(4)
        return if (words.isNotEmpty()) {
            words.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } } + ": 30s Cinematic"
        } else {
            "Cinematic Sequence: 30s"
        }
    }

    private fun buildContinuitySheet(idea: String): String {
        val lower = idea.lowercase()
        return when {
            lower.contains("detective") || lower.contains("noir") ->
                "Lead Protagonist: Tall, athletic detective in a weatherproof graphite trench coat with glowing amber collar piping, dark slicked-back hair, silver wrist chronometer. Consistent face structure and trench coat in all shots."
            lower.contains("cyborg") || lower.contains("robot") || lower.contains("android") ->
                "Lead Protagonist: Sleek obsidian-composite cybernetic humanoid with glowing cyan fiber-optic seams, seamless matte-black armor plates, and piercing blue ocular sensors."
            lower.contains("warrior") || lower.contains("knight") || lower.contains("fantasy") ->
                "Lead Protagonist: Battle-tested guardian in engraved brushed-steel plate armor, crimson hooded mantle, braided silver hair, carrying a luminous rune-inscribed hilt."
            lower.contains("girl") || lower.contains("woman") ->
                "Lead Protagonist: Young female protagonist with striking hazel eyes, dark braided hair with a copper bead, fitted tactical flight suit with golden hazard markings."
            else ->
                "Lead Protagonist: Heroic protagonist with determined gaze, dark hair, signature matte-charcoal tactical jacket featuring illuminated cyan telemetry seams. Exact same clothing, face and build across every scene."
        }
    }

    private fun getSceneAccentColor(sceneNumber: Int): Long {
        return when (sceneNumber) {
            1 -> 0xFF00E5FF
            2 -> 0xFF00B0FF
            3 -> 0xFF76FF03
            4 -> 0xFFFFD700
            5 -> 0xFFFF9100
            else -> 0xFFFF3D00
        }
    }
}
