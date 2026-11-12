package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(
    val route: String,
    val title: String,
    val shortLabel: String,
    val icon: ImageVector,
    val description: String
) {
    CHAT(
        route = "chat",
        title = "CLOUKE AI Chat",
        shortLabel = "Chat",
        icon = Icons.Default.AutoAwesome,
        description = "Intelligent conversational assistant with streaming & code handoff"
    ),
    COLTEF(
        route = "coltef",
        title = "Coltef Workspace",
        shortLabel = "Coltef",
        icon = Icons.Default.Code,
        description = "Full IDE with file explorer, editor, live preview & virtual terminal"
    ),
    LIBRARY(
        route = "library",
        title = "Resource Library",
        shortLabel = "Library",
        icon = Icons.AutoMirrored.Filled.MenuBook,
        description = "Saved prompts, code snippets, custom AI personas & templates"
    ),
    PROJECTS(
        route = "projects",
        title = "Project Portfolio",
        shortLabel = "Projects",
        icon = Icons.Default.Folder,
        description = "Tracked active workspaces, Git repositories, branches & environments"
    ),
    IMAGES(
        route = "images",
        title = "Image Studio",
        shortLabel = "Images",
        icon = Icons.Default.Image,
        description = "AI image generation, aspect-ratio tuning & creative canvas"
    ),
    REMOTE(
        route = "remote",
        title = "Remote Runners",
        shortLabel = "Remote",
        icon = Icons.Default.CloudQueue,
        description = "Remote execution workers, build nodes & deployment webhooks"
    ),
    SCHEDULED(
        route = "scheduled",
        title = "Scheduled Tasks",
        shortLabel = "Scheduled",
        icon = Icons.Default.Schedule,
        description = "Automated cron triggers, periodic audits & scheduled prompts"
    ),
    EXPLORE(
        route = "explore",
        title = "Explore Ecosystem",
        shortLabel = "Explore",
        icon = Icons.Default.Explore,
        description = "Curated starter templates, plugins, AI agents & community workflows"
    ),
    ADMIN(
        route = "admin",
        title = "Admin Telemetry",
        shortLabel = "Admin",
        icon = Icons.Default.AdminPanelSettings,
        description = "Real token consumption, API quotas, latency & security health"
    )
}
