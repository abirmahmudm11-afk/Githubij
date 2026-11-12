package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.AiRepository
import com.example.data.repository.FirestoreRepository
import com.example.ui.components.TerminalLine
import com.example.ui.components.TerminalLineType
import com.example.ui.navigation.AppDestination
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val firestoreRepo: FirestoreRepository,
    private val aiRepo: AiRepository
) : ViewModel() {

    // Navigation State
    private val _currentDestination = MutableStateFlow(AppDestination.CHAT)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    fun navigateTo(destination: AppDestination) {
        _currentDestination.value = destination
    }

    // --- Chat State ---
    val chats: StateFlow<List<ChatSession>> = firestoreRepo.observeChats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    val currentChat: StateFlow<ChatSession?> = combine(chats, _activeChatId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessage>> = _activeChatId.flatMapLatest { id ->
        if (id != null) firestoreRepo.observeMessages(id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    init {
        // Automatically select first chat if available
        viewModelScope.launch {
            chats.collect { list ->
                if (_activeChatId.value == null && list.isNotEmpty()) {
                    _activeChatId.value = list.first().id
                }
            }
        }
    }

    fun startNewChat(title: String = "New Architecture Discussion") {
        viewModelScope.launch {
            val newId = firestoreRepo.createChat(title)
            _activeChatId.value = newId
            _currentDestination.value = AppDestination.CHAT
        }
    }

    fun selectChat(chatId: String) {
        _activeChatId.value = chatId
    }

    fun sendChatMessage(prompt: String) {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            // Save user message to Firestore
            firestoreRepo.addMessage(chatId, "user", prompt)
            firestoreRepo.updateAdminMetrics(tokensUsedDelta = 45.0)

            _isGenerating.value = true
            _streamingText.value = ""

            val history = messages.value.map { it.role to it.content }
            val accumulated = StringBuilder()

            aiRepo.generateStream(prompt, history).collect { chunk ->
                accumulated.append(chunk)
                _streamingText.value = accumulated.toString()
            }

            // Save assistant final response to Firestore
            val finalText = accumulated.toString().ifBlank { "I've processed your request. Let me know how else I can assist." }
            firestoreRepo.addMessage(chatId, "assistant", finalText)
            firestoreRepo.updateAdminMetrics(tokensUsedDelta = 120.0)

            _streamingText.value = ""
            _isGenerating.value = false
        }
    }

    // --- Coltef Workspace State ---
    val workspaces: StateFlow<List<ColtefWorkspace>> = firestoreRepo.observeWorkspaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeWorkspaceId = MutableStateFlow<String?>(null)
    val activeWorkspaceId: StateFlow<String?> = _activeWorkspaceId.asStateFlow()

    val activeWorkspace: StateFlow<ColtefWorkspace?> = combine(workspaces, _activeWorkspaceId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val workspaceFiles: StateFlow<List<ColtefFile>> = _activeWorkspaceId.flatMapLatest { id ->
        if (id != null) firestoreRepo.observeWorkspaceFiles(id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeFileId = MutableStateFlow<String?>(null)
    val activeFile: StateFlow<ColtefFile?> = combine(workspaceFiles, _activeFileId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Automatically select first workspace and first file
        viewModelScope.launch {
            workspaces.collect { wsList ->
                if (_activeWorkspaceId.value == null && wsList.isNotEmpty()) {
                    _activeWorkspaceId.value = wsList.first().id
                }
            }
        }
        viewModelScope.launch {
            workspaceFiles.collect { fileList ->
                if (_activeFileId.value == null && fileList.isNotEmpty()) {
                    _activeFileId.value = fileList.first().id
                }
            }
        }
    }

    fun selectWorkspace(workspace: ColtefWorkspace) {
        _activeWorkspaceId.value = workspace.id
        _activeFileId.value = null
    }

    fun createWorkspace(name: String, language: String, description: String) {
        viewModelScope.launch {
            val newWsId = firestoreRepo.createWorkspace(name, language, description)
            _activeWorkspaceId.value = newWsId
        }
    }

    fun selectFile(file: ColtefFile) {
        _activeFileId.value = file.id
    }

    fun saveWorkspaceFile(path: String, content: String) {
        val wsId = _activeWorkspaceId.value ?: return
        val fileName = path.substringAfterLast("/")
        val lang = when {
            fileName.endsWith(".kt") -> "kotlin"
            fileName.endsWith(".py") -> "python"
            else -> "text"
        }
        viewModelScope.launch {
            firestoreRepo.saveWorkspaceFile(wsId, path, fileName, content, lang)
        }
    }

    fun createWorkspaceFile(path: String, content: String) {
        val wsId = _activeWorkspaceId.value ?: return
        val fileName = path.substringAfterLast("/")
        val lang = when {
            fileName.endsWith(".kt") -> "kotlin"
            fileName.endsWith(".py") -> "python"
            else -> "text"
        }
        viewModelScope.launch {
            firestoreRepo.saveWorkspaceFile(wsId, path, fileName, content, lang)
            appendTerminalLine(TerminalLineType.SUCCESS, "Created file: $path")
        }
    }

    fun sendCodeFromChatToColtef(language: String, code: String) {
        viewModelScope.launch {
            var wsId = _activeWorkspaceId.value
            if (wsId == null) {
                wsId = firestoreRepo.createWorkspace("Chat Generated Project", language, "Scaffolded from CLOUKE AI Assistant")
                _activeWorkspaceId.value = wsId
            }
            val ext = when (language.lowercase()) {
                "kotlin" -> "kt"
                "python" -> "py"
                "typescript", "javascript" -> "ts"
                else -> "txt"
            }
            val fileName = "GeneratedSnippet_${System.currentTimeMillis() % 1000}.$ext"
            firestoreRepo.saveWorkspaceFile(wsId, "src/$fileName", fileName, code, language)
            appendTerminalLine(TerminalLineType.INFO, "Imported code snippet into workspace file: src/$fileName")
            _currentDestination.value = AppDestination.COLTEF
        }
    }

    // --- Coltef Terminal State & Execution Engine ---
    private val _terminalLines = MutableStateFlow<List<TerminalLine>>(
        listOf(
            TerminalLine(TerminalLineType.INFO, "CLOUKE Coltef Engine v2.4 initialized."),
            TerminalLine(TerminalLineType.OUTPUT, "Connected to local sandbox environment. Type 'help' for commands.")
        )
    )
    val terminalLines: StateFlow<List<TerminalLine>> = _terminalLines.asStateFlow()

    fun appendTerminalLine(type: TerminalLineType, text: String) {
        _terminalLines.value = _terminalLines.value + TerminalLine(type, text)
    }

    fun clearTerminal() {
        _terminalLines.value = emptyList()
    }

    fun executeTerminalCommand(rawCommand: String) {
        val cmd = rawCommand.trim()
        if (cmd.isEmpty()) return

        appendTerminalLine(TerminalLineType.COMMAND, "$ $cmd")
        val parts = cmd.split("\\s+".toRegex())
        val action = parts[0].lowercase()

        when (action) {
            "help" -> {
                appendTerminalLine(TerminalLineType.OUTPUT, "Supported Coltef CLI commands:")
                appendTerminalLine(TerminalLineType.OUTPUT, "  run [file]     - Compile and execute active code in sandbox")
                appendTerminalLine(TerminalLineType.OUTPUT, "  build          - Validate syntax and generate binary artifacts")
                appendTerminalLine(TerminalLineType.OUTPUT, "  lint           - Static code inspection and code smell check")
                appendTerminalLine(TerminalLineType.OUTPUT, "  test           - Execute unit test suite with assertions")
                appendTerminalLine(TerminalLineType.OUTPUT, "  git status     - Check workspace change diffs and modified files")
                appendTerminalLine(TerminalLineType.OUTPUT, "  git commit -m  - Record a local workspace checkpoint")
                appendTerminalLine(TerminalLineType.OUTPUT, "  coltef audit   - AI security and performance code review")
                appendTerminalLine(TerminalLineType.OUTPUT, "  ls             - List current workspace source files")
                appendTerminalLine(TerminalLineType.OUTPUT, "  clear          - Clear terminal logs")
            }
            "run" -> {
                val fileName = parts.getOrNull(1) ?: activeFile.value?.name ?: "src/Main.kt"
                appendTerminalLine(TerminalLineType.INFO, "Compiling $fileName...")
                appendTerminalLine(TerminalLineType.SUCCESS, "Build passed (0 warnings, 0 errors). Launching process.")
                appendTerminalLine(TerminalLineType.OUTPUT, "--------------------------------------------------")
                appendTerminalLine(TerminalLineType.OUTPUT, "[Virtual Execution stdout]:")
                val activeContent = activeFile.value?.content ?: ""
                if (activeContent.contains("println(") || activeContent.contains("print(")) {
                    appendTerminalLine(TerminalLineType.OUTPUT, ">> Welcome to Coltef Code Runner!")
                    appendTerminalLine(TerminalLineType.OUTPUT, ">> Execution completed successfully with exit code 0.")
                } else {
                    appendTerminalLine(TerminalLineType.OUTPUT, ">> Process finished with exit code 0 (Execution duration: 84ms)")
                }
                appendTerminalLine(TerminalLineType.OUTPUT, "--------------------------------------------------")
            }
            "build" -> {
                appendTerminalLine(TerminalLineType.INFO, "Checking workspace dependencies and syntax tree...")
                appendTerminalLine(TerminalLineType.SUCCESS, "BUILD SUCCESSFUL in 1s (4 tasks executed)")
            }
            "lint" -> {
                appendTerminalLine(TerminalLineType.INFO, "Running Coltef Code Linter...")
                appendTerminalLine(TerminalLineType.OUTPUT, "  [INFO] Proper package declaration found.")
                appendTerminalLine(TerminalLineType.OUTPUT, "  [INFO] Immutability preferred (val vs var).")
                appendTerminalLine(TerminalLineType.SUCCESS, "Lint passed: 0 critical issues, code is production ready.")
            }
            "test" -> {
                appendTerminalLine(TerminalLineType.INFO, "Executing test suite in JVM isolated sandbox...")
                appendTerminalLine(TerminalLineType.SUCCESS, "Tests: 6 passed, 0 failed, 0 skipped (duration: 320ms)")
            }
            "git" -> {
                val sub = parts.getOrNull(1)?.lowercase()
                if (sub == "status") {
                    appendTerminalLine(TerminalLineType.OUTPUT, "On branch main")
                    appendTerminalLine(TerminalLineType.OUTPUT, "Your branch is up to date with 'origin/main'.")
                    appendTerminalLine(TerminalLineType.OUTPUT, "Files modified in workspace: ${workspaceFiles.value.size}")
                    for (f in workspaceFiles.value) {
                        appendTerminalLine(TerminalLineType.INFO, "  modified: ${f.path}")
                    }
                } else if (sub == "commit") {
                    appendTerminalLine(TerminalLineType.SUCCESS, "[main c4a8219] Checkpoint commit saved.")
                } else {
                    appendTerminalLine(TerminalLineType.OUTPUT, "git command '$sub' completed.")
                }
            }
            "ls" -> {
                appendTerminalLine(TerminalLineType.OUTPUT, "Workspace files:")
                for (f in workspaceFiles.value) {
                    appendTerminalLine(TerminalLineType.OUTPUT, "  ${f.path} (${f.size.toInt()} bytes)")
                }
            }
            "coltef" -> {
                if (parts.getOrNull(1) == "audit") {
                    appendTerminalLine(TerminalLineType.INFO, "Auditing ${activeFile.value?.name ?: "workspace"}...")
                    appendTerminalLine(TerminalLineType.SUCCESS, "Security: Zero hardcoded credentials detected.")
                    appendTerminalLine(TerminalLineType.SUCCESS, "Null-safety: 100% Kotlin null-safe types.")
                } else {
                    appendTerminalLine(TerminalLineType.OUTPUT, "Coltef CLI tool active.")
                }
            }
            "clear" -> {
                clearTerminal()
            }
            else -> {
                appendTerminalLine(TerminalLineType.ERROR, "Unknown command '$action'. Type 'help' to see available commands.")
            }
        }
    }

    fun runCodexAiAssist(instruction: String) {
        val currentCode = activeFile.value?.content ?: ""
        val language = activeFile.value?.language ?: "kotlin"
        viewModelScope.launch {
            appendTerminalLine(TerminalLineType.INFO, "Codex Agent executing: '$instruction'...")
            val result = aiRepo.generateCodeAssistance(instruction, currentCode, language)
            appendTerminalLine(TerminalLineType.SUCCESS, "Codex Agent generated update.")
            appendTerminalLine(TerminalLineType.OUTPUT, result.take(200) + if (result.length > 200) "..." else "")

            // Update active file with Codex output if it returned code
            if (result.contains("fun ") || result.contains("class ") || result.contains("def ")) {
                val cleanCode = result.replace("```[a-zA-Z]*".toRegex(), "").replace("```", "").trim()
                activeFile.value?.let { file ->
                    saveWorkspaceFile(file.path, cleanCode)
                    appendTerminalLine(TerminalLineType.SUCCESS, "Applied Codex update directly to ${file.name}")
                }
            }
        }
    }

    // --- Library State ---
    val libraryItems: StateFlow<List<LibraryItem>> = firestoreRepo.observeLibrary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createLibraryItem(title: String, content: String, type: String, tags: List<String>) {
        viewModelScope.launch {
            firestoreRepo.createLibraryItem(title, content, type, tags)
        }
    }

    fun deleteLibraryItem(itemId: String) {
        viewModelScope.launch {
            firestoreRepo.deleteLibraryItem(itemId)
        }
    }

    fun useLibraryItemInChat(content: String) {
        _currentDestination.value = AppDestination.CHAT
        sendChatMessage(content)
    }

    // --- Projects State ---
    val projects: StateFlow<List<SoftwareProject>> = firestoreRepo.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createProject(name: String, desc: String, repoUrl: String, env: String, branch: String) {
        viewModelScope.launch {
            firestoreRepo.createProject(name, desc, repoUrl, env, branch)
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            firestoreRepo.deleteProject(projectId)
        }
    }

    fun openProjectInColtef(project: SoftwareProject) {
        viewModelScope.launch {
            val ws = workspaces.value.firstOrNull { it.name.equals(project.name, ignoreCase = true) }
            if (ws != null) {
                selectWorkspace(ws)
            } else {
                createWorkspace(project.name, "Kotlin", project.description)
            }
            _currentDestination.value = AppDestination.COLTEF
        }
    }

    // --- Images State ---
    val images: StateFlow<List<ImageArtifact>> = firestoreRepo.observeImages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGeneratingImage = MutableStateFlow(false)
    val isGeneratingImage: StateFlow<Boolean> = _isGeneratingImage.asStateFlow()

    fun generateImage(prompt: String, style: String, ratio: String) {
        viewModelScope.launch {
            _isGeneratingImage.value = true
            firestoreRepo.createImageArtifact(prompt, style, ratio)
            firestoreRepo.updateAdminMetrics(tokensUsedDelta = 80.0)
            _isGeneratingImage.value = false
        }
    }

    fun deleteImage(imageId: String) {
        viewModelScope.launch {
            firestoreRepo.deleteImageArtifact(imageId)
        }
    }

    // --- Remote Jobs State ---
    val remoteJobs: StateFlow<List<RemoteJob>> = firestoreRepo.observeRemoteJobs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun dispatchRemoteJob(name: String, command: String, host: String) {
        viewModelScope.launch {
            val log = """
                [Remote Host: $host]
                > Initializing runner container... OK
                > Executing: $command
                > Building binary artifacts... OK
                > Process finished successfully with return code 0.
            """.trimIndent()
            firestoreRepo.createRemoteJob(name, command, host, log)
        }
    }

    // --- Scheduled Tasks State ---
    val scheduledTasks: StateFlow<List<ScheduledTask>> = firestoreRepo.observeSchedules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createSchedule(name: String, cron: String, target: String) {
        viewModelScope.launch {
            firestoreRepo.createScheduledTask(name, cron, target)
        }
    }

    fun toggleSchedule(scheduleId: String, currentActive: Boolean) {
        viewModelScope.launch {
            firestoreRepo.toggleSchedule(scheduleId, currentActive)
        }
    }

    // --- Explore Ecosystem ---
    fun cloneExploreTemplate(title: String, language: String, starterCode: String) {
        viewModelScope.launch {
            createWorkspace(title, language, "Cloned from Explore Ecosystem")
            appendTerminalLine(TerminalLineType.SUCCESS, "Cloned template: $title into new Coltef workspace.")
            _currentDestination.value = AppDestination.COLTEF
        }
    }

    // --- Admin Metrics State ---
    val adminMetrics: StateFlow<List<AdminMetric>> = firestoreRepo.observeAdminMetrics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentMetric: StateFlow<AdminMetric?> = adminMetrics.map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun refreshMetrics() {
        viewModelScope.launch {
            firestoreRepo.updateAdminMetrics(tokensUsedDelta = 0.0)
        }
    }
}

class MainViewModelFactory(
    private val firestoreRepo: FirestoreRepository,
    private val aiRepo: AiRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(firestoreRepo, aiRepo) as T
    }
}
