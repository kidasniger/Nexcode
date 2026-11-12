package com.nexcode.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setSignOutDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showSignOutDialog = visible) }
    }

    fun confirmSignOut() {
        _uiState.update {
            it.copy(
                showSignOutDialog = false,
                isSignedOut = true,
                accountUsername = "Non connecté",
                accountPlan = "Aucun compte associé",
                userNotice = "Déconnexion de GitHub effectuée."
            )
        }
    }

    fun clearCaches() {
        _uiState.update {
            it.copy(
                isCacheCleared = true,
                storageGradleText = "Caches Gradle · 0 Mo",
                storageUsedText = "1,3 Go utilisés",
                storageBarFillRatio = 0.33f,
                userNotice = "Caches Gradle vidés (480 Mo libérés)."
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
    }

    fun increaseFontSize() {
        _uiState.update { current ->
            val nextSize = (current.editorFontSizeSp + 1).coerceAtMost(22)
            current.copy(editorFontSizeSp = nextSize)
        }
    }

    fun decreaseFontSize() {
        _uiState.update { current ->
            val nextSize = (current.editorFontSizeSp - 1).coerceAtLeast(10)
            current.copy(editorFontSizeSp = nextSize)
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
    }

    fun toggleLineNumbers(enabled: Boolean) {
        _uiState.update { it.copy(lineNumbers = enabled) }
    }

    fun toggleAutoCloseBrackets(enabled: Boolean) {
        _uiState.update { it.copy(autoCloseBrackets = enabled) }
    }

    fun exportProjectsNow() {
        _uiState.update {
            it.copy(
                userNotice = "Export de tous les projets (.zip) téléchargé avec succès."
            )
        }
    }

    fun toggleAutoBackup(enabled: Boolean) {
        _uiState.update { it.copy(autoBackupEnabled = enabled) }
    }

    fun dismissNotice() {
        _uiState.update { it.copy(userNotice = null) }
    }
}
