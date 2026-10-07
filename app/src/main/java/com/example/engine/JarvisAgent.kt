package com.example.engine

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.AppDatabase
import com.example.data.HistoryEntity
import com.example.data.ModelConfig
import com.example.data.ProjectDao
import com.example.data.ProjectEntity
import com.example.network.GeminiContent
import com.example.network.GeminiGenerationConfig
import com.example.network.GeminiPart
import com.example.network.GeminiRequest
import com.example.network.GeminiThinkingConfig
import com.example.network.ParsedJarvisAction
import com.example.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

class JarvisAgent(
    private val context: Context,
    private val toolManager: ToolManager,
    private val projectDao: ProjectDao,
    private val historyDao: com.example.data.HistoryDao
) {
    var activeModel: String = ModelConfig.MODEL_FLASH
    var customApiKey: String = ""

    private fun getApiKey(): String {
        return when {
            customApiKey.isNotBlank() -> customApiKey
            BuildConfig.GEMINI_API_KEY.isNotBlank() && !BuildConfig.GEMINI_API_KEY.startsWith("MY_") -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }
    }

    suspend fun processCommand(
        userInput: String,
        currentProject: ProjectEntity? = null,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): ActionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val telemetry = SystemTelemetry.getTelemetry(context)

        // 1. Check Offline Status
        if (!telemetry.isConnected) {
            val localResult = processOfflineCommand(userInput, currentProject)
            val duration = System.currentTimeMillis() - startTime
            recordHistory(userInput, localResult.intent.name, localResult, duration)
            return@withContext localResult
        }

        // 2. Prepare Gemini prompt with context
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            val localFallback = processOfflineCommand(userInput, currentProject)
            if (localFallback.intent != JarvisIntent.UNKNOWN && localFallback.intent != JarvisIntent.CHAT_RESPONSE) {
                val duration = System.currentTimeMillis() - startTime
                recordHistory(userInput, localFallback.intent.name, localFallback, duration)
                return@withContext localFallback
            }
            return@withContext ActionResult(
                success = false,
                intent = JarvisIntent.UNKNOWN,
                spokenResponseBn = "Gemini API Key সেট করা নেই। দয়া করে Settings-এ গিয়ে আপনার API key প্রদান করুন।",
                spokenResponseEn = "Gemini API key is not configured. Please add your API key in Settings.",
                displayMessage = "Gemini API Key missing. You can provide it in Settings or via Secrets panel."
            )
        }

        // 3. Query Gemini AI Brain
        try {
            val parsedAction = queryGemini(userInput, currentProject, conversationHistory, apiKey)
            val actionResult = executeParsedAction(parsedAction, currentProject)
            val duration = System.currentTimeMillis() - startTime
            recordHistory(userInput, parsedAction.intent, actionResult, duration)
            return@withContext actionResult
        } catch (e: Exception) {
            Log.e("JarvisAgent", "Gemini error: ${e.message}", e)
            // Try local fallback
            val fallback = processOfflineCommand(userInput, currentProject)
            if (fallback.intent != JarvisIntent.UNKNOWN) {
                val duration = System.currentTimeMillis() - startTime
                recordHistory(userInput, fallback.intent.name, fallback, duration)
                return@withContext fallback
            }
            val errResult = ActionResult(
                success = false,
                intent = JarvisIntent.UNKNOWN,
                spokenResponseBn = "আমি কমান্ডটি প্রক্রিয়া করতে পারিনি: ${e.localizedMessage}",
                spokenResponseEn = "I could not process the request: ${e.localizedMessage}",
                displayMessage = "AI Agent error: ${e.message}"
            )
            val duration = System.currentTimeMillis() - startTime
            recordHistory(userInput, "ERROR", errResult, duration)
            return@withContext errResult
        }
    }

    private suspend fun queryGemini(
        userInput: String,
        currentProject: ProjectEntity?,
        conversationHistory: List<Pair<String, String>>,
        apiKey: String
    ): ParsedJarvisAction {
        val contents = mutableListOf<GeminiContent>()

        // Add history turns (last 4 turns)
        conversationHistory.takeLast(4).forEach { (user, ai) ->
            contents.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = user))))
            contents.add(GeminiContent(role = "model", parts = listOf(GeminiPart(text = ai))))
        }

        // Add current project context if present
        val projectContext = if (currentProject != null) {
            "\n[ACTIVE CODE PROJECT IN MEMORY: Name: '${currentProject.name}', HTML Length: ${currentProject.htmlCode.length}, CSS Length: ${currentProject.cssCode.length}, JS Length: ${currentProject.jsCode.length}]\n"
        } else ""

        val promptText = """
User Command: "$userInput"
$projectContext

Respond with strict JSON with no extra markdown backticks if possible:
{
  "thought": "Reasoning here",
  "intent": "OPEN_APP | CHECK_APP_INSTALLED | OPEN_PLAY_STORE | CLOSE_APP | WEB_SEARCH | OPEN_URL | SEARCH_YOUTUBE | OPEN_YOUTUBE | CREATE_WEB_PROJECT | EDIT_CODE | RUN_LOCAL_WEB_PREVIEW | OPEN_PROJECT | COPY_CLIPBOARD | OPEN_SETTINGS | OPEN_CAMERA | OPEN_MAPS | SEARCH_MAPS | SHOW_NOTIFICATION | GO_HOME | CHAT_RESPONSE",
  "app_name": "name of app if applicable",
  "package_hint": "package id if known (e.g. com.whatsapp, org.telegram.messenger)",
  "query": "search query if applicable",
  "url": "url if applicable",
  "project_name": "project name if creating or editing code",
  "html": "full HTML string if creating or editing code",
  "css": "full CSS string if creating or editing code",
  "js": "full JavaScript string if creating or editing code",
  "spoken_response_bn": "Short natural response in Bengali",
  "spoken_response_en": "Short natural response in English",
  "requires_confirmation": false,
  "risk_level": "LOW | MEDIUM | HIGH"
}
""".trimIndent()

        contents.add(GeminiContent(role = "user", parts = listOf(GeminiPart(text = promptText))))

        val isPro = activeModel == ModelConfig.MODEL_PRO
        val request = GeminiRequest(
            contents = contents,
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = ModelConfig.SYSTEM_INSTRUCTION))),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.4f,
                thinkingConfig = if (isPro) GeminiThinkingConfig(thinkingLevel = "high") else null
            )
        )

        val response = RetrofitClient.geminiService.generateContent(
            model = activeModel,
            apiKey = apiKey,
            request = request
        )

        val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        return parseGeminiJson(rawText)
    }

    private fun parseGeminiJson(rawText: String): ParsedJarvisAction {
        val clean = rawText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            val json = JSONObject(clean)
            ParsedJarvisAction(
                thought = json.optString("thought", ""),
                intent = json.optString("intent", "CHAT_RESPONSE"),
                appName = json.optString("app_name", ""),
                packageHint = json.optString("package_hint", ""),
                query = json.optString("query", ""),
                url = json.optString("url", ""),
                projectName = json.optString("project_name", ""),
                htmlCode = json.optString("html", ""),
                cssCode = json.optString("css", ""),
                jsCode = json.optString("js", ""),
                spokenResponseBn = json.optString("spoken_response_bn", ""),
                spokenResponseEn = json.optString("spoken_response_en", ""),
                rawResponse = clean,
                requiresConfirmation = json.optBoolean("requires_confirmation", false),
                riskLevel = json.optString("risk_level", "LOW")
            )
        } catch (e: Exception) {
            // If raw text wasn't valid JSON, treat as chat response
            ParsedJarvisAction(
                thought = "Direct response",
                intent = "CHAT_RESPONSE",
                spokenResponseBn = clean,
                spokenResponseEn = clean,
                rawResponse = clean
            )
        }
    }

    private suspend fun executeParsedAction(
        action: ParsedJarvisAction,
        currentProject: ProjectEntity?
    ): ActionResult {
        val intent = JarvisIntent.fromString(action.intent)

        return when (intent) {
            JarvisIntent.OPEN_APP -> {
                toolManager.openApp(action.appName, action.packageHint)
            }
            JarvisIntent.CHECK_APP_INSTALLED -> {
                toolManager.checkAppInstalled(action.appName, action.packageHint)
            }
            JarvisIntent.OPEN_PLAY_STORE -> {
                toolManager.openPlayStore(action.appName, action.packageHint)
            }
            JarvisIntent.CLOSE_APP -> {
                toolManager.closeApp(action.appName)
            }
            JarvisIntent.WEB_SEARCH -> {
                toolManager.webSearch(action.query.ifBlank { action.appName })
            }
            JarvisIntent.SEARCH_YOUTUBE, JarvisIntent.OPEN_YOUTUBE -> {
                toolManager.searchYouTube(action.query)
            }
            JarvisIntent.OPEN_CAMERA -> {
                toolManager.openCamera()
            }
            JarvisIntent.OPEN_MAPS, JarvisIntent.SEARCH_MAPS -> {
                toolManager.openMaps(action.query)
            }
            JarvisIntent.OPEN_SETTINGS -> {
                toolManager.openSettings()
            }
            JarvisIntent.GO_HOME -> {
                toolManager.goHome()
            }
            JarvisIntent.COPY_CLIPBOARD -> {
                toolManager.copyToClipboard(action.query.ifBlank { action.rawResponse })
            }
            JarvisIntent.SHOW_NOTIFICATION -> {
                toolManager.showNotification("JARVIS Alert", action.spokenResponseEn.ifBlank { action.query })
            }
            JarvisIntent.CREATE_WEB_PROJECT -> {
                val projectName = action.projectName.ifBlank { "New Project" }
                val project = ProjectEntity(
                    id = UUID.randomUUID().toString(),
                    name = projectName,
                    description = "Created by JARVIS Coding Agent",
                    htmlCode = action.htmlCode.ifBlank { "<h1>$projectName</h1><p>Ready to build!</p>" },
                    cssCode = action.cssCode.ifBlank { "body { font-family: sans-serif; background: #0b0f19; color: #fff; padding: 20px; }" },
                    jsCode = action.jsCode.ifBlank { "console.log('$projectName loaded');" }
                )
                projectDao.insertProject(project)
                ActionResult(
                    success = true,
                    intent = JarvisIntent.CREATE_WEB_PROJECT,
                    spokenResponseBn = action.spokenResponseBn.ifBlank { "$projectName তৈরি করেছি এবং preview চালু করেছি।" },
                    spokenResponseEn = action.spokenResponseEn.ifBlank { "Created $projectName and launched preview." },
                    displayMessage = "Created and saved project '$projectName'.",
                    payload = project
                )
            }
            JarvisIntent.EDIT_CODE -> {
                if (currentProject != null) {
                    val updated = currentProject.copy(
                        htmlCode = action.htmlCode.ifBlank { currentProject.htmlCode },
                        cssCode = action.cssCode.ifBlank { currentProject.cssCode },
                        jsCode = action.jsCode.ifBlank { currentProject.jsCode },
                        updatedAt = System.currentTimeMillis()
                    )
                    projectDao.updateProject(updated)
                    ActionResult(
                        success = true,
                        intent = JarvisIntent.EDIT_CODE,
                        spokenResponseBn = action.spokenResponseBn.ifBlank { "কোড আপডেট করা হয়েছে।" },
                        spokenResponseEn = action.spokenResponseEn.ifBlank { "Code updated successfully, sir." },
                        displayMessage = "Updated project '${currentProject.name}'.",
                        payload = updated
                    )
                } else {
                    ActionResult(
                        success = false,
                        intent = JarvisIntent.EDIT_CODE,
                        spokenResponseBn = "কোন সক্রিয় প্রজেক্ট পাওয়া যায়নি।",
                        spokenResponseEn = "No active project found to edit.",
                        displayMessage = "No active project selected to edit code."
                    )
                }
            }
            JarvisIntent.RUN_LOCAL_WEB_PREVIEW -> {
                ActionResult(
                    success = true,
                    intent = JarvisIntent.RUN_LOCAL_WEB_PREVIEW,
                    spokenResponseBn = action.spokenResponseBn.ifBlank { "প্রিভিউ চালু করেছি।" },
                    spokenResponseEn = action.spokenResponseEn.ifBlank { "Live preview running." },
                    displayMessage = "Live preview opened."
                )
            }
            JarvisIntent.OPEN_PROJECT -> {
                val latest = projectDao.getLatestProject()
                if (latest != null) {
                    ActionResult(
                        success = true,
                        intent = JarvisIntent.OPEN_PROJECT,
                        spokenResponseBn = "আগের প্রজেক্ট '${latest.name}' খুলে দিয়েছি।",
                        spokenResponseEn = "Loaded project '${latest.name}'.",
                        displayMessage = "Loaded project '${latest.name}'.",
                        payload = latest
                    )
                } else {
                    ActionResult(
                        success = false,
                        intent = JarvisIntent.OPEN_PROJECT,
                        spokenResponseBn = "কোন সেভ করা প্রজেক্ট পাওয়া যায়নি।",
                        spokenResponseEn = "No saved projects found.",
                        displayMessage = "Project library is empty."
                    )
                }
            }
            else -> {
                // CHAT_RESPONSE or UNKNOWN
                ActionResult(
                    success = true,
                    intent = JarvisIntent.CHAT_RESPONSE,
                    spokenResponseBn = action.spokenResponseBn.ifBlank { action.rawResponse },
                    spokenResponseEn = action.spokenResponseEn.ifBlank { action.rawResponse },
                    displayMessage = action.rawResponse
                )
            }
        }
    }

    private suspend fun processOfflineCommand(
        input: String,
        currentProject: ProjectEntity?
    ): ActionResult {
        val lower = input.trim().lowercase()

        return when {
            lower.contains("whatsapp") || lower.contains("হোয়াটসঅ্যাপ") -> {
                toolManager.openApp("WhatsApp", "com.whatsapp")
            }
            lower.contains("youtube") || lower.contains("ইউটিউব") -> {
                val query = lower.replace("youtube", "").replace("ইউটিউব", "")
                    .replace("খুলে", "").replace("search", "").replace("করো", "").trim()
                toolManager.searchYouTube(query)
            }
            lower.contains("chrome") || lower.contains("ক্রোম") -> {
                toolManager.openApp("Chrome", "com.android.chrome")
            }
            lower.contains("telegram") || lower.contains("টেলিগ্রাম") -> {
                toolManager.openApp("Telegram", "org.telegram.messenger")
            }
            lower.contains("camera") || lower.contains("ক্যামেরা") -> {
                toolManager.openCamera()
            }
            lower.contains("settings") || lower.contains("সেটিংস") -> {
                toolManager.openSettings()
            }
            lower.contains("maps") || lower.contains("ম্যাপস") -> {
                toolManager.openMaps()
            }
            lower.contains("home") || lower.contains("হোম") -> {
                toolManager.goHome()
            }
            lower.contains("বন্ধ") || lower.contains("close") -> {
                toolManager.closeApp("")
            }
            lower.contains("run") || lower.contains("চালাও") || lower.contains("preview") -> {
                ActionResult(
                    success = true,
                    intent = JarvisIntent.RUN_LOCAL_WEB_PREVIEW,
                    spokenResponseBn = "অফলাইন প্রিভিউ চালু করেছি।",
                    spokenResponseEn = "Offline preview launched.",
                    displayMessage = "Running local preview in offline mode."
                )
            }
            lower.contains("project") || lower.contains("প্রজেক্ট") -> {
                val latest = projectDao.getLatestProject()
                if (latest != null) {
                    ActionResult(
                        success = true,
                        intent = JarvisIntent.OPEN_PROJECT,
                        spokenResponseBn = "আগের প্রজেক্ট '${latest.name}' খুলে দিয়েছি।",
                        spokenResponseEn = "Loaded offline project '${latest.name}'.",
                        displayMessage = "Loaded project '${latest.name}'.",
                        payload = latest
                    )
                } else {
                    ActionResult(
                        success = false,
                        intent = JarvisIntent.OPEN_PROJECT,
                        spokenResponseBn = "কোন সেভ করা প্রজেক্ট নেই।",
                        spokenResponseEn = "No saved offline projects found.",
                        displayMessage = "No saved projects."
                    )
                }
            }
            else -> {
                ActionResult(
                    success = false,
                    intent = JarvisIntent.UNKNOWN,
                    spokenResponseBn = "ইন্টারনেট সংযোগ পাওয়া যায়নি। অফলাইন মোডে অ্যাপ খোলা, কোড এডিটর এবং লোকাল প্রজেক্ট চালানো সম্ভব।",
                    spokenResponseEn = "No internet connection detected. In offline mode, you can open local apps and run saved code projects.",
                    displayMessage = "Offline Mode: AI generation and web search require internet."
                )
            }
        }
    }

    private suspend fun recordHistory(
        userInput: String,
        intentName: String,
        result: ActionResult,
        duration: Long
    ) {
        try {
            historyDao.insertHistory(
                HistoryEntity(
                    userInput = userInput,
                    intent = intentName,
                    actionPayload = result.displayMessage,
                    status = if (result.success) "SUCCESS" else "FAILED",
                    spokenResponse = result.spokenResponseBn.ifBlank { result.spokenResponseEn },
                    executionTimeMs = duration
                )
            )
        } catch (e: Exception) {
            Log.e("JarvisAgent", "Failed to record history", e)
        }
    }
}
