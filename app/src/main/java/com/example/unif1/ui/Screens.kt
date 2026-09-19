package com.example.unif1.ui

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowWidthSizeClass
import com.example.unif1.data.health.HealthAvailability
import com.example.unif1.data.training.*
import com.example.unif1.ui.theme.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TrainingViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToPlan: () -> Unit,
    onNavigateToReadiness: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onAskUniF1: () -> Unit
) {
    val context by viewModel.trainingContext.collectAsStateWithLifecycle()
    val history by viewModel.trainingHistory.collectAsStateWithLifecycle()
    val availability by viewModel.availability.collectAsStateWithLifecycle()
    val permissionsGranted by viewModel.permissionsGranted.collectAsStateWithLifecycle()

    val permissionsLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) {
        viewModel.checkHealthConnectStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "UniF1 Kit",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    Surface(
                        modifier = Modifier.padding(end = 12.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = borderStroke()
                    ) {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Rounded.Tune, contentDescription = "Settings", modifier = Modifier.size(20.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                availability != HealthAvailability.Installed -> HealthConnectUnavailableState(availability)
                !permissionsGranted -> PermissionRequiredState {
                    permissionsLauncher.launch(viewModel.permissions)
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        item {
                            ReadinessHero(
                                readiness = context.readiness,
                                onClick = onNavigateToReadiness
                            )
                        }

                        item {
                            TodayPlanCard(
                                session = context.currentPlan?.sessions?.find { it.date == LocalDate.now() },
                                onClick = onNavigateToPlan
                            )
                        }

                        item {
                            UniF1SectionHeader(title = "Component Data")
                            WeeklyOverview(stats = context.thisWeek)
                        }

                        if (context.goalProgress.isNotEmpty()) {
                            item {
                                UniF1SectionHeader(title = "Sequence Targets", action = "Review", onActionClick = onNavigateToGoals)
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    context.goalProgress.take(2).forEach { progress ->
                                        GoalProgressCard(progress, onClick = onNavigateToGoals)
                                    }
                                }
                            }
                        }

                        if (history.isNotEmpty()) {
                            item {
                                UniF1SectionHeader(title = "Kit History")
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = borderStroke()
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                                        history.take(3).forEachIndexed { index, activity ->
                                            TrainingActivityRow(
                                                activity = activity,
                                                onClick = { onNavigateToDetail(activity.id) }
                                            )
                                            if (index < 2 && index < history.size - 1) {
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        item {
                            CoachEntryCard(onClick = onAskUniF1)
                        }
                        
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun TrainingScreen(
    viewModel: TrainingViewModel,
    onActivityClick: (String) -> Unit
) {
    val state by viewModel.trendsState.collectAsStateWithLifecycle()
    val context by viewModel.trainingContext.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                Text(text = "SEQUENCE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                Text(text = "High-Res Metrics", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            item {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    border = borderStroke()
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(7, 30, 90).forEach { days ->
                            val selected = state.selectedDays == days
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.fetchTrends(days) },
                                shape = CircleShape,
                                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            ) {
                                Text(
                                    text = "${days}D",
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("TOTAL MAGNITUDE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f km", state.stats.totalDistanceMeters / 1000.0),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatDurationCompact(state.stats.totalDuration).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            item {
                UniF1SectionHeader(title = "Intensity Calibration")
                val ls = context.loadSummary
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = borderStroke()
                ) {
                    Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Current Baseline", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.5.sp)
                        Text(
                            text = String.format(Locale.US, "%.1f", ls.currentWeekLoad),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val (icon, color) = when {
                                ls.loadTrend == LoadTrend.INCREASING -> Icons.AutoMirrored.Rounded.TrendingUp to MaterialTheme.unif1Colors.ready
                                ls.loadTrend == LoadTrend.DECREASING -> Icons.AutoMirrored.Rounded.TrendingDown to MaterialTheme.unif1Colors.recovery
                                else -> Icons.Rounded.HorizontalRule to MaterialTheme.colorScheme.outline
                            }
                            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = color)
                            Text(
                                text = "${ls.loadTrend.name} (${ls.percentageChange?.let { String.format(Locale.US, "%.1f%%", it) } ?: "0%"})",
                                style = MaterialTheme.typography.labelMedium,
                                color = color
                            )
                        }
                    }
                }
            }

            item {
                UniF1SectionHeader(title = "Category Breakdown")
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = borderStroke()
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        context.trends.sportDistribution.forEach { (type, percentage) ->
                            SportDistributionRow(type, percentage)
                        }
                    }
                }
            }

            item {
                UniF1SectionHeader(title = "Kit History")
                Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                    state.activities.forEachIndexed { index, activity ->
                        TrainingActivityRow(activity, onClick = { onActivityClick(activity.id) })
                        if (index < state.activities.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun SportDistributionRow(type: TrainingType, percentage: Double) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        val (icon, color) = when(type) {
            TrainingType.RUN -> Icons.AutoMirrored.Rounded.DirectionsRun to MaterialTheme.unif1Colors.run
            TrainingType.BIKE -> Icons.Rounded.DirectionsBike to MaterialTheme.unif1Colors.bike
            TrainingType.SWIM -> Icons.Rounded.Pool to MaterialTheme.unif1Colors.swim
        }
        Surface(shape = CircleShape, color = color.copy(alpha = 0.25f), modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = type.name, style = MaterialTheme.typography.labelLarge)
                Text(text = "${(percentage * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).clip(CircleShape)) {
                Box(modifier = Modifier.fillMaxWidth(percentage.toFloat()).fillMaxHeight().background(color, CircleShape))
            }
        }
    }
}

@Composable
fun CoachScreen(viewModel: TrainingViewModel) {
    val state by viewModel.aiState.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                Text(text = "INTELLIGENCE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, letterSpacing = 2.sp)
                Text(text = "Advisor Sequence", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
                state = listState,
                contentPadding = PaddingValues(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                if (state.messages.isEmpty()) {
                    item {
                        EmptyState(
                            title = "ADVISOR STANDBY",
                            description = "Inquire regarding sequence calibration, performance indices, or volume alignment.",
                            icon = Icons.Rounded.AutoAwesome
                        )
                    }
                }

                items(state.messages) { message ->
                    AIMessageFlow(message = message.text, isFromAI = message.isFromAI)
                }

                if (state.isLoading) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(20.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.tertiary)
                            Text("Engine processing...", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                state.error?.let { error ->
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Text(
                                        text = "COACH OFFLINE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                TextButton(
                                    onClick = { viewModel.analyzeTraining() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Retry Connection")
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                border = borderStroke()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Inquire with coach...") },
                        shape = CircleShape,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                    IconButton(
                        onClick = {
                            if (input.isNotBlank()) {
                                viewModel.sendMessage(input)
                                input = ""
                            }
                        },
                        modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.onSurface, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.surface)
                    }
                }
            }
        }
    }
}

@Composable
fun AIMessageFlow(message: String, isFromAI: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromAI) Alignment.Start else Alignment.End
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isFromAI) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface,
            contentColor = if (isFromAI) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
            border = if (isFromAI) borderStroke() else null,
            shadowElevation = if (isFromAI) 0.dp else 4.dp
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 26.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingDetailScreen(
    activityId: String,
    viewModel: TrainingViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.detailState.collectAsStateWithLifecycle()
    val activity = state.activity

    LaunchedEffect(activityId) {
        viewModel.fetchActivityDetails(activityId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(activity?.type?.name ?: "Performance", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, border = borderStroke()) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        if (state.isLoading) LoadingState()
        else if (activity != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(44.dp)
            ) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = DateTimeFormatter.ofPattern("EEEE, MMM d · h:mm a").withZone(ZoneId.systemDefault()).format(activity.startTime).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = activity.distanceMeters?.let { String.format(Locale.US, "%.2f km", it / 1000.0) } ?: "Session",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatDurationCompact(activity.duration).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 2.sp
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            DetailStatCard(label = "Heart Rate", value = "${activity.averageHeartRate ?: "--"} bpm", icon = Icons.Rounded.Favorite, modifier = Modifier.weight(1f))
                            DetailStatCard(label = "Intensity", value = activity.intensity.name, icon = Icons.Rounded.Bolt, modifier = Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            DetailStatCard(label = "Energy", value = "${activity.calories?.toInt() ?: "--"} kcal", icon = Icons.Rounded.LocalFireDepartment, modifier = Modifier.weight(1f))
                            DetailStatCard(label = "Load Index", value = String.format(Locale.US, "%.1f", activity.calculatedLoad), icon = Icons.Rounded.Insights, modifier = Modifier.weight(1f))
                        }
                    }
                }

                item {
                    Surface(
                        onClick = { viewModel.sendMessage("Analyze my ${activity.type.name} session from ${activity.startTime}") },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                                Text("Inquire with Advisor", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(themeViewModel: ThemeViewModel) {
    val selectedTheme by themeViewModel.selectedTheme.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                Text(text = "CONFIGURATION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, letterSpacing = 2.sp)
                Text(text = "Engine Settings", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            item { 
                Text(
                    "APPEARANCE", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp), 
                    letterSpacing = 2.sp
                ) 
            }
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = borderStroke()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        AppTheme.entries.forEach { theme ->
                            ThemeOptionRow(
                                theme = theme,
                                selected = selectedTheme == theme,
                                onClick = { themeViewModel.setTheme(theme) }
                            )
                        }
                    }
                }
            }

            item { 
                Text(
                    "GENERAL", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp), 
                    letterSpacing = 2.sp
                ) 
            }
            item { SettingsListItem(icon = Icons.Rounded.Notifications, title = "Notifications") }
            item { SettingsListItem(icon = Icons.Rounded.Security, title = "Privacy & Security") }
            
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) }
            
            item { Text("ENGINE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp), letterSpacing = 2.sp) }
            item { SettingsListItem(icon = Icons.Rounded.Info, title = "About UniF1", subtitle = "Kit Version 1.0.0") }
        }
    }
}

@Composable
fun ThemeOptionRow(
    theme: AppTheme,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = null // handled by Row
        )
        Text(
            text = when (theme) {
                AppTheme.SYSTEM -> "System Default"
                AppTheme.LIGHT -> "Light"
                AppTheme.DARK -> "Dark"
            },
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SettingsListItem(icon: ImageVector, title: String, subtitle: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable { },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(44.dp),
            border = borderStroke()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(20.dp))
    }
}
