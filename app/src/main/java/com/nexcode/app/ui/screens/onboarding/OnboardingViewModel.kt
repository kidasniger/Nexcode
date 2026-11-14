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
    // Écran 3 : GitHub
    val gitHubToken: String = "",
    val isTokenVisible: Boolean = false,
    val isGitHubConnected: Boolean = false,
    val gitHubUsername: String = "",
    val gitHubRepoCount: Int = 0,
    val isGitHubError: Boolean = false,
    val gitHubErrorMessage: String? = null,
    val isConnectingGitHub: Boolean = false,
    val showDisconnectDialog: Boolean = false,
    val userNotice: String? = null,

    // Écran 4 : Configuration IA
    val selectedProvider: String = "Anthropic",
    val customBaseUrl: String = "",
    val apiKey: String = "",
    val isApiKeyVisible: Boolean = false,
    val selectedModel: String = "claude-sonnet-4.5",
    val showModelSheet: Boolean = false,
    val isTestingAi: Boolean = false,
    val aiTestSuccess: Boolean? = null,
    val aiTestMessage: String? = null
)

class OnboardingViewModel(
    private val context: Context,
    overrideGitService: GitHubRepositoryService? = null,
    overrideAiService: AiRepositoryService? = null
) : ViewModel() {

    private val secureVault = NexcodeSecureVault(context)
    private val preferences = NexcodePreferences(context)
    private val gitHubService = overrideGitService ?: GitHubRepositoryService(context)
    private val aiService = overrideAiService ?: AiRepositoryService(context)

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            apiKey = secureVault.getApiKey().orEmpty(),
            gitHubToken = secureVault.getGitToken().orEmpty(),
            isGitHubConnected = !secureVault.getGitToken().isNullOrBlank()
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        refreshSessionFromStorage()
        viewModelScope.launch {
            preferences.aiProviderFlow.collect { provider ->
                if (provider.isNotBlank()) {
                    _uiState.update { it.copy(selectedProvider = provider) }
                }
            }
        }
        viewModelScope.launch {
            preferences.aiCustomUrlFlow.collect { url ->
                if (url.isNotBlank()) {
                    _uiState.update { it.copy(customBaseUrl = url) }
                }
            }
        }
    }

    fun refreshSessionFromStorage() {
        viewModelScope.launch { refreshSessionFromStorageSync() }
    }

    /**
     * Variante suspend déterministe : lis réellement le coffre et les préférences
     * puis met à jour l'état. Les tests JVM l'appellent directement pour éviter de
     * dépendre de la reprise d'une continuation Main sur le looper Robolectric.
     */
    suspend fun refreshSessionFromStorageSync() {
        val savedToken = secureVault.getGitToken().orEmpty()
        val login = preferences.gitLoginFlow.first()
        val repoCount = preferences.gitRepoCountFlow.first()
        _uiState.update {
            it.copy(
                gitHubToken = savedToken,
                isGitHubConnected = savedToken.isNotBlank() && login.isNotBlank(),
                gitHubUsername = if (savedToken.isNotBlank()) login else "",
                gitHubRepoCount = if (savedToken.isNotBlank()) repoCount else 0
            )
        }
    }

    fun updateGitHubToken(token: String) {
        _uiState.update { it.copy(gitHubToken = token, isGitHubError = false, gitHubErrorMessage = null) }
    }

    fun toggleTokenVisibility() {
        _uiState.update { it.copy(isTokenVisible = !it.isTokenVisible) }
    }

    fun connectWithToken(onSuccess: (() -> Unit)? = null) {
        val token = _uiState.value.gitHubToken.trim()
        if (token.isBlank()) {
            _uiState.update {
                it.copy(
                    isGitHubError = true,
                    gitHubErrorMessage = "Veuillez saisir un jeton d'accès personnel GitHub."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isConnectingGitHub = true, isGitHubError = false, gitHubErrorMessage = null) }
            when (val res = gitHubService.verifyAndSaveToken(token)) {
                is GitHubResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isConnectingGitHub = false,
                            isGitHubError = false,
                            gitHubErrorMessage = null,
                            isGitHubConnected = true,
                            gitHubToken = token,
                            gitHubUsername = res.data.user.login,
                            gitHubRepoCount = res.data.repoCount,
                            userNotice = "Connecté à GitHub en tant que ${res.data.user.login}"
                        )
                    }
                    onSuccess?.invoke()
                }
                is GitHubResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isConnectingGitHub = false,
                            isGitHubError = true,
                            gitHubErrorMessage = res.message
                        )
                    }
                }
                is GitHubResult.NetworkError -> {
                    _uiState.update {
                        it.copy(
                            isConnectingGitHub = false,
                            isGitHubError = true,
                            gitHubErrorMessage = null
                        )
                    }
                }
            }
        }
    }

    fun retryGitHubConnection() {
        connectWithToken()
    }

    fun dismissNotice() {
        _uiState.update { it.copy(userNotice = null) }
    }

    fun showDisconnectModal(show: Boolean) {
        _uiState.update { it.copy(showDisconnectDialog = show) }
    }

    fun disconnectGitHub() {
        secureVault.clearGitToken()
        viewModelScope.launch {
            gitHubService.clearGitSession()
        }
        _uiState.update {
            it.copy(
                isGitHubConnected = false,
                gitHubToken = "",
                gitHubUsername = "",
                gitHubRepoCount = 0,
                showDisconnectDialog = false,
                userNotice = "Compte GitHub déconnecté."
            )
        }
    }

    fun selectAiProvider(provider: String) {
        _uiState.update { it.copy(selectedProvider = provider) }
    }

    fun updateCustomBaseUrl(url: String) {
        _uiState.update { it.copy(customBaseUrl = url) }
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
            _uiState.update { it.copy(isTestingAi = true, aiTestSuccess = null, aiTestMessage = null) }
            // Test réseau réel via AiRepositoryService (aucune réponse simulée)
            val state = _uiState.value
            val config = AiEndpointConfig(
                provider = AiProviderType.fromDisplayName(state.selectedProvider),
                model = state.selectedModel,
                apiKey = state.apiKey.trim(),
                customBaseUrl = state.customBaseUrl.trim()
            )
            val result = aiService.testConnection(config)
            _uiState.update {
                it.copy(
                    isTestingAi = false,
                    aiTestSuccess = result.success,
                    aiTestMessage = result.message
                )
            }
        }
    }

    suspend fun completeOnboardingSync() {
        if (_uiState.value.apiKey.isNotBlank()) {
            secureVault.saveApiKey(_uiState.value.apiKey)
        }
        if (_uiState.value.gitHubToken.isNotBlank()) {
            secureVault.saveGitToken(_uiState.value.gitHubToken)
        }
        preferences.setAiProvider(_uiState.value.selectedProvider)
        preferences.setAiCustomUrl(_uiState.value.customBaseUrl)
        preferences.setAiModel(_uiState.value.selectedModel)
        preferences.setOnboardingDone(true)
    }

    fun completeOnboarding(onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            completeOnboardingSync()
            onCompleted()
        }
    }
}
