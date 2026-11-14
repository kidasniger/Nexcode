package com.nexcode.app.ui.screens.build

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.concurrent.thread
import kotlin.math.min

/**
 * ViewModel des écrans de build (21 à 24).
 *
 * Le paramètre [context] est nullable car les tests unitaires JVM construisent le
 * ViewModel sans contexte Android : aucune API Android n'est donc appelée depuis
 * cette classe (le worker de progression n'est démarré qu'avec un contexte réel).
 */
class BuildViewModel(context: Context? = null) {

    // Contexte conservé pour la persistance future des builds (tests : null).
    private val appContext: Context? = context?.applicationContext

    private val _uiState = MutableStateFlow(BuildUiState())
    val uiState: StateFlow<BuildUiState> = _uiState.asStateFlow()

    // Génération de compilation : incrémentée à chaque (re)démarrage ou annulation
    // pour invalider proprement le worker de progression précédent.
    @Volatile
    private var generation: Int = 0

    // Écran 21 : configuration du build

    fun setVariant(variant: BuildVariant) {
        _uiState.update {
            it.copy(
                selectedVariant = variant,
                // En release, le mode debuggable est désactivé automatiquement (maquette).
                isDebuggableEnabled = if (variant == BuildVariant.RELEASE) false else it.isDebuggableEnabled
            )
        }
    }

    fun toggleR8() {
        _uiState.update { it.copy(isR8Enabled = !it.isR8Enabled) }
    }

    fun toggleExportMapping() {
        _uiState.update { it.copy(isExportMappingEnabled = !it.isExportMappingEnabled) }
    }

    fun toggleDebuggable() {
        _uiState.update { it.copy(isDebuggableEnabled = !it.isDebuggableEnabled) }
    }

    // Écran 22 : compilation (simulation locale)

    fun startCompilation(onStarted: () -> Unit = {}) {
        generation += 1
        val currentGeneration = generation
        _uiState.update {
            it.copy(
                compilationStatus = CompilationStatus.RUNNING,
                progressPercent = 10,
                steps = defaultBuildSteps().mapIndexed { index, step ->
                    if (index == 0) {
                        step.copy(status = BuildStepStatus.RUNNING)
                    } else {
                        step.copy(status = BuildStepStatus.PENDING)
                    }
                }
            )
        }
        onStarted()
        // Sans contexte Android (tests unitaires), aucun worker n'est démarré :
        // la progression reste déterministe.
        if (appContext != null) {
            startProgressWorker(currentGeneration)
        }
    }

    fun cancelCompilation() {
        generation += 1
        _uiState.update { it.copy(compilationStatus = CompilationStatus.CANCELLED) }
    }

    // Écran 23 : logs et erreurs

    fun setLogFilter(filter: String) {
        _uiState.update { it.copy(selectedLogFilter = filter) }
    }

    fun toggleLogVariant() {
        _uiState.update { it.copy(logVariantSuccess = !it.logVariantSuccess) }
    }

    // Écran 24 : notices

    fun dismissNotice() {
        _uiState.update { it.copy(workflowNotice = null) }
    }

    /**
     * Worker de progression simulée : avance la barre et fait basculer les étapes
     * jusqu'à 100 % (statut SUCCESS). Il s'arrête dès que la génération change
     * (relance ou annulation) ou que le statut n'est plus RUNNING.
     */
    private fun startProgressWorker(currentGeneration: Int) {
        thread(name = "nexcode-build-progress", isDaemon = true) {
            val stepThresholds = listOf(18, 34, 52, 66, 80, 90, 100)
            var percent = 10
            while (percent < 100) {
                Thread.sleep(900)
                val state = _uiState.value
                if (generation != currentGeneration || state.compilationStatus != CompilationStatus.RUNNING) {
                    return@thread
                }
                percent = min(100, percent + 9)
                _uiState.update { current ->
                    val updatedSteps = current.steps.mapIndexed { index, step ->
                        val threshold = stepThresholds.getOrElse(index) { 100 }
                        when {
                            percent >= threshold -> step.copy(status = BuildStepStatus.SUCCESS)
                            index == stepThresholds.indexOfFirst { percent < it } ->
                                step.copy(status = BuildStepStatus.RUNNING)
                            else -> step
                        }
                    }
                    current.copy(progressPercent = percent, steps = updatedSteps)
                }
                if (percent >= 100) {
                    _uiState.update { it.copy(compilationStatus = CompilationStatus.SUCCESS) }
                }
            }
        }
    }
}
