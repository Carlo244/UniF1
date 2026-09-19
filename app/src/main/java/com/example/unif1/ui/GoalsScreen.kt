package com.example.unif1.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.unif1.data.training.*
import com.example.unif1.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(viewModel: TrainingViewModel) {
    val context by viewModel.trainingContext.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "TARGETS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                        Text("Objectives", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(2.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "New Objective")
            }
        }
    ) { innerPadding ->
        if (context.goalProgress.isEmpty()) {
            EmptyState(
                title = "No objectives defined",
                description = "Establish weekly training targets to synchronize with the UniF1 advisor.",
                icon = Icons.Rounded.TrackChanges
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                val activeGoals = context.goalProgress.filter { it.goal.active }
                val inactiveGoals = context.goalProgress.filter { !it.goal.active }

                if (activeGoals.isNotEmpty()) {
                    item { UniF1SectionHeader(title = "Active Targets") }
                    items(activeGoals) { progress ->
                        ObjectiveItem(
                            progress = progress,
                            onDelete = { viewModel.deleteGoal(progress.goal.id) },
                            onToggle = { viewModel.toggleGoal(progress.goal) }
                        )
                    }
                }

                if (inactiveGoals.isNotEmpty()) {
                    item { UniF1SectionHeader(title = "Paused Sequence") }
                    items(inactiveGoals) { progress ->
                        ObjectiveItem(
                            progress = progress,
                            onDelete = { viewModel.deleteGoal(progress.goal.id) },
                            onToggle = { viewModel.toggleGoal(progress.goal) }
                        )
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        if (showAddDialog) {
            AddGoalDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { metric, sport, target ->
                    viewModel.addGoal(metric, sport, target)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun ObjectiveItem(
    progress: GoalProgress,
    onDelete: () -> Unit,
    onToggle: () -> Unit
) {
    val g = progress.goal
    val (icon, color) = when(g.sport) {
        TrainingType.RUN -> Icons.AutoMirrored.Rounded.DirectionsRun to MaterialTheme.unif1Colors.run
        TrainingType.BIKE -> Icons.Rounded.DirectionsBike to MaterialTheme.unif1Colors.bike
        TrainingType.SWIM -> Icons.Rounded.Pool to MaterialTheme.unif1Colors.swim
        null -> Icons.Rounded.Explore to MaterialTheme.unif1Colors.swim
    }
    
    val statusColor = if (g.active) when(progress.status) {
        GoalStatus.COMPLETED -> MaterialTheme.unif1Colors.ready
        GoalStatus.BEHIND -> MaterialTheme.unif1Colors.recovery
        else -> MaterialTheme.colorScheme.primary
    } else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = borderStroke()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.3f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${g.sport?.name ?: "ALL"} ${g.metric.name.replace("WEEKLY_", "").lowercase().replace("_", " ")}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = progress.status.name.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                    )
                }
                Switch(
                    checked = g.active, 
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                val current = if (g.metric == GoalMetric.WEEKLY_DISTANCE) progress.currentValue / 1000.0 else progress.currentValue
                val target = if (g.metric == GoalMetric.WEEKLY_DISTANCE) progress.targetValue / 1000.0 else progress.targetValue
                val unit = when(g.metric) {
                    GoalMetric.WEEKLY_DISTANCE -> "km"
                    GoalMetric.WEEKLY_DURATION -> "h"
                    GoalMetric.WEEKLY_SESSIONS -> "sessions"
                }

                Text(
                    text = buildString {
                        append(String.format(Locale.US, "%.1f", current))
                        append(" / ")
                        append(target.toInt())
                        append(" ")
                        append(unit)
                    },
                    style = MaterialTheme.typography.displaySmall,
                    color = if (g.active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = "${(progress.progressPercentage * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    color = statusColor
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Floating progress bar with depth
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.progressPercentage.toFloat().coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(MaterialTheme.colorScheme.primary, color)
                            ),
                            shape = CircleShape
                        )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Omit Target", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun AddGoalDialog(onDismiss: () -> Unit, onAdd: (GoalMetric, TrainingType?, Double) -> Unit) {
    var metric by remember { mutableStateOf(GoalMetric.WEEKLY_DISTANCE) }
    var sport by remember { mutableStateOf<TrainingType?>(null) }
    var targetText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Objective", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Column {
                    Text("MEASUREMENT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        GoalMetric.entries.forEach { m ->
                            FilterChip(
                                selected = metric == m,
                                onClick = { metric = m },
                                label = { Text(m.name.replace("WEEKLY_", "").lowercase()) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                Column {
                    Text("CATEGORY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        FilterChip(
                            selected = sport == null,
                            onClick = { sport = null },
                            label = { Text("ALL") },
                            shape = RoundedCornerShape(12.dp)
                        )
                        TrainingType.entries.forEach { t ->
                            FilterChip(
                                selected = sport == t,
                                onClick = { sport = t },
                                label = { Text(t.name) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Target Magnitude") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        },
        confirmButton = {
            Surface(
                onClick = {
                    val target = targetText.toDoubleOrNull() ?: 0.0
                    if (target > 0) {
                        val finalTarget = if (metric == GoalMetric.WEEKLY_DISTANCE) target * 1000 else target
                        onAdd(metric, sport, finalTarget)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                    Text("Establish", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { 
                Text("Cancel", style = MaterialTheme.typography.labelMedium) 
            } 
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
