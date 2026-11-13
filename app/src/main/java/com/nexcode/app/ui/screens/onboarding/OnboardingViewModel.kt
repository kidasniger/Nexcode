package com.nexcode.app.ui.screens.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexcode.app.data.ai.AiEndpointConfig
import com.nexcode.app.data.ai.AiProviderType
import com.nexcode.app.data.ai.AiRepositoryService
import com.nexcode.app.data.github.repository.GitHubRepositoryService
import com.nexcode.app.data.github.repository.GitHubResult
import com.nexcode.app.data.preferences.NexcodePreferences
import com.nexcode.app.data.preferences.NexcodeSecureVault
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    // Écran 1
    val preparationProgress: Float = 0.62f,

    // Écran 3 : GitHub (données réelles)
    val gitHubToken: String = "",
    val isTokenVisible: Boolean = false,
    val isGitHubConnected: Boolean = false,
    val isConnectingGitHub: Boolean = false,
    val gitHubUsername: String = "",
    val gitHubRepoCount: Int = 0,
    val isGitHubError: Boolean = false,
    val gitHubErrorMessage: String? = null,
    val showDisconnectDialog: Boolean = false,
    val userNotice: String? = null,

    // Écran 4 : Configuration IA
    val selectedProvider: String = "Anthropic",
    val apiKey: String = "",
    val isApiKeyVisible: Boolean = false,
    val customBaseUrl: String = "",
    val selectedModel: String = "claude-sonnet-4.5",
    val showModelSheet: Boolean = false,
    val isTestingAi: Boolean = false,
    val aiTestSuccess: Boolean? = null,
    val aiTestMessage: String? = null
)

class OnboardingViewModel(
    private val context: Context,
    private val gitService: GitHubRepositoryService = GitHubRepositoryService(context),
    private val aiService: AiRepositoryService = AiRepositoryService(context)
) : ViewModel() {

    private val secureVault = NexcodeSecureVault(context)
    private val preferences = NexcodePreferences(context)

    private val initialSavedToken = secureVault.getGitToken().orEmpty()

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            apiKey = secureVault.getApiKey().orEmpty(),
            gitHubToken = initialSavedToken,
            isGitHubConnected = initialSavedToken.isNotBlank()
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        loadSavedGitHubSession()
        loadSavedAiConfig()
    }

    fun refreshSessionFromStorage() {
        loadSavedGitHubSession()
        loadSavedAiConfig()
    }

    private fun loadSavedAiConfig() {
        viewModelScope.launch {
            val savedProvider = runCatching { preferences.aiProviderFlow.first() }
                .getOrDefault(NexcodePreferences.DEFAULT_AI_PROVIDER)
            val savedModel = runCatching { preferences.aiModelFlow.first() }
                .getOrDefault(NexcodePreferences.DEFAULT_AI_MODEL)
            val savedCustomUrl = runCatching { preferences.aiCustomUrlFlow.first() }
                .getOrDefault(NexcodePreferences.DEFAULT_AI_CUSTOM_URL)
            val savedKey = secureVault.getApiKey().orEmpty()

            _uiState.update {
                it.copy(
                    selectedProvider = savedProvider.ifBlank { NexcodePreferences.DEFAULT_AI_PROVIDER },
                    selectedModel = savedModel.ifBlank { NexcodePreferences.DEFAULT_AI_MODEL },
                    customBaseUrl = savedCustomUrl,
                    apiKey = savedKey
                )
            }
        }
    }

    private fun loadSavedGitHubSession() {
        viewModelScope.launch {
            val savedToken = secureVault.getGitToken().orEmpty()
            if (savedToken.isBlank()) {
                _uiState.update {
                    it.copy(
                        gitHubToken = "",
                        isGitHubConnected = false,
                        gitHubUsername = "",
                        gitHubRepoCount = 0
                    )
                }
                return@launch
            }
            val cachedLogin = preferences.gitLoginFlow.first()
            val cachedRepoCount = preferences.gitRepoCountFlow.first()
            _uiState.update {
                it.copy(
                    gitHubToken = savedToken,
                    isGitHubConnected = cachedLogin.isNotBlank(),
                    gitHubUsername = cachedLogin,
                    gitHubRepoCount = cachedRepoCount
                )
            }
            when (val result = gitService.getAuthenticatedUser()) {
                is GitHubResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isGitHubConnected = true,
                            isGitHubError = false,
                            gitHubErrorMessage = null,
                            gitHubUsername = result.data.user.login,
                            gitHubRepoCount = result.data.repoCount
                        )
                    }
                }
                is GitHubResult.Error -> {
                    if (result.code == 401) {
                        _uiState.update {
                            it.copy(
                                isGitHubConnected = false,
                                isGitHubError = true,
                                gitHubErrorMessage = result.message
                            )
                        }
                    }
                }
                is GitHubResult.NetworkError -> Unit
            }
        }
    }

    fun updateGitHubToken(token: String) {
        _uiState.update {
            it.copy(
                gitHubToken = token,
                isGitHubConnected = false,
                gitHubErrorMessage = null
            )
        }
    }

    fun toggleTokenVisibility() {
        _uiState.update { it.copy(isTokenVisible = !it.isTokenVisible) }
    }

    fun toggleGitHubErrorState() {
        _uiState.update { it.copy(isGitHubError = !it.isGitHubError) }
    }

    fun connectWithToken(onConnected: (() -> Unit)? = null) {
        val token = _uiState.value.gitHubToken.trim()
        if (token.isBlank()) {
            _uiState.update {
                it.copy(
                    isGitHubError = true,
                    isGitHubConnected = false,
                    gitHubErrorMessage = "Veuillez saisir un jeton d'accès personnel GitHub."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isConnectingGitHub = true,
                    isGitHubError = false,
                    gitHubErrorMessage = null
                )
            }
            when (val result = gitService.verifyAndSaveToken(token)) {
                is GitHubResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isConnectingGitHub = false,
                            isGitHubConnected = true,
                            isGitHubError = false,
                            gitHubErrorMessage = null,
                            gitHubToken = token,
                            gitHubUsername = result.data.user.login,
                            gitHubRepoCount = result.data.repoCount
                        )
                    }
                    onConnected?.invoke()
                }
                is GitHubResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isConnectingGitHub = false,
                            isGitHubConnected = false,
                            isGitHubError = true,
                            gitHubErrorMessage = null
                        )
                    }
                }
                is GitHubResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isConnectingGitHub = false,
                            isGitHubConnected = false,
                            isGitHubError = true,
                            gitHubErrorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun retryGitHubConnection() {
        connectWithToken()
    }

    fun showDeviceCodeNotice() {
        _uiState.update {
            it.copy(userNotice = "Utilisez un jeton d'accès personnel (PAT) avec les portées repo et workflow.")
        }
    }

    fun dismissNotice() {
        _uiState.update { it.copy(userNotice = null) }
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
                gitHubUsername = "",
                gitHubRepoCount = 0,
                isGitHubError = false,
                gitHubErrorMessage = null,
                showDisconnectDialog = false
            )
        }
        viewModelScope.launch {
            gitService.clearGitSession()
        }
    }

    fun selectAiProvider(provider: String) {
        _uiState.update { current ->
            val suggestedModel = when (AiProviderType.fromDisplayName(provider)) {
                AiProviderType.OPENAI -> if (current.selectedModel.startsWith("claude", ignoreCase = true)) "gpt-4o" else current.selectedModel
                AiProviderType.ANTHROPIC -> if (current.selectedModel.startsWith("gpt", ignoreCase = true)) "claude-sonnet-4.5" else current.selectedModel
                AiProviderType.CUSTOM -> if (current.selectedModel.startsWith("claude", ignoreCase = true)) "qwen2.5-coder:7b" else current.selectedModel
            }
            current.copy(
                selectedProvider = provider,
                selectedModel = suggestedModel,
                aiTestSuccess = null,
                aiTestMessage = null
            )
        }
        viewModelScope.launch {
            preferences.setAiProvider(provider)
            preferences.setAiModel(_uiState.value.selectedModel)
        }
    }

    fun updateApiKey(key: String) {
        _uiState.update { it.copy(apiKey = key, aiTestSuccess = null, aiTestMessage = null) }
    }

    fun updateCustomBaseUrl(url: String) {
        _uiState.update { it.copy(customBaseUrl = url, aiTestSuccess = null, aiTestMessage = null) }
    }

    fun toggleApiKeyVisibility() {
        _uiState.update { it.copy(isApiKeyVisible = !it.isApiKeyVisible) }
    }

    fun showModelBottomSheet(show: Boolean) {
        _uiState.update { it.copy(showModelSheet = show) }
    }

    suspend fun selectModelSync(model: String) {
        _uiState.update {
            it.copy(
                selectedModel = model,
                showModelSheet = false,
                aiTestSuccess = null,
                aiTestMessage = null
            )
        }
        preferences.setAiModel(model)
    }

    fun selectModel(model: String) {
        viewModelScope.launch {
            selectModelSync(model)
        }
    }

    suspend fun testAiConnectionSync(): Boolean {
        val state = _uiState.value
        _uiState.update { it.copy(isTestingAi = true, aiTestSuccess = null, aiTestMessage = null) }
        val config = AiEndpointConfig(
            provider = AiProviderType.fromDisplayName(state.selectedProvider),
            model = state.selectedModel,
            apiKey = state.apiKey.trim(),
            customBaseUrl = state.customBaseUrl.trim()
        )
        val result = aiService.testConnection(config)
        if (result.success) {
            aiService.saveConfig(
                providerName = state.selectedProvider,
                model = state.selectedModel,
                apiKey = state.apiKey.trim(),
                customBaseUrl = state.customBaseUrl.trim()
            )
        }
        _uiState.update {
            it.copy(
                isTestingAi = false,
                aiTestSuccess = result.success,
                aiTestMessage = result.message
            )
        }
        return result.success
    }

    fun testAiConnection() {
        viewModelScope.launch {
            testAiConnectionSync()
        }
    }

    suspend fun completeOnboardingSync() {
        val state = _uiState.value
        aiService.saveConfig(
            providerName = state.selectedProvider,
            model = state.selectedModel,
            apiKey = state.apiKey.trim().takeIf { it.isNotEmpty() },
            customBaseUrl = state.customBaseUrl.trim()
        )
        if (state.gitHubToken.isNotBlank() && state.isGitHubConnected) {
            secureVault.saveGitToken(state.gitHubToken)
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
