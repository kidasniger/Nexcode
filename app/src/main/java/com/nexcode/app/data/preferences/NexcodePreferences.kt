package com.nexcode.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nexcode_settings")

class NexcodePreferences(private val context: Context) {

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_EDITOR_FONT_SIZE = intPreferencesKey("editor_font_size")
        val KEY_EDITOR_WORD_WRAP = booleanPreferencesKey("editor_word_wrap")
        val KEY_EDITOR_SHOW_LINE_NUMBERS = booleanPreferencesKey("editor_show_line_numbers")
        val KEY_AI_MODEL = stringPreferencesKey("ai_model")
        val KEY_AI_PROVIDER = stringPreferencesKey("ai_provider")
        val KEY_AI_CUSTOM_URL = stringPreferencesKey("ai_custom_url")
        val KEY_AI_AUTO_SUGGEST = booleanPreferencesKey("ai_auto_suggest")
        val KEY_BUILD_AUTO_INSTALL = booleanPreferencesKey("build_auto_install")
        val KEY_GIT_USER_NAME = stringPreferencesKey("git_user_name")
        val KEY_GIT_USER_EMAIL = stringPreferencesKey("git_user_email")
        val KEY_GIT_LOGIN = stringPreferencesKey("git_login")
        val KEY_GIT_REPO_COUNT = intPreferencesKey("git_repo_count")
        val KEY_GIT_PLAN = stringPreferencesKey("git_plan")
        val KEY_ACTIVE_PROJECT_ID = androidx.datastore.preferences.core.longPreferencesKey("active_project_id")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")

        // Valeurs par defaut sures
        const val DEFAULT_THEME_MODE = "SYSTEM"
        const val DEFAULT_FONT_SIZE = 13
        const val DEFAULT_WORD_WRAP = true
        const val DEFAULT_LINE_NUMBERS = true
        const val DEFAULT_AI_MODEL = "claude-sonnet-4.5"
        const val DEFAULT_AI_PROVIDER = "Anthropic"
        const val DEFAULT_AI_CUSTOM_URL = ""
        const val DEFAULT_AUTO_SUGGEST = true
        const val DEFAULT_AUTO_INSTALL = false
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: DEFAULT_THEME_MODE
    }

    val editorFontSizeFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_EDITOR_FONT_SIZE] ?: DEFAULT_FONT_SIZE
    }

    val editorWordWrapFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_EDITOR_WORD_WRAP] ?: DEFAULT_WORD_WRAP
    }

    val editorShowLineNumbersFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_EDITOR_SHOW_LINE_NUMBERS] ?: DEFAULT_LINE_NUMBERS
    }

    val aiModelFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_AI_MODEL] ?: DEFAULT_AI_MODEL
    }

    val aiProviderFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_AI_PROVIDER] ?: DEFAULT_AI_PROVIDER
    }

    val aiCustomUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_AI_CUSTOM_URL] ?: DEFAULT_AI_CUSTOM_URL
    }

    val aiAutoSuggestFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AI_AUTO_SUGGEST] ?: DEFAULT_AUTO_SUGGEST
    }

    val buildAutoInstallFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BUILD_AUTO_INSTALL] ?: DEFAULT_AUTO_INSTALL
    }

    val gitUserNameFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_GIT_USER_NAME] ?: ""
    }

    val gitUserEmailFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_GIT_USER_EMAIL] ?: ""
    }

    val gitLoginFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_GIT_LOGIN] ?: ""
    }

    val gitRepoCountFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_GIT_REPO_COUNT] ?: 0
    }

    val gitPlanFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_GIT_PLAN] ?: "free"
    }

    val activeProjectIdFlow: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_PROJECT_ID]
    }

    val onboardingDoneFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_DONE] ?: false
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    suspend fun setEditorFontSize(size: Int) {
        context.dataStore.edit { it[KEY_EDITOR_FONT_SIZE] = size }
    }

    suspend fun setEditorWordWrap(wrap: Boolean) {
        context.dataStore.edit { it[KEY_EDITOR_WORD_WRAP] = wrap }
    }

    suspend fun setEditorShowLineNumbers(show: Boolean) {
        context.dataStore.edit { it[KEY_EDITOR_SHOW_LINE_NUMBERS] = show }
    }

    suspend fun setAiModel(model: String) {
        context.dataStore.edit { it[KEY_AI_MODEL] = model }
    }

    suspend fun setAiProvider(provider: String) {
        context.dataStore.edit { it[KEY_AI_PROVIDER] = provider }
    }

    suspend fun setAiCustomUrl(url: String) {
        context.dataStore.edit { it[KEY_AI_CUSTOM_URL] = url }
    }

    suspend fun setAiAutoSuggest(autoSuggest: Boolean) {
        context.dataStore.edit { it[KEY_AI_AUTO_SUGGEST] = autoSuggest }
    }

    suspend fun setBuildAutoInstall(autoInstall: Boolean) {
        context.dataStore.edit { it[KEY_BUILD_AUTO_INSTALL] = autoInstall }
    }

    suspend fun setGitUser(name: String, email: String) {
        context.dataStore.edit {
            it[KEY_GIT_USER_NAME] = name
            it[KEY_GIT_USER_EMAIL] = email
        }
    }

    suspend fun setGitAccount(
        login: String,
        name: String,
        email: String,
        repoCount: Int,
        plan: String
    ) {
        context.dataStore.edit {
            it[KEY_GIT_LOGIN] = login
            it[KEY_GIT_USER_NAME] = name
            it[KEY_GIT_USER_EMAIL] = email
            it[KEY_GIT_REPO_COUNT] = repoCount
            it[KEY_GIT_PLAN] = plan
        }
    }

    suspend fun clearGitAccount() {
        context.dataStore.edit {
            it.remove(KEY_GIT_LOGIN)
            it.remove(KEY_GIT_USER_NAME)
            it.remove(KEY_GIT_USER_EMAIL)
            it.remove(KEY_GIT_REPO_COUNT)
            it.remove(KEY_GIT_PLAN)
        }
    }

    suspend fun setActiveProjectId(projectId: Long) {
        context.dataStore.edit { it[KEY_ACTIVE_PROJECT_ID] = projectId }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = done }
    }
}
