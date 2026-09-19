package com.example.unif1.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.unif1.data.training.*
import com.example.unif1.ui.theme.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanPerformanceScreen(viewModel: TrainingViewModel, onBack: () -> Unit) {
    val performance by viewModel.planPerformance.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "COMPLIANCE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                        Text("Performance Index", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        if (performance == null) {
            EmptyState(
                title = "No athletic history",
                description = "Initiate a training sequence to generate performance compliance data.",
                icon = Icons.Rounded.Insights
            )
        } else {
            PerformanceContent(performance!!, Modifier.padding(innerPadding))
        }
    }
}

@Composable
fun PerformanceContent(summary: PlannedVsActualSummary, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(36.dp)
    ) {
        item {
            Text(
                text = "WEEK OF ${summary.weekStartDate.format(DateTimeFormatter.ofPattern("MMM d"))}".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.5.sp
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Column(modifier = Modifier.padding(vertical = 40.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ADHERENCE INDEX", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                    Text(
                        text = "${(summary.adherencePercentage * 100).toInt()}%",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        PerformanceMetric(label = "Verified", value = summary.completedSessionCount.toString(), color = MaterialTheme.unif1Colors.ready)
                        PerformanceMetric(label = "Omitted", value = summary.missedSessionCount.toString(), color = MaterialTheme.unif1Colors.recovery)
                        PerformanceMetric(label = "Deferred", value = summary.skippedSessionCount.toString(), color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        item {
            UniF1SectionHeader(title = "Volume Alignment")
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                CompactComparison(
                    label = "Distance Accuracy",
                    actual = summary.actualDistanceMeters / 1000.0,
                    planned = summary.plannedDistanceMeters / 1000.0,
                    unit = "km"
                )
                CompactComparison(
                    label = "Duration Accuracy",
                    actual = summary.actualDurationSeconds.toDouble() / 3600.0,
                    planned = summary.plannedDurationSeconds.toDouble() / 3600.0,
                    unit = "h"
                )
            }
        }

        item {
            UniF1SectionHeader(title = "Category Efficiency")
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                summary.sportComparisons.values.filter { it.plannedSessions > 0 }.forEach { comp ->
                    SportPerformanceCard(comp)
                }
            }
        }
    }
}

@Composable
fun PerformanceMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = color)
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
    }
}

@Composable
fun CompactComparison(label: String, actual: Double, planned: Double, unit: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = "${String.format(Locale.US, "%.1f", actual)} / ${String.format(Locale.US, "%.1f", planned)} $unit",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Progress bar with Soft Glow aesthetic
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
            val progress = if (planned > 0) (actual / planned).toFloat().coerceIn(0f, 1.2f) else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceAtMost(1f))
                    .fillMaxHeight()
                    .background(
                        brush = Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary)),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun SportPerformanceCard(comp: SportPerformanceComparison) {
    val color = when(comp.type) {
        TrainingType.RUN -> MaterialTheme.unif1Colors.run
        TrainingType.BIKE -> MaterialTheme.unif1Colors.bike
        TrainingType.SWIM -> MaterialTheme.unif1Colors.swim
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = borderStroke()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(comp.type.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = color.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${comp.actualSessions} / ${comp.plannedSessions} SESSIONS",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f).height(4.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(comp.distanceAdherence.toFloat().coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(color, CircleShape)
                    )
                }
                Text(
                    text = "${(comp.distanceAdherence * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
