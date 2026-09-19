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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingReadinessScreen(viewModel: TrainingViewModel, onBack: () -> Unit) {
    val context by viewModel.trainingContext.collectAsStateWithLifecycle()
    val readiness = context.readiness

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "DIAGNOSTICS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                        Text("Readiness Report", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                navigationIcon = {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, border = borderStroke()) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(36.dp)
        ) {
            item {
                val statusColor = when (readiness.status) {
                    TrainingReadinessStatus.READY -> MaterialTheme.unif1Colors.ready
                    TrainingReadinessStatus.CAUTION -> MaterialTheme.unif1Colors.caution
                    TrainingReadinessStatus.RECOVERY_FOCUS -> MaterialTheme.unif1Colors.recovery
                    TrainingReadinessStatus.INSUFFICIENT_DATA -> MaterialTheme.colorScheme.outline
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = borderStroke()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 48.dp, horizontal = 24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { if (readiness.status == TrainingReadinessStatus.INSUFFICIENT_DATA) 0f else readiness.score / 100f },
                                modifier = Modifier.size(180.dp),
                                color = statusColor,
                                strokeWidth = 2.dp,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                strokeCap = StrokeCap.Round
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (readiness.status == TrainingReadinessStatus.INSUFFICIENT_DATA) "--" else readiness.score.toString(),
                                    style = MaterialTheme.typography.displayLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "INDEX",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(36.dp))
                        Text(
                            text = readiness.status.name.replace("_", " "),
                            style = MaterialTheme.typography.headlineMedium,
                            color = statusColor
                        )
                        Text(
                            text = if (readiness.status == TrainingReadinessStatus.INSUFFICIENT_DATA) "Awaiting sequence history" else "Conditioned for current plan",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (readiness.status != TrainingReadinessStatus.INSUFFICIENT_DATA) {
                item {
                    UniF1SectionHeader(title = "Primary Indicators")
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = borderStroke()
                    ) {
                        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            if (readiness.reasons.isNotEmpty()) {
                                readiness.reasons.forEach { reason ->
                                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), modifier = Modifier.size(24.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                                            }
                                        }
                                        Text(reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.unif1Colors.ready)
                                    Text("Engine metrics within established baseline.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }

                item {
                    UniF1SectionHeader(title = "Core Telemetry")
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            DetailStatCard(label = "Load Trend", value = readiness.loadSpikeStatus.name.replace("_", " "), icon = Icons.Rounded.StackedLineChart, modifier = Modifier.weight(1f))
                            DetailStatCard(label = "Active Sequence", value = "${readiness.consecutiveActiveDays}d", icon = Icons.Rounded.Timer, modifier = Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            DetailStatCard(label = "Intensity (7D)", value = readiness.hardSessionsLast7Days.toString(), icon = Icons.Rounded.Bolt, modifier = Modifier.weight(1f))
                            DetailStatCard(label = "Recovery State", value = readiness.daysSinceLastWorkout?.let { "${it}d" } ?: "Current", icon = Icons.Rounded.SelfImprovement, modifier = Modifier.weight(1f))
                        }
                    }
                }
                
                item {
                    Surface(
                        onClick = { viewModel.sendMessage("Explain my training readiness status.") },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                                Text("Inquire with Advisor", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            } else {
                item {
                    EmptyState(
                        title = "Engine Calibration",
                        description = "Analysis requires 3 training sessions to calibrate your unique performance index.",
                        icon = Icons.Rounded.Analytics
                    )
                }
            }
            
            item {
                Text(
                    text = "UniF1 advisor interpretation. Independent of medical diagnosis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )
            }
        }
    }
}
