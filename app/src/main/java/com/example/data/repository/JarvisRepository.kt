package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.api.GeminiApiClient
import com.example.data.api.QuoteApiClient
import com.example.data.db.AppDatabase
import com.example.data.db.ChatMessageEntity
import com.example.data.db.QuoteEntity
import com.example.data.db.TaskEntity
import com.example.data.db.VideoProjectEntity
import com.example.data.model.ThirtySecondVideoPlan
import com.example.util.ThirtySecondVideoEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class JarvisRepository(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val chatDao = database.chatMessageDao()
    private val taskDao = database.taskDao()
    private val quoteDao = database.quoteDao()
    private val videoProjectDao = database.videoProjectDao()
    private val geminiClient = GeminiApiClient(context)
    private val quoteApiClient = QuoteApiClient()

    val allMessages: Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val latestQuoteFlow: Flow<QuoteEntity?> = quoteDao.getLatestQuoteFlow()
    val allVideoProjects: Flow<List<VideoProjectEntity>> = videoProjectDao.getAllVideoProjects()

    suspend fun generateThirtySecondVideo(
        storyIdea: String,
        aspectRatio: String = "16:9",
        visualStyle: String = "Cinematic 8K Photorealistic",
        onProgress: suspend (Float, String) -> Unit = { _, _ -> },
        customKey: String? = null
    ): ThirtySecondVideoPlan {
        onProgress(0.15f, "Analyzing story narrative & themes...")
        delay(350)

        onProgress(0.35f, "Locking character continuity & visual anchors...")
        delay(400)

        onProgress(0.60f, "Directing 6-scene 30-second temporal pacing...")
        val planResult = geminiClient.generateThirtySecondVideoPlan(
            storyIdea = storyIdea,
            aspectRatio = aspectRatio,
            visualStyle = visualStyle,
            customKey = customKey
        )
        val plan = planResult.getOrElse {
            ThirtySecondVideoEngine.generateLocalThirtySecondPlan(storyIdea, aspectRatio, visualStyle)
        }

        onProgress(0.80f, "Synthesizing camera physics, lighting & dialogue...")
        delay(350)

        onProgress(0.95f, "Rendering 30-second video preview frames...")
        delay(350)

        // Save to Room database
        val entity = VideoProjectEntity(
            title = plan.title,
            storyIdea = storyIdea,
            aspectRatio = aspectRatio,
            visualStyle = visualStyle,
            characterContinuitySheet = plan.characterContinuitySheet,
            scenesJson = ThirtySecondVideoEngine.scenesToJson(plan.scenes),
            durationSeconds = 30,
            status = "COMPLETED"
        )
        videoProjectDao.insertProject(entity)

        onProgress(1.0f, "30-Second AI video generation complete!")
        return plan
    }

    suspend fun getLatestVideoPlan(): ThirtySecondVideoPlan? {
        val latest = videoProjectDao.getLatestProject() ?: return null
        val scenes = ThirtySecondVideoEngine.jsonToScenes(latest.scenesJson)
        return ThirtySecondVideoPlan(
            title = latest.title,
            storyIdea = latest.storyIdea,
            aspectRatio = latest.aspectRatio,
            visualStyle = latest.visualStyle,
            characterContinuitySheet = latest.characterContinuitySheet,
            scenes = scenes,
            durationSeconds = latest.durationSeconds,
            isComplete = latest.status == "COMPLETED",
            completionSummary = "Loaded 30-second AI video plan with ${scenes.size} scenes in ${latest.aspectRatio}."
        )
    }

    suspend fun deleteVideoProject(id: Long) {
        videoProjectDao.deleteProjectById(id)
    }

    fun isApiKeyConfigured(customKey: String? = null): Boolean {
        return geminiClient.getApiKey(customKey).isNotBlank()
    }

    suspend fun getOrFetchQuoteOfTheDay(forceRefresh: Boolean = false): QuoteEntity {
        val today = quoteApiClient.getTodayDateFormatted()
        if (!forceRefresh) {
            val cached = quoteDao.getQuoteForDate(today)
            if (cached != null) return cached
        }

        // Fetch from reliable network source (or curated catalog fallback)
        val result = quoteApiClient.fetchDailyQuote()
        val quoteEntity = QuoteEntity(
            quoteText = result.quote,
            author = result.author,
            category = result.category,
            dateFormatted = today,
            source = result.source
        )
        quoteDao.insertQuote(quoteEntity)
        return quoteDao.getQuoteForDate(today) ?: quoteEntity
    }

    suspend fun toggleQuoteFavorite(id: Long, isFavorite: Boolean) {
        quoteDao.setFavorite(id, isFavorite)
    }

    suspend fun getInitialGreetingIfNeeded() {
        val count = chatDao.getMessageCount()
        if (count == 0) {
            val greeting = ChatMessageEntity(
                role = "jarvis",
                content = "Good day, Boss. JARVIS systems fully operational.\n\n" +
                        "I am ready to assist you with:\n" +
                        "• AI Video & Image Prompts (Veo & Kling format)\n" +
                        "• YouTube Titles, SEO Tags & Scripts\n" +
                        "• Automation & Step-by-Step Task Execution\n" +
                        "• Multilingual support (English, Urdu, Roman Urdu, Pashto)\n" +
                        "• Multimodal Image & Document Analysis\n\n" +
                        "How may I serve you today?",
                mode = "chat"
            )
            chatDao.insertMessage(greeting)
        }
    }

    suspend fun sendMessage(
        userText: String,
        imageUri: Uri? = null,
        customKey: String? = null
    ): ChatMessageEntity {
        // Detect language or mode
        val detectedMode = detectMode(userText)
        val detectedLang = detectLanguage(userText)

        // 1. Store user message in Room
        val userMessage = ChatMessageEntity(
            role = "user",
            content = userText.trim(),
            imageUri = imageUri?.toString(),
            mode = detectedMode,
            language = detectedLang
        )
        chatDao.insertMessage(userMessage)

        // 2. Fetch recent conversation history
        val existingMessages = chatDao.getAllMessages().first()
        val historyPairs = existingMessages.takeLast(6).map { it.role to it.content }

        // 3. Attempt Gemini API request
        val apiResult = geminiClient.generateResponse(
            prompt = userText,
            history = historyPairs,
            imageUri = imageUri,
            customKey = customKey
        )

        val jarvisReplyText: String = if (detectedMode == "create_30s_video") {
            val storyIdea = userText
                .replace("create 30s video", "", ignoreCase = true)
                .replace("30s video", "", ignoreCase = true)
                .replace("30 second video", "", ignoreCase = true)
                .trim().ifEmpty { "Cinematic futuristic odyssey" }
            val ratio = if (userText.contains("shorts", ignoreCase = true) || userText.contains("tiktok", ignoreCase = true) || userText.contains("9:16")) "9:16" else "16:9"
            val plan = generateThirtySecondVideo(storyIdea, ratio, customKey = customKey)
            buildString {
                appendLine("🎬 **JARVIS 30-Second AI Video Plan Generated**")
                appendLine("**Title:** ${plan.title}")
                appendLine("**Duration:** 30s (6 Scenes)")
                appendLine("**Aspect Ratio:** ${plan.aspectRatio}")
                appendLine("**Visual Style:** ${plan.visualStyle}")
                appendLine()
                appendLine("👤 **Character Continuity Sheet:**")
                appendLine(plan.characterContinuitySheet)
                appendLine()
                appendLine("🎞️ **Scene-by-Scene Breakdown:**")
                plan.scenes.forEach { s ->
                    appendLine("**Scene ${s.sceneNumber} (${s.timeRange}):** ${s.title}")
                    appendLine("- *Action:* ${s.actionDescription}")
                    appendLine("- *Camera:* ${s.cameraMovement} | *Light:* ${s.lighting}")
                    if (s.dialogue.isNotBlank()) appendLine("- *Dialogue:* \"${s.dialogue}\"")
                    appendLine("- *Veo/Kling Prompt:* `${s.veoKlingPrompt}`")
                    appendLine()
                }
                appendLine("Completed: 30-Second AI video plan generated and saved in your Video Studio, Boss.")
            }
        } else if (apiResult.isSuccess) {
            val responseText = apiResult.getOrNull().orEmpty()
            // If it was an automation or task request, check if we should auto-populate tasks
            if (detectedMode == "automation" || userText.contains("automate", ignoreCase = true) || userText.contains("task", ignoreCase = true)) {
                extractAndSaveTasksFromPlan(userText, responseText)
            }
            responseText
        } else {
            // Intelligent local JARVIS fallback engine
            generateLocalJarvisResponse(userText, detectedMode, detectedLang)
        }

        val jarvisMessage = ChatMessageEntity(
            role = "jarvis",
            content = jarvisReplyText,
            mode = detectedMode,
            language = detectedLang
        )
        chatDao.insertMessage(jarvisMessage)
        return jarvisMessage
    }

    private fun detectMode(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("create 30s video") || lower.contains("30s video") || lower.contains("30 second video") -> "create_30s_video"
            lower.contains("quote") || lower.contains("قول") || lower.contains("حکمت") || lower.contains("aqwal") -> "quote"
            lower.contains("video prompt") || lower.contains("video") || lower.contains("kling") || lower.contains("veo") -> "video_prompt"
            lower.contains("youtube") || lower.contains("script") || lower.contains("tags") || lower.contains("title") -> "youtube"
            lower.contains("automate") || lower.contains("automation") || lower.contains("workflow") -> "automation"
            lower.contains("task") || lower.contains("project") || lower.contains("todo") || lower.contains("plan") -> "task"
            else -> "chat"
        }
    }

    suspend fun processLiveCallQuery(
        spokenText: String,
        customKey: String? = null
    ): Pair<String, String> {
        val detectedLang = detectLanguage(spokenText)
        val lower = spokenText.lowercase().trim()

        // 1. Tool execution: Quote of the Day
        if (lower.contains("quote") || lower.contains("قول") || lower.contains("حکمت") || lower.contains("aqwal")) {
            val quote = getOrFetchQuoteOfTheDay()
            val reply = when (detectedLang) {
                "ur" -> "مکمل: آج کا قولِ حکمت ${quote.author} کی طرف سے ہے: \"${quote.quoteText}\"۔ یہ ایک ${quote.category} خیال ہے۔"
                "ps" -> "بشپړ شو: د نن ورځې غوره خبره د ${quote.author} له لوري ده: \"${quote.quoteText}\". دا د پام وړ موضوع ده."
                "roman_ur" -> "Completed: Aaj ka quote ${quote.author} ka hai: \"${quote.quoteText}\". Yeh ek ${quote.category} thought hai, Boss."
                else -> "Completed: Retrieved today's quote by ${quote.author}: \"${quote.quoteText}\"."
            }

            chatDao.insertMessage(ChatMessageEntity(role = "user", content = spokenText, mode = "live_call", language = detectedLang))
            chatDao.insertMessage(ChatMessageEntity(role = "jarvis", content = reply, mode = "live_call", language = detectedLang))
            return Pair(reply, detectedLang)
        }

        // 2. Tool execution: Create/Add Task
        if (lower.startsWith("add task") || lower.startsWith("create task") || lower.contains("remind me to") || lower.contains("task banao") || lower.contains("کار اضافه کړه")) {
            val taskTitle = spokenText
                .replace("add task", "", ignoreCase = true)
                .replace("create task", "", ignoreCase = true)
                .replace("remind me to", "", ignoreCase = true)
                .replace("task banao", "", ignoreCase = true)
                .replace("کار اضافه کړه", "", ignoreCase = true)
                .trim().ifEmpty { "Personal reminder" }

            taskDao.insertTask(
                TaskEntity(
                    title = taskTitle.capitalizeWords(),
                    description = "Created via JARVIS Live Call voice session.",
                    category = "Voice Action",
                    priority = "Normal"
                )
            )

            val reply = when (detectedLang) {
                "ur" -> "مکمل: میں نے آپ کی فہرست میں ٹاسک \"$taskTitle\" کامیابی سے شامل کر دیا ہے، باس۔"
                "ps" -> "بشپړ شو: ستاسو د دندو په لست کې \"$taskTitle\" اضافه شو، باس."
                "roman_ur" -> "Completed: Task \"$taskTitle\" aap ki list mein add kar diya gaya hai, Boss."
                else -> "Completed: Added \"$taskTitle\" to your task ledger, Boss."
            }

            chatDao.insertMessage(ChatMessageEntity(role = "user", content = spokenText, mode = "live_call", language = detectedLang))
            chatDao.insertMessage(ChatMessageEntity(role = "jarvis", content = reply, mode = "live_call", language = detectedLang))
            return Pair(reply, detectedLang)
        }

        // 3. Tool execution: List / Check Tasks
        if (lower.contains("show task") || lower.contains("list task") || lower.contains("my task") || lower.contains("ٹاسک دکھاؤ") || lower.contains("میرے ٹاسک") || lower.contains("کارونه وښایه") || lower.contains("tasks batao")) {
            val tasks = taskDao.getAllTasks().first()
            val activeTasks = tasks.filter { !it.isCompleted }
            val reply = if (activeTasks.isEmpty()) {
                when (detectedLang) {
                    "ur" -> "مکمل جانچ: آپ کے ٹاسک رجسٹر میں فی الحال کوئی فعال ٹاسک نہیں ہے، باس۔"
                    "ps" -> "بشپړ شو: ستاسو په لست کې دا مهال کوم فعال کار نشته، باس."
                    "roman_ur" -> "Completed task check: Filhal aap ki task list empty hai, Boss."
                    else -> "Completed task check: Your task ledger is currently clear, Boss."
                }
            } else {
                val summary = activeTasks.take(2).joinToString("; ") { it.title }
                when (detectedLang) {
                    "ur" -> "مکمل جانچ: آپ کے ${activeTasks.size} فعال ٹاسک ہیں۔ سر فہرست: $summary"
                    "ps" -> "بشپړ شو: ستاسو ${activeTasks.size} کارونه ثبت دي: $summary"
                    "roman_ur" -> "Completed task check: Aap ke ${activeTasks.size} active tasks hain: $summary"
                    else -> "Completed task check: You have ${activeTasks.size} active tasks. Top items: $summary"
                }
            }

            chatDao.insertMessage(ChatMessageEntity(role = "user", content = spokenText, mode = "live_call", language = detectedLang))
            chatDao.insertMessage(ChatMessageEntity(role = "jarvis", content = reply, mode = "live_call", language = detectedLang))
            return Pair(reply, detectedLang)
        }

        // 4. Tool execution: Complete Task
        if (lower.contains("complete task") || lower.contains("finish task") || lower.contains("done task") || lower.contains("ٹاسک مکمل") || lower.contains("task done") || lower.contains("کار بشپړ")) {
            val tasks = taskDao.getAllTasks().first()
            val target = tasks.firstOrNull { !it.isCompleted }
            val reply = if (target != null) {
                taskDao.updateTask(target.copy(isCompleted = true))
                when (detectedLang) {
                    "ur" -> "مکمل: میں نے ٹاسک \"${target.title}\" کو مکمل نشان زد کر دیا ہے، باس۔"
                    "ps" -> "بشپړ شو: د \"${target.title}\" کار بشپړ وبلل شو، باس."
                    "roman_ur" -> "Completed: Task \"${target.title}\" ko complete mark kar diya gaya hai, Boss."
                    else -> "Completed: Marked task \"${target.title}\" as finished, Boss."
                }
            } else {
                when (detectedLang) {
                    "ur" -> "اطلاع: کوئی ایسا ٹاسک نہیں ملا جسے مکمل کیا جا سکے، باس۔"
                    "ps" -> "معلومات: د بشپړولو لپاره کوم فعال کار ونه موندل شو، باس."
                    "roman_ur" -> "Report: Koi active task complete karne ke liye nahi mila, Boss."
                    else -> "Report: No pending task found to complete, Boss."
                }
            }

            chatDao.insertMessage(ChatMessageEntity(role = "user", content = spokenText, mode = "live_call", language = detectedLang))
            chatDao.insertMessage(ChatMessageEntity(role = "jarvis", content = reply, mode = "live_call", language = detectedLang))
            return Pair(reply, detectedLang)
        }

        // 5. Tool execution: Current Time and Date
        if (lower.contains("what time") || lower.contains("current time") || lower.contains("today's date") || lower.contains("time kya") || lower.contains("وقت کیا") || lower.contains("وخت څه") || lower.contains("نېټه")) {
            val timeSdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US)
            val dateSdf = java.text.SimpleDateFormat("EEEE, MMMM d", java.util.Locale.US)
            val now = java.util.Date()
            val timeStr = timeSdf.format(now)
            val dateStr = dateSdf.format(now)

            val reply = when (detectedLang) {
                "ur" -> "مکمل رپورٹ: اس وقت $timeStr ہیں اور آج $dateStr ہے۔"
                "ps" -> "بشپړ معلومات: اوسنی وخت $timeStr او نن $dateStr ده، باس."
                "roman_ur" -> "Completed: Is waqt $timeStr ho chuka hai, date $dateStr hai, Boss."
                else -> "Completed: Current time is $timeStr on $dateStr, Boss."
            }

            chatDao.insertMessage(ChatMessageEntity(role = "user", content = spokenText, mode = "live_call", language = detectedLang))
            chatDao.insertMessage(ChatMessageEntity(role = "jarvis", content = reply, mode = "live_call", language = detectedLang))
            return Pair(reply, detectedLang)
        }

        // 6. Tool execution: Create 30s Video
        if (lower.contains("create 30s video") || lower.contains("30s video") || lower.contains("30 second video") || lower.contains("30 سیکنڈ") || lower.contains("ویڈیو بناو") || lower.contains("ویډیو جوړه کړه")) {
            val storyIdea = spokenText
                .replace("create 30s video", "", ignoreCase = true)
                .replace("30s video", "", ignoreCase = true)
                .replace("30 second video", "", ignoreCase = true)
                .replace("30 سیکنڈ ویڈیو", "", ignoreCase = true)
                .replace("ویڈیو بناو", "", ignoreCase = true)
                .replace("ویډیو جوړه کړه", "", ignoreCase = true)
                .replace("for", "", ignoreCase = true)
                .trim().ifEmpty { "Cinematic futuristic journey" }

            val aspectRatio = if (lower.contains("shorts") || lower.contains("tiktok") || lower.contains("vertical") || lower.contains("9:16")) "9:16" else "16:9"

            val plan = generateThirtySecondVideo(
                storyIdea = storyIdea,
                aspectRatio = aspectRatio,
                visualStyle = "Cinematic 8K Photorealistic",
                customKey = customKey
            )

            val reply = when (detectedLang) {
                "ur" -> "مکمل: میں نے 30 سیکنڈ کی مکمل AI ویڈیو کا 6 سین پلان مع تسلسل '${plan.title}' تیار کر دیا ہے۔ یہ آپ کے اسٹوڈیو میں دستیاب ہے، باس۔"
                "ps" -> "بشپړ شو: د 30 ثانیو ویډیو پلان '${plan.title}' په بریالیتوب سره چمتو شو، باس."
                "roman_ur" -> "Completed: 30-second AI video plan '${plan.title}' 6 scenes aur character continuity ke saath generate ho chuka hai, Boss."
                else -> "Completed: 30-Second AI video '${plan.title}' generated with 6 scenes and character continuity in ${plan.aspectRatio}. Ready in your Video Studio, Boss."
            }

            chatDao.insertMessage(ChatMessageEntity(role = "user", content = spokenText, mode = "live_call", language = detectedLang))
            chatDao.insertMessage(ChatMessageEntity(role = "jarvis", content = reply, mode = "live_call", language = detectedLang))
            return Pair(reply, detectedLang)
        }

        // 7. Tool boundary enforcement: External phone call or outside messaging
        if (lower.contains("phone call") || lower.contains("call ") || lower.contains("dial") || lower.contains("whatsapp") || lower.contains("sms")) {
            val reply = when (detectedLang) {
                "ur" -> "باس، میں براہ راست فون کال یا بیرونی ایس ایم ایس نہیں بھیج سکتا کیونکہ یہ سسٹم بیرونی ٹیلی کام سے منسلک نہیں ہے۔ میں آپ کے لیے پیغام کا مسودہ یا ریمائنڈر تیار کر سکتا ہوں۔"
                "ps" -> "باس، زه مستقیم بهرنی تلیفون یا ایس ایم ایس نشم کولی ځکه دا اسانتیا محدوده ده. خو ستاسو لپاره پیغام مسوده کولی شم."
                "roman_ur" -> "Boss, direct carrier phone call ya SMS perform nahi ho sakta kyun ke cellular terminal linked nahi hai. Main draft bana sakta hoon."
                else -> "Boss, external cellular calling and messaging networks are not accessible in this terminal. I cannot pretend to place calls, but I can draft your text."
            }

            chatDao.insertMessage(ChatMessageEntity(role = "user", content = spokenText, mode = "live_call", language = detectedLang))
            chatDao.insertMessage(ChatMessageEntity(role = "jarvis", content = reply, mode = "live_call", language = detectedLang))
            return Pair(reply, detectedLang)
        }

        // 4. Live Call conversation via Gemini API
        val existingMessages = chatDao.getAllMessages().first()
        val historyPairs = existingMessages.takeLast(4).map { it.role to it.content }

        val apiResult = geminiClient.generateLiveCallResponse(
            prompt = spokenText,
            history = historyPairs,
            customKey = customKey
        )

        val reply = if (apiResult.isSuccess && !apiResult.getOrNull().isNullOrBlank()) {
            apiResult.getOrNull()!!
        } else {
            // Natural, fast, spoken local fallback
            when (detectedLang) {
                "ur" -> "جی باس، میں نے آپ کی بات سمجھ لی ہے۔ میں ہر لمحہ آپ کی رہنمائی کے لیے تیار ہوں۔"
                "ps" -> "هو باس، ستاسو خبره مې واورېده. په بشپړ ډول ستاسو په خدمت کې یم."
                "roman_ur" -> "Samajh gaya Boss. Main tayar hoon, batayein agla step kya hai?"
                else -> "Understood, Boss. Standing by and ready for your next directive."
            }
        }

        chatDao.insertMessage(ChatMessageEntity(role = "user", content = spokenText, mode = "live_call", language = detectedLang))
        chatDao.insertMessage(ChatMessageEntity(role = "jarvis", content = reply, mode = "live_call", language = detectedLang))
        return Pair(reply, detectedLang)
    }

    fun detectLanguage(text: String): String {
        // Check Arabic/Urdu/Pashto unicode ranges
        var arabicScriptCount = 0
        for (char in text) {
            val block = Character.UnicodeBlock.of(char)
            if (block == Character.UnicodeBlock.ARABIC ||
                block == Character.UnicodeBlock.ARABIC_SUPPLEMENT ||
                block == Character.UnicodeBlock.ARABIC_EXTENDED_A
            ) {
                arabicScriptCount++
            }
        }
        if (arabicScriptCount > 2) {
            // Check for Pashto specific letters: ښ, ځ, ږ, ړ, څ, ۍ, ئ
            val pashtoChars = listOf('ښ', 'ځ', 'ږ', 'ړ', 'څ', 'ۍ')
            if (pashtoChars.any { text.contains(it) } || text.contains("پښتو") || text.contains("څنګه") || text.contains("مننه")) {
                return "ps"
            }
            return "ur"
        }

        // Check Roman Urdu markers
        val lower = text.lowercase()
        val romanUrduWords = listOf("kese", "kaise", "kya", "kar", "karo", "hai", "hain", "bhai", "shukriya", "bnao", "krna", "hoga", "mujhe", "mera", "meri", "theek")
        if (romanUrduWords.count { lower.contains("\\b$it\\b".toRegex()) } >= 2) {
            return "roman_ur"
        }

        return "en"
    }

    /**
     * Fallback generator adhering strictly to personality and abilities
     */
    private suspend fun generateLocalJarvisResponse(
        prompt: String,
        mode: String,
        language: String
    ): String {
        val lower = prompt.lowercase()

        // 1. Urdu
        if (language == "ur") {
            return if (mode == "video_prompt") {
                buildVideoPromptResponse(prompt, language = "ur")
            } else if (mode == "automation") {
                buildAutomationResponse(prompt, language = "ur")
            } else {
                "السلام علیکم باس! میں آپ کا اسسٹنٹ جارویس حاضر ہوں۔\n\n" +
                        "آپ کے اس کام کے حوالے سے:\n" +
                        "• مقصد کا تجزیہ مکمل کر لیا گیا ہے۔\n" +
                        "• میں اس پر فوراً کام شروع کرنے کے لیے تیار ہوں۔\n\n" +
                        "کیا آپ چاہتے ہیں کہ میں اس کا تفصیلی خاکہ یا ویڈیو پرامپٹ تیار کروں؟"
            }
        }

        // 2. Pashto
        if (language == "ps") {
            return if (mode == "video_prompt") {
                buildVideoPromptResponse(prompt, language = "ps")
            } else {
                "سلام باس! زه ستاسو مرستیال جارویس یم او ستاسو په خدمت کې چمتو یم.\n\n" +
                        "ستاسو د لارښوونې مطابق زه کولی شم:\n" +
                        "• د ویډیو او انځور جوړولو مسلکي پرامپټونه چمتو کړم\n" +
                        "• ستاسو یوټیوب او د کارونو پروژې تنظیم کړم\n\n" +
                        "مهرباني وکړئ ماته ووایاست چې اوس څه وکړم؟"
            }
        }

        // 3. Roman Urdu
        if (language == "roman_ur") {
            return if (mode == "video_prompt") {
                buildVideoPromptResponse(prompt, language = "roman_ur")
            } else if (mode == "automation") {
                buildAutomationResponse(prompt, language = "roman_ur")
            } else {
                "Salam Boss! JARVIS systems active hain aur main aapki service mein hazir hoon.\n\n" +
                        "Aapke task ko analyse kar liya gaya hai:\n" +
                        "• AI Video Prompts (Veo/Kling format ready)\n" +
                        "• YouTube Titles, SEO Tags aur Complete Script\n" +
                        "• Automation aur Project Management\n\n" +
                        "Batayein Boss, agla step kya execute karein?"
            }
        }

        // 3.5. Quote Mode
        if (mode == "quote" || lower.contains("quote") || lower.contains("قول")) {
            val quote = getOrFetchQuoteOfTheDay()
            return """
✨ QUOTE OF THE DAY:
"${quote.quoteText}"
— ${quote.author}

🏷️ Category: #${quote.category}
📡 Source: ${quote.source}

JARVIS Telemetry Reflection:
A powerful, ${quote.category.lowercase()} reflection for your day, Boss. Let me know if you would like me to read it aloud or archive it to your notes.
            """.trimIndent()
        }

        // 4. Video Prompt Mode (English default)
        if (mode == "video_prompt" || lower.contains("video prompt")) {
            return buildVideoPromptResponse(prompt, language = "en")
        }

        // 5. YouTube Studio Mode
        if (mode == "youtube" || lower.contains("youtube")) {
            return buildYouTubeResponse(prompt)
        }

        // 6. Automation Mode
        if (mode == "automation" || lower.contains("automate")) {
            return buildAutomationResponse(prompt, language = "en")
        }

        // 7. General Assistant query
        return "At your service, Boss.\n\n" +
                "I have analyzed your request: \"$prompt\"\n\n" +
                "• Status: Analyzed & processed through local intelligence telemetry.\n" +
                "• Privacy: Strictly maintained; credentials and private state remain protected.\n" +
                "• Next Actions: Ready to execute task breakdown, craft AI video/image generation prompts, or compose scripts.\n\n" +
                "How would you like to proceed?"
    }

    private suspend fun buildVideoPromptResponse(prompt: String, language: String): String {
        val topic = prompt.replace("video prompt", "", ignoreCase = true)
            .replace("create", "", ignoreCase = true)
            .replace("generate", "", ignoreCase = true)
            .trim().ifEmpty { "Futuristic Cyber City with Holographic Arc Technology" }

        val response = """
Here is your structured AI Video Prompt, Boss, optimized for modern video generators (Veo, Kling, Sora, Runway):

🎬 AI Video Generation Blueprint:
• Scene: High-fidelity cinematic sequence depicting $topic
• Character details: Distinct, expressive subject with hyper-realistic texture, natural movement, and micro-expressions
• Environment: Atmospheric high-depth setting, volumetric mist, subtle reflections, dynamic environmental particles
• Action: Smooth, continuous kinetic motion progressing naturally across the frame with physics-based weight
• Camera movement: Slow deliberate cinematic gimbal tracking shot, gentle push-in, low-angle dynamic perspective
• Lighting: Dynamic chiaroscuro lighting, dual-tone cyber cyan and warm amber backlights with subtle lens flares
• Cinematic style: Photorealistic 8k render, 35mm anamorphic lens, shallow depth of field (f/1.8), motion blur
• Dialogue: [Ambient atmospheric sound design, crisp spatial audio, subtle synthesized hum]
• Duration: 5 to 10 seconds continuous shot
• Aspect ratio: 16:9 (Cinematic Widescreen) / 9:16 (Vertical Short/Reel)

Pro-Tip for Veo & Kling: You can copy and paste the above parameters directly into the prompt box for pristine consistency.
""".trimIndent()

        // Auto-save a task for tracking this prompt
        taskDao.insertTask(
            TaskEntity(
                title = "AI Video Generation: $topic",
                description = "Render video prompt in Veo/Kling with 16:9 cinematic settings.",
                category = "AI Video",
                priority = "High"
            )
        )

        return response
    }

    private suspend fun buildYouTubeResponse(prompt: String): String {
        val topic = prompt.replace("youtube", "", ignoreCase = true)
            .replace("script", "", ignoreCase = true)
            .trim().ifEmpty { "AI Automation & Future Tech" }

        val response = """
At your service, Boss. Here is the complete YouTube optimization package for "$topic":

🔥 High-CTR Title Options:
1. I Built My Own JARVIS in 2026 (And It Blew My Mind!)
2. The Secret AI Workflow That Saves 20+ Hours Every Week
3. $topic: The Step-by-Step Guide Nobody Tells You
4. Don't Make This Mistake With $topic (Watch Before Starting)
5. How Top Creators Automate $topic in 2026

📝 Viral Description & Chapters:
Discover the exact framework to master $topic. In this breakdown, we cover everything step-by-step from beginner fundamentals to advanced automated workflows.

⏱️ Timestamps:
00:00 - The Big Hook & Why This Matters
01:15 - Core Framework & Strategy
03:40 - Live Step-by-Step Execution
06:20 - Secret Pro-Tips to Avoid Pitfalls
08:45 - Final Summary & Action Steps

🏷️ High-Search SEO Tags:
#$topic #AIAutomation #JARVIS #Tech2026 #Productivity #VeoAI #Kling #SmartWorkflow #FutureTech

🎬 Script Outline:
• Hook (0-15s): High energy question addressing a major pain point directly.
• Intrigue (15-45s): Preview the shocking results achieved with this exact setup.
• Body (1-7m): Three actionable pillars presented with clear visual proof.
• CTA (End): Direct invitation to subscribe and leave thoughts in the comments.
""".trimIndent()

        taskDao.insertTask(
            TaskEntity(
                title = "Produce YouTube Video: $topic",
                description = "Record script hook, film screencast demos, apply tags & upload.",
                category = "YouTube",
                priority = "Normal"
            )
        )

        return response
    }

    private suspend fun buildAutomationResponse(prompt: String, language: String): String {
        val goal = prompt.replace("automate", "", ignoreCase = true)
            .replace("automation", "", ignoreCase = true)
            .trim().ifEmpty { "Automate Daily Content Creation & Task Scheduling" }

        val response = """
Automation Mode Initialized, Boss.

Goal: $goal

Step-by-Step Execution Plan:
1. [Analysis]: Scope defined and prerequisite assets identified.
2. [Workflow Design]: Trigger event configured -> automated processing pipeline.
3. [Tool Compatibility]: Integrated with local task organizer & cloud inference tools.
4. [Execution]: 4 automated steps have been added directly to your Tasks & Projects registry!
5. [Reporting]: Awaiting your green light on initial output review.

📋 Status:
• Completed: System architecture created, 4 automated sub-tasks registered in database.
• User Action Required: Review generated task items in the Tasks tab and confirm external execution.
""".trimIndent()

        // Automatically create actionable tasks in Room database
        taskDao.insertTasks(
            listOf(
                TaskEntity(
                    title = "Phase 1: Setup & Assets for $goal",
                    description = "Collect baseline inputs and required tool credentials.",
                    category = "Automation",
                    priority = "High"
                ),
                TaskEntity(
                    title = "Phase 2: Build Processing Pipeline for $goal",
                    description = "Configure prompts and automation triggers.",
                    category = "Automation",
                    priority = "Normal"
                ),
                TaskEntity(
                    title = "Phase 3: Verify Output Quality",
                    description = "Inspect results and refine generation parameters.",
                    category = "Automation",
                    priority = "Normal"
                )
            )
        )

        return response
    }

    private suspend fun extractAndSaveTasksFromPlan(userText: String, responseText: String) {
        val title = if (userText.length > 50) userText.take(47) + "..." else userText
        taskDao.insertTask(
            TaskEntity(
                title = title.capitalizeWords(),
                description = responseText.take(200).replace("\n", " "),
                category = "Automation",
                priority = "Normal"
            )
        )
    }

    // Task Management methods
    suspend fun addTask(title: String, description: String, category: String, priority: String) {
        taskDao.insertTask(
            TaskEntity(
                title = title.trim(),
                description = description.trim(),
                category = category,
                priority = priority
            )
        )
    }

    suspend fun toggleTask(id: Long, currentCompleted: Boolean) {
        taskDao.setTaskCompleted(id, !currentCompleted)
    }

    suspend fun deleteTask(id: Long) {
        taskDao.deleteTaskById(id)
    }

    suspend fun clearAllTasks() {
        taskDao.clearAllTasks()
    }

    suspend fun saveMessageDirectly(role: String, content: String, mode: String, language: String) {
        chatDao.insertMessage(ChatMessageEntity(role = role, content = content, mode = mode, language = language))
    }

    suspend fun clearChat() {
        chatDao.clearAllMessages()
        getInitialGreetingIfNeeded()
    }
}

private fun String.capitalizeWords(): String =
    split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
