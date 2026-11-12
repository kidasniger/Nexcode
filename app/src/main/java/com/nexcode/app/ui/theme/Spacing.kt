package com.nexcode.app.ui.theme

import androidx.compose.ui.unit.dp

// Grille de 8 dp et dimensions exactes de la maquette
object NexcodeSpacing {
    val none = 0.dp
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp      // Grille unité 1
    val md = 12.dp     // Grille intermédiaire
    val base = 16.dp   // Grille unité 2
    val lg = 20.dp
    val xl = 24.dp     // Grille unité 3
    val xxl = 32.dp    // Grille unité 4
    val xxxl = 40.dp
    val giant = 48.dp  // Grille unité 6

    // Cible tactile minimale conforme aux directives (48 dp minimum)
    val minTouchTarget = 48.dp

    // Barre du bas (navigation)
    val bottomNavHeight = 82.dp

    // Éléments spécifiques de la maquette
    val topBarHeight = 56.dp
    val screenHorizontalPadding = 16.dp
}
