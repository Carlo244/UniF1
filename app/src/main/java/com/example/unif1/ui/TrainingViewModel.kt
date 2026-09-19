
package com.example.unif1.ui

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unif1.data.ai.GeminiService
import com.example.unif1.data.ai.CoachQuestionType
import com.example.unif1.data.ai.TrainingDataFormatter
import com.example.unif1.data.health.HealthAvailability
import com.example.unif1.data.training.TrainingActivity
import com.example.unif1.data.training.GoalMetric
import com.example.unif1.data.training.PlanGoalAlignment
import com.example.unif1.data.training.PlannedTrainingSession
import com.example.unif1.data.training.PlannedVsActualAnalyzer
import com.example.unif1.data.training.PlannedVsActualSummary
import com.example.unif1.data.training.SessionStatus
import com.example.unif1.data.training.TrainingAnalyzer
import com.example.unif1.data.training.TrainingContext
import com.example.unif1.data.training.TrainingGoal
import com.example.unif1.data.training.TrainingGoalRepository
import com.example.unif1.data.training.TrainingPlan
import com.example.unif1.data.training.TrainingPlanAnalyzer
import com.example.unif1.data.training.TrainingPlanRepository
import com.example.unif1.data.training.TrainingRepository
import com.example.unif1.data.training.TrainingStats
import com.example.unif1.data.training.TrainingType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TrendsState(
    val activities: List<TrainingActivity> = emptyList(),
    val stats: TrainingStats = TrainingStats(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedDays: Int = 7
)

data class TrainingDetailState(
    val activity: TrainingActivity? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ChatMessage(
    val text: String,
    val isFromAI: Boolean
)

data class TrainingAiUiState(
    val isLoading: Boolean = false,
    val response: String = "",
    val error: String? = null,
    val messages: List<ChatMessage> = emptyList()
)

class TrainingViewModel(
    private val repository: TrainingRepository,
    private val goalRepository: TrainingGoalRepository,
    private val planRepository: TrainingPlanRepository,
    private val context: android.content.Context,
    private val geminiService: GeminiService = GeminiService(context)
) : ViewModel() {

    private val analyzer = TrainingAnalyzer(repository)
    private val planAnalyzer = TrainingPlanAnalyzer()
    private val pvaAnalyzer = PlannedVsActualAnalyzer()
    private val formatter = TrainingDataFormatter()

    val permissions = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    val goals: StateFlow<List<TrainingGoal>> = goalRepository.goals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentPlan = MutableStateFlow<TrainingPlan?>(null)
    val currentPlan: StateFlow<TrainingPlan?> = _currentPlan.asStateFlow()

    private val _planPerformance = MutableStateFlow<PlannedVsActualSummary?>(null)
    val planPerformance: StateFlow<PlannedVsActualSummary?> = _planPerformance.asStateFlow()

    private val _planProposal = MutableStateFlow<TrainingPlan?>(null)
    val planProposal: StateFlow<TrainingPlan?> = _planProposal.asStateFlow()

    private val _isGeneratingPlan = MutableStateFlow(false)
    val isGeneratingPlan: StateFlow<Boolean> = _isGeneratingPlan.asStateFlow()

    private val _trainingContext = MutableStateFlow(TrainingContext())
    val trainingContext: StateFlow<TrainingContext> = _trainingContext.asStateFlow()

    private val _trainingHistory30d = MutableStateFlow<List<TrainingActivity>>(emptyList())

    /*
     * ONE canonical training list.
     *
     * Every screen reads from this dataset.
     */
    private val _trainingHistory =
        MutableStateFlow<List<TrainingActivity>>(emptyList())

    val trainingHistory: StateFlow<List<TrainingActivity>> =
        _trainingHistory.asStateFlow()

    private val _trainingStats =
        MutableStateFlow(TrainingStats())

    val trainingStats: StateFlow<TrainingStats> =
        _trainingStats.asStateFlow()

    private val _isLoading =
        MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> =
        _isLoading.asStateFlow()

    private val _error =
        MutableStateFlow<String?>(null)

    val error: StateFlow<String?> =
        _error.asStateFlow()

    private val _availability =
        MutableStateFlow<HealthAvailability>(HealthAvailability.Installed)

    val availability: StateFlow<HealthAvailability> =
        _availability.asStateFlow()

    private val _permissionsGranted =
        MutableStateFlow(false)

    val permissionsGranted: StateFlow<Boolean> =
        _permissionsGranted.asStateFlow()

    private val _trendsState =
        MutableStateFlow(TrendsState())

    val trendsState: StateFlow<TrendsState> =
        _trendsState.asStateFlow()

    private val _detailState =
        MutableStateFlow(TrainingDetailState())

    val detailState: StateFlow<TrainingDetailState> =
        _detailState.asStateFlow()

    private val _aiState =
        MutableStateFlow(
            TrainingAiUiState(
                messages = listOf(
                    ChatMessage(
                        text = "Hi! I'm your UniF1 coach. I can analyze your training consistency, volume, and balance. How can I help you today?",
                        isFromAI = true
                    )
                )
            )
        )

    val aiState: StateFlow<TrainingAiUiState> =
        _aiState.asStateFlow()

    /*
     * Compatibility properties for existing Home UI.
     *
     * These are derived from trainingHistory.
     * They are NOT separate datasets.
     */
    val todayTraining: StateFlow<List<TrainingActivity>> = trainingHistory
        .map { history ->
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)

            history.filter { activity ->
                activity.startTime
                    .atZone(zone)
                    .toLocalDate() == today
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentTraining: StateFlow<List<TrainingActivity>> = trainingHistory
        .map { history ->
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)

            history
                .filter { activity ->
                    activity.startTime
                        .atZone(zone)
                        .toLocalDate() != today
                }
                .take(5)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            planRepository.plans.collect { plans ->
                val zone = ZoneId.systemDefault()
                val startOfThisWeek = LocalDate.now(zone).with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                _currentPlan.value = plans.find { it.weekStartDate == startOfThisWeek }
            }
        }
        refreshData()
    }

    /*
     * The ONLY deduplication function.
     *
     * TrainingActivity.id comes directly from
     * Health Connect record.metadata.id.
     */
    private fun deduplicate(
        activities: List<TrainingActivity>
    ): List<TrainingActivity> {
        return activities
            .distinctBy { activity -> activity.id }
            .sortedByDescending { activity -> activity.startTime }
    }

    /*
     * Load the complete 7-day dataset.
     *
     * Every refresh replaces the previous dataset.
     */
    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val availability = repository.checkAvailability()
                _availability.value = availability

                if (availability != HealthAvailability.Installed) {
                    _isLoading.value = false
                    return@launch
                }

                val hasPermissions = repository.hasAllPermissions()
                _permissionsGranted.value = hasPermissions

                if (!hasPermissions) {
                    _isLoading.value = false
                    return@launch
                }

                val last7d = deduplicate(
                    repository.fetchLastNDaysTraining(7)
                ).map { analyzer.enrichActivity(it) }

                val prev7d = deduplicate(
                    repository.fetchPreviousNDaysTraining(7, 7)
                ).map { analyzer.enrichActivity(it) }

                val last30d = deduplicate(
                    repository.fetchLastNDaysTraining(30)
                ).map { analyzer.enrichActivity(it) }
                _trainingHistory30d.value = last30d

                val zone = ZoneId.systemDefault()
                val todayDate = LocalDate.now(zone)
                val today = last7d.filter {
                    it.startTime.atZone(zone).toLocalDate() == todayDate
                }

                _trainingHistory.value = last7d

                val stats = repository.calculateStats(last7d)
                _trainingStats.value = stats

                val currentGoals = goals.value
                val activePlan = _currentPlan.value
                val context = analyzer.analyze(today, last7d, prev7d, last30d, currentGoals, activePlan, zone)
                
                // Calculate Plan Performance
                val performance = activePlan?.let { 
                    pvaAnalyzer.analyze(it, last7d, todayDate, zone) 
                }
                _planPerformance.value = performance
                
                _trainingContext.value = context.copy(planPerformance = performance)

                _trendsState.value = _trendsState.value.copy(
                    activities = last7d,
                    stats = stats,
                    isLoading = false,
                    error = null
                )

            } catch (e: Exception) {
                val message =
                    e.message ?: "Failed to load training data"

                _error.value = message

                _trendsState.value = _trendsState.value.copy(
                    isLoading = false,
                    error = message
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkHealthConnectStatus() {
        refreshData()
    }

    /*
     * Goal Management
     */
    fun addGoal(metric: GoalMetric, sport: TrainingType?, target: Double) {
        viewModelScope.launch {
            goalRepository.addGoal(metric, sport, target)
            refreshData()
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            goalRepository.deleteGoal(goalId)
            refreshData()
        }
    }

    fun toggleGoal(goal: TrainingGoal) {
        viewModelScope.launch {
            goalRepository.updateGoal(goal.copy(active = !goal.active))
            refreshData()
        }
    }

    /*
     * Plan Management
     */
    fun generatePlanProposal() {
        viewModelScope.launch {
            _isGeneratingPlan.value = true
            try {
                val zone = ZoneId.systemDefault()
                val today = LocalDate.now(zone)
                val startOfNextWeek = today.with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY))
                val endOfNextWeek = startOfNextWeek.plusDays(6)
                
                val trainingData = formatter.formatForPlanGeneration(
                    context = _trainingContext.value,
                    activities = _trainingHistory30d.value,
                    currentDate = today,
                    currentTime = LocalTime.now(zone),
                    zone = zone
                )
                
                val proposal = geminiService.generateTrainingPlan(trainingData, startOfNextWeek, endOfNextWeek)
                _planProposal.value = proposal
            } catch (e: Exception) {
                _error.value = "Failed to generate plan: ${e.message}"
            } finally {
                _isGeneratingPlan.value = false
            }
        }
    }

    fun saveProposedPlan() {
        viewModelScope.launch {
            _planProposal.value?.let { 
                planRepository.savePlan(it)
                _planProposal.value = null
            }
        }
    }

    fun discardProposal() {
        _planProposal.value = null
    }

    fun updateSessionStatus(session: PlannedTrainingSession, newStatus: SessionStatus) {
        viewModelScope.launch {
            _currentPlan.value?.let { plan ->
                planRepository.updateSession(plan.id, session.copy(status = newStatus))
            }
        }
    }

    fun calculatePlanAlignment(plan: TrainingPlan): List<PlanGoalAlignment> {
        return planAnalyzer.calculateGoalAlignment(plan, goals.value)
    }

    /*
     * Trends.
     */
    fun fetchTrends(days: Int) {
        viewModelScope.launch {
            _trendsState.value = _trendsState.value.copy(
                isLoading = true,
                selectedDays = days,
                error = null
            )

            try {
                val zone = ZoneId.systemDefault()

                val startOfRange = LocalDate.now(zone)
                    .minusDays((days - 1).toLong())
                    .atStartOfDay(zone)
                    .toInstant()

                val activities = deduplicate(
                    repository.getActivities(
                        startTime = startOfRange,
                        endTime = Instant.now()
                    )
                )

                val stats =
                    repository.calculateStats(activities)

                _trendsState.value =
                    _trendsState.value.copy(
                        activities = activities,
                        stats = stats,
                        isLoading = false,
                        error = null
                    )

            } catch (e: Exception) {
                _trendsState.value =
                    _trendsState.value.copy(
                        isLoading = false,
                        error =
                            e.message
                                ?: "Failed to fetch trends"
                    )
            }
        }
    }

    /*
     * Activity details.
     *
     * Search the canonical list first.
     */
    fun fetchActivityDetails(activityId: String) {
        viewModelScope.launch {
            _detailState.value =
                TrainingDetailState(
                    isLoading = true
                )

            val activity =
                _trainingHistory.value.firstOrNull {
                        activity -> activity.id == activityId
                }

            if (activity != null) {
                _detailState.value = TrainingDetailState(
                    activity = activity,
                    isLoading = false
                )
                // When details are fetched, we prepare the context for AI analysis of THIS workout
                val zone = ZoneId.systemDefault()
                _trainingContext.value = analyzer.analyzeWithSelectedWorkout(
                    context = _trainingContext.value,
                    selectedWorkoutId = activityId,
                    history = _trainingHistory30d.value,
                    zoneId = zone
                )
            } else {
                _detailState.value = TrainingDetailState(
                    isLoading = false,
                    error = "Activity not found"
                )
            }
        }
    }

    /*
     * Gemini analysis.
     */
    fun analyzeTraining() {
        if (_aiState.value.isLoading) return
        viewModelScope.launch {
            _aiState.value =
                _aiState.value.copy(
                    isLoading = true,
                    response = "",
                    error = null
                )

            try {
                val zone = ZoneId.systemDefault()
                val trainingData = formatter.formatForQuestion(
                    type = CoachQuestionType.WEEKLY_REVIEW,
                    context = _trainingContext.value,
                    activities = _trainingHistory30d.value,
                    currentDate = LocalDate.now(zone),
                    currentTime = LocalTime.now(zone),
                    zone = zone
                )

                val result =
                    geminiService.getTrainingSummary(
                        trainingData
                    )

                _aiState.value =
                    _aiState.value.copy(
                        isLoading = false,
                        response = result,
                        error = null,
                        messages =
                            _aiState.value.messages +
                                    ChatMessage(
                                        text = result,
                                        isFromAI = true
                                    )
                    )

            } catch (e: Exception) {
                _aiState.value =
                    _aiState.value.copy(
                        isLoading = false,
                        response = "",
                        error = e.message ?: "UniF1 Coach is temporarily unavailable. Please try again in a moment."
                    )
            }
        }
    }

    /*
     * Gemini chat.
     */
    fun sendMessage(message: String) {
        val trimmed = message.trim()

        if (
            trimmed.isEmpty() ||
            _aiState.value.isLoading
        ) {
            return
        }

        val userMessage =
            ChatMessage(
                text = trimmed,
                isFromAI = false
            )

        val pendingMessages =
            _aiState.value.messages + userMessage

        _aiState.value =
            _aiState.value.copy(
                isLoading = true,
                error = null,
                messages = pendingMessages
            )

        viewModelScope.launch {
            try {
                val zone = ZoneId.systemDefault()
                val questionType = detectQuestionType(trimmed)
                val selectedActivities = formatter.selectActivitiesForQuestion(
                    type = questionType,
                    message = trimmed,
                    context = _trainingContext.value,
                    activities = _trainingHistory30d.value,
                    zone = zone
                )
                val baseContext = formatter.formatForQuestion(
                    type = questionType,
                    context = _trainingContext.value,
                    activities = selectedActivities,
                    currentDate = LocalDate.now(zone),
                    currentTime = LocalTime.now(zone),
                    zone = zone
                )
                val trainingData = if (baseContext.isBlank() && _trainingHistory30d.value.isNotEmpty()) {
                    formatter.formatForQuestion(
                        type = CoachQuestionType.GENERAL_COACHING,
                        context = _trainingContext.value,
                        activities = _trainingHistory30d.value.take(6),
                        currentDate = LocalDate.now(zone),
                        currentTime = LocalTime.now(zone),
                        zone = zone
                    )
                } else {
                    baseContext
                }
                val firstSection = trainingData.lineSequence().firstOrNull()?.trim().orEmpty()
                Log.d(
                    "UniF1-AI",
                    "AI DEBUG: questionType=$questionType, availableActivities=${_trainingHistory30d.value.size}, selectedActivities=${selectedActivities.size}, contextChars=${trainingData.length}, firstSection=$firstSection"
                )

                val conversationHistory =
                    pendingMessages
                        .takeLast(6)
                        .map { messageItem ->
                            if (messageItem.isFromAI) {
                                "Coach: ${messageItem.text}"
                            } else {
                                "Athlete: ${messageItem.text}"
                            }
                        }

                val response =
                    geminiService.getChatResponse(
                        userMessage = trimmed,
                        trainingData = trainingData,
                        conversationHistory =
                            conversationHistory
                    )

                _aiState.value =
                    _aiState.value.copy(
                        isLoading = false,
                        error = null,
                        messages =
                            pendingMessages +
                                    ChatMessage(
                                        text = response,
                                        isFromAI = true
                                    )
                    )

            } catch (e: Exception) {
                _aiState.value =
                    _aiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "UniF1 Coach is temporarily unavailable. Please try again in a moment.",
                        messages = pendingMessages
                    )
            }
        }
    }

    private fun detectQuestionType(message: String): CoachQuestionType {
        val lower = message.lowercase()
        return when {
            listOf("selected workout", "this workout", "latest workout", "last workout", "my run", "my bike", "my swim", "what did i train today", "what was my last run", "how far did i run", "analyze workout", "how was my run today").any { lower.contains(it) } ->
                CoachQuestionType.WORKOUT_SPECIFIC
            listOf("weekly review", "this week", "last week summary").any { lower.contains(it) } ->
                CoachQuestionType.WEEKLY_REVIEW
            listOf("compare weeks", "weekly comparison", "this week vs last week").any { lower.contains(it) } ->
                CoachQuestionType.WEEKLY_COMPARISON
            listOf("progress", "multi-week", "trend over").any { lower.contains(it) } ->
                CoachQuestionType.MULTI_WEEK_PROGRESS
            listOf("balance", "sport balance", "run bike swim ratio").any { lower.contains(it) } ->
                CoachQuestionType.TRAINING_BALANCE
            listOf("goal", "goals", "target").any { lower.contains(it) } ->
                CoachQuestionType.TRAINING_GOALS
            listOf("load", "intensity", "hard sessions").any { lower.contains(it) } ->
                CoachQuestionType.TRAINING_LOAD
            listOf("plan adherence", "planned vs actual", "followed my plan").any { lower.contains(it) } ->
                CoachQuestionType.PLAN_ADHERENCE
            listOf("training plan", "plan for", "next week plan").any { lower.contains(it) } ->
                CoachQuestionType.TRAINING_PLAN
            else -> CoachQuestionType.GENERAL_COACHING
        }
    }

    /*
     * Reset AI chat.
     */
    fun clearAnalysis() {
        _aiState.value =
            TrainingAiUiState(
                messages = listOf(
                    ChatMessage(
                        text = "Hi! I'm your UniF1 coach. I can analyze your training consistency, volume, and balance. How can I help you today?",
                        isFromAI = true
                    )
                )
            )
        // Clear selected workout when AI state is cleared? 
        // Actually, maybe better to keep it if the user just navigated from Detail.
        // But for safety against stale context:
        _trainingContext.value = _trainingContext.value.copy(selectedWorkoutAnalysis = null)
    }

    /*
     * Compatibility property.
     */
    val permissionsToRequest = repository.permissions
}
