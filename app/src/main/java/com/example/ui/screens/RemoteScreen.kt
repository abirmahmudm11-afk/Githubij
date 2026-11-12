package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RemoteJob
import com.example.ui.theme.CloukeEmerald

@Composable
fun RemoteScreen(
    jobs: List<RemoteJob>,
    onDispatchJob: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDispatchDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDispatchDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_dispatch_job")
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Dispatch Job")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Node Health Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(CloukeEmerald))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "Remote Cluster: runner-prod-01", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "CPU: 12% • RAM: 2.1 / 16 GB • Uptime: 99.98%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Surface(shape = RoundedCornerShape(6.dp), color = CloukeEmerald.copy(alpha = 0.15f)) {
                        Text(text = "ONLINE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CloukeEmerald, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }

            if (jobs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(text = "No Remote Jobs Dispatched", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text(text = "Trigger remote cloud builds, test runs, or benchmark scripts across runner nodes.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(jobs) { job ->
                        RemoteJobCard(job = job)
                    }
                }
            }
        }
    }

    if (showDispatchDialog) {
        var name by remember { mutableStateOf("") }
        var command by remember { mutableStateOf("gradle assembleRelease --no-daemon") }
        var host by remember { mutableStateOf("runner-prod-01") }

        AlertDialog(
            onDismissRequest = { showDispatchDialog = false },
            title = { Text("Dispatch Remote Job") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Job Name") },
                        modifier = Modifier.fillMaxWidth().testTag("job_name_input")
                    )
                    OutlinedTextField(
                        value = command,
                        onValueChange = { command = it },
                        label = { Text("Command") },
                        modifier = Modifier.fillMaxWidth().testTag("job_command_input")
                    )
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("Target Host") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && command.isNotBlank()) {
                            onDispatchJob(name.trim(), command.trim(), host.trim())
                            showDispatchDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_dispatch_job")
                ) {
                    Text("Dispatch")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDispatchDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RemoteJobCard(job: RemoteJob) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().testTag("job_card_${job.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = job.name.ifBlank { "Remote Job" }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "Host: ${job.host} • Status: ${job.status}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CloukeEmerald.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = job.status.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CloukeEmerald,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF18181B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "$ ${job.command}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF93C5FD),
                    modifier = Modifier.padding(8.dp)
                )
            }

            if (job.outputLog.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0F0F11),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = job.outputLog,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        color = Color(0xFFE4E4E7),
                        maxLines = 4,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
