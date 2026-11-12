package com.nexcode.app.ui.screens.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexcode.app.data.preferences.NexcodePreferences
import com.nexcode.app.data.preferences.NexcodeSecureVault
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    // Écran 1
    val preparationProgress: Float = 0.62f,

    // Écran 3 : GitHub
    val gitHubToken: String = "ghp_9f2Xk7QzLm4Np8Rt",
    val isTokenVisible: Boolean = false,
    val isGitHubConnected: Boolean = true,
    val gitHubUsername: String = "nimbus-dev",
    val gitHubRepoCount: Int = 4,
    val isGitHubError: Boolean = false,
    val showDisconnectDialog: Boolean = false,

    // Écran 4 : Configuration IA
    val selectedProvider: String = "Anthropic",
    val apiKey: String = "sk-ant-api03-x9L2mQ7vNp4Tt8Rw",
    val isApiKeyVisible: Boolean = false,
    val selectedModel: String = "claude-sonnet-4.5",
    val showModelSheet: Boolean = false,
    val isTestingAi: Boolean = false,
    val aiTestSuccess: Boolean? = null
)

class OnboardingViewModel(
    context: Context
) : ViewModel() {

    private val secureVault = NexcodeSecureVault(context)
    private val preferences = NexcodePreferences(context)

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            apiKey = secureVault.getApiKey() ?: "sk-ant-api03-x9L2mQ7vNp4Tt8Rw",
            gitHubToken = secureVault.getGitToken() ?: "ghp_9f2Xk7QzLm4Np8Rt"
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun updateGitHubToken(token: String) {
        _uiState.update { it.copy(gitHubToken = token) }
    }

    fun toggleTokenVisibility() {
        _uiState.update { it.copy(isTokenVisible = !it.isTokenVisible) }
    }

    fun toggleGitHubErrorState() {
        _uiState.update { it.copy(isGitHubError = !it.isGitHubError) }
    }

    fun retryGitHubConnection() {
        viewModelScope.launch {
            val token = _uiState.value.gitHubToken.trim()
            if (token.isBlank()) {
                _uiState.update { it.copy(isGitHubError = true) }
                return@launch
            }
            val service = com.nexcode.app.data.github.repository.GitHubRepositoryService(context)
            when (val res = service.verifyAndSaveToken(token)) {
                is com.nexcode.app.data.github.repository.GitHubResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isGitHubError = false,
                            isGitHubConnected = true,
                            gitHubToken = token
                        )
                    }
                }
                is com.nexcode.app.data.github.repository.GitHubResult.Error,
                is com.nexcode.app.data.github.repository.GitHubResult.NetworkError -> {
                    _uiState.update { it.copy(isGitHubError = true) }
                }
            }
        }
    }

    fun showDisconnectModal(show: Boolean) {
        _uiState.update { it.copy(showDisconnectDialog = show) }
    }

    fun disconnectGitHub() {
        secureVault.clearGitToken()
        _uiState.update {
            it.copy(
                isGitHubConnected = false,
                gitHubToken = "",
                showDisconnectDialog = false
            )
        }
    }

    fun selectAiProvider(provider: String) {
        _uiState.update { it.copy(selectedProvider = provider) }
    }

    fun updateApiKey(key: String) {
        _uiState.update { it.copy(apiKey = key) }
    }

    fun toggleApiKeyVisibility() {
        _uiState.update { it.copy(isApiKeyVisible = !it.isApiKeyVisible) }
    }

    fun showModelBottomSheet(show: Boolean) {
        _uiState.update { it.copy(showModelSheet = show) }
    }

    suspend fun selectModelSync(model: String) {
        _uiState.update { it.copy(selectedModel = model, showModelSheet = false) }
        preferences.setAiModel(model)
    }

    fun selectModel(model: String) {
        viewModelScope.launch {
            selectModelSync(model)
        }
    }

    fun testAiConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingAi = true, aiTestSuccess = null) }
            delay(412) // Simulation des 412 ms de la maquette
            val success = _uiState.value.apiKey.isNotBlank()
            _uiState.update { it.copy(isTestingAi = false, aiTestSuccess = success) }
        }
    }

    suspend fun completeOnboardingSync() {
        secureVault.saveApiKey(_uiState.value.apiKey)
        if (_uiState.value.gitHubToken.isNotBlank()) {
            secureVault.saveGitToken(_uiState.value.gitHubToken)
        }
        preferences.setOnboardingDone(true)
    }

    fun completeOnboarding(onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            completeOnboardingSync()
            onCompleted()
        }
    }
}
