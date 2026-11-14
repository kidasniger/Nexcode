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
    private val updateManager = context?.let { com.nexcode.app.updater.AppUpdateManager(it) }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observePreferences()
        observeAppPreferences()
        observeUpdateStates()
        refreshAccountAndStorage()
        refreshAppVersion()
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

    private fun observeAppPreferences() {
        val prefs = preferences ?: return
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                prefs.notifyBuildCompletedFlow,
                prefs.notifyBuildErrorsFlow,
                prefs.notifyDependencyUpdatesFlow,
                prefs.editorAutoCloseBracketsFlow,
                prefs.autoBackupEnabledFlow
            ) { notifyBuild, notifyErrors, notifyDeps, autoBrackets, autoBackup ->
                _uiState.update {
                    it.copy(
                        notifyBuildCompleted = notifyBuild,
                        notifyBuildErrors = notifyErrors,
                        notifyDependencyUpdates = notifyDeps,
                        autoCloseBrackets = autoBrackets,
                        autoBackupEnabled = autoBackup
                    )
                }
            }.collect {}
        }
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                prefs.editorCodeThemeFlow,
                prefs.editorIndentationFlow
            ) { codeTheme, indentation -> codeTheme to indentation }.collect { (codeTheme, indentation) ->
                _uiState.update {
                    it.copy(
                        codeTheme = CodeThemePreference.entries.firstOrNull { c -> c.name == codeTheme }
                            ?: CodeThemePreference.NEX_DARK,
                        indentation = IndentationPreference.entries.firstOrNull { i -> i.name == indentation }
                            ?: IndentationPreference.SPACES_4
                    )
                }
            }
        }
    }

    /**
     * Version réelle de l'application lue depuis le PackageManager.
     */
    private fun refreshAppVersion() {
        val ctx = context ?: return
        try {
            val packageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            val versionName = packageInfo.versionName ?: "1.0"
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION") packageInfo.versionCode.toLong()
            }
            _uiState.update { it.copy(appVersion = "$versionName (build $versionCode)") }
        } catch (_: Exception) {
            _uiState.update { it.copy(appVersion = "1.0") }
        }
    }

    /**
     * Mises à jour : états réels provenant de l'AppUpdateManager (releases GitHub).
     */
    private fun observeUpdateStates() {
        val manager = updateManager ?: return
        viewModelScope.launch {
            manager.updateCheckState.collect { state ->
                _uiState.update {
                    when (state) {
                        is com.nexcode.app.updater.UpdateCheckState.Idle -> it.copy(
                            updateStatus = "Vérification manuelle",
                            isUpdateChecking = false,
                            updateAvailableVersion = null,
                            updateError = null
                        )
                        is com.nexcode.app.updater.UpdateCheckState.Checking -> it.copy(
                            updateStatus = "Vérification en cours…",
                            isUpdateChecking = true,
                            updateError = null
                        )
                        is com.nexcode.app.updater.UpdateCheckState.UpdateAvailable -> it.copy(
                            updateStatus = "Mise à jour ${state.latestVersion} disponible",
                            isUpdateChecking = false,
                            updateAvailableVersion = state.latestVersion,
                            updateReleaseNotes = state.releaseNotes,
                            updateError = null
                        )
                        is com.nexcode.app.updater.UpdateCheckState.UpToDate -> it.copy(
                            updateStatus = "À jour (v${state.currentVersion})",
                            isUpdateChecking = false,
                            updateAvailableVersion = null,
                            updateError = null
                        )
                        is com.nexcode.app.updater.UpdateCheckState.Error -> it.copy(
                            updateStatus = "Vérification impossible",
                            isUpdateChecking = false,
                            updateError = state.message
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            manager.downloadState.collect { state ->
                _uiState.update {
                    when (state) {
                        is com.nexcode.app.updater.DownloadState.Downloading -> it.copy(
                            updateDownloadProgress = state.progressPercent
                        )
                        else -> it.copy(updateDownloadProgress = null)
                    }
                }
            }
        }
    }

    fun checkForAppUpdates() {
        val manager = updateManager ?: return
        viewModelScope.launch { manager.checkForUpdates() }
    }

    fun downloadAppUpdate() {
        val manager = updateManager ?: return
        val state = manager.updateCheckState.value
        if (state is com.nexcode.app.updater.UpdateCheckState.UpdateAvailable) {
            viewModelScope.launch { manager.downloadAndInstallApk(state.downloadUrl) }
        }
    }

    fun setUiTheme(theme: UiThemePreference) {
        _uiState.update { it.copy(uiTheme = theme) }
        preferences?.let { prefs ->
            viewModelScope.launch {
                prefs.setThemeMode(theme.name)
            }
        }
    }

    fun toggleNotifyBuildCompleted(enabled: Boolean) {
        _uiState.update { it.copy(notifyBuildCompleted = enabled) }
        viewModelScope.launch { preferences?.setNotifyBuildCompleted(enabled) }
    }

    fun toggleNotifyBuildErrors(enabled: Boolean) {
        _uiState.update { it.copy(notifyBuildErrors = enabled) }
        viewModelScope.launch { preferences?.setNotifyBuildErrors(enabled) }
    }

    fun toggleNotifyDependencyUpdates(enabled: Boolean) {
        _uiState.update { it.copy(notifyDependencyUpdates = enabled) }
        viewModelScope.launch { preferences?.setNotifyDependencyUpdates(enabled) }
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
        viewModelScope.launch { preferences?.setEditorCodeTheme(codeTheme.name) }
    }

    fun setIndentation(indentation: IndentationPreference) {
        _uiState.update { it.copy(indentation = indentation) }
        viewModelScope.launch { preferences?.setEditorIndentation(indentation.name) }
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
        viewModelScope.launch { preferences?.setEditorAutoCloseBrackets(enabled) }
    }

    /**
     * Export réel : crée une archive .zip de tous les projets dans le cache.
     */
    fun exportProjectsNow() {
        val ctx = context ?: run {
            _uiState.update { it.copy(userNotice = "Export impossible sans contexte applicatif.") }
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val projectsDir = File(ctx.filesDir, "projects")
                    if (!projectsDir.exists() || projectsDir.listFiles().isNullOrEmpty()) {
                        return@withContext null to "Aucun projet à exporter."
                    }
                    val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRANCE)
                        .format(java.util.Date())
                    val outputFile = File(ctx.cacheDir, "nexcode_projets_$stamp.zip")
                    java.util.zip.ZipOutputStream(outputFile.outputStream().buffered()).use { zos ->
                        projectsDir.walkTopDown().filter { it.isFile }.forEach { file ->
                            val relative = file.relativeTo(projectsDir).path
                            zos.putNextEntry(java.util.zip.ZipEntry(relative))
                            file.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                    outputFile to "Export créé : ${outputFile.name} (${formatFrenchSize(outputFile.length())})"
                } catch (e: Exception) {
                    null to "Échec de l'export : ${e.message ?: "erreur inconnue"}"
                }
            }
            val (file, message) = result
            _uiState.update {
                it.copy(
                    userNotice = message,
                    lastExportText = file?.let { f -> "Dernier export : ${f.name}" }
                        ?: "Export local des projets au format .zip"
                )
            }
        }
    }

    fun toggleAutoBackup(enabled: Boolean) {
        _uiState.update { it.copy(autoBackupEnabled = enabled) }
        viewModelScope.launch { preferences?.setAutoBackupEnabled(enabled) }
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
