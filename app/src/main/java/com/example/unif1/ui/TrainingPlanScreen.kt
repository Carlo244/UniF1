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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingPlanScreen(
    viewModel: TrainingViewModel,
    onViewPerformance: () -> Unit
) {
    val currentPlan by viewModel.currentPlan.collectAsStateWithLifecycle()
    val planProposal by viewModel.planProposal.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGeneratingPlan.collectAsStateWithLifecycle()
    val performance by viewModel.planPerformance.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "SEQUENCE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                        Text("Weekly Schedule", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                planProposal != null -> {
                    PlanReviewContent(
                        proposal = planProposal!!,
                        alignment = viewModel.calculatePlanAlignment(planProposal!!),
                        onSave = { viewModel.saveProposedPlan() },
                        onDiscard = { viewModel.discardProposal() }
                    )
                }
                isGenerating -> LoadingState()
                currentPlan != null -> {
                    SavedPlanContent(
                        plan = currentPlan!!,
                        performance = performance,
                        onViewPerformance = onViewPerformance,
                        onUpdateStatus = { session, status -> viewModel.updateSessionStatus(session, status) },
                        onRegenerate = { viewModel.generatePlanProposal() }
                    )
                }
                else -> {
                    EmptyState(
                        title = "No sequence active",
                        description = "Generate an intelligent training schedule calibrated to your unique objectives.",
                        icon = Icons.Rounded.CalendarMonth
                    )
                    Box(modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, bottom = 40.dp), contentAlignment = Alignment.BottomCenter) {
                        Surface(
                            onClick = { viewModel.generatePlanProposal() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Text("Synthesize Schedule", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavedPlanContent(
    plan: TrainingPlan,
    performance: PlannedVsActualSummary?,
    onViewPerformance: () -> Unit,
    onUpdateStatus: (PlannedTrainingSession, SessionStatus) -> Unit,
    onRegenerate: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        if (performance != null) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onViewPerformance() },
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = borderStroke()
                ) {
                    Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ADHERENCE INDEX", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                            Text("${(performance.adherencePercentage * 100).toInt()}% Compliance", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text("Current weekly progression overview", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    }
                }
            }
        }

        item {
            UniF1SectionHeader(title = "Timeline")
        }
        
        items(plan.sessions) { session ->
            PlannedSessionItem(session = session, onUpdateStatus = { onUpdateStatus(session, it) })
        }
        
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                onClick = onRegenerate,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = borderStroke()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Regenerate proposal", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun PlannedSessionItem(session: PlannedTrainingSession, onUpdateStatus: (SessionStatus) -> Unit) {
    val isRest = session.status == SessionStatus.REST
    val isToday = session.date == LocalDate.now()
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        border = if (isToday) borderStroke() else null
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(52.dp)) {
                Text(session.date.format(DateTimeFormatter.ofPattern("EEE")).uppercase(), style = MaterialTheme.typography.labelSmall, color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                Text(session.date.dayOfMonth.toString(), style = MaterialTheme.typography.titleLarge, color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isRest) "Active Recovery" else session.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!isRest) {
                    val details = buildString {
                        append(session.sport.name)
                        session.targetDistanceMeters?.let { append(" · ${String.format(Locale.US, "%.1f km", it / 1000.0)}") }
                        session.intensity?.let { append(" · $it") }
                    }
                    Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            if (!isRest) {
                IconButton(
                    onClick = { 
                        val nextStatus = when(session.status) {
                            SessionStatus.PLANNED -> SessionStatus.COMPLETED
                            SessionStatus.COMPLETED -> SessionStatus.SKIPPED
                            else -> SessionStatus.PLANNED
                        }
                        onUpdateStatus(nextStatus)
                    },
                    modifier = Modifier.size(40.dp).background(
                        color = when(session.status) {
                            SessionStatus.COMPLETED -> MaterialTheme.unif1Colors.ready.copy(alpha = 0.2f)
                            SessionStatus.SKIPPED -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        },
                        shape = CircleShape
                    )
                ) {
                    val (icon, color) = when(session.status) {
                        SessionStatus.COMPLETED -> Icons.Rounded.CheckCircle to MaterialTheme.unif1Colors.ready
                        SessionStatus.SKIPPED -> Icons.Rounded.Block to MaterialTheme.colorScheme.onSurfaceVariant
                        SessionStatus.MISSED -> Icons.Rounded.Error to MaterialTheme.unif1Colors.recovery
                        else -> Icons.Rounded.RadioButtonUnchecked to MaterialTheme.colorScheme.outline
                    }
                    Icon(icon, contentDescription = "Status", modifier = Modifier.size(24.dp), tint = color)
                }
            }
        }
    }
}

@Composable
fun PlanReviewContent(
    proposal: TrainingPlan,
    alignment: List<PlanGoalAlignment>,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        item {
            Text(text = "PROPOSAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
            Text("Athletic Sequence", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Text("Engineered for optimal alignment with your established objectives.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        item {
            UniF1SectionHeader(title = "Objective Calibration")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = borderStroke()
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    alignment.forEach { align ->
                        val planned = if (align.goal.metric == GoalMetric.WEEKLY_DISTANCE) align.plannedValue / 1000.0 else align.plannedValue
                        val target = if (align.goal.metric == GoalMetric.WEEKLY_DISTANCE) align.goal.targetValue / 1000.0 else align.goal.targetValue
                        
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                Text("${align.goal.sport?.name ?: "ALL"} ${align.goal.metric.name.replace("WEEKLY_", "").lowercase()}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                                Text("${planned.toInt()} / ${target.toInt()}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(align.alignmentPercentage.toFloat().coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }

        item { UniF1SectionHeader(title = "Proposed Timeline") }
        
        items(proposal.sessions) { session ->
            PlannedSessionItem(session = session, onUpdateStatus = {})
        }
        
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    onClick = onDiscard,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = borderStroke()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Discard", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Surface(
                    onClick = onSave,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Apply Sequence", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
