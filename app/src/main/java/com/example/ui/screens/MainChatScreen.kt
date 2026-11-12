package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.ui.components.CodeBlockView
import com.example.ui.theme.CloukeEmerald
import kotlinx.coroutines.launch

@Composable
fun MainChatScreen(
    currentChat: ChatSession?,
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    streamingText: String,
    onSendMessage: (String) -> Unit,
    onNewChat: () -> Unit,
    onSendCodeToColtef: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var promptInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val suggestionsScroll = rememberScrollState()

    LaunchedEffect(messages.size, streamingText) {
        val totalItems = messages.size + (if (isGenerating && streamingText.isNotEmpty()) 1 else 0)
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sub-header: Active Chat Title & Model Badge
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(CloukeEmerald)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentChat?.model ?: "gemini-3.8-flash",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = currentChat?.title ?: "New Chat",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = onNewChat,
                    modifier = Modifier.testTag("new_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddComment,
                        contentDescription = "Start New Chat",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Message Thread Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty() && !isGenerating) {
                // Empty State: ChatGPT-style Welcome & Prompt Suggestions
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CloukeEmerald,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "What can I help you build today?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "Ask questions, analyze architectures, or scaffold complete code into Coltef.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    // Suggestion Cards Grid
                    val suggestions = listOf(
                        "Scaffold a Kotlin Room database for user notes" to "Build a complete Kotlin Room Database architecture with Dao, Entity, and repository layer.",
                        "Write a Coltef Python script to parse JSON logs" to "Write a clean Python script that parses structured JSON logs and calculates request frequencies.",
                        "Explain Gemini App Check attestation" to "Explain how Firebase App Check protects Android apps against unauthorized API calls with Attestation tokens.",
                        "Generate high-performance Compose list with caching" to "Write a Jetpack Compose LazyColumn with memory caching and derived state optimization."
                    )

                    for ((title, fullPrompt) in suggestions) {
                        Surface(
                            onClick = {
                                promptInput = fullPrompt
                                onSendMessage(fullPrompt)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("suggestion_card_${title.take(10)}")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowOutward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(messages) { message ->
                        ChatMessageItem(
                            message = message,
                            onSendCodeToColtef = onSendCodeToColtef
                        )
                    }

                    if (isGenerating && streamingText.isNotEmpty()) {
                        item {
                            ChatMessageItem(
                                message = ChatMessage(
                                    role = "assistant",
                                    content = streamingText
                                ),
                                onSendCodeToColtef = onSendCodeToColtef,
                                isStreaming = true
                            )
                        }
                    }
                }
            }
        }

        // Bottom Input Dock (ChatGPT Minimalist Pill Bar)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { /* Media attachment support */ },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Attach file",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                TextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    placeholder = {
                        Text(
                            "Message CLOUKE...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    maxLines = 4,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field")
                )

                IconButton(
                    onClick = {
                        if (promptInput.isNotBlank() && !isGenerating) {
                            val text = promptInput.trim()
                            promptInput = ""
                            onSendMessage(text)
                        }
                    },
                    enabled = promptInput.isNotBlank() && !isGenerating,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (promptInput.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = if (promptInput.isNotBlank() && !isGenerating) Color.White else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    onSendCodeToColtef: (String, String) -> Unit,
    isStreaming: Boolean = false
) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                shape = CircleShape,
                color = CloukeEmerald,
                modifier = Modifier
                    .size(28.dp)
                    .padding(top = 2.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
            border = if (!isUser) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
            modifier = Modifier.widthIn(max = 580.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Parse code blocks vs regular text
                val blocks = parseContentBlocks(message.content)
                for (block in blocks) {
                    when (block) {
                        is ContentBlock.Text -> {
                            Text(
                                text = block.text,
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        is ContentBlock.Code -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            CodeBlockView(
                                code = block.code,
                                language = block.language,
                                onSendToColtef = onSendCodeToColtef
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                if (isStreaming) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = CloukeEmerald
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Streaming from Gemini 3.8...",
                            fontSize = 11.sp,
                            color = CloukeEmerald
                        )
                    }
                }
            }
        }
    }
}

private sealed interface ContentBlock {
    data class Text(val text: String) : ContentBlock
    data class Code(val language: String, val code: String) : ContentBlock
}

private fun parseContentBlocks(raw: String): List<ContentBlock> {
    val result = mutableListOf<ContentBlock>()
    val fencePattern = Regex("```([a-zA-Z0-9_]*)\\n?([\\s\\S]*?)```")
    var lastIndex = 0

    for (match in fencePattern.findAll(raw)) {
        if (match.range.first > lastIndex) {
            val textPart = raw.substring(lastIndex, match.range.first).trim()
            if (textPart.isNotEmpty()) {
                result.add(ContentBlock.Text(textPart))
            }
        }
        val language = match.groupValues[1].ifBlank { "kotlin" }
        val code = match.groupValues[2].trimEnd()
        result.add(ContentBlock.Code(language, code))
        lastIndex = match.range.last + 1
    }

    if (lastIndex < raw.length) {
        val remaining = raw.substring(lastIndex).trim()
        if (remaining.isNotEmpty()) {
            result.add(ContentBlock.Text(remaining))
        }
    }

    return if (result.isEmpty()) listOf(ContentBlock.Text(raw)) else result
}
