package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ColtefFile
import com.example.data.model.ColtefWorkspace
import com.example.ui.components.TerminalLine
import com.example.ui.components.TerminalLineType
import com.example.ui.components.TerminalView
import com.example.ui.theme.CloukeCobalt
import com.example.ui.theme.CloukeEmerald
import com.example.ui.theme.CodeBackgroundDark

@Composable
fun ColtefScreen(
    workspaces: List<ColtefWorkspace>,
    activeWorkspace: ColtefWorkspace?,
    files: List<ColtefFile>,
    activeFile: ColtefFile?,
    terminalLines: List<TerminalLine>,
    onSelectWorkspace: (ColtefWorkspace) -> Unit,
    onCreateWorkspace: (String, String, String) -> Unit,
    onSelectFile: (ColtefFile) -> Unit,
    onSaveFile: (String, String) -> Unit,
    onCreateFile: (String, String) -> Unit,
    onExecuteTerminal: (String) -> Unit,
    onClearTerminal: () -> Unit,
    onAiAssist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showCreateWorkspaceDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showAiAssistDialog by remember { mutableStateOf(false) }
    var aiInstruction by remember { mutableStateOf("") }
    var isTerminalExpanded by remember { mutableStateOf(true) }

    var editorContent by remember(activeFile?.id) { mutableStateOf(activeFile?.content ?: "") }
    val editorVerticalScroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Workspace Selector & Top Toolbar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = CloukeCobalt,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activeWorkspace?.name ?: "No Workspace Selected",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = activeWorkspace?.language ?: "General",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            if (activeFile != null) {
                                onSaveFile(activeFile.path, editorContent)
                                onExecuteTerminal("run ${activeFile.name}")
                            } else {
                                onExecuteTerminal("run")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CloukeEmerald),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("coltef_run_btn")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Run", fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = { showAiAssistDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("coltef_codex_btn")
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Codex AI", fontSize = 12.sp)
                    }

                    IconButton(
                        onClick = { showCreateWorkspaceDialog = true },
                        modifier = Modifier.size(32.dp).testTag("new_workspace_btn")
                    ) {
                        Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = "New Workspace", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Main Editor + File Tree Split Area
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Left Mini File Explorer
            Column(
                modifier = Modifier
                    .width(130.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant))
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXPLORER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = { showCreateFileDialog = true },
                        modifier = Modifier.size(22.dp).testTag("new_file_btn")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "Add file", modifier = Modifier.size(14.dp))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(files) { file ->
                        val isSelected = activeFile?.id == file.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectFile(file) }
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (file.name.endsWith(".kt")) Icons.Default.Code else Icons.Default.Description,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = file.name,
                                fontSize = 12.sp,
                                maxLines = 1,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Right Code Editor
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(CodeBackgroundDark)
            ) {
                // Tab Header Bar
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF18181B))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(files) { file ->
                        val isSelected = activeFile?.id == file.id
                        Surface(
                            onClick = { onSelectFile(file) },
                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                            color = if (isSelected) Color(0xFF27272A) else Color(0xFF131316),
                            border = if (isSelected) BorderStroke(1.dp, Color(0xFF3F3F46)) else null,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = file.name,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) Color.White else Color(0xFFA1A1AA)
                                )
                            }
                        }
                    }
                }

                // Editor Text Input Area with Line Numbers
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(editorVerticalScroll)
                        .padding(8.dp)
                ) {
                    val lines = editorContent.lines()
                    val lineCount = maxOf(lines.size, 1)
                    val lineNumbersText = (1..lineCount).joinToString("\n") { it.toString().padStart(3, ' ') }

                    Text(
                        text = lineNumbersText,
                        color = Color(0xFF52525B),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(end = 12.dp)
                    )

                    TextField(
                        value = editorContent,
                        onValueChange = {
                            editorContent = it
                            if (activeFile != null) {
                                onSaveFile(activeFile.path, it)
                            }
                        },
                        textStyle = LocalTextStyle.current.copy(
                            color = Color(0xFFE4E4E7),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("code_editor_field")
                    )
                }

                // Terminal Toggle Bar
                Surface(
                    onClick = { isTerminalExpanded = !isTerminalExpanded },
                    color = Color(0xFF18181B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isTerminalExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                contentDescription = null,
                                tint = Color(0xFFA1A1AA),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TERMINAL & CODEX RUNNER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA1A1AA),
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = "${terminalLines.size} events",
                            fontSize = 10.sp,
                            color = Color(0xFF71717A),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Collapsible Terminal
                if (isTerminalExpanded) {
                    TerminalView(
                        lines = terminalLines,
                        onExecuteCommand = onExecuteTerminal,
                        onClear = onClearTerminal,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Create Workspace Dialog
    if (showCreateWorkspaceDialog) {
        var wsName by remember { mutableStateOf("") }
        var wsLanguage by remember { mutableStateOf("Kotlin") }
        var wsDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateWorkspaceDialog = false },
            title = { Text("Create Coltef Workspace") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = wsName,
                        onValueChange = { wsName = it },
                        label = { Text("Workspace Name") },
                        modifier = Modifier.fillMaxWidth().testTag("ws_name_input")
                    )
                    OutlinedTextField(
                        value = wsLanguage,
                        onValueChange = { wsLanguage = it },
                        label = { Text("Primary Language (Kotlin, Python, TypeScript)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = wsDesc,
                        onValueChange = { wsDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (wsName.isNotBlank()) {
                            onCreateWorkspace(wsName.trim(), wsLanguage.trim(), wsDesc.trim())
                            showCreateWorkspaceDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_ws")
                ) {
                    Text("Create Workspace")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateWorkspaceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Create File Dialog
    if (showCreateFileDialog) {
        var fileName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateFileDialog = false },
            title = { Text("New File") },
            text = {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File Name (e.g. Utils.kt, app.py)") },
                    modifier = Modifier.fillMaxWidth().testTag("new_file_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fileName.isNotBlank()) {
                            val defaultContent = when {
                                fileName.endsWith(".kt") -> "// $fileName\n"
                                fileName.endsWith(".py") -> "# $fileName\n"
                                else -> "// $fileName\n"
                            }
                            onCreateFile("src/$fileName", defaultContent)
                            showCreateFileDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_file")
                ) {
                    Text("Add File")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Codex AI Assistant Dialog
    if (showAiAssistDialog) {
        AlertDialog(
            onDismissRequest = { showAiAssistDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = CloukeEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Coltef Codex Assistant")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Instruct Coltef to refactor, write unit tests, or implement a function for ${activeFile?.name ?: "active file"}:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = aiInstruction,
                        onValueChange = { aiInstruction = it },
                        placeholder = { Text("e.g. Add validation logic and handle null cases...") },
                        modifier = Modifier.fillMaxWidth().height(120.dp).testTag("codex_instruction_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (aiInstruction.isNotBlank()) {
                            val instruction = aiInstruction.trim()
                            showAiAssistDialog = false
                            aiInstruction = ""
                            onAiAssist(instruction)
                        }
                    },
                    modifier = Modifier.testTag("confirm_codex_run")
                ) {
                    Text("Execute Codex")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiAssistDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
