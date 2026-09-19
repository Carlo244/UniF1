package com.example.unif1.data.training

import java.time.Duration
import java.time.Instant
import java.util.Locale

data class TrainingContext(
    val generatedAt: Instant = Instant.now(),
    val todayActivities: List<TrainingActivity> = emptyList(),
    val last7Days: TrainingStats = TrainingStats(),
    val last30Days: TrainingStats = TrainingStats(),
    val previous7Days: TrainingStats = TrainingStats(),
    val thisWeek: TrainingStats = TrainingStats(),
    val lastWeek: TrainingStats = TrainingStats(),
    val weeklyProgress: List<WeeklyProgress> = emptyList(),
    val overallBalance: TrainingBalance = TrainingBalance(),
    val runStats: SportSpecificStats = SportSpecificStats(TrainingType.RUN),
    val bikeStats: SportSpecificStats = SportSpecificStats(TrainingType.BIKE),
    val swimStats: SportSpecificStats = SportSpecificStats(TrainingType.SWIM),
    val trends: DetailedTrends = DetailedTrends(),
    val coachingContext: CoachingContext = CoachingContext(),
    val selectedWorkoutAnalysis: WorkoutAnalysisContext? = null,
    val goalProgress: List<GoalProgress> = emptyList(),
    val currentPlan: TrainingPlan? = null,
    val planPerformance: PlannedVsActualSummary? = null,
    val loadSummary: TrainingLoadSummary = TrainingLoadSummary(),
    val readiness: TrainingReadiness = TrainingReadiness()
) {
    fun toPromptString(): String {
        val builder = StringBuilder()
        builder.append("Athlete Training Context:\n")
        builder.append("- Weekly Volume: ${String.format(Locale.US, "%.2f km", last7Days.totalDistanceMeters / 1000.0)}\n")
        builder.append("- Sessions: ${last7Days.totalActivities}\n")
        builder.append("- Consistency Score: ${String.format(Locale.US, "%.1f", trends.consistencyScore * 100)}%\n")
        builder.append("- Volume Change: ${trends.volumeChangePercentage?.let { String.format(Locale.US, "%.1f%%", it) } ?: "N/A"}\n")
        
        builder.append("\nSport Specifics (Last 7 Days):\n")
        listOf(runStats, bikeStats, swimStats).forEach { stats ->
            if (stats.sessionCount > 0) {
                builder.append("- ${stats.type.name}: ${stats.sessionCount} sessions, ${String.format(Locale.US, "%.2f km", stats.totalDistanceMeters / 1000.0)}, Pace/Speed: ${stats.avgPaceOrSpeed ?: "N/A"}\n")
            }
        }
        
        if (trends.highlights.isNotEmpty()) {
            builder.append("\nHighlights:\n- ${trends.highlights.joinToString("\n- ")}\n")
        }
        if (trends.concerns.isNotEmpty()) {
            builder.append("\nConcerns:\n- ${trends.concerns.joinToString("\n- ")}\n")
        }
        
        return builder.toString()
    }
}

data class SportSpecificStats(
    val type: TrainingType,
    val sessionCount: Int = 0,
    val totalDistanceMeters: Double = 0.0,
    val totalDuration: Duration = Duration.ZERO,
    val avgHeartRate: Long? = null,
    val longestDistanceMeters: Double? = null,
    val longestDuration: Duration? = null,
    val weeklyVolumeMeters: Double = 0.0,
    val avgPaceOrSpeed: String? = null
)

data class DetailedTrends(
    val volumeChangePercentage: Double? = null,
    val consistencyScore: Double = 0.0, // 0.0 to 1.0
    val frequencyTrend: String = "stable", // "increasing", "decreasing", "stable"
    val sportDistribution: Map<TrainingType, Double> = emptyMap(),
    val highlights: List<String> = emptyList(),
    val concerns: List<String> = emptyList(),
    val activeWeeksCount: Int = 0,
    val avgWorkoutsPerWeek: Double = 0.0,
    val longestActiveStreak: Int = 0
)

data class CoachingContext(
    val mostRecentWorkout: TrainingActivity? = null,
    val daysSinceLastWorkout: Long? = null,
    val workoutCountLast3Days: Int = 0,
    val workoutCountLast7Days: Int = 0,
    val workoutCountLast14Days: Int = 0,
    val consecutiveActiveDays: Int = 0
)

data class WorkoutAnalysisContext(
    val workout: TrainingActivity,
    val comparison: SameSportComparison? = null
)

data class SameSportComparison(
    val recentWorkoutCount: Int,
    val avgDistanceMeters: Double?,
    val avgDuration: Duration?,
    val avgCalories: Double?,
    val avgHeartRate: Long?,
    val distanceDiffPercentage: Double?,
    val durationDiffPercentage: Double?,
    val hrDiffPercentage: Double?
)
