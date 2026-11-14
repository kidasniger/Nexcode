package com.nexcode.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.nexcode.app.ui.components.NexcodeBanner
import com.nexcode.app.ui.components.NexcodeBannerVariant
import com.nexcode.app.ui.components.NexcodeBottomBar
import com.nexcode.app.ui.components.NexcodeCard
import com.nexcode.app.ui.components.NexcodeNavTab
import com.nexcode.app.ui.components.NexcodeTopBar
import com.nexcode.app.ui.screens.onboarding.OnboardingViewModel
import com.nexcode.app.ui.screens.onboarding.ScreenS01Launch
import com.nexcode.app.ui.screens.onboarding.ScreenS02Presentation
import com.nexcode.app.ui.screens.onboarding.ScreenS03GitHub
import com.nexcode.app.ui.screens.onboarding.ScreenS04AiConfig
import com.nexcode.app.ui.screens.projects.ProjectsViewModel
import com.nexcode.app.ui.screens.projects.ScreenS05Projects
import com.nexcode.app.ui.screens.projects.ScreenS06NewProject
import com.nexcode.app.ui.screens.projects.ScreenS07CreateWizard
import com.nexcode.app.ui.screens.projects.ScreenS08ProjectDetails
import com.nexcode.app.ui.screens.projects.ScreenS09ImportProject
import com.nexcode.app.ui.screens.editor.EditorViewModel
import com.nexcode.app.ui.screens.editor.ScreenS10FileExplorer
import com.nexcode.app.ui.screens.editor.ScreenS11CodeEditor
import com.nexcode.app.ui.screens.editor.ScreenS12SearchReplace
import com.nexcode.app.ui.screens.editor.ScreenS13Resources
import com.nexcode.app.ui.screens.editor.ScreenS14GradleDependencies
import com.nexcode.app.ui.screens.chat.ChatViewModel
import com.nexcode.app.ui.screens.chat.ScreenS15AiChat
import com.nexcode.app.ui.screens.chat.ScreenS16History
import com.nexcode.app.ui.screens.chat.ScreenS17ModificationReview
import com.nexcode.app.ui.screens.designer.DesignerViewModel
import com.nexcode.app.ui.screens.designer.ScreenS18VisualDesigner
import com.nexcode.app.ui.screens.designer.ScreenS19Properties
import com.nexcode.app.ui.screens.designer.ScreenS20Preview
import com.nexcode.app.ui.screens.build.BuildViewModel
import com.nexcode.app.ui.screens.build.ScreenS21BuildConfig
import com.nexcode.app.ui.screens.build.ScreenS22Compilation
import com.nexcode.app.ui.screens.build.ScreenS23BuildLogs
import com.nexcode.app.ui.screens.build.ScreenS24ApkList
import com.nexcode.app.ui.screens.git.GitViewModel
import com.nexcode.app.ui.screens.git.ScreenS25GitRepository
import com.nexcode.app.ui.screens.git.ScreenS26CommitHistory
import com.nexcode.app.ui.screens.git.ScreenS27ConflictResolution
import com.nexcode.app.ui.screens.settings.ScreenS28Settings
import com.nexcode.app.ui.screens.settings.ScreenS29AppearanceEditor
import com.nexcode.app.ui.screens.settings.ScreenS30BackupAbout
import com.nexcode.app.ui.screens.settings.SettingsViewModel
import com.nexcode.app.ui.theme.NexcodeTheme

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun NexcodeNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val onboardingViewModel = remember { OnboardingViewModel(context.applicationContext) }
    val projectsViewModel = remember { ProjectsViewModel(context.applicationContext) }
    val editorViewModel = remember { EditorViewModel(context.applicationContext) }
    val chatViewModel = remember { ChatViewModel(context.applicationContext) }
    val designerViewModel = remember { DesignerViewModel(context.applicationContext) }
    val buildViewModel = remember { BuildViewModel(context.applicationContext) }
    val gitViewModel = remember { GitViewModel(context.applicationContext) }
    val settingsViewModel = remember { SettingsViewModel(context.applicationContext) }

    // --- Écran initial selon l'onboarding ---
    // Le flag « onboarding_done » est persisté dans DataStore (OnboardingViewModel
    // l'écrit à la fin de l'onboarding) mais n'était jamais LU au démarrage : le
    // start destination restait codé en dur sur la route S01 et l'onboarding
    // réapparaissait à chaque lancement. On lit le flag ici pour décider de
    // l'écran initial (S05 Projets si l'onboarding est déjà complété).
    // Tant que DataStore n'a pas répondu (quelques millisecondes), on ne compose
    // rien : cela évite que l'écran S01 ne « clignote » avant la redirection.
    val preferences = remember { NexcodePreferences(context.applicationContext) }
    val onboardingDone by preferences.onboardingDoneFlow.collectAsStateWithLifecycle(
        initialValue = null
    )
    if (onboardingDone == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NexcodeTheme.colors.bg)
        )
        return
    }

    // Décision figée à la première composition du NavHost : si le flag passe à
    // « true » en cours de session (fin de l'onboarding), le start destination ne
    // doit pas changer, sinon le graphe serait reconstruit et la pile réinitialisée.
    val resolvedStartDestination = remember {
        if (onboardingDone == true) NexcodeScreen.S05.route else NexcodeScreen.S01.route
    }

    // Liaison réelle du projet sélectionné à tous les ViewModels qui en dépendent :
    // chat (historique persistant), éditeur (fichiers réels), build (GitHub Actions)
    // et git (dépôt local réel).
    val projectsUiState by projectsViewModel.uiState.collectAsState()
    val boundProject = projectsUiState.selectedProject
    LaunchedEffect(boundProject?.id, boundProject?.localPath) {
        chatViewModel.bindProject(boundProject)
        editorViewModel.bindProject(boundProject)
        buildViewModel.bindProject(boundProject)
        if (boundProject != null && boundProject.localPath.isNotBlank()) {
            gitViewModel.bindProjectAndRefreshSync(
                projectName = boundProject.name,
                fullName = boundProject.gitRepo,
                localPath = boundProject.localPath
            )
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentScreen = NexcodeScreen.fromRoute(currentRoute)

    val currentTab = currentScreen.tab ?: NexcodeNavTab.PROJECTS

    val hasCustomScreenTopBar = currentRoute in listOf(
        NexcodeScreen.S01.route,
        NexcodeScreen.S02.route,
        NexcodeScreen.S03.route,
        NexcodeScreen.S04.route,
        NexcodeScreen.S05.route,
        NexcodeScreen.S06.route,
        NexcodeScreen.S07.route,
        NexcodeScreen.S08.route,
        NexcodeScreen.S09.route,
        NexcodeScreen.S10.route,
        NexcodeScreen.S11.route,
        NexcodeScreen.S12.route,
        NexcodeScreen.S13.route,
        NexcodeScreen.S14.route,
        NexcodeScreen.S15.route,
        NexcodeScreen.S16.route,
        NexcodeScreen.S17.route,
        NexcodeScreen.S18.route,
        NexcodeScreen.S19.route,
        NexcodeScreen.S20.route,
        NexcodeScreen.S21.route,
        NexcodeScreen.S22.route,
        NexcodeScreen.S23.route,
        NexcodeScreen.S24.route,
        NexcodeScreen.S25.route,
        NexcodeScreen.S26.route,
        NexcodeScreen.S27.route,
        NexcodeScreen.S28.route,
        NexcodeScreen.S29.route,
        NexcodeScreen.S30.route
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            // La topBar globale n'est affichée que pour les écrans coquilles (s10 à s30)
            if (currentScreen.hasBottomBar && !hasCustomScreenTopBar) {
                NexcodeTopBar(
                    title = currentScreen.title,
                    subtitle = currentScreen.tab?.title,
                    navigationIcon = if (currentRoute != NexcodeScreen.S05.route) {
                        painterResource(id = R.drawable.ic_back)
                    } else null,
                    onNavigationClick = {
                        handleBackNavigation(navController, currentScreen)
                    }
                )
            }
        },
        bottomBar = {
            if (currentScreen.hasBottomBar) {
                NexcodeBottomBar(
                    selectedTab = currentTab,
                    onTabSelected = { selectedTab ->
                        val targetRoute = NexcodeScreen.getRootRouteForTab(selectedTab)
                        if (currentRoute != targetRoute) {
                            navController.navigate(targetRoute) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = resolvedStartDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(NexcodeTheme.colors.bg)
        ) {
            // Écran 1 : Lancement
            composable(route = NexcodeScreen.S01.route) {
                ScreenS01Launch(
                    onNavigateNext = {
                        navController.navigate(NexcodeScreen.S02.route)
                    }
                )
            }

            // Écran 2 : Présentation
            composable(route = NexcodeScreen.S02.route) {
                BackHandler {
                    navController.popBackStack()
                }
                ScreenS02Presentation(
                    onNavigateNext = {
                        navController.navigate(NexcodeScreen.S03.route)
                    }
                )
            }

            // Écran 3 : Connexion GitHub
            composable(route = NexcodeScreen.S03.route) {
                BackHandler {
                    navController.popBackStack()
                }
                ScreenS03GitHub(
                    viewModel = onboardingViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateNext = {
                        navController.navigate(NexcodeScreen.S04.route)
                    }
                )
            }

            // Écran 4 : Configuration de l'IA
            composable(route = NexcodeScreen.S04.route) {
                BackHandler {
                    navController.popBackStack()
                }
                ScreenS04AiConfig(
                    viewModel = onboardingViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onFinish = {
                        navController.navigate(NexcodeScreen.S05.route) {
                            popUpTo(NexcodeScreen.S01.route) { inclusive = true }
                        }
                    }
                )
            }

            // Écran 5 : Projets
            composable(route = NexcodeScreen.S05.route) {
                ScreenS05Projects(
                    viewModel = projectsViewModel,
                    onNavigateNewProject = {
                        navController.navigate(NexcodeScreen.S06.route)
                    },
                    onNavigateImportProject = {
                        navController.navigate(NexcodeScreen.S09.route)
                    },
                    onNavigateProjectDetails = { projectId ->
                        projectsViewModel.selectProjectById(projectId)
                        navController.navigate(NexcodeScreen.S08.route)
                    }
                )
            }

            // Écran 6 : Nouveau projet (Choix du modèle)
            composable(route = NexcodeScreen.S06.route) {
                BackHandler {
                    navController.popBackStack()
                }
                ScreenS06NewProject(
                    viewModel = projectsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateNext = {
                        navController.navigate(NexcodeScreen.S07.route)
                    }
                )
            }

            // Écran 7 : Assistant de création
            composable(route = NexcodeScreen.S07.route) {
                BackHandler {
                    navController.popBackStack()
                }
                ScreenS07CreateWizard(
                    viewModel = projectsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateNext = {
                        navController.navigate(NexcodeScreen.S08.route)
                    }
                )
            }

            // Écran 8 : Détails du projet
            composable(route = NexcodeScreen.S08.route) {
                BackHandler {
                    navController.navigate(NexcodeScreen.S05.route) {
                        popUpTo(NexcodeScreen.S05.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
                ScreenS08ProjectDetails(
                    viewModel = projectsViewModel,
                    onNavigateBack = {
                        navController.navigate(NexcodeScreen.S05.route) {
                            popUpTo(NexcodeScreen.S05.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onNavigateEditor = {
                        navController.navigate(NexcodeScreen.S10.route)
                    },
                    onNavigateBuild = {
                        navController.navigate(NexcodeScreen.S21.route)
                    },
                    onNavigateAiChat = {
                        navController.navigate(NexcodeScreen.S15.route)
                    },
                    onNavigateDesigner = {
                        navController.navigate(NexcodeScreen.S18.route)
                    },
                    onNavigateGitBranch = {
                        navController.navigate(NexcodeScreen.S25.route)
                    },
                    onNavigateBuildReport = {
                        navController.navigate(NexcodeScreen.S23.route)
                    },
                    onNavigateBuildLogs = {
                        navController.navigate(NexcodeScreen.S24.route)
                    }
                )
            }

            // Écran 9 : Importer un projet
            composable(route = NexcodeScreen.S09.route) {
                BackHandler {
                    navController.popBackStack()
                }
                ScreenS09ImportProject(
                    viewModel = projectsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Écran 10 : Explorateur de fichiers
            composable(route = NexcodeScreen.S10.route) {
                BackHandler {
                    handleBackNavigation(navController, NexcodeScreen.S10)
                }
                ScreenS10FileExplorer(
                    viewModel = editorViewModel,
                    onNavigateBack = { handleBackNavigation(navController, NexcodeScreen.S10) },
                    onNavigateSearch = { navController.navigate(NexcodeScreen.S12.route) },
                    onNavigateEditor = { navController.navigate(NexcodeScreen.S11.route) },
                    onNavigateResources = { navController.navigate(NexcodeScreen.S13.route) },
                    onNavigateDependencies = { navController.navigate(NexcodeScreen.S14.route) }
                )
            }

            // Écran 11 : Éditeur de code
            composable(route = NexcodeScreen.S11.route) {
                BackHandler {
                    navController.navigate(NexcodeScreen.S10.route) {
                        popUpTo(NexcodeScreen.S10.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
                ScreenS11CodeEditor(
                    viewModel = editorViewModel,
                    onNavigateBack = {
                        navController.navigate(NexcodeScreen.S10.route) {
                            popUpTo(NexcodeScreen.S10.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onNavigateSearch = { navController.navigate(NexcodeScreen.S12.route) },
                    onNavigateSettings = { navController.navigate(NexcodeScreen.S28.route) },
                    onNavigateDependencies = { navController.navigate(NexcodeScreen.S14.route) },
                    onNavigateBuild = { navController.navigate(NexcodeScreen.S21.route) },
                    onNavigateAiChat = { navController.navigate(NexcodeScreen.S15.route) }
                )
            }

            // Écran 12 : Rechercher et remplacer
            composable(route = NexcodeScreen.S12.route) {
                BackHandler {
                    navController.popBackStack()
                }
                ScreenS12SearchReplace(
                    viewModel = editorViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Écran 13 : Ressources
            composable(route = NexcodeScreen.S13.route) {
                BackHandler {
                    navController.popBackStack()
                }
                ScreenS13Resources(
                    viewModel = editorViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateCorrectInSearch = { navController.navigate(NexcodeScreen.S12.route) }
                )
            }

            // Écran 14 : Dépendances Gradle
            composable(route = NexcodeScreen.S14.route) {
                BackHandler {
                    navController.popBackStack()
                }
                ScreenS14GradleDependencies(
                    viewModel = editorViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Écran 15 : Chat IA
            composable(route = NexcodeScreen.S15.route) {
                BackHandler {
                    handleBackNavigation(navController, NexcodeScreen.S15)
                }
                ScreenS15AiChat(
                    viewModel = chatViewModel,
                    onNavigateBack = { handleBackNavigation(navController, NexcodeScreen.S15) },
                    onNavigateHistory = { navController.navigate(NexcodeScreen.S16.route) },
                    onNavigateReview = { navController.navigate(NexcodeScreen.S17.route) }
                )
            }

            // Écran 16 : Historique des conversations
            composable(route = NexcodeScreen.S16.route) {
                BackHandler {
                    navController.popBackStack()
                }
                ScreenS16History(
                    viewModel = chatViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateChat = { navController.popBackStack() }
                )
            }

            // Écran 17 : Revue des modifications
            composable(route = NexcodeScreen.S17.route) {
                BackHandler {
                    navController.popBackStack()
                }
                ScreenS17ModificationReview(
                    viewModel = chatViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Écran 18 : Concepteur visuel
            composable(route = NexcodeScreen.S18.route) {
                BackHandler {
                    handleBackNavigation(navController, NexcodeScreen.S18)
                }
                ScreenS18VisualDesigner(
                    viewModel = designerViewModel,
                    onNavigateBack = { handleBackNavigation(navController, NexcodeScreen.S18) },
                    onNavigateProperties = { navController.navigate(NexcodeScreen.S19.route) },
                    onNavigatePreview = { navController.navigate(NexcodeScreen.S20.route) }
                )
            }

            // Écran 19 : Propriétés
            composable(route = NexcodeScreen.S19.route) {
                BackHandler {
                    navController.popBackStack()
                }
                ScreenS19Properties(
                    viewModel = designerViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigatePreview = { navController.navigate(NexcodeScreen.S20.route) }
                )
            }

            // Écran 20 : Aperçu de l'application
            composable(route = NexcodeScreen.S20.route) {
                BackHandler {
                    navController.popBackStack()
                }
                ScreenS20Preview(
                    viewModel = designerViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateDesigner = {
                        navController.navigate(NexcodeScreen.S18.route) {
                            popUpTo(NexcodeScreen.S18.route) { inclusive = true }
                        }
                    }
                )
            }

            // Écran 21 : Configuration du build
            composable(route = NexcodeScreen.S21.route) {
                BackHandler {
                    handleBackNavigation(navController, NexcodeScreen.S21)
                }
                ScreenS21BuildConfig(
                    viewModel = buildViewModel,
                    onNavigateBack = { handleBackNavigation(navController, NexcodeScreen.S21) },
                    onNavigateCompilation = {
                        navController.navigate(NexcodeScreen.S22.route)
                    }
                )
            }

            // Écran 22 : Compilation
            composable(route = NexcodeScreen.S22.route) {
                BackHandler {
                    navController.popBackStack()
                }
                ScreenS22Compilation(
                    viewModel = buildViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateLogs = {
                        navController.navigate(NexcodeScreen.S23.route)
                    },
                    onNavigateApk = {
                        navController.navigate(NexcodeScreen.S24.route)
                    },
                    onNavigateAiChat = {
                        navController.navigate(NexcodeScreen.S15.route)
                    }
                )
            }

            // Écran 23 : Logs et erreurs
            composable(route = NexcodeScreen.S23.route) {
                BackHandler {
                    navController.popBackStack()
                }
                ScreenS23BuildLogs(
                    viewModel = buildViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateAiChat = {
                        navController.navigate(NexcodeScreen.S15.route)
                    },
                    onNavigateBuildConfig = {
                        navController.navigate(NexcodeScreen.S21.route) {
                            popUpTo(NexcodeScreen.S21.route) { inclusive = true }
                        }
                    }
                )
            }

            // Écran 24 : APK générés
            composable(route = NexcodeScreen.S24.route) {
                BackHandler {
                    navController.popBackStack()
                }
                ScreenS24ApkList(
                    viewModel = buildViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateGit = {
                        navController.navigate(NexcodeScreen.S25.route)
                    }
                )
            }

            // Écran 25 : Dépôt GitHub
            composable(route = NexcodeScreen.S25.route) {
                BackHandler {
                    handleBackNavigation(navController, NexcodeScreen.S25)
                }
                ScreenS25GitRepository(
                    viewModel = gitViewModel,
                    onNavigateBack = { handleBackNavigation(navController, NexcodeScreen.S25) },
                    onNavigateHistory = {
                        navController.navigate(NexcodeScreen.S26.route)
                    },
                    onNavigateConflict = {
                        navController.navigate(NexcodeScreen.S27.route)
                    }
                )
            }

            // Écran 26 : Historique des commits
            composable(route = NexcodeScreen.S26.route) {
                BackHandler {
                    navController.popBackStack()
                }
                ScreenS26CommitHistory(
                    viewModel = gitViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateConflict = {
                        navController.navigate(NexcodeScreen.S27.route)
                    }
                )
            }

            // Écran 27 : Résolution de conflits
            composable(route = NexcodeScreen.S27.route) {
                BackHandler {
                    navController.popBackStack()
                }
                ScreenS27ConflictResolution(
                    viewModel = gitViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Écran 28 : Réglages
            composable(route = NexcodeScreen.S28.route) {
                BackHandler {
                    handleBackNavigation(navController, NexcodeScreen.S28)
                }
                ScreenS28Settings(
                    viewModel = settingsViewModel,
                    onNavigateBack = { handleBackNavigation(navController, NexcodeScreen.S28) },
                    onNavigateAppearance = {
                        navController.navigate(NexcodeScreen.S29.route)
                    },
                    onNavigateAiConfig = {
                        navController.navigate(NexcodeScreen.S04.route)
                    },
                    onNavigateBackupAbout = {
                        navController.navigate(NexcodeScreen.S30.route)
                    },
                    onNavigateGitHub = {
                        val target = if (settingsViewModel.uiState.value.isSignedOut) {
                            NexcodeScreen.S03.route
                        } else {
                            NexcodeScreen.S25.route
                        }
                        navController.navigate(target)
                    }
                )
            }

            // Écran 29 : Apparence et éditeur
            composable(route = NexcodeScreen.S29.route) {
                BackHandler {
                    navController.popBackStack()
                }
                ScreenS29AppearanceEditor(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Écran 30 : Sauvegarde et à propos
            composable(route = NexcodeScreen.S30.route) {
                BackHandler {
                    navController.popBackStack()
                }
                ScreenS30BackupAbout(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Écrans coquilles restants (tous les 30 écrans sont maintenant implémentés)
            val implementedRoutes = listOf(
                "s01", "s02", "s03", "s04",
                "s05", "s06", "s07", "s08", "s09",
                "s10", "s11", "s12", "s13", "s14",
                "s15", "s16", "s17",
                "s18", "s19", "s20",
                "s21", "s22", "s23", "s24",
                "s25", "s26", "s27",
                "s28", "s29", "s30"
            )
            NexcodeScreen.ALL_SCREENS.filter { it.route !in implementedRoutes }.forEach { screen ->
                composable(route = screen.route) {
                    BackHandler(enabled = screen.route != NexcodeScreen.S05.route) {
                        handleBackNavigation(navController, screen)
                    }

                    EmptyScreenShell(screen = screen)
                }
            }
        }
    }
}

// Fonction de retour : retourne vers la racine de l'onglet ou vers S05
private fun handleBackNavigation(navController: NavHostController, currentScreen: NexcodeScreen) {
    val tab = currentScreen.tab
    if (tab != null) {
        val rootRoute = NexcodeScreen.getRootRouteForTab(tab)
        if (currentScreen.route != rootRoute) {
            navController.popBackStack()
        } else if (rootRoute != NexcodeScreen.S05.route) {
            navController.navigate(NexcodeScreen.S05.route) {
                popUpTo(NexcodeScreen.S05.route) { inclusive = false }
                launchSingleTop = true
            }
        }
    } else {
        navController.popBackStack()
    }
}

// Coquille vide portant le titre de l'ecran
@Composable
fun EmptyScreenShell(
    screen: NexcodeScreen,
    modifier: Modifier = Modifier
) {
    val colors = NexcodeTheme.colors

    Box(
        modifier = modifier
            .testTag("screen_${screen.route}")
            .fillMaxSize()
            .background(colors.bg)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        NexcodeCard(
            modifier = Modifier.testTag("card_${screen.route}")
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${screen.route.uppercase()} - ${screen.title}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.txt
                )
                if (screen.tab != null) {
                    Text(
                        text = "Onglet : ${screen.tab.title}",
                        fontSize = 13.sp,
                        color = colors.accent
                    )
                } else {
                    Text(
                        text = "Écran hors onglets",
                        fontSize = 13.sp,
                        color = colors.txt2
                    )
                }
                NexcodeBanner(
                    text = "Coquille vide prête pour intégration détaillée.",
                    icon = painterResource(id = R.drawable.ic_info),
                    variant = NexcodeBannerVariant.SUCCESS
                )
            }
        }
    }
}
