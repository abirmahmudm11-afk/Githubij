package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val model: String = "gemini-3.8-flash",
    val pinned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "user", // "user" or "assistant"
    val content: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ColtefWorkspace(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val language: String = "Kotlin",
    val description: String = "",
    val status: String = "Active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ColtefFile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val path: String = "",
    val name: String = "",
    val content: String = "",
    val language: String = "kotlin",
    val size: Double = 0.0,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LibraryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val type: String = "prompt", // "prompt", "snippet", "persona", "template"
    val tags: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SoftwareProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val status: String = "In Development",
    val repoUrl: String = "",
    val environment: String = "Production",
    val branch: String = "main",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ImageArtifact(
    @DocumentId val id: String = "",
    val userId: String = "",
    val prompt: String = "",
    val style: String = "Minimal Cyber",
    val aspectRatio: String = "1:1",
    val status: String = "Ready",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class RemoteJob(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val command: String = "",
    val host: String = "runner-prod-01",
    val status: String = "Success",
    val outputLog: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ScheduledTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val cronExpression: String = "0 */4 * * *",
    val targetModule: String = "Coltef Linter",
    val active: Boolean = true,
    val lastRun: String = "Just now",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AdminMetric(
    @DocumentId val id: String = "",
    val userId: String = "",
    val tokenUsage: Double = 0.0,
    val requestCount: Double = 0.0,
    val averageLatencyMs: Double = 0.0,
    val activeSessions: Double = 0.0,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
