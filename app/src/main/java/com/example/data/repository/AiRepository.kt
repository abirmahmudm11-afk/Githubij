package com.example.data.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "AiRepository"

data class AiModelTier(
    val id: String,
    val name: String,
    val description: String,
    val badge: String,
    val isDefault: Boolean = false
)

class AiRepository {

    companion object {
        val SUPPORTED_MODELS = listOf(
            AiModelTier(
                id = "gemini-3.8-flash",
                name = "CLOUKE Balanced",
                description = "Default high-speed multimodal assistant with code reasoning",
                badge = "Balanced",
                isDefault = true
            ),
            AiModelTier(
                id = "gemini-2.5-flash",
                name = "CLOUKE Fast",
                description = "Ultra-low latency for instant answers and quick iteration",
                badge = "Fast"
            ),
            AiModelTier(
                id = "gemini-2.5-pro",
                name = "CLOUKE Reasoning",
                description = "Deep architectural analysis, math, and comprehensive synthesis",
                badge = "Reasoning"
            ),
            AiModelTier(
                id = "gemini-3.8-flash-code",
                name = "Coltef Coding",
                description = "Specialized for full-stack Kotlin, Python, and TypeScript development",
                badge = "Coding"
            )
        )
    }

    private fun resolveGenerativeModel(modelId: String) = try {
        val targetEngine = when (modelId) {
            "gemini-2.5-flash" -> "gemini-2.5-flash"
            "gemini-2.5-pro" -> "gemini-2.5-pro"
            else -> "gemini-3.8-flash"
        }
        Firebase.ai.generativeModel(targetEngine)
    } catch (e: Exception) {
        Log.w(TAG, "Fallback to default gemini-3.8-flash: ${e.message}")
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    fun generateStream(
        prompt: String,
        historyMessages: List<Pair<String, String>> = emptyList(),
        modelId: String = "gemini-3.8-flash"
    ): Flow<String> = flow {
        try {
            val model = resolveGenerativeModel(modelId)

            val systemContext = when (modelId) {
                "gemini-3.8-flash-code" -> """
                    You are CLOUKE AI with specialized Coltef Code Intelligence.
                    Deliver clean, idiomatic, production-ready code. Always specify explicit language identifiers in triple backtick code blocks. Include concise explanations and emphasize maintainability, safety, and modern patterns.
                """.trimIndent()
                "gemini-2.5-pro" -> """
                    You are CLOUKE AI in Reasoning Mode.
                    Break down complex inquiries systematically. When explaining multi-step thinking or timelines, structure your thoughts with clear markdown headings and bulleted timelines where appropriate.
                """.trimIndent()
                "gemini-2.5-flash" -> """
                    You are CLOUKE AI in Fast Mode.
                    Deliver direct, concise, and high-impact answers with minimal fluff.
                """.trimIndent()
                else -> """
                    You are CLOUKE AI, an advanced conversational intelligence and developer companion powering the CLOUKE + Coltef workspace.
                    Provide insightful, production-grade, highly structured answers. Format code blocks using triple backticks with explicit language identifiers. Maintain a natural, direct, and professional tone.
                """.trimIndent()
            }

            val fullPrompt = buildString {
                appendLine(systemContext)
                if (historyMessages.isNotEmpty()) {
                    appendLine("--- Previous Conversation Context ---")
                    for ((role, text) in historyMessages.takeLast(8)) {
                        appendLine("[$role]: $text")
                    }
                    appendLine("--- End Previous Context ---")
                }
                appendLine("User: $prompt")
            }

            withTimeout(75_000L) {
                model.generateContentStream(fullPrompt).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) {
                        emit(chunkText)
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Streaming timed out after 75s", e)
            emit("\n[CLOUKE AI Notice: Request timed out. Please try again or break down your prompt.]")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI streaming failed", e)
            emit("\n[CLOUKE AI Notice: Failed to contact AI engine (${e.localizedMessage ?: "Unknown network state"}). Please verify connectivity.]")
        }
    }

    suspend fun generateCodeAssistance(
        commandOrPrompt: String,
        currentFileContent: String,
        language: String
    ): String {
        return try {
            val model = resolveGenerativeModel("gemini-3.8-flash")
            val prompt = """
                You are Coltef Code Intelligence inside CLOUKE.
                Target Language: $language
                Instruction: $commandOrPrompt
                Current Source Code:
                ```$language
                $currentFileContent
                ```
                Respond with the updated source code or concise code analysis. Output executable code ready for compilation.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            response.text ?: "// No response generated by model."
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Code assistance failed", e)
            "// Coltef Error: ${e.localizedMessage ?: "Failed to generate code."}"
        }
    }
}
