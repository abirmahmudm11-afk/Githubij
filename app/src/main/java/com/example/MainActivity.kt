package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.auth.AuthManager
import com.example.data.repository.AiRepository
import com.example.data.repository.FirestoreRepository
import com.example.ui.components.GoogleSignInScreen
import com.example.ui.components.NavigationShell
import com.example.ui.navigation.AppDestination
import com.example.ui.screens.*
import com.example.ui.theme.CloukeTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.MainViewModelFactory
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureAppCheck(this, intent)
        enableEdgeToEdge()

        val authManager = AuthManager(this)

        setContent {
            CloukeTheme {
                AppContent(authManager = authManager)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(authManager: AuthManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        authManager.attemptAutoSignIn(
            scope = scope,
            onAuthSuccess = { currentUser = Firebase.auth.currentUser },
            onUnauthenticated = { /* stay on sign-in screen */ }
        )
    }

    if (currentUser == null) {
        GoogleSignInScreen(
            authManager = authManager,
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        val uid = currentUser!!.uid
        val userEmail = currentUser!!.email ?: "developer@clouke.ai"

        key(uid) {
            val firestoreRepo = remember(uid) { FirestoreRepository(context) }
            val aiRepo = remember { AiRepository() }
            val viewModel: MainViewModel = viewModel(
                key = uid,
                factory = MainViewModelFactory(firestoreRepo, aiRepo)
            )

            LaunchedEffect(uid) {
                firestoreRepo.syncUserProfile(
                    displayName = currentUser!!.displayName ?: "CLOUKE Developer",
                    email = userEmail,
                    photoUrl = currentUser!!.photoUrl?.toString() ?: ""
                )
            }

            val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
            val chats by viewModel.chats.collectAsStateWithLifecycle()
            val activeChatId by viewModel.activeChatId.collectAsStateWithLifecycle()
            val chatSearchQuery by viewModel.chatSearchQuery.collectAsStateWithLifecycle()

            if (currentDestination != AppDestination.CHAT) {
                BackHandler {
                    viewModel.navigateTo(AppDestination.CHAT)
                }
            }

            NavigationShell(
                currentDestination = currentDestination,
                onNavigate = { dest -> viewModel.navigateTo(dest) },
                chats = chats,
                activeChatId = activeChatId,
                searchQuery = chatSearchQuery,
                onSearchQueryChange = { query -> viewModel.setSearchQuery(query) },
                onSelectChat = { id -> viewModel.selectChat(id) },
                onNewChat = { viewModel.startNewChat() },
                onPinChat = { id, pinned -> viewModel.pinChat(id, pinned) },
                onRenameChat = { id, title -> viewModel.renameChat(id, title) },
                onDeleteChat = { id -> viewModel.deleteChat(id) },
                userEmail = userEmail,
                onSignOut = {
                    authManager.signOut(scope) {
                        currentUser = null
                    }
                },
                drawerState = drawerState,
                scope = scope
            ) { innerPadding ->
                Surface(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    when (currentDestination) {
                        AppDestination.CHAT -> {
                            val currentChat by viewModel.currentChat.collectAsStateWithLifecycle()
                            val messages by viewModel.messages.collectAsStateWithLifecycle()
                            val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
                            val streamingText by viewModel.streamingText.collectAsStateWithLifecycle()
                            val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
                            val pendingAttachment by viewModel.pendingAttachment.collectAsStateWithLifecycle()
                            val isVoiceActive by viewModel.isVoiceActive.collectAsStateWithLifecycle()

                            MainChatScreen(
                                currentChat = currentChat,
                                messages = messages,
                                isGenerating = isGenerating,
                                streamingText = streamingText,
                                selectedModel = selectedModel,
                                availableModels = viewModel.availableModels,
                                pendingAttachment = pendingAttachment,
                                isVoiceActive = isVoiceActive,
                                onSelectModel = { model -> viewModel.selectModel(model) },
                                onSendMessage = { text -> viewModel.sendChatMessage(text) },
                                onStopGeneration = { viewModel.stopGeneration() },
                                onRegenerate = { viewModel.regenerateLastResponse() },
                                onNewChat = { viewModel.startNewChat() },
                                onAttachFile = { name, summary -> viewModel.attachFile(name, summary) },
                                onClearAttachment = { viewModel.clearAttachment() },
                                onToggleVoice = { viewModel.toggleVoiceListening() },
                                onSendCodeToColtef = { lang, code -> viewModel.sendCodeFromChatToColtef(lang, code) },
                                onOpenDrawer = { scope.launch { drawerState.open() } }
                            )
                        }

                        AppDestination.COLTEF -> {
                            val workspaces by viewModel.workspaces.collectAsStateWithLifecycle()
                            val activeWorkspace by viewModel.activeWorkspace.collectAsStateWithLifecycle()
                            val workspaceFiles by viewModel.workspaceFiles.collectAsStateWithLifecycle()
                            val activeFile by viewModel.activeFile.collectAsStateWithLifecycle()
                            val terminalLines by viewModel.terminalLines.collectAsStateWithLifecycle()

                            ColtefScreen(
                                workspaces = workspaces,
                                activeWorkspace = activeWorkspace,
                                files = workspaceFiles,
                                activeFile = activeFile,
                                terminalLines = terminalLines,
                                onSelectWorkspace = { ws -> viewModel.selectWorkspace(ws) },
                                onCreateWorkspace = { name, lang, desc -> viewModel.createWorkspace(name, lang, desc) },
                                onSelectFile = { file -> viewModel.selectFile(file) },
                                onSaveFile = { path, content -> viewModel.saveWorkspaceFile(path, content) },
                                onCreateFile = { path, content -> viewModel.createWorkspaceFile(path, content) },
                                onExecuteTerminal = { cmd -> viewModel.executeTerminalCommand(cmd) },
                                onClearTerminal = { viewModel.clearTerminal() },
                                onAiAssist = { instr -> viewModel.runCodexAiAssist(instr) }
                            )
                        }

                        AppDestination.LIBRARY -> {
                            val items by viewModel.libraryItems.collectAsStateWithLifecycle()
                            LibraryScreen(
                                items = items,
                                onCreateItem = { title, content, type, tags ->
                                    viewModel.createLibraryItem(title, content, type, tags)
                                },
                                onDeleteItem = { id -> viewModel.deleteLibraryItem(id) },
                                onUseInChat = { content -> viewModel.useLibraryItemInChat(content) }
                            )
                        }

                        AppDestination.PROJECTS -> {
                            val projects by viewModel.projects.collectAsStateWithLifecycle()
                            ProjectsScreen(
                                projects = projects,
                                onCreateProject = { name, desc, repo, env, branch ->
                                    viewModel.createProject(name, desc, repo, env, branch)
                                },
                                onDeleteProject = { id -> viewModel.deleteProject(id) },
                                onOpenInColtef = { proj -> viewModel.openProjectInColtef(proj) }
                            )
                        }

                        AppDestination.IMAGES -> {
                            val images by viewModel.images.collectAsStateWithLifecycle()
                            val isGeneratingImage by viewModel.isGeneratingImage.collectAsStateWithLifecycle()

                            ImagesScreen(
                                images = images,
                                isGenerating = isGeneratingImage,
                                onGenerateImage = { prompt, style, ratio ->
                                    viewModel.generateImage(prompt, style, ratio)
                                },
                                onDeleteImage = { id -> viewModel.deleteImage(id) }
                            )
                        }

                        AppDestination.REMOTE -> {
                            val jobs by viewModel.remoteJobs.collectAsStateWithLifecycle()
                            RemoteScreen(
                                jobs = jobs,
                                onDispatchJob = { name, cmd, host ->
                                    viewModel.dispatchRemoteJob(name, cmd, host)
                                }
                            )
                        }

                        AppDestination.SCHEDULED -> {
                            val schedules by viewModel.scheduledTasks.collectAsStateWithLifecycle()
                            ScheduledScreen(
                                tasks = schedules,
                                onCreateSchedule = { name, cron, target ->
                                    viewModel.createSchedule(name, cron, target)
                                },
                                onToggleActive = { id, active ->
                                    viewModel.toggleSchedule(id, active)
                                }
                            )
                        }

                        AppDestination.EXPLORE -> {
                            ExploreScreen(
                                onCloneToColtef = { title, lang, starter ->
                                    viewModel.cloneExploreTemplate(title, lang, starter)
                                }
                            )
                        }

                        AppDestination.ADMIN -> {
                            val metric by viewModel.currentMetric.collectAsStateWithLifecycle()
                            AdminScreen(
                                metric = metric,
                                onRefreshMetrics = { viewModel.refreshMetrics() }
                            )
                        }
                    }
                }
            }
        }
    }
}
