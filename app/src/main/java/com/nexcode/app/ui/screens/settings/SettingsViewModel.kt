package com.nexcode.app.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexcode.app.data.github.repository.GitHubRepositoryService
import com.nexcode.app.data.preferences.NexcodePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class SettingsViewModel(
    private val context: Context? = null,
    overrideGitService: GitHubRepositoryService? = null
) : ViewModel() {

    private val gitService = overrideGitService ?: context?.let { GitHubRepositoryService(it) }
    private val preferences = context?.let { NexcodePreferences(it) }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observePreferences()
        refreshAccountAndStorage()
    }

    private fun observePreferences() {
        val prefs = preferences ?: return
        viewModelScope.launch {
            combine(
                prefs.gitLoginFlow,
                prefs.gitPlanFlow,
                prefs.aiModelFlow,
                prefs.aiProviderFlow,
                prefs.themeModeFlow
            ) { login, plan, aiModel, aiProvider, themeMode ->
                PrefsSnapshot(login, plan, aiModel, aiProvider, themeMode)
            }.collect { (login, plan, aiModel, aiProvider, themeMode) ->
                val hasToken = gitService?.hasSavedToken() == true
                val connected = hasToken && login.isNotBlank()
                val uiTheme = when (themeMode.uppercase(Locale.ROOT)) {
                    "LIGHT" -> UiThemePreference.LIGHT
                    "DARK" -> UiThemePreference.DARK
                    else -> UiThemePreference.SYSTEM
                }
                _uiState.update { current ->
                    current.copy(
                        isSignedOut = !connected,
                        accountUsername = if (connected) login else "Non connecté",
                        accountInitials = if (connected) computeInitials(login) else "?",
                        accountPlan = if (connected) "github.com/$login · $plan" else "Aucun compte associé",
                        aiModelSummary = "$aiModel · $aiProvider",
                        uiTheme = uiTheme
                    )
                }
            }
        }
    }

    fun refreshAccountAndStorage() {
        viewModelScope.launch {
            refreshAccountAndStorageSync()
        }
    }

    suspend fun refreshAccountAndStorageSync() {
        val prefs = preferences
        if (prefs != null) {
            val login = prefs.gitLoginFlow.first()
            val plan = prefs.gitPlanFlow.first()
            val hasToken = gitService?.hasSavedToken() == true
            val connected = hasToken && login.isNotBlank()
            _uiState.update {
                it.copy(
                    isSignedOut = !connected,
                    accountUsername = if (connected) login else "Non connecté",
                    accountInitials = if (connected) computeInitials(login) else "?",
                    accountPlan = if (connected) "github.com/$login · $plan" else "Aucun compte associé"
                )
            }
        }

        val ctx = context ?: return
        val (projectsBytes, apkBytes, cacheBytes) = withContext(Dispatchers.IO) {
            val pBytes = dirSize(File(ctx.filesDir, "projects"))
            val aBytes = dirSize(File(ctx.filesDir, "apks"))
            val cBytes = dirSize(ctx.cacheDir)
            Triple(pBytes, aBytes, cBytes)
        }
        val totalBytes = projectsBytes + apkBytes + cacheBytes
        val maxBytes = 4L * 1024L * 1024L * 1024L
        val ratio = (totalBytes.toFloat() / maxBytes.toFloat()).coerceIn(0f, 1f)
        val cacheFormatted = formatFrenchSize(cacheBytes)

        _uiState.update {
            it.copy(
                storageProjectsText = "Projets · ${formatFrenchSize(projectsBytes)}",
                storageGradleText = "Caches · $cacheFormatted",
                storageApkText = "APK · ${formatFrenchSize(apkBytes)}",
                storageUsedText = "${formatFrenchSize(totalBytes)} utilisés",
                storageBarFillRatio = ratio,
                clearCacheButtonLabel = "Vider les caches ($cacheFormatted)"
            )
        }
    }

    fun setSignOutDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showSignOutDialog = visible) }
    }

    fun confirmSignOut() {
        viewModelScope.launch {
            confirmSignOutSync()
        }
    }

    suspend fun confirmSignOutSync() {
        gitService?.clearGitSession()
        _uiState.update {
            it.copy(
                showSignOutDialog = false,
                isSignedOut = true,
                accountInitials = "?",
                accountUsername = "Non connecté",
                accountPlan = "Aucun compte associé",
                userNotice = "Déconnexion de GitHub effectuée."
            )
        }
    }

    fun clearCaches() {
        viewModelScope.launch {
            clearCachesSync()
        }
    }

    suspend fun clearCachesSync() {
        val ctx = context
        val freed = if (ctx != null) {
            withContext(Dispatchers.IO) {
                val before = dirSize(ctx.cacheDir)
                ctx.cacheDir.listFiles()?.forEach { child ->
                    child.deleteRecursively()
                }
                before
            }
        } else {
            0L
        }
        refreshAccountAndStorageSync()
        _uiState.update {
            it.copy(
                isCacheCleared = true,
                storageGradleText = "Caches · 0 Mo",
                clearCacheButtonLabel = "Vider les caches (0 Mo)",
                userNotice = "Caches vidés (${formatFrenchSize(freed)} libérés)."
            )
        }
    }

    fun toggleNotifyBuildCompleted(enabled: Boolean) {
        _uiState.update { it.copy(notifyBuildCompleted = enabled) }
    }

    fun toggleNotifyBuildErrors(enabled: Boolean) {
        _uiState.update { it.copy(notifyBuildErrors = enabled) }
    }

    fun toggleNotifyDependencyUpdates(enabled: Boolean) {
        _uiState.update { it.copy(notifyDependencyUpdates = enabled) }
    }

    fun setUiTheme(theme: UiThemePreference) {
        _uiState.update { it.copy(uiTheme = theme) }
        preferences?.let { prefs ->
            viewModelScope.launch {
                prefs.setThemeMode(theme.name)
            }
        }
    }

    fun increaseFontSize() {
        val nextSize = (_uiState.value.editorFontSizeSp + 1).coerceAtMost(22)
        _uiState.update { it.copy(editorFontSizeSp = nextSize) }
        preferences?.let { prefs ->
            viewModelScope.launch { prefs.setEditorFontSize(nextSize) }
        }
    }

    fun decreaseFontSize() {
        val nextSize = (_uiState.value.editorFontSizeSp - 1).coerceAtLeast(10)
        _uiState.update { it.copy(editorFontSizeSp = nextSize) }
        preferences?.let { prefs ->
            viewModelScope.launch { prefs.setEditorFontSize(nextSize) }
        }
    }

    fun setCodeTheme(codeTheme: CodeThemePreference) {
        _uiState.update { it.copy(codeTheme = codeTheme) }
    }

    fun setIndentation(indentation: IndentationPreference) {
        _uiState.update { it.copy(indentation = indentation) }
    }

    fun toggleWordWrap(enabled: Boolean) {
        _uiState.update { it.copy(wordWrap = enabled) }
        preferences?.let { prefs ->
            viewModelScope.launch { prefs.setEditorWordWrap(enabled) }
        }
    }

    fun toggleLineNumbers(enabled: Boolean) {
        _uiState.update { it.copy(lineNumbers = enabled) }
        preferences?.let { prefs ->
            viewModelScope.launch { prefs.setEditorShowLineNumbers(enabled) }
        }
    }

    fun toggleAutoCloseBrackets(enabled: Boolean) {
        _uiState.update { it.copy(autoCloseBrackets = enabled) }
    }

    fun exportProjectsNow() {
        _uiState.update {
            it.copy(
                userNotice = "Export de tous les projets (.zip) prêt."
            )
        }
    }

    fun toggleAutoBackup(enabled: Boolean) {
        _uiState.update { it.copy(autoBackupEnabled = enabled) }
    }

    fun dismissNotice() {
        _uiState.update { it.copy(userNotice = null) }
    }

    private data class PrefsSnapshot(
        val login: String,
        val plan: String,
        val aiModel: String,
        val aiProvider: String,
        val themeMode: String
    )

    companion object {
        fun computeInitials(username: String): String {
            val parts = username.trim().split(Regex("[-_\\s]+")).filter { it.isNotBlank() }
            return when {
                parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
                username.trim().length >= 2 -> username.trim().take(2).uppercase(Locale.ROOT)
                username.isNotBlank() -> username.trim().take(1).uppercase(Locale.ROOT)
                else -> "?"
            }
        }

        private fun dirSize(dir: File): Long {
            if (!dir.exists()) return 0L
            if (dir.isFile) return dir.length()
            var total = 0L
            dir.walkTopDown().forEach { f ->
                if (f.isFile) total += f.length()
            }
            return total
        }

        fun formatFrenchSize(bytes: Long): String {
            if (bytes <= 0L) return "0 Mo"
            val kb = bytes / 1024.0
            if (kb < 1024.0) return String.format(Locale.FRANCE, "%.0f Ko", kb.coerceAtLeast(1.0))
            val mb = kb / 1024.0
            if (mb < 1024.0) return String.format(Locale.FRANCE, "%.1f Mo", mb)
            val gb = mb / 1024.0
            return String.format(Locale.FRANCE, "%.1f Go", gb)
        }
    }
}
