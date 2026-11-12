package com.nexcode.app.ui.screens.designer

import androidx.annotation.DrawableRes
import com.example.R

enum class PaletteToolType(
    val label: String,
    @DrawableRes val iconRes: Int
) {
    TEXT("Texte", R.drawable.ic_text),
    IMAGE("Image", R.drawable.ic_image),
    BUTTON("Bouton", R.drawable.ic_play),
    FIELD("Champ", R.drawable.ic_edit),
    CARD("Carte", R.drawable.ic_layout),
    LIST("Liste", R.drawable.ic_list),
    COLUMN("Colonne", R.drawable.ic_down),
    ROW("Ligne", R.drawable.ic_lr),
    TOGGLE("Commut.", R.drawable.ic_toggle),
    GRID("Grille", R.drawable.ic_grid)
}

data class CityWeatherItem(
    val id: String,
    val name: String,
    val description: String,
    val temperature: String,
    val isSelected: Boolean = false,
    val isFaded: Boolean = false
)

data class DesignerUiState(
    // S18 : Concepteur visuel
    val selectedTool: PaletteToolType? = null,
    val selectedComponentId: String = "carte_3",
    val isCardSelected: Boolean = true,
    val citiesList: List<CityWeatherItem> = listOf(
        CityWeatherItem(
            id = "c1",
            name = "Lyon",
            description = "Nuageux · 14 °C",
            temperature = "14°",
            isSelected = true,
            isFaded = false
        ),
        CityWeatherItem(
            id = "c2",
            name = "Paris",
            description = "Éclaircies · 12 °C",
            temperature = "12°",
            isSelected = false,
            isFaded = true
        ),
        CityWeatherItem(
            id = "c3",
            name = "Brest",
            description = "Pluie · 11 °C",
            temperature = "11°",
            isSelected = false,
            isFaded = true
        )
    ),

    // S19 : Proprietes
    val componentName: String = "Carte #3",
    val componentSubtitle: String = "Contient 2 textes · parent : Colonne racine",
    val isComponentEnabled: Boolean = true,
    val posX: String = "16 dp",
    val posY: String = "220 dp",
    val widthMode: String = "Remplir",
    val heightVal: String = "96 dp",
    val cornerRadius: Int = 12,
    val cornerRadiusProgress: Float = 0.48f,
    val bgColorHex: String = "#121826",
    val borderVal: String = "Aucune",
    val elevationVal: String = "0 dp",
    val paddingDp: Int = 16,
    val marginDp: Int = 8,
    val internalDp: Int = 10,
    val alignmentIndex: Int = 1, // 0 = horizontal (lr), 1 = vertical (list), 2 = free (move)
    val isCodeGenerated: Boolean = false,

    // S20 : Apercu
    val isDarkPreview: Boolean = true,
    val isLandscape: Boolean = false,
    val deviceName: String = "Pixel 8",
    val previewBannerText: String = "Aperçu statique généré il y a 1 min · Compose compilé sans erreur"
)
