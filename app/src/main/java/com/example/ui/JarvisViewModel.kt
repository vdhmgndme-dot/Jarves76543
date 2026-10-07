package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.HistoryEntity
import com.example.data.ModelConfig
import com.example.data.ProjectEntity
import com.example.data.SpeechLanguage
import com.example.engine.ActionResult
import com.example.engine.JarvisAgent
import com.example.engine.JarvisIntent
import com.example.engine.SystemTelemetry
import com.example.engine.TelemetryState
import com.example.engine.ToolManager
import com.example.service.JarvisAssistantService
import com.example.ui.components.AssistantCoreState
import com.example.voice.SpeechRecognitionManager
import com.example.voice.TextToSpeechManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class ActiveTab {
    HUD,
    TERMINAL,
    CODE_STUDIO,
    PROJECTS_AND_TOOLS,
    SETTINGS
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "USER" or "JARVIS"
    val text: String,
    val intent: String? = null,
    val thought: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true,
    val requiresPlayStoreOffer: Boolean = false,
    val appName: String = "",
    val packageName: String = ""
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val projectDao = db.projectDao()
    private val historyDao = db.historyDao()
    val toolManager = ToolManager(application)
    private val agent = JarvisAgent(application, toolManager, projectDao, historyDao)

    // UI Navigation
    private val _currentTab = MutableStateFlow(ActiveTab.HUD)
    val currentTab: StateFlow<ActiveTab> = _currentTab.asStateFlow()

    // Telemetry
    private val _telemetry = MutableStateFlow(SystemTelemetry.getTelemetry(application))
    val telemetry: StateFlow<TelemetryState> = _telemetry.asStateFlow()

    // Assistant Core State & Audio
    private val _coreState = MutableStateFlow(AssistantCoreState.IDLE)
    val coreState: StateFlow<AssistantCoreState> = _coreState.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _lastSpokenStatus = MutableStateFlow("All systems functional. Ready for instructions.")
    val lastSpokenStatus: StateFlow<String> = _lastSpokenStatus.asStateFlow()

    // Conversation
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "JARVIS",
                text = "Online and at your service, sir. You can speak in Bengali or English."
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // Projects & Coding Workspace
    val projects: StateFlow<List<ProjectEntity>> = projectDao.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyLogs: StateFlow<List<HistoryEntity>> = historyDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeProject = MutableStateFlow<ProjectEntity?>(null)
    val activeProject: StateFlow<ProjectEntity?> = _activeProject.asStateFlow()

    // Pending confirmation dialog
    private val _pendingConfirmation = MutableStateFlow<PendingConfirmAction?>(null)
    val pendingConfirmation: StateFlow<PendingConfirmAction?> = _pendingConfirmation.asStateFlow()

    // Settings
    private val _selectedModel = MutableStateFlow(ModelConfig.MODEL_FLASH)
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(SpeechLanguage.AUTO)
    val selectedLanguage: StateFlow<SpeechLanguage> = _selectedLanguage.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _developerMode = MutableStateFlow(false)
    val developerMode: StateFlow<Boolean> = _developerMode.asStateFlow()

    private val _apiKeyInput = MutableStateFlow("")
    val apiKeyInput: StateFlow<String> = _apiKeyInput.asStateFlow()

    private val _foregroundServiceEnabled = MutableStateFlow(false)
    val foregroundServiceEnabled: StateFlow<Boolean> = _foregroundServiceEnabled.asStateFlow()

    // Speech Managers
    private var speechRecognizer: SpeechRecognitionManager? = null
    private var textToSpeech: TextToSpeechManager? = null

    init {
        initSpeech()
        seedSampleProjectIfEmpty()
        refreshTelemetry()
    }

    private fun initSpeech() {
        textToSpeech = TextToSpeechManager(
            context = getApplication(),
            onStart = {
                _coreState.value = AssistantCoreState.SPEAKING
            },
            onDone = {
                if (_coreState.value == AssistantCoreState.SPEAKING) {
                    _coreState.value = AssistantCoreState.IDLE
                }
            },
            onError = {
                _coreState.value = AssistantCoreState.ERROR
            }
        )

        speechRecognizer = SpeechRecognitionManager(
            context = getApplication(),
            onStateChanged = { state ->
                when (state) {
                    SpeechRecognitionManager.SpeechState.LISTENING -> {
                        _coreState.value = AssistantCoreState.LISTENING
                    }
                    SpeechRecognitionManager.SpeechState.PROCESSING -> {
                        _coreState.value = AssistantCoreState.THINKING
                    }
                    SpeechRecognitionManager.SpeechState.IDLE -> {
                        if (_coreState.value == AssistantCoreState.LISTENING) {
                            _coreState.value = AssistantCoreState.IDLE
                        }
                    }
                    else -> {}
                }
            },
            onRmsChanged = { amp ->
                _audioAmplitude.value = amp
            },
            onResult = { text ->
                submitUserCommand(text)
            },
            onError = { err ->
                _coreState.value = AssistantCoreState.ERROR
                _lastSpokenStatus.value = err
                viewModelScope.launch {
                    kotlinx.coroutines.delay(2000)
                    if (_coreState.value == AssistantCoreState.ERROR) {
                        _coreState.value = AssistantCoreState.IDLE
                    }
                }
            }
        )
    }

    private fun seedSampleProjectIfEmpty() {
        viewModelScope.launch {
            val latest = projectDao.getLatestProject()
            if (latest == null) {
                val sampleLudo = ProjectEntity(
                    id = "sample_ludo_game",
                    name = "Ludo Mini Game",
                    description = "Interactive 2-player mini board game with animated dice roll",
                    htmlCode = """
<!DOCTYPE html>
<html>
<head>
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Ludo Mini Game</title>
</head>
<body>
  <div class="card">
    <div class="hud-tag">JARVIS // WEB PREVIEW</div>
    <h2>Ludo Mini Arena</h2>
    <div class="turn-box" id="turn">Red Player's Turn</div>
    <div class="dice-box">
      <div id="dice" class="dice">🎲</div>
      <button class="roll-btn" onclick="rollDice()">ROLL DICE</button>
    </div>
    <div class="track" id="track"></div>
    <div class="log" id="log">Ready to play! Click roll.</div>
  </div>
</body>
</html>
""".trimIndent(),
                    cssCode = """
body {
  margin: 0;
  padding: 16px;
  background: #070A12;
  color: #E8F1FC;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  display: flex;
  justify-content: center;
}
.card {
  width: 100%;
  max-width: 400px;
  background: #0D1424;
  border: 1px solid #00F0FF;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 0 24px rgba(0,240,255,0.15);
}
.hud-tag {
  font-size: 10px;
  color: #00F0FF;
  letter-spacing: 1px;
  font-weight: bold;
}
h2 {
  margin: 8px 0 16px 0;
  color: #00F0FF;
}
.turn-box {
  background: #142036;
  padding: 8px 12px;
  border-radius: 8px;
  font-weight: bold;
  text-align: center;
  color: #FF3366;
}
.dice-box {
  display: flex;
  align-items: center;
  justify-content: space-around;
  margin: 20px 0;
}
.dice {
  font-size: 54px;
  cursor: pointer;
  transition: transform 0.2s;
}
.roll-btn {
  background: #00F0FF;
  color: #001F29;
  border: none;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: bold;
  cursor: pointer;
}
.track {
  display: flex;
  gap: 6px;
  justify-content: center;
  margin: 16px 0;
}
.cell {
  width: 38px;
  height: 38px;
  border: 1px solid #1C3254;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
}
.cell.red-pos { background: #FF3366; color: #fff; }
.cell.green-pos { background: #00FF9D; color: #000; }
.log {
  font-size: 12px;
  color: #88A2C4;
  text-align: center;
  margin-top: 10px;
}
""".trimIndent(),
                    jsCode = """
let redPos = 0;
let greenPos = 0;
let turn = 'Red';
const maxCells = 6;

function renderTrack() {
  const track = document.getElementById('track');
  track.innerHTML = '';
  for(let i=0; i<maxCells; i++) {
    const d = document.createElement('div');
    d.className = 'cell';
    if(redPos === i && greenPos === i) {
      d.innerText = '⚔️';
    } else if(redPos === i) {
      d.classList.add('red-pos');
      d.innerText = 'R';
    } else if(greenPos === i) {
      d.classList.add('green-pos');
      d.innerText = 'G';
    } else {
      d.innerText = (i+1);
    }
    track.appendChild(d);
  }
}

function rollDice() {
  const diceEl = document.getElementById('dice');
  const val = Math.floor(Math.random() * 6) + 1;
  const diceIcons = ['⚀','⚁','⚂','⚃','⚄','⚅'];
  diceEl.innerText = diceIcons[val-1];
  
  if(turn === 'Red') {
    redPos = (redPos + val) % maxCells;
    document.getElementById('log').innerText = 'Red rolled ' + val + '! Position: ' + (redPos+1);
    turn = 'Green';
    document.getElementById('turn').innerText = "Green Player's Turn";
    document.getElementById('turn').style.color = '#00FF9D';
  } else {
    greenPos = (greenPos + val) % maxCells;
    document.getElementById('log').innerText = 'Green rolled ' + val + '! Position: ' + (greenPos+1);
    turn = 'Red';
    document.getElementById('turn').innerText = "Red Player's Turn";
    document.getElementById('turn').style.color = '#FF3366';
  }
  renderTrack();
}

renderTrack();
console.log("Ludo game loaded and initialized.");
""".trimIndent()
                )
                projectDao.insertProject(sampleLudo)
                _activeProject.value = sampleLudo
            } else {
                _activeProject.value = latest
            }
        }
    }

    fun setTab(tab: ActiveTab) {
        _currentTab.value = tab
    }

    fun refreshTelemetry() {
        _telemetry.value = SystemTelemetry.getTelemetry(getApplication())
    }

    fun toggleVoiceListening() {
        if (speechRecognizer?.isListening == true) {
            speechRecognizer?.stopListening()
            _coreState.value = AssistantCoreState.IDLE
        } else {
            val langCode = _selectedLanguage.value.code
            speechRecognizer?.startListening(langCode)
        }
    }

    fun submitUserCommand(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        // 1. Add to messages feed
        val userMsg = ChatMessage(sender = "USER", text = trimmed)
        _messages.value = _messages.value + userMsg

        _coreState.value = AssistantCoreState.THINKING

        // Check if user is confirming or canceling Play Store download
        val pending = _pendingConfirmation.value
        if (pending != null && pending.isPlayStoreOffer) {
            val lower = trimmed.lowercase()
            if (lower.contains("হ্যাঁ") || lower.contains("yes") || lower.contains("download") || lower.contains("install") || lower.contains("ঠিক আছে")) {
                confirmPendingAction()
                return
            } else if (lower.contains("না") || lower.contains("no") || lower.contains("cancel") || lower.contains("দরকার নেই")) {
                dismissPendingAction()
                val cancelMsg = "ঠিক আছে, Play Store খোলা বাতিল করা হয়েছে।"
                speakResponse(cancelMsg)
                _messages.value = _messages.value + ChatMessage(sender = "JARVIS", text = cancelMsg)
                return
            }
        }

        viewModelScope.launch {
            val history = _messages.value.map { it.sender to it.text }
            val result = agent.processCommand(trimmed, _activeProject.value, history)

            handleExecutionResult(result, trimmed)
        }
    }

    private fun handleExecutionResult(result: ActionResult, originalInput: String) {
        _coreState.value = AssistantCoreState.EXECUTING

        if (result.payload is ProjectEntity) {
            _activeProject.value = result.payload
            // If project was created, navigate to Code Studio
            if (result.intent == JarvisIntent.CREATE_WEB_PROJECT) {
                _currentTab.value = ActiveTab.CODE_STUDIO
            }
        } else if (result.intent == JarvisIntent.RUN_LOCAL_WEB_PREVIEW) {
            _currentTab.value = ActiveTab.CODE_STUDIO
        }

        val spokenText = if (result.spokenResponseBn.isNotBlank()) {
            result.spokenResponseBn
        } else {
            result.spokenResponseEn
        }

        _lastSpokenStatus.value = spokenText

        // Check if Play Store offer is required
        if (result.requiresPlayStoreOffer) {
            _pendingConfirmation.value = PendingConfirmAction(
                title = "Install Application",
                message = spokenText,
                isPlayStoreOffer = true,
                appName = result.appName,
                packageName = result.packageName,
                onConfirm = {
                    toolManager.openPlayStore(result.appName, result.packageName)
                }
            )
        }

        val aiMsg = ChatMessage(
            sender = "JARVIS",
            text = spokenText,
            intent = result.intent.name,
            thought = result.displayMessage,
            isSuccess = result.success,
            requiresPlayStoreOffer = result.requiresPlayStoreOffer,
            appName = result.appName,
            packageName = result.packageName
        )
        _messages.value = _messages.value + aiMsg

        // Speak through TTS
        speakResponse(spokenText)

        refreshTelemetry()
    }

    fun speakResponse(text: String) {
        textToSpeech?.speak(text, _selectedLanguage.value.code)
    }

    fun confirmPendingAction() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        pending.onConfirm()
        val confirmMsg = "ঠিক আছে, Play Store page খুলছি।"
        speakResponse(confirmMsg)
        _messages.value = _messages.value + ChatMessage(sender = "JARVIS", text = confirmMsg)
    }

    fun dismissPendingAction() {
        _pendingConfirmation.value = null
    }

    // Coding Workspace Actions
    fun setActiveProject(project: ProjectEntity) {
        _activeProject.value = project
    }

    fun updateActiveProjectCode(html: String, css: String, js: String) {
        val current = _activeProject.value ?: return
        val updated = current.copy(
            htmlCode = html,
            cssCode = css,
            jsCode = js,
            updatedAt = System.currentTimeMillis()
        )
        _activeProject.value = updated
        viewModelScope.launch {
            projectDao.updateProject(updated)
        }
    }

    fun createNewProject(name: String) {
        val newProj = ProjectEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            description = "Custom User Project",
            htmlCode = "<h1>$name</h1>\n<p>Build your app here.</p>",
            cssCode = "body { background: #0A0E1A; color: #00F0FF; font-family: sans-serif; padding: 20px; }",
            jsCode = "console.log('Project started');"
        )
        viewModelScope.launch {
            projectDao.insertProject(newProj)
            _activeProject.value = newProj
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            projectDao.deleteProject(project)
            if (_activeProject.value?.id == project.id) {
                _activeProject.value = projectDao.getLatestProject()
            }
        }
    }

    fun clearChatHistory() {
        _messages.value = listOf(
            ChatMessage(
                sender = "JARVIS",
                text = "Memory cleared, sir. Ready for instructions."
            )
        )
    }

    fun clearAllCommandHistory() {
        viewModelScope.launch {
            historyDao.clearAllHistory()
        }
    }

    // Settings
    fun setSelectedModel(modelId: String) {
        _selectedModel.value = modelId
        agent.activeModel = modelId
    }

    fun setSelectedLanguage(lang: SpeechLanguage) {
        _selectedLanguage.value = lang
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        textToSpeech?.setSpeechRate(rate)
    }

    fun setDeveloperMode(enabled: Boolean) {
        _developerMode.value = enabled
    }

    fun setCustomApiKey(key: String) {
        _apiKeyInput.value = key
        agent.customApiKey = key
    }

    fun toggleForegroundService(enable: Boolean) {
        _foregroundServiceEnabled.value = enable
        if (enable) {
            JarvisAssistantService.startService(getApplication())
        } else {
            JarvisAssistantService.stopService(getApplication())
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
    }
}

data class PendingConfirmAction(
    val title: String,
    val message: String,
    val isPlayStoreOffer: Boolean = false,
    val appName: String = "",
    val packageName: String = "",
    val onConfirm: () -> Unit
)
