package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.repository.AiModelTier
import com.example.ui.components.CodeBlockView
import com.example.ui.theme.CloukeCobalt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainChatScreen(
    currentChat: ChatSession?,
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    streamingText: String,
    selectedModel: AiModelTier,
    availableModels: List<AiModelTier>,
    pendingAttachment: String?,
    isVoiceActive: Boolean,
    onSelectModel: (AiModelTier) -> Unit,
    onSendMessage: (String) -> Unit,
    onStopGeneration: () -> Unit,
    onRegenerate: () -> Unit,
    onNewChat: () -> Unit,
    onAttachFile: (String, String) -> Unit,
    onClearAttachment: () -> Unit,
    onToggleVoice: () -> Unit,
    onSendCodeToColtef: (String, String) -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var promptInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showModelSheet by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }

    // Scroll smoothly to bottom when new messages or streaming chunks arrive
    LaunchedEffect(messages.size, streamingText) {
        val total = messages.size + (if (isGenerating && streamingText.isNotEmpty()) 1 else 0)
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Reference Header: Hamburger Menu on Left, Model Selector Pill in Center, New Chat on Right
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("chat_open_drawer_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Open Navigation Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Model Selector Pill
                    Surface(
                        onClick = { showModelSheet = true },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.testTag("model_selector_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(CloukeCobalt)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedModel.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Select Model",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onNewChat,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("chat_new_chat_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddComment,
                            contentDescription = "Start New Chat",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Floating Pill Composer matching reference images
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Staged Attachment indicator if any
                if (!pendingAttachment.isNullOrEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Attachment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = pendingAttachment,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onClearAttachment,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove attachment",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Voice Recording Active Banner
                if (isVoiceActive) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Listening... Speak your prompt or instruction",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onToggleVoice) {
                                Text("Done", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Main Pill Input Bar
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Plus (+) Attachment Button
                        IconButton(
                            onClick = { showAttachmentSheet = true },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("chat_attachment_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add attachment or tool",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Multiline text input
                        TextField(
                            value = promptInput,
                            onValueChange = { promptInput = it },
                            placeholder = {
                                Text(
                                    text = "Ask about your past thinking or add a new note...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (promptInput.isNotBlank() && !isGenerating) {
                                        val text = promptInput.trim()
                                        promptInput = ""
                                        onSendMessage(text)
                                    }
                                }
                            ),
                            maxLines = 5,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_field")
                        )

                        // Microphone Button
                        IconButton(
                            onClick = {
                                onToggleVoice()
                                if (!isVoiceActive && promptInput.isBlank()) {
                                    promptInput = "Synthesize an architectural plan for "
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("chat_voice_button")
                        ) {
                            Icon(
                                imageVector = if (isVoiceActive) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = if (isVoiceActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Send or Stop Generation Button
                        if (isGenerating) {
                            // Stop Generation Button
                            IconButton(
                                onClick = onStopGeneration,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface)
                                    .testTag("chat_stop_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Generation",
                                    tint = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            val canSend = promptInput.isNotBlank() || !pendingAttachment.isNullOrEmpty()
                            IconButton(
                                onClick = {
                                    if (canSend) {
                                        val text = promptInput.trim()
                                        promptInput = ""
                                        onSendMessage(text)
                                    }
                                },
                                enabled = canSend,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (canSend) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                                    .testTag("chat_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Send Message",
                                    tint = if (canSend) MaterialTheme.colorScheme.surface else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (messages.isEmpty() && !isGenerating) {
                // ==========================================
                // REFERENCE WELCOME SCREEN (matching screenshot)
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Square black logo mark with rounded corners
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF111315),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "CLOUKE Logo",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Hey Developer!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "How can I help you?",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp, bottom = 28.dp)
                    )

                    // Reference Action Pills (matching screenshot pills)
                    val starterPills = listOf(
                        "Revisit a past idea" to "Review previous system architecture proposals and identify evolution points.",
                        "Capture a new thought" to "Draft a structured technical specification for my new application module.",
                        "Review my thinking on a topic" to "Analyze the trade-offs between local SQLite caching and Cloud Firestore.",
                        "Connect related ideas" to "How does Coltef developer workspace connect with CLOUKE conversational intelligence?",
                        "Scaffold a Kotlin project" to "Write a clean Kotlin Room database architecture with DAO, repository, and models.",
                        "Write a Coltef Python script" to "Create a Python script that parses JSON telemetry logs and computes latencies."
                    )

                    for ((label, fullPrompt) in starterPills) {
                        Surface(
                            onClick = {
                                onSendMessage(fullPrompt)
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .testTag("starter_pill_${label.take(10)}")
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            } else {
                // Conversation Thread
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
                ) {
                    items(messages, key = { it.id.ifEmpty { it.hashCode().toString() } }) { message ->
                        ChatMessageRow(
                            message = message,
                            onSendCodeToColtef = onSendCodeToColtef,
                            onRegenerate = onRegenerate,
                            onEdit = {
                                promptInput = message.content
                            }
                        )
                    }

                    if (isGenerating && streamingText.isNotEmpty()) {
                        item(key = "streaming_response") {
                            ChatMessageRow(
                                message = ChatMessage(
                                    role = "assistant",
                                    content = streamingText
                                ),
                                onSendCodeToColtef = onSendCodeToColtef,
                                onRegenerate = onRegenerate,
                                onEdit = {},
                                isStreaming = true
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Model Selection Bottom Sheet ---
    if (showModelSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModelSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Select Intelligence Tier",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Powered by official Firebase AI with Gemini integration",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                )

                for (model in availableModels) {
                    val isSelected = model.id == selectedModel.id
                    Surface(
                        onClick = {
                            onSelectModel(model)
                            showModelSheet = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("model_option_${model.badge.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = model.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) CloukeCobalt else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = model.badge,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = model.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // --- Attachment Picker Sheet ---
    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Add to Conversation",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val attachmentOptions = listOf(
                    Triple(Icons.Default.Code, "Coltef Active Source Code", "Attach current editor file for contextual debugging") to {
                        onAttachFile("ColtefSource.kt", "Active developer workspace source code")
                    },
                    Triple(Icons.Default.Description, "Technical Project Spec", "Attach system requirements and architecture outline") to {
                        onAttachFile("ProjectSpec.md", "Complete architectural requirements document")
                    },
                    Triple(Icons.Default.Terminal, "Terminal Execution Log", "Attach recent sandbox run logs and crash stacktrace") to {
                        onAttachFile("ExecutionLog.txt", "Coltef sandbox terminal output")
                    }
                )

                for ((item, action) in attachmentOptions) {
                    val (icon, title, desc) = item
                    Surface(
                        onClick = {
                            action()
                            showAttachmentSheet = false
                            Toast.makeText(context, "Attached: $title", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = desc,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ChatMessageRow(
    message: ChatMessage,
    onSendCodeToColtef: (String, String) -> Unit,
    onRegenerate: () -> Unit,
    onEdit: () -> Unit,
    isStreaming: Boolean = false
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isUser = message.role == "user"
    var feedbackState by remember { mutableStateOf<Int?>(null) } // 1 for thumbs up, -1 for down

    if (isUser) {
        // User message: cleanly aligned right in a rounded container
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .testTag("user_chat_bubble")
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = message.content,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    } else {
        // Assistant message: NATURAL PRESENTATION DIRECTLY ON THE CANVAS!
        // No heavy card wrapper around the whole text!
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp)
                .testTag("assistant_message_content")
        ) {
            // Render structured markdown paragraphs, collapsible timeline, and code blocks
            RenderStructuredAssistantResponse(
                content = message.content,
                onSendCodeToColtef = onSendCodeToColtef
            )

            if (isStreaming) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(CloukeCobalt)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Generating response...",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Action Bar under assistant message
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(message.content))
                            Toast.makeText(context, "Copied response", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("msg_copy_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Response",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = onRegenerate,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("msg_regenerate_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Regenerate Response",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            feedbackState = if (feedbackState == 1) null else 1
                            Toast.makeText(context, "Feedback recorded: Helpful", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("msg_thumbs_up_btn")
                    ) {
                        Icon(
                            imageVector = if (feedbackState == 1) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                            contentDescription = "Thumbs Up",
                            tint = if (feedbackState == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            feedbackState = if (feedbackState == -1) null else -1
                            Toast.makeText(context, "Feedback recorded: Needs improvement", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("msg_thumbs_down_btn")
                    ) {
                        Icon(
                            imageVector = if (feedbackState == -1) Icons.Default.ThumbDown else Icons.Default.ThumbDownOffAlt,
                            contentDescription = "Thumbs Down",
                            tint = if (feedbackState == -1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, message.content)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share CLOUKE Response"))
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("msg_share_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Response",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderStructuredAssistantResponse(
    content: String,
    onSendCodeToColtef: (String, String) -> Unit
) {
    val blocks = remember(content) { parseResponseBlocks(content) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (block in blocks) {
            when (block) {
                is ParsedBlock.Heading -> {
                    Text(
                        text = block.text,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                is ParsedBlock.Paragraph -> {
                    Text(
                        text = block.text,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                is ParsedBlock.BulletItem -> {
                    Row(modifier = Modifier.padding(start = 4.dp)) {
                        Text(
                            text = "•",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = block.text,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                is ParsedBlock.TimelineBlock -> {
                    // Reference Timeline Card (matching reference image screen 2)
                    TimelineThinkingCard(
                        title = block.title,
                        steps = block.steps
                    )
                }
                is ParsedBlock.CodeSnippet -> {
                    CodeBlockView(
                        code = block.code,
                        language = block.language,
                        onSendToColtef = onSendCodeToColtef
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineThinkingCard(
    title: String,
    steps: List<Pair<String, String>>
) {
    var isExpanded by remember { mutableStateOf(true) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for ((timePeriod, detail) in steps) {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CloukeCobalt)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = timePeriod,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = detail,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed interface ParsedBlock {
    data class Heading(val text: String) : ParsedBlock
    data class Paragraph(val text: String) : ParsedBlock
    data class BulletItem(val text: String) : ParsedBlock
    data class TimelineBlock(val title: String, val steps: List<Pair<String, String>>) : ParsedBlock
    data class CodeSnippet(val language: String, val code: String) : ParsedBlock
}

private fun parseResponseBlocks(raw: String): List<ParsedBlock> {
    val blocks = mutableListOf<ParsedBlock>()
    val fencePattern = Regex("```([a-zA-Z0-9_]*)\\n?([\\s\\S]*?)```")
    var lastIndex = 0

    // Look for code blocks first
    for (match in fencePattern.findAll(raw)) {
        if (match.range.first > lastIndex) {
            val textChunk = raw.substring(lastIndex, match.range.first)
            blocks.addAll(parseTextParagraphs(textChunk))
        }
        val lang = match.groupValues[1].ifBlank { "kotlin" }
        val code = match.groupValues[2].trimEnd()
        blocks.add(ParsedBlock.CodeSnippet(lang, code))
        lastIndex = match.range.last + 1
    }

    if (lastIndex < raw.length) {
        val remaining = raw.substring(lastIndex)
        blocks.addAll(parseTextParagraphs(remaining))
    }

    return if (blocks.isEmpty()) listOf(ParsedBlock.Paragraph(raw)) else blocks
}

private fun parseTextParagraphs(textChunk: String): List<ParsedBlock> {
    val result = mutableListOf<ParsedBlock>()
    val lines = textChunk.lines()
    var currentParagraph = StringBuilder()

    for (line in lines) {
        val trimmed = line.trim()
        when {
            trimmed.startsWith("#") -> {
                if (currentParagraph.isNotBlank()) {
                    result.add(ParsedBlock.Paragraph(currentParagraph.toString().trim()))
                    currentParagraph = StringBuilder()
                }
                val headingText = trimmed.replace(Regex("^#+\\s*"), "")
                result.add(ParsedBlock.Heading(headingText))
            }
            trimmed.startsWith("•") || trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                if (currentParagraph.isNotBlank()) {
                    result.add(ParsedBlock.Paragraph(currentParagraph.toString().trim()))
                    currentParagraph = StringBuilder()
                }
                val bullet = trimmed.replace(Regex("^[•\\-*]\\s*"), "")
                result.add(ParsedBlock.BulletItem(bullet))
            }
            trimmed.isEmpty() -> {
                if (currentParagraph.isNotBlank()) {
                    result.add(ParsedBlock.Paragraph(currentParagraph.toString().trim()))
                    currentParagraph = StringBuilder()
                }
            }
            else -> {
                if (currentParagraph.isNotEmpty()) currentParagraph.append(" ")
                currentParagraph.append(trimmed)
            }
        }
    }

    if (currentParagraph.isNotBlank()) {
        result.add(ParsedBlock.Paragraph(currentParagraph.toString().trim()))
    }

    return result
}
