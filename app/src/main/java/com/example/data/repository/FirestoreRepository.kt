package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.*
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- User Profile ---
    suspend fun syncUserProfile(displayName: String, email: String, photoUrl: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val payload = mapOf(
            "userId" to uid,
            "displayName" to displayName,
            "email" to email,
            "photoUrl" to photoUrl,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
        }
    }

    // --- Chats ---
    fun observeChats(): Flow<List<ChatSession>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/chats"
        emitAll(
            db.collection("users").document(uid).collection("chats")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(ChatSession::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createChat(title: String, model: String = "gemini-3.8-flash"): String {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("chats").document()
        val payload = mapOf(
            "userId" to uid,
            "title" to title,
            "model" to model,
            "pinned" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteChat(chatId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("chats").document(chatId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun pinChat(chatId: String, pinned: Boolean) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("chats").document(chatId)
        try {
            docRef.update("pinned", pinned, "updatedAt", FieldValue.serverTimestamp()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun renameChat(chatId: String, newTitle: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("chats").document(chatId)
        try {
            docRef.update("title", newTitle, "updatedAt", FieldValue.serverTimestamp()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Messages ---
    fun observeMessages(chatId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/chats/$chatId/messages"
        emitAll(
            db.collection("users").document(uid).collection("chats").document(chatId).collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(ChatMessage::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addMessage(chatId: String, role: String, content: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("chats").document(chatId).collection("messages").document()
        val payload = mapOf(
            "userId" to uid,
            "role" to role,
            "content" to content,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            // Touch chat updatedAt
            db.collection("users").document(uid).collection("chats").document(chatId)
                .update("updatedAt", FieldValue.serverTimestamp()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- Coltef Workspaces ---
    fun observeWorkspaces(): Flow<List<ColtefWorkspace>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/workspaces"
        emitAll(
            db.collection("users").document(uid).collection("workspaces")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(ColtefWorkspace::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createWorkspace(name: String, language: String, description: String): String {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("workspaces").document()
        val payload = mapOf(
            "userId" to uid,
            "name" to name,
            "language" to language,
            "description" to description,
            "status" to "Ready",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            // Seed a starter file for the workspace
            val defaultFileContent = when (language.lowercase()) {
                "kotlin" -> """// CLOUKE Coltef Engine Workspace: $name
package com.clouke.project

fun main() {
    println("Welcome to Coltef Code Runner!")
    println("Engine active. Ready for instructions.")
}
"""
                "python" -> """# CLOUKE Coltef Engine Workspace: $name

def main():
    print("Welcome to Coltef Python Agent!")
    print("Running project: $name")

if __name__ == "__main__":
    main()
"""
                else -> """// $name Project Main Source
console.log("CLOUKE Coltef Workspace running!");
"""
            }
            val fileName = when (language.lowercase()) {
                "kotlin" -> "Main.kt"
                "python" -> "main.py"
                else -> "index.ts"
            }
            saveWorkspaceFile(docRef.id, "src/$fileName", fileName, defaultFileContent, language.lowercase())
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteWorkspace(workspaceId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("workspaces").document(workspaceId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Coltef Workspace Files ---
    fun observeWorkspaceFiles(workspaceId: String): Flow<List<ColtefFile>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/workspaces/$workspaceId/files"
        emitAll(
            db.collection("users").document(uid).collection("workspaces").document(workspaceId).collection("files")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(ColtefFile::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveWorkspaceFile(
        workspaceId: String,
        filePath: String,
        name: String,
        content: String,
        language: String
    ) {
        val uid = requireUserId()
        val safeDocId = filePath.replace("/", "_").replace(".", "_")
        val docRef = db.collection("users").document(uid).collection("workspaces").document(workspaceId).collection("files").document(safeDocId)
        val payload = mapOf(
            "userId" to uid,
            "path" to filePath,
            "name" to name,
            "content" to content,
            "language" to language,
            "size" to content.length.toDouble(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            // Touch workspace updatedAt
            db.collection("users").document(uid).collection("workspaces").document(workspaceId)
                .update("updatedAt", FieldValue.serverTimestamp()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteWorkspaceFile(workspaceId: String, fileDocId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("workspaces").document(workspaceId).collection("files").document(fileDocId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Library Items ---
    fun observeLibrary(): Flow<List<LibraryItem>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/library"
        emitAll(
            db.collection("users").document(uid).collection("library")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(LibraryItem::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createLibraryItem(title: String, content: String, type: String, tags: List<String>) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("library").document()
        val payload = mapOf(
            "userId" to uid,
            "title" to title,
            "content" to content,
            "type" to type,
            "tags" to tags,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteLibraryItem(itemId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("library").document(itemId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Projects ---
    fun observeProjects(): Flow<List<SoftwareProject>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/projects"
        emitAll(
            db.collection("users").document(uid).collection("projects")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(SoftwareProject::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createProject(name: String, description: String, repoUrl: String, environment: String, branch: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("projects").document()
        val payload = mapOf(
            "userId" to uid,
            "name" to name,
            "description" to description,
            "status" to "Active",
            "repoUrl" to repoUrl,
            "environment" to environment,
            "branch" to branch,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteProject(projectId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Images ---
    fun observeImages(): Flow<List<ImageArtifact>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/images"
        emitAll(
            db.collection("users").document(uid).collection("images")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(ImageArtifact::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createImageArtifact(prompt: String, style: String, aspectRatio: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("images").document()
        val payload = mapOf(
            "userId" to uid,
            "prompt" to prompt,
            "style" to style,
            "aspectRatio" to aspectRatio,
            "status" to "Generated",
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteImageArtifact(imageId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("images").document(imageId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Remote Jobs ---
    fun observeRemoteJobs(): Flow<List<RemoteJob>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/remote_jobs"
        emitAll(
            db.collection("users").document(uid).collection("remote_jobs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(RemoteJob::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createRemoteJob(name: String, command: String, host: String, outputLog: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("remote_jobs").document()
        val payload = mapOf(
            "userId" to uid,
            "name" to name,
            "command" to command,
            "host" to host,
            "status" to "Completed",
            "outputLog" to outputLog,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- Scheduled Tasks ---
    fun observeSchedules(): Flow<List<ScheduledTask>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/schedules"
        emitAll(
            db.collection("users").document(uid).collection("schedules")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(ScheduledTask::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createScheduledTask(name: String, cronExpression: String, targetModule: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("schedules").document()
        val payload = mapOf(
            "userId" to uid,
            "name" to name,
            "cronExpression" to cronExpression,
            "targetModule" to targetModule,
            "active" to true,
            "lastRun" to "Scheduled",
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleSchedule(scheduleId: String, currentActive: Boolean) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("schedules").document(scheduleId)
        try {
            docRef.update("active", !currentActive).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Admin Metrics ---
    fun observeAdminMetrics(): Flow<List<AdminMetric>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/admin_metrics"
        emitAll(
            db.collection("users").document(uid).collection("admin_metrics")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(AdminMetric::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun updateAdminMetrics(tokensUsedDelta: Double) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("admin_metrics").document("current_usage")
        val payload = mapOf(
            "userId" to uid,
            "tokenUsage" to FieldValue.increment(tokensUsedDelta),
            "requestCount" to FieldValue.increment(1.0),
            "averageLatencyMs" to 142.0,
            "activeSessions" to 1.0,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }
}
