package com.example.unif1.data.ai

import com.example.unif1.data.training.*
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class CoachQuestionType {
    WORKOUT_SPECIFIC,
    WEEKLY_REVIEW,
    WEEKLY_COMPARISON,
    MULTI_WEEK_PROGRESS,
    TRAINING_BALANCE,
    TRAINING_GOALS,
    TRAINING_LOAD,
    TRAINING_PLAN,
    PLAN_ADHERENCE,
    GENERAL_COACHING
}

class TrainingDataFormatter {

    fun formatForQuestion(
        type: CoachQuestionType,
        context: TrainingContext,
        activities: List<TrainingActivity>,
        currentDate: LocalDate,
        currentTime: LocalTime,
        zone: ZoneId
    ): String {
        val builder = StringBuilder()
        appendTimeHeader(builder, currentDate, currentTime, zone)
        when (type) {
                    CoachQuestionType.WORKOUT_SPECIFIC -> appendWorkoutSpecific(builder, context, activities, zone)
            CoachQuestionType.WEEKLY_REVIEW -> appendWeeklyReview(builder, context)
            CoachQuestionType.WEEKLY_COMPARISON -> appendWeeklyComparison(builder, context)
            CoachQuestionType.MULTI_WEEK_PROGRESS -> appendMultiWeekProgress(builder, context)
            CoachQuestionType.TRAINING_BALANCE -> appendTrainingBalance(builder, context)
            CoachQuestionType.TRAINING_GOALS -> appendTrainingGoals(builder, context)
            CoachQuestionType.TRAINING_LOAD -> appendTrainingLoad(builder, context)
            CoachQuestionType.TRAINING_PLAN -> appendCurrentTrainingPlan(builder, context)
            CoachQuestionType.PLAN_ADHERENCE -> appendPlanAdherence(builder, context)
            CoachQuestionType.GENERAL_COACHING -> appendGeneralCoaching(builder, context)
        }
        if (type == CoachQuestionType.GENERAL_COACHING) {
            appendRecentActivitiesCompact(builder, activities, zone, 6)
        }
        return builder.toString().trim()
    }

    fun selectActivitiesForQuestion(
        type: CoachQuestionType,
        message: String,
        context: TrainingContext,
        activities: List<TrainingActivity>,
        zone: ZoneId,
        today: LocalDate = LocalDate.now(zone)
    ): List<TrainingActivity> {
        if (activities.isEmpty()) return emptyList()
        val sorted = activities.sortedByDescending { it.startTime }
        val lower = message.lowercase()
        val requestedSport = detectRequestedSport(lower)
        val startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        val endOfWeek = startOfWeek.plusDays(6)
        val yesterday = today.minusDays(1)

        fun bySport(list: List<TrainingActivity>): List<TrainingActivity> =
            requestedSport?.let { sport -> list.filter { it.type == sport } } ?: list

        return when (type) {
            CoachQuestionType.WORKOUT_SPECIFIC -> {
                val selected = context.selectedWorkoutAnalysis?.workout?.let { listOf(it) }
                if (!selected.isNullOrEmpty()) return selected
                val base = when {
                    lower.contains("today") -> sorted.filter { it.startTime.atZone(zone).toLocalDate() == today }
                    lower.contains("yesterday") -> sorted.filter { it.startTime.atZone(zone).toLocalDate() == yesterday }
                    lower.contains("latest") || lower.contains("last") -> sorted.take(1)
                    else -> sorted.take(1)
                }
                bySport(base).ifEmpty { bySport(sorted).take(1) }
            }
            CoachQuestionType.WEEKLY_REVIEW,
            CoachQuestionType.WEEKLY_COMPARISON -> {
                bySport(sorted.filter {
                    val d = it.startTime.atZone(zone).toLocalDate()
                    !d.isBefore(startOfWeek) && !d.isAfter(endOfWeek)
                })
            }
            CoachQuestionType.GENERAL_COACHING -> {
                val byDate = when {
                    lower.contains("today") -> sorted.filter { it.startTime.atZone(zone).toLocalDate() == today }
                    lower.contains("yesterday") -> sorted.filter { it.startTime.atZone(zone).toLocalDate() == yesterday }
                    lower.contains("this week") || lower.contains("week") -> sorted.filter {
                        val d = it.startTime.atZone(zone).toLocalDate()
                        !d.isBefore(startOfWeek) && !d.isAfter(endOfWeek)
                    }
                    lower.contains("latest") || lower.contains("last") -> sorted.take(1)
                    else -> sorted.take(6)
                }
                bySport(byDate).ifEmpty { bySport(sorted).take(6) }
            }
            else -> bySport(sorted.take(8))
        }
    }

    fun formatForPlanGeneration(
        context: TrainingContext,
        activities: List<TrainingActivity>,
        currentDate: LocalDate,
        currentTime: LocalTime,
        zone: ZoneId
    ): String {
        val builder = StringBuilder()
        appendTimeHeader(builder, currentDate, currentTime, zone)
        appendWeeklyReview(builder, context)
        appendTrainingLoad(builder, context)
        appendTrainingGoals(builder, context)
        appendCurrentTrainingPlan(builder, context)
        appendRecentActivitiesCompact(builder, activities, zone, 10)
        return builder.toString().trim()
    }

    private fun appendTimeHeader(
        builder: StringBuilder,
        currentDate: LocalDate,
        currentTime: LocalTime,
        zone: ZoneId
    ) {
        builder.append("CURRENT TIME\n")
        builder.append("Date: $currentDate\n")
        builder.append("Time: ${currentTime.format(DateTimeFormatter.ofPattern("HH:mm"))}\n")
        builder.append("Zone: $zone\n\n")
    }

    private fun appendWeeklyReview(builder: StringBuilder, context: TrainingContext) {
        builder.append("CURRENT WEEK\n")
        appendStatsCompact(builder, context.thisWeek)
        builder.append("\nLAST WEEK\n")
        appendStatsCompact(builder, context.lastWeek)
        builder.append("\nWEEKLY TOTALS\n")
        builder.append("ActiveWeeks: ${context.trends.activeWeeksCount}/${context.weeklyProgress.size}\n")
        builder.append("AvgWorkoutsPerWeek: ${String.format(Locale.US, "%.1f", context.trends.avgWorkoutsPerWeek)}\n")
    }

    private fun appendWeeklyComparison(builder: StringBuilder, context: TrainingContext) {
        builder.append("CALENDAR WEEK COMPARISON\n")
        builder.append("THIS WEEK\n")
        appendStatsCompact(builder, context.thisWeek)
        builder.append("\nLAST WEEK\n")
        appendStatsCompact(builder, context.lastWeek)
    }

    private fun appendMultiWeekProgress(builder: StringBuilder, context: TrainingContext) {
        builder.append("MULTI-WEEK TRAINING PROGRESS\n")
        builder.append("ActiveWeeks: ${context.trends.activeWeeksCount}/${context.weeklyProgress.size}\n")
        builder.append("AvgWorkoutsPerWeek: ${String.format(Locale.US, "%.1f", context.trends.avgWorkoutsPerWeek)}\n")
        builder.append("LongestActiveStreak: ${context.trends.longestActiveStreak}\n")
        context.weeklyProgress.forEach { progress ->
            builder.append("${progress.startDate}..${progress.endDate}: ")
            builder.append("Sessions=${progress.stats.totalActivities}, ")
            builder.append("DistanceKm=${String.format(Locale.US, "%.1f", progress.stats.totalDistanceMeters / 1000.0)}, ")
            builder.append("Duration=${formatDuration(progress.stats.totalDuration)}\n")
        }
    }

    private fun appendTrainingBalance(builder: StringBuilder, context: TrainingContext) {
        builder.append("TRAINING BALANCE\n")
        val b = context.overallBalance
        TrainingType.entries.forEach { type ->
            val count = b.sessionsByType[type] ?: 0
            val pct = (b.sessionPercentageByType[type] ?: 0.0) * 100
            builder.append("${type.name}: ${count} (${String.format(Locale.US, "%.0f", pct)}%)\n")
        }
        builder.append("\nWEEKLY BALANCE TREND\n")
        context.weeklyProgress.forEach { progress ->
            builder.append("${progress.startDate}: ")
            builder.append(
                TrainingType.entries.joinToString(", ") { type ->
                    val pct = (progress.balance.sessionPercentageByType[type] ?: 0.0) * 100
                    "${type.name} ${String.format(Locale.US, "%.0f", pct)}%"
                }
            )
            builder.append("\n")
        }
    }

    private fun appendTrainingGoals(builder: StringBuilder, context: TrainingContext) {
        builder.append("TRAINING GOALS\n")
        if (context.goalProgress.isEmpty()) {
            builder.append("None\n")
            return
        }
        context.goalProgress.forEach { progress ->
            val g = progress.goal
            val unit = when (g.metric) {
                GoalMetric.WEEKLY_DISTANCE -> "km"
                GoalMetric.WEEKLY_DURATION -> "h"
                GoalMetric.WEEKLY_SESSIONS -> "sessions"
            }
            val current = if (g.metric == GoalMetric.WEEKLY_DISTANCE) progress.currentValue / 1000.0 else progress.currentValue
            val target = if (g.metric == GoalMetric.WEEKLY_DISTANCE) progress.targetValue / 1000.0 else progress.targetValue
            builder.append("${g.sport?.name ?: "ALL"} ${g.metric.name}: ")
            builder.append("Current=${String.format(Locale.US, "%.1f", current)}$unit, ")
            builder.append("Target=${String.format(Locale.US, "%.1f", target)}$unit, ")
            builder.append("Progress=${(progress.progressPercentage * 100).toInt()}%, ")
            builder.append("Status=${progress.status}\n")
        }
    }

    private fun appendTrainingLoad(builder: StringBuilder, context: TrainingContext) {
        builder.append("TRAINING LOAD & INTENSITY\n")
        val ls = context.loadSummary
        builder.append("CurrentWeekLoad: ${String.format(Locale.US, "%.1f", ls.currentWeekLoad)}\n")
        builder.append("LoadTrend: ${ls.loadTrend}\n")
        ls.percentageChange?.let {
            builder.append("ChangeVsLastWeek: ${String.format(Locale.US, "%.1f%%", it)}\n")
        }
        builder.append("SpikeStatus: ${ls.spikeStatus}\n")
        builder.append("AvgSessionLoad: ${String.format(Locale.US, "%.1f", ls.averageSessionLoad)}\n")
        builder.append("Intensity: ")
        builder.append(ls.intensityDistribution.entries.joinToString(", ") { "${it.key}=${it.value}" })
        builder.append("\n")
    }

    private fun appendCurrentTrainingPlan(builder: StringBuilder, context: TrainingContext) {
        builder.append("CURRENT TRAINING PLAN\n")
        val plan = context.currentPlan
        if (plan == null) {
            builder.append("None\n")
            return
        }
        builder.append("${plan.weekStartDate}..${plan.weekEndDate}\n")
        builder.append("Planned=${plan.sessions.count { it.status != SessionStatus.REST }}, ")
        builder.append("Completed=${plan.sessions.count { it.status == SessionStatus.COMPLETED }}, ")
        builder.append("Skipped=${plan.sessions.count { it.status == SessionStatus.SKIPPED }}, ")
        builder.append("Remaining=${plan.sessions.count { it.status == SessionStatus.PLANNED }}\n")
        plan.sessions.forEach { s ->
            builder.append("${s.date} ${s.sport} ${s.title} ${s.status}\n")
        }
    }

    private fun appendPlanAdherence(builder: StringBuilder, context: TrainingContext) {
        builder.append("PLANNED VS ACTUAL PERFORMANCE\n")
        val summary = context.planPerformance
        if (summary == null) {
            builder.append("No plan-performance data.\n")
            return
        }
        builder.append("${summary.weekStartDate}..${summary.weekEndDate}\n")
        builder.append("Planned=${summary.plannedSessionCount}, Completed=${summary.completedSessionCount}, ")
        builder.append("Skipped=${summary.skippedSessionCount}, Missed=${summary.missedSessionCount}\n")
        builder.append("SessionAdherence=${String.format(Locale.US, "%.1f%%", summary.adherencePercentage * 100)}\n")
        builder.append("DistanceAdherence=${String.format(Locale.US, "%.1f%%", summary.distanceAdherencePercentage * 100)}\n")
        builder.append("DurationAdherence=${String.format(Locale.US, "%.1f%%", summary.durationAdherencePercentage * 100)}\n")
    }

    private fun appendWorkoutSpecific(
            builder: StringBuilder,
            context: TrainingContext,
            activities: List<TrainingActivity>,
            zone: ZoneId
        ) {
            builder.append("SELECTED WORKOUT\n")
            val analysis = context.selectedWorkoutAnalysis
            if (analysis != null) {
                val w = analysis.workout
                val startLocal = w.startTime.atZone(zone)
                val endLocal = w.endTime.atZone(zone)
                val startTimeStr = startLocal.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                val endTimeStr = endLocal.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))

                builder.append("${startLocal.toLocalDate()} | ${w.type}\n")
                builder.append("ID: ${w.id}\n")
                builder.append("Start: $startTimeStr\n")
                builder.append("End: $endTimeStr\n")
                builder.append("Duration: ${formatDuration(w.duration)}\n")
                builder.append("Distance: ${w.distanceMeters?.let { String.format(Locale.US, "%.2f km", it / 1000.0) } ?: "N/A"}\n")
                builder.append("Calories: ${w.calories?.toInt()?.toString() ?: "N/A"} kcal\n")
                builder.append("Average HR: ${w.averageHeartRate?.let { "$it bpm" } ?: "N/A"}\n")
                builder.append("Maximum HR: ${w.maxHeartRate?.let { "$it bpm" } ?: "N/A"}\n")
                builder.append("Training Load: ${String.format(Locale.US, "%.1f", w.calculatedLoad)}\n")
                builder.append("Intensity: ${w.intensity.name}\n")

                analysis.comparison?.let { comp ->
                    builder.append("SAME-SPORT COMPARISON\n")
                    builder.append("RecentCount=${comp.recentWorkoutCount}\n")
                    comp.distanceDiffPercentage?.let { builder.append("DistanceDiff=${String.format(Locale.US, "%.1f%%", it)}\n") }
                    comp.durationDiffPercentage?.let { builder.append("DurationDiff=${String.format(Locale.US, "%.1f%%", it)}\n") }
                    comp.hrDiffPercentage?.let { builder.append("AvgHRDiff=${String.format(Locale.US, "%.1f%%", it)}\n") }
                }
                return
            }

            // Fallback: if selectedWorkoutAnalysis is missing, use provided activities (selectedActivities)
            if (activities.isNotEmpty()) {
                appendSelectedWorkoutFromActivity(builder, activities.first(), zone)
                return
            }

            builder.append("No matching workout in available training data.\n")
        }

    fun appendSelectedWorkoutFromActivity(
        builder: StringBuilder,
        activity: TrainingActivity,
        zone: ZoneId
    ) {
        val startLocal = activity.startTime.atZone(zone)
        val endLocal = activity.endTime.atZone(zone)
        val startTimeStr = startLocal.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
        val endTimeStr = endLocal.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))

        builder.append("${startLocal.toLocalDate()} | ${activity.type}\n")
        builder.append("ID: ${activity.id}\n")
        builder.append("Start: $startTimeStr\n")
        builder.append("End: $endTimeStr\n")
        builder.append("Duration: ${formatDuration(activity.duration)}\n")
        builder.append("Distance: ${activity.distanceMeters?.let { String.format(Locale.US, "%.2f km", it / 1000.0) } ?: "N/A"}\n")
        builder.append("Calories: ${activity.calories?.toInt()?.toString() ?: "N/A"} kcal\n")
        builder.append("Average HR: ${activity.averageHeartRate?.let { "$it bpm" } ?: "N/A"}\n")
        builder.append("Maximum HR: ${activity.maxHeartRate?.let { "$it bpm" } ?: "N/A"}\n")
        builder.append("Training Load: ${String.format(Locale.US, "%.1f", activity.calculatedLoad)}\n")
        builder.append("Intensity: ${activity.intensity.name}\n")
    }

    private fun detectRequestedSport(lower: String): TrainingType? {
        return when {
            lower.contains("run") -> TrainingType.RUN
            lower.contains("bike") || lower.contains("cycling") || lower.contains("cycle") -> TrainingType.BIKE
            lower.contains("swim") -> TrainingType.SWIM
            else -> null
        }
    }

    private fun appendGeneralCoaching(builder: StringBuilder, context: TrainingContext) {
        builder.append("GENERAL COACHING CONTEXT\n")
        val cc = context.coachingContext
        cc.mostRecentWorkout?.let { builder.append("MostRecent=${it.type} ${it.startTime}\n") }
        builder.append("DaysSinceLastWorkout=${cc.daysSinceLastWorkout ?: "N/A"}\n")
        builder.append("Workouts3d=${cc.workoutCountLast3Days}, Workouts7d=${cc.workoutCountLast7Days}, Workouts14d=${cc.workoutCountLast14Days}\n")
        builder.append("ConsecutiveActiveDays=${cc.consecutiveActiveDays}\n")
        appendTrainingLoad(builder, context)
        if (context.currentPlan != null) appendCurrentTrainingPlan(builder, context)
        if (context.goalProgress.isNotEmpty()) appendTrainingGoals(builder, context)
    }

    private fun appendRecentActivitiesCompact(
        builder: StringBuilder,
        activities: List<TrainingActivity>,
        zone: ZoneId,
        limit: Int
    ) {
        builder.append("\nRECENT ACTIVITIES\n")
        if (activities.isEmpty()) {
            builder.append("None\n")
            return
        }
        activities.take(limit).forEach { activity ->
            val startLocal = activity.startTime.atZone(zone)
            val endLocal = activity.endTime.atZone(zone)
            val startTimeStr = startLocal.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
            val endTimeStr = endLocal.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))

            builder.append("${startLocal.toLocalDate()} | ${activity.type}\n")
            builder.append("ID: ${activity.id}\n")
            builder.append("Start: $startTimeStr\n")
            builder.append("End: $endTimeStr\n")
            builder.append("Duration: ${formatDuration(activity.duration)}\n")
            builder.append("Distance: ${activity.distanceMeters?.let { String.format(Locale.US, "%.2f km", it / 1000.0) } ?: "N/A"}\n")
            builder.append("Calories: ${activity.calories?.toInt()?.toString() ?: "N/A"} kcal\n")
            builder.append("Average HR: ${activity.averageHeartRate?.let { "$it bpm" } ?: "N/A"}\n")
            builder.append("Maximum HR: ${activity.maxHeartRate?.let { "$it bpm" } ?: "N/A"}\n")
            builder.append("Training Load: ${String.format(Locale.US, "%.1f", activity.calculatedLoad)}\n")
            builder.append("Intensity: ${activity.intensity.name}\n")
            builder.append("\n")
        }
    }

    private fun appendStatsCompact(builder: StringBuilder, stats: TrainingStats) {
        builder.append("Sessions: ${stats.totalActivities}\n")
        builder.append("Distance: ${String.format(Locale.US, "%.1f km", stats.totalDistanceMeters / 1000.0)}\n")
        builder.append("Duration: ${formatDuration(stats.totalDuration)}\n")
        builder.append("Calories: ${stats.totalCalories.toInt()} kcal\n")
        builder.append("Average HR: ${stats.avgHeartRate?.toString() ?: "N/A"} bpm\n")
        builder.append("Total Training Load: ${String.format(Locale.US, "%.1f", stats.totalLoad)}\n")
        if (stats.typeBreakdown.isNotEmpty()) {
            val sportBreakdown = TrainingType.entries.joinToString(", ") { type ->
                val count = stats.typeBreakdown[type] ?: 0
                "$type:$count"
            }
            builder.append("Sports: $sportBreakdown\n")
        }
        if (stats.intensityDistribution.isNotEmpty()) {
            val intensityStr = stats.intensityDistribution.entries.joinToString(", ") { (k, v) ->
                "${k.name}=$v"
            }
            builder.append("Intensity: $intensityStr\n")
        }
    }

    private fun formatDuration(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = (duration.toMinutes() % 60).toInt()
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}
