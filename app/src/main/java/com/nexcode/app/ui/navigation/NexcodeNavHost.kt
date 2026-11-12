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
import com.nexcode.app.ui.theme.NexcodeTheme

@Composable
fun NexcodeNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = NexcodeScreen.S01.route
) {
    val context = LocalContext.current
    val onboardingViewModel = remember { OnboardingViewModel(context.applicationContext) }
    val projectsViewModel = remember { ProjectsViewModel(context.applicationContext) }

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
        NexcodeScreen.S09.route
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
            startDestination = startDestination,
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

            // Écrans 10 à 30 (Coquilles restantes de l'Éditeur, Chat IA et Build)
            NexcodeScreen.ALL_SCREENS.filter { it.route !in listOf("s01", "s02", "s03", "s04", "s05", "s06", "s07", "s08", "s09") }.forEach { screen ->
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
