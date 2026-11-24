package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.AppContainer
import com.example.ui.AppViewModelFactory
import com.example.ui.ChecklistViewModel
import com.example.ui.ReportsViewModel
import com.example.ui.SetupViewModel
import com.example.ui.screens.ChecklistsScreen
import com.example.ui.screens.ReportDetailScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SetupScreen
import com.example.ui.theme.CustodianCheckTheme
import kotlinx.coroutines.launch

private const val ROUTE_CHECKLISTS = "checklists"
private const val ROUTE_REPORTS = "reports"
private const val ROUTE_REPORT_DETAIL = "report_detail/{runId}"
private const val ROUTE_SETUP = "setup"

private data class TopDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topDestinations = listOf(
    TopDestination(
        route = ROUTE_CHECKLISTS,
        labelRes = R.string.nav_checklists,
        selectedIcon = Icons.Filled.Checklist,
        unselectedIcon = Icons.Outlined.Checklist,
        testTag = "nav_tab_checklists"
    ),
    TopDestination(
        route = ROUTE_REPORTS,
        labelRes = R.string.nav_reports,
        selectedIcon = Icons.Filled.Description,
        unselectedIcon = Icons.Outlined.Description,
        testTag = "nav_tab_reports"
    ),
    TopDestination(
        route = ROUTE_SETUP,
        labelRes = R.string.nav_setup,
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune,
        testTag = "nav_tab_setup"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = AppContainer(applicationContext)
        val factory = AppViewModelFactory(appContainer)

        setContent {
            CustodianCheckTheme {
                CustodianCheckApp(factory = factory)
            }
        }
    }
}

@Composable
fun CustodianCheckApp(factory: AppViewModelFactory) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val checklistViewModel: ChecklistViewModel = viewModel(factory = factory)
    val reportsViewModel: ReportsViewModel = viewModel(factory = factory)
    val setupViewModel: SetupViewModel = viewModel(factory = factory)

    val checklistState by checklistViewModel.uiState.collectAsStateWithLifecycle()
    val reportsState by reportsViewModel.uiState.collectAsStateWithLifecycle()
    val setupState by setupViewModel.uiState.collectAsStateWithLifecycle()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isDetailRoute = currentDestination?.route == ROUTE_REPORT_DETAIL

    val savedMsg = stringResource(R.string.snackbar_saved_report)
    val viewPdfAction = stringResource(R.string.snackbar_action_pdf)

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail && !isDetailRoute) {
                    NavigationBar {
                        topDestinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && !isDetailRoute) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        Spacer(modifier = Modifier.weight(0.15f))
                        topDestinations.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_CHECKLISTS,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(ROUTE_CHECKLISTS) {
                        ChecklistsScreen(
                            uiState = checklistState,
                            onSelectFrequency = checklistViewModel::selectFrequency,
                            onToggleTick = checklistViewModel::toggleTick,
                            onSaveItemDetails = checklistViewModel::saveItemDetails,
                            onCreateCameraTarget = checklistViewModel::createCameraCaptureTarget,
                            onImportPickedPhoto = checklistViewModel::importPickedPhoto,
                            onImportCapturedPhoto = checklistViewModel::importCapturedPhoto,
                            onDeletePhotoFile = checklistViewModel::deletePhotoFile,
                            onFinalizeRun = checklistViewModel::finalizeCurrentRun,
                            onReportSaved = { savedRunId ->
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = savedMsg,
                                        actionLabel = viewPdfAction,
                                        duration = SnackbarDuration.Long
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        navController.navigate("report_detail/$savedRunId")
                                    }
                                }
                            },
                            onNavigateToSetup = { navigateTopLevel(ROUTE_SETUP) }
                        )
                    }

                    composable(ROUTE_REPORTS) {
                        ReportsScreen(
                            uiState = reportsState,
                            onSelectFilter = reportsViewModel::setFilter,
                            onOpenReport = { runId ->
                                navController.navigate("report_detail/$runId")
                            }
                        )
                    }

                    composable(
                        route = ROUTE_REPORT_DETAIL,
                        arguments = listOf(navArgument("runId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val runId = backStackEntry.arguments?.getLong("runId") ?: 0L
                        val detailRun by remember(runId) {
                            reportsViewModel.observeRunDetail(runId)
                        }.collectAsStateWithLifecycle(initialValue = null)

                        ReportDetailScreen(
                            runWithItems = detailRun,
                            onBack = { navController.popBackStack() },
                            onDelete = { id ->
                                reportsViewModel.deleteRun(id) {
                                    navController.popBackStack()
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Report deleted")
                                    }
                                }
                            },
                            onShowMessage = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        )
                    }

                    composable(ROUTE_SETUP) {
                        SetupScreen(
                            uiState = setupState,
                            onSelectFrequency = setupViewModel::selectFrequency,
                            onSaveProfile = setupViewModel::saveProfile,
                            onAddTask = setupViewModel::addTask,
                            onUpdateTask = setupViewModel::updateTask,
                            onMoveTask = setupViewModel::moveTask,
                            onDeleteTask = setupViewModel::deleteTask,
                            onRestoreDefaults = setupViewModel::restoreDefaults,
                            onShowMessage = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        )
                    }
                }
            }
        }
    }
}
