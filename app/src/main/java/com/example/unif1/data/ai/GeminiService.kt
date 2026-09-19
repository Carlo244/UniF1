package com.example.unif1.data.ai

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.example.unif1.R
import com.example.unif1.data.training.TrainingPlan
import java.time.LocalDate
import kotlinx.serialization.json.Json

open class GeminiService(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        private const val MODEL_NAME = "gemini-3.6-flash"

        private const val MAX_HISTORY_MESSAGES = 6

        private const val SYSTEM_INSTRUCTION = """
            You are UniF1, a concise Swim/Bike/Run training coach.
            Use only supplied training data.
            Never invent workouts, metrics, goals, dates, or training history.
            Never diagnose medical conditions.
            Do not claim guaranteed performance improvements.
            Answer the user's question directly.
            Use only relevant supplied context.
            Ignore unrelated context.
            If required data is missing, say so.
            Kotlin calculates factual metrics. Gemini interprets those facts.
            Give concise, practical coaching responses.
            Avoid repeating data unnecessarily.
            Training types: RUN, BIKE, SWIM.
            Training statuses: PLANNED, COMPLETED, SKIPPED, REST.
        """
    }

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(MODEL_NAME)
    }

    open suspend fun generateResponse(prompt: String): String {
        var lastError: Exception? = null
        repeat(2) { attempt ->
            try {
                val fullPrompt = "$SYSTEM_INSTRUCTION\n\n$prompt"
                val response = model.generateContent(fullPrompt)
                val text = response.text?.trim()
                if (!text.isNullOrEmpty()) {
                    return text
                }
                throw IllegalStateException("Gemini returned an empty response.")
            } catch (e: Exception) {
                lastError = e
                Log.e("GeminiService", "Attempt $attempt failed", e)
                if (attempt == 0) {
                    kotlinx.coroutines.delay(1500)
                }
            }
        }
        throw classifyGeminiError(lastError ?: IllegalStateException("Unknown Gemini error."))
    }

    open suspend fun getTrainingSummary(
        trainingData: String
    ): String {

        val prompt = """
            TASK: Summarize the supplied training data.

            CONTEXT:
            $trainingData

            RESPONSE FORMAT:
            1) Weekly overview
            2) Sport balance
            3) Trend analysis
            4) Coach take
            5) One actionable tip
            Keep it concise.

            Do not invent missing information.
        """.trimIndent()

        return generateResponse(prompt)
    }

    open suspend fun getChatResponse(
        userMessage: String,
        trainingData: String,
        conversationHistory: List<String> = emptyList()
    ): String {

        val recentHistory = conversationHistory
            .takeLast(MAX_HISTORY_MESSAGES)

        val historyText = if (recentHistory.isEmpty()) {
            "No previous conversation."
        } else {
            recentHistory.joinToString("\n")
        }

        val prompt = """
            TASK: Answer the athlete's question using the supplied UniF1 context.

            TRAINING CONTEXT:
            $trainingData

            RECENT CONVERSATION:
            $historyText

            USER QUESTION:
            $userMessage

            RESPONSE RULES:
            - Answer the actual question first.
            - Use only relevant supplied facts.
            - Keep the answer concise (typically 100-250 words; shorter for simple questions).
            - If useful, reference the exact date or period being discussed.
            - Do not repeat unrelated training sections.
            - Do not invent missing metrics.
            - If the question cannot be answered from the supplied data, say what is missing.
        """.trimIndent()

        return generateResponse(prompt)
    }

    open suspend fun generateTrainingPlan(
        trainingData: String,
        weekStart: LocalDate,
        weekEnd: LocalDate
    ): TrainingPlan {

        val prompt = """
            TASK:
            Create a practical Swim/Bike/Run training plan for:
            $weekStart to $weekEnd

            ATHLETE CONTEXT:
            $trainingData

            PLANNING RULES:
            - Use only the supplied training history and goals.
            - Respect the athlete's existing goals.
            - Do not invent historical workouts or metrics.
            - Do not assume training every day is required.
            - Include rest days when appropriate.
            - Use only RUN, BIKE, SWIM.
            - Keep the plan realistic relative to the supplied training history.
            - Do not make medical claims.
            - Return ONLY valid JSON for TrainingPlan, no markdown fences and no extra text.
        """.trimIndent()

        val response = generateResponse(prompt)

        return parseTrainingPlan(response)
    }

    private fun parseTrainingPlan(response: String): TrainingPlan {

        val cleanedJson = response
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            json.decodeFromString<TrainingPlan>(cleanedJson)
        } catch (e: Exception) {
            throw IllegalStateException(
                "Gemini returned an invalid training plan.",
                e
            )
        }
    }

    private fun classifyGeminiError(
        exception: Exception
    ): Exception {
        Log.e("UniF1-Gemini", "Gemini error encountered: ${exception.message}", exception)
        val message = exception.message.orEmpty()
        val lowerMessage = message.lowercase()

        return when {
            lowerMessage.contains("429") ||
                    lowerMessage.contains("quota") ||
                    lowerMessage.contains("rate limit") -> {
                IllegalStateException(
                    "UniF1 AI is temporarily rate-limited. Please try again later.",
                    exception
                )
            }

            lowerMessage.contains("503") ||
                    lowerMessage.contains("service unavailable") ||
                    lowerMessage.contains("high demand") -> {
                IllegalStateException(
                    "UniF1 AI is temporarily unavailable. Please try again shortly.",
                    exception
                )
            }

            lowerMessage.contains("403") ||
                    lowerMessage.contains("app check") ||
                    lowerMessage.contains("attestation") ||
                    lowerMessage.contains("permission denied") ||
                    lowerMessage.contains("invalid api key") -> {
                IllegalStateException(
                    "UniF1 AI access was rejected. Check your API key configuration.",
                    exception
                )
            }

            lowerMessage.contains("400") ||
                    lowerMessage.contains("invalid argument") -> {
                IllegalStateException(
                    "UniF1 sent an invalid AI request.",
                    exception
                )
            }

            lowerMessage.contains("404") ||
                    lowerMessage.contains("not found") -> {
                IllegalStateException(
                    "The configured UniF1 AI model could not be found.",
                    exception
                )
            }

            lowerMessage.contains("network") ||
                    lowerMessage.contains("timeout") ||
                    lowerMessage.contains("connection") -> {
                IllegalStateException(
                    "UniF1 could not connect to the AI service.",
                    exception
                )
            }

            else -> {
                IllegalStateException(
                    "UniF1 AI request failed.",
                    exception
                )
            }
        }
    }
}