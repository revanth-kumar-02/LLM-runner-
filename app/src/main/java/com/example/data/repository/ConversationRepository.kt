package com.example.data.repository

import android.content.Context
import com.example.data.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ConversationRepository(private val context: Context) {

    val chatFile = File(context.filesDir, "conversations.json")

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    init {
        loadMessages()
    }

    private fun loadMessages() {
        if (!chatFile.exists()) {
            _messages.value = emptyList()
            return
        }

        try {
            val content = chatFile.readText()
            val array = JSONArray(content)
            val list = mutableListOf<ChatMessage>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val text = obj.optString("text", "")
                
                // Exclude any legacy mock responses or corrupt entries
                if (text.contains("processed your query locally using Local Model", ignoreCase = true) ||
                    text.contains("A CPU executes instructions", ignoreCase = true) ||
                    (!obj.optBoolean("isUser", false) && text.isBlank())) {
                    continue
                }

                val cleanedText = if (text.contains("<think>")) {
                    text.replace(Regex("<think>[\\s\\S]*?</think>"), "").trim()
                } else {
                    text
                }

                list.add(
                    ChatMessage(
                        id = obj.getString("id"),
                        isUser = obj.getBoolean("isUser"),
                        text = cleanedText,
                        timestamp = obj.getString("timestamp"),
                        isStreaming = false,
                        tokenRate = null,
                        tokenCount = obj.optInt("tokenCount", 0),
                        modelName = obj.optString("modelName", ""),
                        codeSnippet = if (obj.has("codeSnippet") && !obj.isNull("codeSnippet")) obj.getString("codeSnippet") else null,
                        codeLanguage = if (obj.has("codeLanguage") && !obj.isNull("codeLanguage")) obj.getString("codeLanguage") else null
                    )
                )
            }
            _messages.value = list
            if (list.isEmpty()) {
                chatFile.delete()
            }
        } catch (_: Exception) {
            _messages.value = emptyList()
        }
    }

    private fun persistMessages() {
        try {
            val array = JSONArray()
            _messages.value.forEach { msg ->
                val obj = JSONObject().apply {
                    put("id", msg.id)
                    put("isUser", msg.isUser)
                    put("text", msg.text)
                    put("timestamp", msg.timestamp)
                    put("tokenRate", msg.tokenRate)
                    put("tokenCount", msg.tokenCount)
                    put("modelName", msg.modelName)
                    put("codeSnippet", msg.codeSnippet)
                    put("codeLanguage", msg.codeLanguage)
                }
                array.put(obj)
            }
            chatFile.writeText(array.toString())
        } catch (_: Exception) {}
    }

    fun addMessage(message: ChatMessage) {
        _messages.update { it + message }
        persistMessages()
    }

    fun updateMessage(message: ChatMessage) {
        _messages.update { list ->
            list.map { if (it.id == message.id) message else it }
        }
        if (!message.isStreaming) {
            persistMessages()
        }
    }

    fun clearConversations() {
        _messages.value = emptyList()
        if (chatFile.exists()) {
            chatFile.delete()
        }
    }
}
