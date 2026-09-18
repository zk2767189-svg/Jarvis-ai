package com.example.data.model

data class VideoScene(
    val sceneNumber: Int,
    val timeRange: String, // e.g. "00:00 - 00:05"
    val title: String,
    val characterDetails: String,
    val actionDescription: String,
    val cameraMovement: String,
    val lighting: String,
    val dialogue: String,
    val soundFx: String,
    val veoKlingPrompt: String,
    val visualColorAccent: Long = 0xFF00E5FF
)

data class ThirtySecondVideoPlan(
    val title: String,
    val storyIdea: String,
    val aspectRatio: String = "16:9", // "9:16" or "16:9"
    val visualStyle: String = "Cinematic 8K Photorealistic",
    val characterContinuitySheet: String,
    val scenes: List<VideoScene>,
    val durationSeconds: Int = 30,
    val isComplete: Boolean = true,
    val completionSummary: String = ""
)
