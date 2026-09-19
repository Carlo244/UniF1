package com.example.unif1.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.unif1.data.health.HealthConnectRepository
import com.example.unif1.data.training.TrainingGoalRepository
import com.example.unif1.data.training.TrainingPlanRepository
import com.example.unif1.data.training.TrainingRepository
import com.example.unif1.navigation.UniF1Route
import com.example.unif1.ui.theme.*

@Composable
fun UniF1App() {
    val context = LocalContext.current
    val themeViewModel: ThemeViewModel = viewModel()
    val appTheme by themeViewModel.selectedTheme.collectAsStateWithLifecycle()

    UniF1Theme(appTheme = appTheme) {
        val healthConnectRepository = remember { HealthConnectRepository(context) }
        val goalRepository = remember { TrainingGoalRepository(context) }
        val planRepository = remember { TrainingPlanRepository(context) }
        val repository = remember { TrainingRepository(healthConnectRepository) }
        val trainingViewModel: TrainingViewModel = viewModel {
            TrainingViewModel(repository, goalRepository, planRepository, context)
        }
        val backStack = rememberNavBackStack(UniF1Route.Home)
        
        val currentRoute = backStack.lastOrNull() as? UniF1Route ?: UniF1Route.Home

        val navItems = listOf(
            NavItem("Central", UniF1Route.Home, Icons.Rounded.Explore),
            NavItem("Metrics", UniF1Route.Trends, Icons.Rounded.Insights),
            NavItem("Sequence", UniF1Route.Plan, Icons.Rounded.Timeline),
            NavItem("Advisor", UniF1Route.AI, Icons.Rounded.AutoAwesome),
            NavItem("Targets", UniF1Route.Goals, Icons.Rounded.TrackChanges)
        )

        // Using Scaffold with a custom Floating Navigation Bar for the Kit aesthetic
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                FloatingKitNavigation(
                    navItems = navItems,
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (currentRoute != route) {
                            backStack.clear()
                            backStack.add(route)
                        }
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }
                ) { key ->
                    NavEntry(key) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (key) {
                                UniF1Route.Home -> {
                                    HomeScreen(
                                        viewModel = trainingViewModel,
                                        onNavigateToDetail = { activityId ->
                                            backStack.add(UniF1Route.TrainingDetail(activityId))
                                        },
                                        onNavigateToGoals = {
                                            backStack.clear()
                                            backStack.add(UniF1Route.Goals)
                                        },
                                        onNavigateToPlan = {
                                            backStack.clear()
                                            backStack.add(UniF1Route.Plan)
                                        },
                                        onNavigateToReadiness = {
                                            backStack.add(UniF1Route.TrainingReadiness)
                                        }, 
                                        onNavigateToSettings = {
                                            backStack.add(UniF1Route.Settings)
                                        },
                                        onAskUniF1 = {
                                            backStack.clear()
                                            backStack.add(UniF1Route.AI)
                                        }
                                    )
                                }
                                UniF1Route.Trends -> {
                                    TrainingScreen(
                                        viewModel = trainingViewModel,
                                        onActivityClick = { activityId ->
                                            backStack.add(UniF1Route.TrainingDetail(activityId))
                                        }
                                    )
                                }
                                is UniF1Route.TrainingDetail -> {
                                    TrainingDetailScreen(
                                        activityId = key.activityId,
                                        viewModel = trainingViewModel,
                                        onBack = {
                                            if (backStack.size > 1) backStack.removeAt(backStack.size - 1)
                                        }
                                    )
                                }
                                UniF1Route.AI -> {
                                    CoachScreen(viewModel = trainingViewModel)
                                }
                                UniF1Route.Goals -> {
                                    GoalsScreen(viewModel = trainingViewModel)
                                }
                                UniF1Route.Plan -> {
                                    TrainingPlanScreen(
                                        viewModel = trainingViewModel,
                                        onViewPerformance = { backStack.add(UniF1Route.PlanPerformance) }
                                    )
                                }
                                UniF1Route.PlanPerformance -> {
                                    PlanPerformanceScreen(
                                        viewModel = trainingViewModel,
                                        onBack = { backStack.removeAt(backStack.size - 1) }
                                    )
                                }
                                UniF1Route.TrainingReadiness -> {
                                    TrainingReadinessScreen(
                                        viewModel = trainingViewModel,
                                        onBack = { backStack.removeAt(backStack.size - 1) }
                                    )
                                }
                                UniF1Route.Settings -> SettingsScreen(themeViewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingKitNavigation(
    navItems: List<NavItem>,
    currentRoute: UniF1Route,
    onNavigate: (UniF1Route) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .height(64.dp)
                .shadow(12.dp, CircleShape),
            shape = CircleShape,
            color = MaterialTheme.unif1Colors.glass,
            border = borderStroke()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                navItems.forEach { item ->
                    val selected = when {
                        currentRoute == item.route -> true
                        currentRoute is UniF1Route.TrainingDetail && item.route == UniF1Route.Trends -> true
                        else -> false
                    }
                    
                    KitNavItem(
                        item = item,
                        selected = selected,
                        onClick = { onNavigate(item.route) }
                    )
                }
            }
        }
    }
}

@Composable
fun KitNavItem(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        color = Color.Transparent
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontSize = 8.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

data class NavItem(
    val label: String,
    val route: UniF1Route,
    val icon: ImageVector
)
