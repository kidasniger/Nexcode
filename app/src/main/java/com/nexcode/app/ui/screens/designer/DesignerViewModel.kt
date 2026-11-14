package com.nexcode.app.ui.screens.designer

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DesignerViewModel(
    @Suppress("UNUSED_PARAMETER") context: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DesignerUiState())
    val uiState: StateFlow<DesignerUiState> = _uiState.asStateFlow()

    private var componentIdCounter = 0

    // Dimensions de cadre réelles par appareil : changer d'appareil change
    // réellement la taille de la maquette affichée
    private val deviceDimensions = linkedMapOf(
        "Pixel 8" to Pair(246, 520),
        "Pixel Tablet" to Pair(340, 480),
        "Galaxy S24" to Pair(240, 510)
    )

    private fun nextComponentId(): String {
        componentIdCounter++
        return "composant_$componentIdCounter"
    }

    private fun nextComponentName(type: PaletteToolType): String {
        val sameTypeCount = _uiState.value.components.count { it.type == type } + 1
        return "${type.label} $sameTypeCount"
    }

    // ------------------------------------------------------------------
    // S18 : palette et plan de travail local
    // ------------------------------------------------------------------

    fun selectTool(tool: PaletteToolType) {
        _uiState.update {
            it.copy(selectedTool = if (it.selectedTool == tool) null else tool)
        }
    }

    /**
     * Ajoute un composant réel au plan de travail, à partir de l'outil sélectionné.
     * Le composant créé est sélectionné.
     */
    fun addComponentFromTool() {
        val tool = _uiState.value.selectedTool ?: return
        val component = DesignerComponent(
            id = nextComponentId(),
            type = tool,
            name = nextComponentName(tool),
            text = if (tool.acceptsText) tool.label else ""
        )
        _uiState.update {
            it.copy(
                components = it.components + component,
                selectedComponentId = component.id
            )
        }
    }

    fun selectComponent(id: String) {
        _uiState.update { it.copy(selectedComponentId = id) }
    }

    // ------------------------------------------------------------------
    // S19 : propriétés réelles du composant sélectionné
    // ------------------------------------------------------------------

    private fun updateSelected(transform: (DesignerComponent) -> DesignerComponent) {
        _uiState.update { state ->
            val selected = state.selectedComponent ?: return@update state
            state.copy(
                components = state.components.map {
                    if (it.id == selected.id) transform(it) else it
                }
            )
        }
    }

    fun toggleComponentEnabled() {
        updateSelected { it.copy(isEnabled = !it.isEnabled) }
    }

    fun updateComponentText(text: String) {
        updateSelected { it.copy(text = text) }
    }

    fun updatePosX(x: String) {
        val value = x.filter { it.isDigit() }.toIntOrNull() ?: return
        updateSelected { it.copy(posX = value) }
    }

    fun updatePosY(y: String) {
        val value = y.filter { it.isDigit() }.toIntOrNull() ?: return
        updateSelected { it.copy(posY = value) }
    }

    fun updateHeight(h: String) {
        val value = h.filter { it.isDigit() }.toIntOrNull() ?: return
        updateSelected { it.copy(heightDp = value) }
    }

    fun cycleWidthMode() {
        val modes = listOf("Remplir", "Ajuster", "160 dp", "240 dp")
        updateSelected { component ->
            val next = modes[(modes.indexOf(component.widthMode) + 1) % modes.size]
            component.copy(widthMode = next)
        }
    }

    fun setCornerRadiusProgress(progress: Float) {
        val radius = (progress * 25).toInt()
        updateSelected { it.copy(cornerRadius = radius) }
    }

    fun updateBgColor(hex: String) {
        updateSelected { it.copy(bgColorHex = hex) }
    }

    /**
     * Duplique réellement le composant sélectionné : copie intégrale de toutes
     * ses propriétés, insérée juste après l'original et sélectionnée.
     */
    fun duplicateComponent() {
        _uiState.update { state ->
            val selected = state.selectedComponent ?: return@update state
            val copy = selected.copy(
                id = nextComponentId(),
                name = "${selected.name} (copie)"
            )
            val index = state.components.indexOfFirst { it.id == selected.id }
            val updated = state.components.toMutableList().apply {
                add(index + 1, copy)
            }
            state.copy(
                components = updated,
                selectedComponentId = copy.id
            )
        }
    }

    /**
     * Supprime réellement le composant sélectionné de la liste, puis
     * sélectionne le composant suivant (ou aucun si la liste est vide).
     */
    fun deleteComponent() {
        _uiState.update { state ->
            val selected = state.selectedComponent ?: return@update state
            val index = state.components.indexOfFirst { it.id == selected.id }
            val updated = state.components.filterNot { it.id == selected.id }
            val nextSelected = if (updated.isEmpty()) {
                null
            } else {
                updated.getOrNull(index.coerceAtMost(updated.lastIndex))?.id ?: updated.last().id
            }
            state.copy(
                components = updated,
                selectedComponentId = nextSelected
            )
        }
    }

    // ------------------------------------------------------------------
    // S20 : aperçu réel du plan de travail
    // ------------------------------------------------------------------

    fun togglePreviewTheme(darkMode: Boolean) {
        _uiState.update { it.copy(isDarkPreview = darkMode) }
    }

    fun toggleOrientation() {
        _uiState.update { state ->
            val isLandscape = !state.isLandscape
            val dims = refreshPhoneDimensions(state.deviceName, isLandscape)
            state.copy(
                isLandscape = isLandscape,
                phoneWidthDp = dims.first,
                phoneHeightDp = dims.second
            )
        }
    }

    fun cycleDevice() {
        val devices = deviceDimensions.keys.toList()
        if (devices.isEmpty()) return
        _uiState.update { state ->
            val currentIndex = devices.indexOf(state.deviceName).coerceAtLeast(0)
            val nextDevice = devices[(currentIndex + 1) % devices.size]
            val dims = refreshPhoneDimensions(nextDevice, state.isLandscape)
            state.copy(
                deviceName = nextDevice,
                phoneWidthDp = dims.first,
                phoneHeightDp = dims.second
            )
        }
    }

    private fun refreshPhoneDimensions(deviceName: String, isLandscape: Boolean): Pair<Int, Int> {
        val (portraitWidth, portraitHeight) = deviceDimensions[deviceName] ?: Pair(246, 520)
        return if (isLandscape) Pair(portraitHeight, portraitWidth) else Pair(portraitWidth, portraitHeight)
    }
}
