package com.example.engine

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

enum class JarvisIntent {
    OPEN_APP,
    CHECK_APP_INSTALLED,
    OPEN_PLAY_STORE,
    CLOSE_APP,
    WEB_SEARCH,
    OPEN_URL,
    SEARCH_YOUTUBE,
    OPEN_YOUTUBE,
    CREATE_WEB_PROJECT,
    EDIT_CODE,
    RUN_LOCAL_WEB_PREVIEW,
    OPEN_PROJECT,
    SAVE_PROJECT,
    DELETE_PROJECT,
    COPY_CLIPBOARD,
    OPEN_SETTINGS,
    OPEN_CAMERA,
    OPEN_MAPS,
    SEARCH_MAPS,
    SHOW_NOTIFICATION,
    GO_HOME,
    CHAT_RESPONSE,
    UNKNOWN;

    companion object {
        fun fromString(name: String): JarvisIntent {
            return try {
                valueOf(name.trim().uppercase())
            } catch (e: Exception) {
                UNKNOWN
            }
        }
    }
}

data class ActionDefinition(
    val intent: JarvisIntent,
    val description: String,
    val riskLevel: RiskLevel,
    val requiresPermission: String? = null
)

object ActionRegistry {
    val ALL_ACTIONS: Map<JarvisIntent, ActionDefinition> = mapOf(
        JarvisIntent.OPEN_APP to ActionDefinition(
            JarvisIntent.OPEN_APP,
            "Check and launch an installed application",
            RiskLevel.LOW
        ),
        JarvisIntent.CHECK_APP_INSTALLED to ActionDefinition(
            JarvisIntent.CHECK_APP_INSTALLED,
            "Query whether an application is installed on device",
            RiskLevel.LOW
        ),
        JarvisIntent.OPEN_PLAY_STORE to ActionDefinition(
            JarvisIntent.OPEN_PLAY_STORE,
            "Open official Google Play Store listing for an app",
            RiskLevel.LOW
        ),
        JarvisIntent.CLOSE_APP to ActionDefinition(
            JarvisIntent.CLOSE_APP,
            "Navigate Home and explain Android background policy",
            RiskLevel.MEDIUM
        ),
        JarvisIntent.WEB_SEARCH to ActionDefinition(
            JarvisIntent.WEB_SEARCH,
            "Search the web using official browser intents",
            RiskLevel.LOW
        ),
        JarvisIntent.OPEN_URL to ActionDefinition(
            JarvisIntent.OPEN_URL,
            "Open valid web URL in the browser",
            RiskLevel.LOW
        ),
        JarvisIntent.SEARCH_YOUTUBE to ActionDefinition(
            JarvisIntent.SEARCH_YOUTUBE,
            "Search YouTube or open video queries",
            RiskLevel.LOW
        ),
        JarvisIntent.OPEN_YOUTUBE to ActionDefinition(
            JarvisIntent.OPEN_YOUTUBE,
            "Launch YouTube application or web fallback",
            RiskLevel.LOW
        ),
        JarvisIntent.CREATE_WEB_PROJECT to ActionDefinition(
            JarvisIntent.CREATE_WEB_PROJECT,
            "Generate HTML/CSS/JS code, save project, launch local WebView preview",
            RiskLevel.LOW
        ),
        JarvisIntent.EDIT_CODE to ActionDefinition(
            JarvisIntent.EDIT_CODE,
            "Modify HTML, CSS, or JS of current project",
            RiskLevel.LOW
        ),
        JarvisIntent.RUN_LOCAL_WEB_PREVIEW to ActionDefinition(
            JarvisIntent.RUN_LOCAL_WEB_PREVIEW,
            "Run local sandboxed WebView preview of project",
            RiskLevel.LOW
        ),
        JarvisIntent.OPEN_PROJECT to ActionDefinition(
            JarvisIntent.OPEN_PROJECT,
            "Load a saved coding project into workspace",
            RiskLevel.LOW
        ),
        JarvisIntent.SAVE_PROJECT to ActionDefinition(
            JarvisIntent.SAVE_PROJECT,
            "Save coding project to local database",
            RiskLevel.LOW
        ),
        JarvisIntent.DELETE_PROJECT to ActionDefinition(
            JarvisIntent.DELETE_PROJECT,
            "Permanently delete a saved project",
            RiskLevel.HIGH
        ),
        JarvisIntent.COPY_CLIPBOARD to ActionDefinition(
            JarvisIntent.COPY_CLIPBOARD,
            "Copy content to system clipboard",
            RiskLevel.LOW
        ),
        JarvisIntent.OPEN_SETTINGS to ActionDefinition(
            JarvisIntent.OPEN_SETTINGS,
            "Open Android system settings",
            RiskLevel.LOW
        ),
        JarvisIntent.OPEN_CAMERA to ActionDefinition(
            JarvisIntent.OPEN_CAMERA,
            "Launch default camera app",
            RiskLevel.LOW
        ),
        JarvisIntent.OPEN_MAPS to ActionDefinition(
            JarvisIntent.OPEN_MAPS,
            "Open Google Maps",
            RiskLevel.LOW
        ),
        JarvisIntent.SEARCH_MAPS to ActionDefinition(
            JarvisIntent.SEARCH_MAPS,
            "Search location on Maps",
            RiskLevel.LOW
        ),
        JarvisIntent.SHOW_NOTIFICATION to ActionDefinition(
            JarvisIntent.SHOW_NOTIFICATION,
            "Display an alert notification to the user",
            RiskLevel.MEDIUM
        ),
        JarvisIntent.GO_HOME to ActionDefinition(
            JarvisIntent.GO_HOME,
            "Navigate to Android Home screen",
            RiskLevel.LOW
        ),
        JarvisIntent.CHAT_RESPONSE to ActionDefinition(
            JarvisIntent.CHAT_RESPONSE,
            "Conversational response from JARVIS",
            RiskLevel.LOW
        )
    )

    fun getRisk(intent: JarvisIntent): RiskLevel {
        return ALL_ACTIONS[intent]?.riskLevel ?: RiskLevel.LOW
    }
}

data class ActionResult(
    val success: Boolean,
    val intent: JarvisIntent,
    val spokenResponseBn: String,
    val spokenResponseEn: String,
    val displayMessage: String,
    val payload: Any? = null,
    val requiresPlayStoreOffer: Boolean = false,
    val appName: String = "",
    val packageName: String = "",
    val errorCode: String? = null
)
