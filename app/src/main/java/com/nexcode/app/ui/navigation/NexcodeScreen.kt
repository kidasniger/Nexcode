package com.nexcode.app.ui.navigation

import com.nexcode.app.ui.components.NexcodeNavTab

sealed class NexcodeScreen(
    val route: String,
    val title: String,
    val tab: NexcodeNavTab?,
    val hasBottomBar: Boolean
) {
    // Ecrans hors onglets (1 a 4)
    data object S01 : NexcodeScreen("s01", "Bienvenue", null, false)
    data object S02 : NexcodeScreen("s02", "Configuration initiale", null, false)
    data object S03 : NexcodeScreen("s03", "Connexion & Validation", null, false)
    data object S04 : NexcodeScreen("s04", "Initialisation des outils", null, false)

    // Onglet Projets (5 a 9)
    data object S05 : NexcodeScreen("s05", "Projets", NexcodeNavTab.PROJECTS, true)
    data object S06 : NexcodeScreen("s06", "Nouveau projet", NexcodeNavTab.PROJECTS, true)
    data object S07 : NexcodeScreen("s07", "Cloner un depot", NexcodeNavTab.PROJECTS, true)
    data object S08 : NexcodeScreen("s08", "Parametres du projet", NexcodeNavTab.PROJECTS, true)
    data object S09 : NexcodeScreen("s09", "Details du projet", NexcodeNavTab.PROJECTS, true)

    // Onglet Editeur (10 a 14 et 18 a 20)
    data object S10 : NexcodeScreen("s10", "Explorateur de fichiers", NexcodeNavTab.EDITOR, true)
    data object S11 : NexcodeScreen("s11", "Éditeur de code", NexcodeNavTab.EDITOR, true)
    data object S12 : NexcodeScreen("s12", "Rechercher et remplacer", NexcodeNavTab.EDITOR, true)
    data object S13 : NexcodeScreen("s13", "Ressources", NexcodeNavTab.EDITOR, true)
    data object S14 : NexcodeScreen("s14", "Dépendances Gradle", NexcodeNavTab.EDITOR, true)

    // Onglet Chat IA (15 a 17)
    data object S15 : NexcodeScreen("s15", "Chat IA", NexcodeNavTab.CHAT, true)
    data object S16 : NexcodeScreen("s16", "Historique IA", NexcodeNavTab.CHAT, true)
    data object S17 : NexcodeScreen("s17", "Revue des modifications IA", NexcodeNavTab.CHAT, true)

    // Onglet Editeur (suite: 18 a 20)
    data object S18 : NexcodeScreen("s18", "Historique des commits", NexcodeNavTab.EDITOR, true)
    data object S19 : NexcodeScreen("s19", "Diff & Staging", NexcodeNavTab.EDITOR, true)
    data object S20 : NexcodeScreen("s20", "Terminal integre", NexcodeNavTab.EDITOR, true)

    // Onglet Build (21 a 30)
    data object S21 : NexcodeScreen("s21", "Build & Execution", NexcodeNavTab.BUILD, true)
    data object S22 : NexcodeScreen("s22", "Compilation en cours", NexcodeNavTab.BUILD, true)
    data object S23 : NexcodeScreen("s23", "Rapport d'APK", NexcodeNavTab.BUILD, true)
    data object S24 : NexcodeScreen("s24", "Journaux de compilation", NexcodeNavTab.BUILD, true)
    data object S25 : NexcodeScreen("s25", "Erreurs de build", NexcodeNavTab.BUILD, true)
    data object S26 : NexcodeScreen("s26", "Profilage & Performances", NexcodeNavTab.BUILD, true)
    data object S27 : NexcodeScreen("s27", "Signature de l'APK", NexcodeNavTab.BUILD, true)
    data object S28 : NexcodeScreen("s28", "Partage & Exportation", NexcodeNavTab.BUILD, true)
    data object S29 : NexcodeScreen("s29", "Historique des builds", NexcodeNavTab.BUILD, true)
    data object S30 : NexcodeScreen("s30", "Parametres generaux", NexcodeNavTab.BUILD, true)

    companion object {
        val ALL_SCREENS: List<NexcodeScreen> by lazy {
            listOf(
                S01, S02, S03, S04,
                S05, S06, S07, S08, S09,
                S10, S11, S12, S13, S14,
                S15, S16, S17,
                S18, S19, S20,
                S21, S22, S23, S24, S25, S26, S27, S28, S29, S30
            )
        }

        fun fromRoute(route: String?): NexcodeScreen {
            if (route == null) return S01
            return ALL_SCREENS.firstOrNull { it.route == route } ?: S05
        }

        fun getRootRouteForTab(tab: NexcodeNavTab): String {
            return when (tab) {
                NexcodeNavTab.PROJECTS -> S05.route
                NexcodeNavTab.EDITOR -> S11.route
                NexcodeNavTab.CHAT -> S15.route
                NexcodeNavTab.BUILD -> S21.route
            }
        }
    }
}
