package com.nexcode.app.data.ai

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

interface AiStreamingClient {
    suspend fun testConnection(config: AiEndpointConfig): AiConnectionTestResult

    fun streamChat(
        config: AiEndpointConfig,
        systemPrompt: String,
        messages: List<AiChatRequestMessage>
    ): Flow<AiStreamEvent>
}

class OkHttpAiStreamingClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) : AiStreamingClient {

    override suspend fun testConnection(config: AiEndpointConfig): AiConnectionTestResult =
        withContext(Dispatchers.IO) {
            val validationError = validateConfig(config)
            if (validationError != null) {
                return@withContext AiConnectionTestResult(success = false, message = validationError)
            }

            val startNs = System.nanoTime()
            try {
                val request = buildTestRequest(config)
                client.newCall(request).execute().use { response ->
                    val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
                    if (response.isSuccessful) {
                        AiConnectionTestResult(
                            success = true,
                            latencyMs = elapsedMs,
                            message = "Connexion réussie · $elapsedMs ms · modèle joignable"
                        )
                    } else {
                        val rawErr = runCatching { response.body?.string().orEmpty() }.getOrDefault("")
                        val detail = extractApiErrorMessage(rawErr)
                        val msg = mapHttpErrorMessage(response.code, detail)
                        AiConnectionTestResult(success = false, latencyMs = elapsedMs, message = msg)
                    }
                }
            } catch (_: IOException) {
                AiConnectionTestResult(
                    success = false,
                    message = "Impossible de joindre le serveur IA. Vérifiez votre connexion réseau."
                )
            } catch (e: Exception) {
                AiConnectionTestResult(
                    success = false,
                    message = e.message ?: "Échec du test de connexion IA."
                )
            }
        }

    override fun streamChat(
        config: AiEndpointConfig,
        systemPrompt: String,
        messages: List<AiChatRequestMessage>
    ): Flow<AiStreamEvent> = flow {
        val validationError = validateConfig(config)
        if (validationError != null) {
            emit(AiStreamEvent.Error(validationError, code = 400))
            return@flow
        }

        val request = buildStreamRequest(config, systemPrompt, messages)
        var call: Call? = null
        val accumulated = StringBuilder()

        try {
            call = client.newCall(request)
            val response = call.execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    val errBody = runCatching { resp.body?.string().orEmpty() }.getOrDefault("")
                    val detail = extractApiErrorMessage(errBody)
                    emit(
                        AiStreamEvent.Error(
                            message = mapHttpErrorMessage(resp.code, detail),
                            code = resp.code
                        )
                    )
                    return@flow
                }

                val source = resp.body?.source()
                if (source == null) {
                    emit(AiStreamEvent.Error("Réponse vide du serveur IA."))
                    return@flow
                }

                val coroutineCtx = currentCoroutineContext()
                var currentEventName = ""

                while (coroutineCtx.isActive && !source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.isBlank()) {
                        currentEventName = ""
                        continue
                    }
                    if (line.startsWith("event:")) {
                        currentEventName = line.removePrefix("event:").trim()
                        continue
                    }
                    if (!line.startsWith("data:")) {
                        // Support des flux JSON ligne par ligne (Ollama /api/chat ou /api/generate)
                        if (line.trimStart().startsWith("{")) {
                            val chunk = parseRawJsonStreamLine(line)
                            if (chunk.isNotEmpty()) {
                                accumulated.append(chunk)
                                emit(AiStreamEvent.Delta(chunk))
                            }
                        }
                        continue
                    }

                    val data = line.removePrefix("data:").trim()
                    if (data == "[DONE]") {
                        break
                    }

                    when (config.provider) {
                        AiProviderType.ANTHROPIC -> {
                            val parsed = parseAnthropicSseData(currentEventName, data)
                            if (parsed.errorMessage != null) {
                                emit(AiStreamEvent.Error(parsed.errorMessage))
                                return@flow
                            }
                            if (parsed.deltaText.isNotEmpty()) {
                                accumulated.append(parsed.deltaText)
                                emit(AiStreamEvent.Delta(parsed.deltaText))
                            }
                            if (parsed.isStop) {
                                break
                            }
                        }
                        AiProviderType.OPENAI, AiProviderType.CUSTOM -> {
                            val parsed = parseOpenAiSseData(data)
                            if (parsed.errorMessage != null) {
                                emit(AiStreamEvent.Error(parsed.errorMessage))
                                return@flow
                            }
                            if (parsed.deltaText.isNotEmpty()) {
                                accumulated.append(parsed.deltaText)
                                emit(AiStreamEvent.Delta(parsed.deltaText))
                            }
                            if (parsed.isStop) {
                                break
                            }
                        }
                    }
                }

                if (coroutineCtx.isActive) {
                    emit(AiStreamEvent.Completed(accumulated.toString()))
                }
            }
        } catch (ce: CancellationException) {
            call?.cancel()
            throw ce
        } catch (_: IOException) {
            if (!currentCoroutineContext().isActive) {
                throw CancellationException("Analyse interrompue")
            }
            emit(
                AiStreamEvent.Error(
                    message = "Connexion réseau perdue pendant la génération.",
                    isNetworkError = true
                )
            )
        } catch (e: Exception) {
            emit(AiStreamEvent.Error(e.message ?: "Erreur inattendue lors de l'appel IA."))
        }
    }.flowOn(Dispatchers.IO)

    private fun validateConfig(config: AiEndpointConfig): String? {
        return when (config.provider) {
            AiProviderType.ANTHROPIC -> {
                if (config.apiKey.isBlank()) {
                    "Aucune clé API Anthropic configurée. Rendez-vous dans Réglages → IA."
                } else null
            }
            AiProviderType.OPENAI -> {
                if (config.apiKey.isBlank()) {
                    "Aucune clé API OpenAI configurée. Rendez-vous dans Réglages → IA."
                } else null
            }
            AiProviderType.CUSTOM -> {
                val url = config.customBaseUrl.trim()
                if (url.isBlank() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
                    "URL du serveur personnalisé invalide. Saisissez une URL http:// ou https:// à l'écran 4."
                } else null
            }
        }
    }

    private fun buildTestRequest(config: AiEndpointConfig): Request {
        val jsonType = "application/json; charset=utf-8".toMediaType()
        return when (config.provider) {
            AiProviderType.ANTHROPIC -> {
                val payload = JSONObject().apply {
                    put("model", mapAnthropicModelId(config.model))
                    put("max_tokens", 1)
                    put(
                        "messages",
                        JSONArray().put(
                            JSONObject().apply {
                                put("role", "user")
                                put("content", "ping")
                            }
                        )
                    )
                }.toString()
                Request.Builder()
                    .url("https://api.anthropic.com/v1/messages")
                    .header("x-api-key", config.apiKey.trim())
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json")
                    .post(payload.toRequestBody(jsonType))
                    .build()
            }
            AiProviderType.OPENAI -> {
                Request.Builder()
                    .url("https://api.openai.com/v1/models")
                    .header("Authorization", "Bearer ${config.apiKey.trim()}")
                    .get()
                    .build()
            }
            AiProviderType.CUSTOM -> {
                val base = normalizeCustomBaseUrl(config.customBaseUrl)
                val builder = Request.Builder().url("$base/models").get()
                if (config.apiKey.isNotBlank()) {
                    builder.header("Authorization", "Bearer ${config.apiKey.trim()}")
                }
                builder.build()
            }
        }
    }

    private fun buildStreamRequest(
        config: AiEndpointConfig,
        systemPrompt: String,
        messages: List<AiChatRequestMessage>
    ): Request {
        val jsonType = "application/json; charset=utf-8".toMediaType()
        return when (config.provider) {
            AiProviderType.ANTHROPIC -> {
                val jsonMessages = JSONArray()
                messages.forEach { msg ->
                    jsonMessages.put(
                        JSONObject().apply {
                            put("role", if (msg.role == "assistant") "assistant" else "user")
                            put("content", msg.content)
                        }
                    )
                }
                val payload = JSONObject().apply {
                    put("model", mapAnthropicModelId(config.model))
                    put("max_tokens", 4096)
                    put("stream", true)
                    if (systemPrompt.isNotBlank()) {
                        put("system", systemPrompt)
                    }
                    put("messages", jsonMessages)
                }.toString()

                Request.Builder()
                    .url("https://api.anthropic.com/v1/messages")
                    .header("x-api-key", config.apiKey.trim())
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json")
                    .header("accept", "text/event-stream")
                    .post(payload.toRequestBody(jsonType))
                    .build()
            }
            AiProviderType.OPENAI, AiProviderType.CUSTOM -> {
                val url = if (config.provider == AiProviderType.OPENAI) {
                    "https://api.openai.com/v1/chat/completions"
                } else {
                    "${normalizeCustomBaseUrl(config.customBaseUrl)}/chat/completions"
                }
                val jsonMessages = JSONArray()
                if (systemPrompt.isNotBlank()) {
                    jsonMessages.put(
                        JSONObject().apply {
                            put("role", "system")
                            put("content", systemPrompt)
                        }
                    )
                }
                messages.forEach { msg ->
                    jsonMessages.put(
                        JSONObject().apply {
                            put("role", if (msg.role == "assistant") "assistant" else "user")
                            put("content", msg.content)
                        }
                    )
                }
                val payload = JSONObject().apply {
                    put("model", config.model.ifBlank { "gpt-4o" })
                    put("stream", true)
                    put("messages", jsonMessages)
                }.toString()

                val builder = Request.Builder()
                    .url(url)
                    .header("content-type", "application/json")
                    .header("accept", "text/event-stream")
                    .post(payload.toRequestBody(jsonType))
                if (config.apiKey.isNotBlank()) {
                    builder.header("Authorization", "Bearer ${config.apiKey.trim()}")
                }
                builder.build()
            }
        }
    }

    private data class ParsedSseChunk(
        val deltaText: String = "",
        val isStop: Boolean = false,
        val errorMessage: String? = null
    )

    private fun parseAnthropicSseData(eventName: String, data: String): ParsedSseChunk {
        return runCatching {
            val obj = JSONObject(data)
            val type = obj.optString("type", eventName)
            when (type) {
                "content_block_delta" -> {
                    val delta = obj.optJSONObject("delta")
                    val text = delta?.optString("text").orEmpty()
                    ParsedSseChunk(deltaText = text)
                }
                "message_stop" -> ParsedSseChunk(isStop = true)
                "error" -> {
                    val errObj = obj.optJSONObject("error")
                    val msg = errObj?.optString("message")?.takeIf { it.isNotBlank() }
                        ?: "Erreur du flux Anthropic."
                    ParsedSseChunk(errorMessage = msg)
                }
                else -> ParsedSseChunk()
            }
        }.getOrDefault(ParsedSseChunk())
    }

    private fun parseOpenAiSseData(data: String): ParsedSseChunk {
        return runCatching {
            val obj = JSONObject(data)
            if (obj.has("error")) {
                val errObj = obj.optJSONObject("error")
                val msg = errObj?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: "Erreur du flux OpenAI."
                return@runCatching ParsedSseChunk(errorMessage = msg)
            }
            val choices = obj.optJSONArray("choices") ?: return@runCatching ParsedSseChunk()
            if (choices.length() == 0) return@runCatching ParsedSseChunk()
            val first = choices.optJSONObject(0) ?: return@runCatching ParsedSseChunk()
            val delta = first.optJSONObject("delta")
            val content = delta?.optString("content", "").orEmpty()
            val finishReason = first.optString("finish_reason", "")
            val stopped = finishReason == "stop" || finishReason == "length"
            ParsedSseChunk(deltaText = content, isStop = stopped && content.isEmpty())
        }.getOrDefault(ParsedSseChunk())
    }

    private fun parseRawJsonStreamLine(line: String): String {
        return runCatching {
            val obj = JSONObject(line)
            val msgContent = obj.optJSONObject("message")?.optString("content").orEmpty()
            if (msgContent.isNotEmpty()) return@runCatching msgContent
            obj.optString("response", "")
        }.getOrDefault("")
    }

    companion object {
        fun mapAnthropicModelId(uiModel: String): String {
            return when (uiModel.trim()) {
                "claude-sonnet-4.5" -> "claude-sonnet-4-5"
                "claude-opus-4.1" -> "claude-opus-4-1"
                "claude-haiku-4.5" -> "claude-haiku-4-5"
                else -> uiModel.trim().ifBlank { "claude-sonnet-4-5" }
            }
        }

        fun normalizeCustomBaseUrl(rawUrl: String): String {
            val trimmed = rawUrl.trim().trimEnd('/')
            return when {
                trimmed.endsWith("/chat/completions") -> trimmed.removeSuffix("/chat/completions")
                trimmed.endsWith("/v1") -> trimmed
                else -> "$trimmed/v1"
            }
        }

        fun extractApiErrorMessage(rawBody: String): String? {
            if (rawBody.isBlank()) return null
            return runCatching {
                val json = JSONObject(rawBody)
                json.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: json.optString("message").takeIf { it.isNotBlank() }
            }.getOrNull()
        }

        fun mapHttpErrorMessage(code: Int, detail: String?): String {
            val suffix = detail?.take(120)?.let { " ($it)" }.orEmpty()
            return when (code) {
                401 -> "Clé API invalide ou expirée (401)$suffix."
                403 -> "Accès refusé par le fournisseur IA (403)$suffix."
                404 -> "Modèle ou endpoint introuvable (404)$suffix."
                429 -> "Quota ou limite de requêtes dépassée (429)$suffix."
                in 500..599 -> "Erreur interne du serveur IA ($code)$suffix."
                else -> "Erreur API IA ($code)$suffix."
            }
        }
    }
}
