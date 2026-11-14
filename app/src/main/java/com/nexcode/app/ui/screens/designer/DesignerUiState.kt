package com.nexcode.app.ui.screens.designer

import androidx.annotation.DrawableRes
import com.example.R

enum class PaletteToolType(
    val label: String,
    @DrawableRes val iconRes: Int,
    // Indique si le composant possède un contenu texte réellement éditable
    val acceptsText: Boolean
) {
    TEXT("Texte", R.drawable.ic_text, true),
    IMAGE("Image", R.drawable.ic_image, false),
    BUTTON("Bouton", R.drawable.ic_play, true),
    FIELD("Champ", R.drawable.ic_edit, true),
    CARD("Carte", R.drawable.ic_layout, true),
    LIST("Liste", R.drawable.ic_list, false),
    COLUMN("Colonne", R.drawable.ic_down, false),
    ROW("Ligne", R.drawable.ic_lr, false),
    TOGGLE("Commut.", R.drawable.ic_toggle, false),
    GRID("Grille", R.drawable.ic_grid, false)
}

/**
 * Composant du plan de travail local : toutes les propriétés sont réellement
 * éditables par l'utilisateur. Le plan reste en mémoire (non enregistré dans
 * le projet) et l'interface l'indique honnêtement.
 */
data class DesignerComponent(
    val id: String,
    val type: PaletteToolType,
    val name: String,
    val text: String = "",
    val isEnabled: Boolean = true,
    val posX: Int = 16,
    val posY: Int = 220,
    val widthMode: String = "Remplir",
    val heightDp: Int = 96,
    val cornerRadius: Int = 12,
    val bgColorHex: String = "#121826"
)

data class DesignerUiState(
    // S18 : Concepteur visuel (plan de travail local)
    val selectedTool: PaletteToolType? = null,
    val components: List<DesignerComponent> = emptyList(),
    val selectedComponentId: String? = null,

    // S20 : Aperçu (dimensions réelles du cadre selon l'appareil choisi)
    val isDarkPreview: Boolean = true,
    val isLandscape: Boolean = false,
    val deviceName: String = "Pixel 8",
    val phoneWidthDp: Int = 246,
    val phoneHeightDp: Int = 520
) {
    val selectedComponent: DesignerComponent?
        get() = components.firstOrNull { it.id == selectedComponentId }
}
