package com.nexcode.app.ui.screens.designer

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DesignerViewModel(
    context: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DesignerUiState())
    val uiState: StateFlow<DesignerUiState> = _uiState.asStateFlow()

    fun selectTool(tool: PaletteToolType) {
        _uiState.update {
            it.copy(selectedTool = if (it.selectedTool == tool) null else tool)
        }
    }

    fun selectCard(select: Boolean) {
        _uiState.update {
            it.copy(
                isCardSelected = select,
                selectedComponentId = if (select) "carte_3" else ""
            )
        }
    }

    fun toggleComponentEnabled() {
        _uiState.update { it.copy(isComponentEnabled = !it.isComponentEnabled) }
    }

    fun updatePosX(x: String) {
        _uiState.update { it.copy(posX = x) }
    }

    fun updatePosY(y: String) {
        _uiState.update { it.copy(posY = y) }
    }

    fun updateHeight(h: String) {
        _uiState.update { it.copy(heightVal = h) }
    }

    fun cycleWidthMode() {
        val modes = listOf("Remplir", "Ajuster", "160 dp", "240 dp")
        val next = modes[(modes.indexOf(_uiState.value.widthMode) + 1) % modes.size]
        _uiState.update { it.copy(widthMode = next) }
    }

    fun setCornerRadiusProgress(progress: Float) {
        val radius = (progress * 25).toInt()
        _uiState.update {
            it.copy(
                cornerRadiusProgress = progress,
                cornerRadius = radius
            )
        }
    }

    fun setAlignment(index: Int) {
        _uiState.update { it.copy(alignmentIndex = index) }
    }

    fun duplicateComponent() {
        _uiState.update {
            it.copy(
                componentName = "Carte #4",
                componentSubtitle = "Contient 2 textes · parent : Colonne racine",
                posY = "324 dp"
            )
        }
    }

    fun deleteComponent() {
        _uiState.update {
            it.copy(
                isCardSelected = false,
                selectedComponentId = ""
            )
        }
    }

    fun generateComposeCode() {
        _uiState.update { it.copy(isCodeGenerated = true) }
    }

    fun togglePreviewTheme(darkMode: Boolean) {
        _uiState.update { it.copy(isDarkPreview = darkMode) }
    }

    fun toggleOrientation() {
        _uiState.update { it.copy(isLandscape = !it.isLandscape) }
    }

    fun cycleDevice() {
        val devices = listOf("Pixel 8", "Pixel Tablet", "Galaxy S24")
        val currentIndex = devices.indexOf(_uiState.value.deviceName)
        val nextDevice = devices[(currentIndex + 1) % devices.size]
        _uiState.update { it.copy(deviceName = nextDevice) }
    }
}
