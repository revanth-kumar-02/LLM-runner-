package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "local_ai_preferences")

class SettingsRepository(context: Context) {

    private val appContext = context.applicationContext
    private object PreferencesKeys {
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
    }

    private val prefs: SharedPreferences = context.getSharedPreferences("local_ai_settings", Context.MODE_PRIVATE)

    private val defaultThreads = Runtime.getRuntime().availableProcessors().coerceIn(2, 8)

    private val _settings = MutableStateFlow(
        AppSettings(
            activeModelId = prefs.getString("active_model_id", null),
            contextLength = prefs.getInt("context_length", 4096),
            temperature = prefs.getFloat("temperature", 0.7f),
            maxResponseTokens = prefs.getInt("max_tokens", 2048),
            cpuThreads = prefs.getInt("cpu_threads", defaultThreads),
            inferenceMode = "CPU / NPU Accelerated",
            conversationHistoryEnabled = prefs.getBoolean("history_enabled", true),
            themeName = prefs.getString("theme_name", "Warm Light") ?: "Warm Light",
            readingTextSize = prefs.getString("reading_text_size", "Medium (16px)") ?: "Medium (16px)",
            isOnboardingCompleted = prefs.getBoolean("onboarding_completed", false)
        )
    )
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            appContext.dataStore.data
                .map { preferences ->
                    preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] ?: prefs.getBoolean("onboarding_completed", false)
                }
                .collectLatest { completed ->
                    _settings.update { it.copy(isOnboardingCompleted = completed) }
                }
        }
    }

    fun setOnboardingCompleted(completed: Boolean = true) {
        prefs.edit().putBoolean("onboarding_completed", completed).apply()
        CoroutineScope(Dispatchers.IO).launch {
            appContext.dataStore.edit { preferences ->
                preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] = completed
            }
        }
        _settings.update { it.copy(isOnboardingCompleted = completed) }
    }

    fun updateContextLength(newLength: Int) {
        prefs.edit().putInt("context_length", newLength).apply()
        _settings.update { it.copy(contextLength = newLength) }
    }

    fun updateTemperature(temp: Float) {
        prefs.edit().putFloat("temperature", temp).apply()
        _settings.update { it.copy(temperature = temp) }
    }

    fun updateMaxTokens(tokens: Int) {
        prefs.edit().putInt("max_tokens", tokens).apply()
        _settings.update { it.copy(maxResponseTokens = tokens) }
    }

    fun updateCpuThreads(threads: Int) {
        prefs.edit().putInt("cpu_threads", threads).apply()
        _settings.update { it.copy(cpuThreads = threads) }
    }

    fun updateHistoryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("history_enabled", enabled).apply()
        _settings.update { it.copy(conversationHistoryEnabled = enabled) }
    }

    fun setActiveModelId(modelId: String?) {
        prefs.edit().putString("active_model_id", modelId).apply()
        _settings.update { it.copy(activeModelId = modelId) }
    }
}
