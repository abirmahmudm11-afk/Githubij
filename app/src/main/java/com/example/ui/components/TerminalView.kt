package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class TerminalLine(
    val type: TerminalLineType,
    val text: String,
    val timestamp: String = ""
)

enum class TerminalLineType {
    COMMAND, OUTPUT, SUCCESS, ERROR, INFO
}

@Composable
fun TerminalView(
    lines: List<TerminalLine>,
    onExecuteCommand: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var commandInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val chipsScroll = rememberScrollState()

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0C0D10))
            .padding(8.dp)
            .testTag("coltef_terminal")
    ) {
        // Terminal Title Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFFEF4444))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFFF59E0B))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "coltef-agent ~ /workspace (codex v2.4)",
                    color = Color(0xFF9CA3AF),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = onClear,
                modifier = Modifier
                    .size(24.dp)
                    .testTag("clear_terminal_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear terminal",
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Suggestions Quick Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(chipsScroll)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val suggestions = listOf("run", "build", "lint", "test", "git status", "coltef audit", "help")
            for (cmd in suggestions) {
                Surface(
                    onClick = {
                        commandInput = cmd
                        onExecuteCommand(cmd)
                        commandInput = ""
                    },
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF1F2937),
                    modifier = Modifier.testTag("terminal_quick_$cmd")
                ) {
                    Text(
                        text = "$ $cmd",
                        color = Color(0xFF93C5FD),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = Color(0xFF1F2937), thickness = 0.5.dp)

        // Terminal Log Area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f, fill = false)
                .heightIn(min = 120.dp, max = 220.dp)
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            items(lines) { line ->
                val color = when (line.type) {
                    TerminalLineType.COMMAND -> Color(0xFF60A5FA)
                    TerminalLineType.SUCCESS -> Color(0xFF34D399)
                    TerminalLineType.ERROR -> Color(0xFFF87171)
                    TerminalLineType.INFO -> Color(0xFFFBBF24)
                    TerminalLineType.OUTPUT -> Color(0xFFE5E7EB)
                }
                Text(
                    text = line.text,
                    color = color,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }

        // Command Prompt Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF13151A), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "➜ coltef $ ",
                color = Color(0xFF34D399),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )

            BasicTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                textStyle = TextStyle(
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                ),
                cursorBrush = SolidColor(Color(0xFF34D399)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (commandInput.isNotBlank()) {
                            onExecuteCommand(commandInput.trim())
                            commandInput = ""
                        }
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("terminal_input_field")
            )

            IconButton(
                onClick = {
                    if (commandInput.isNotBlank()) {
                        onExecuteCommand(commandInput.trim())
                        commandInput = ""
                    }
                },
                modifier = Modifier
                    .size(24.dp)
                    .testTag("run_command_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Execute Command",
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
