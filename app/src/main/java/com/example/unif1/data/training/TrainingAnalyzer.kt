package com.example.unif1.data.training

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class TrainingAnalyzer(
    private val repository: TrainingRepository
) {

    fun enrichActivity(activity: TrainingActivity): TrainingActivity {
        val intensity = calculateIntensity(activity)
        val load = calculateLoad(activity, intensity)
        return activity.copy(intensity = intensity, calculatedLoad = load)
    }

    private fun calculateIntensity(activity: TrainingActivity): Intensity {
        val hr = activity.averageHeartRate
        return if (hr != null) {
            when {
                hr < 120 -> Intensity.EASY
                hr <= 145 -> Intensity.MODERATE
                hr <= 170 -> Intensity.HARD
                else -> Intensity.VERY_HARD
            }
        } else {
            // Fallback to duration-based intensity
            val minutes = activity.duration.toMinutes()
            when {
                minutes < 30 -> Intensity.EASY
                minutes <= 60 -> Intensity.MODERATE
                minutes <= 120 -> Intensity.HARD
                else -> Intensity.VERY_HARD
            }
        }
    }

    private fun calculateLoad(activity: TrainingActivity, intensity: Intensity): Double {
        val multiplier = when (intensity) {
            Intensity.EASY -> 1.0
            Intensity.MODERATE -> 2.0
            Intensity.HARD -> 4.0
            Intensity.VERY_HARD -> 6.0
        }
        return (activity.duration.seconds / 60.0) * multiplier
    }

    fun analyze(
        today: List<TrainingActivity>,
        last7Days: List<TrainingActivity>,
        previous7Days: List<TrainingActivity>,
        last30Days: List<TrainingActivity>,
        goals: List<TrainingGoal> = emptyList(),
        plan: TrainingPlan? = null,
        zoneId: ZoneId = ZoneId.systemDefault(),
        todayDate: LocalDate = LocalDate.now(zoneId)
    ): TrainingContext {
        val stats7d = repository.calculateStats(last7Days)
        val statsPrev7d = repository.calculateStats(previous7Days)
        val stats30d = repository.calculateStats(last30Days)

        
        // Multi-week analysis (Last 4 calendar weeks)
        val weeklyProgress = mutableListOf<WeeklyProgress>()
        val startOfThisWeek = todayDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        
        for (i in 0 until 4) {
            val weekStart = startOfThisWeek.minusWeeks(i.toLong())
            val weekEnd = if (i == 0) todayDate else weekStart.plusDays(6)
            
            val weekActivities = last30Days.filter {
                val actDate = it.startTime.atZone(zoneId).toLocalDate()
                !actDate.isBefore(weekStart) && !actDate.isAfter(weekEnd)
            }
            
            val weekStats = repository.calculateStats(weekActivities)
            weeklyProgress.add(
                WeeklyProgress(
                    startDate = weekStart,
                    endDate = weekEnd,
                    stats = weekStats,
                    balance = calculateBalance(weekStats)
                )
            )
        }
        
        // Ensure chronological order (oldest first for trend analysis)
        val chronologicalProgress = weeklyProgress.reversed()

        val statsThisWeek = weeklyProgress[0].stats
        val statsLastWeek = weeklyProgress[1].stats
        
        val overallBalance = calculateBalance(stats30d)
        val todayPlanned = plan?.sessions?.find { it.date == todayDate }

        return TrainingContext(
            generatedAt = Instant.now(),
            todayActivities = today,
            last7Days = stats7d,
            last30Days = stats30d,
            previous7Days = statsPrev7d,
            thisWeek = statsThisWeek,
            lastWeek = statsLastWeek,
            weeklyProgress = chronologicalProgress,
            overallBalance = overallBalance,
            runStats = calculateSportStats(TrainingType.RUN, last7Days),
            bikeStats = calculateSportStats(TrainingType.BIKE, last7Days),
            swimStats = calculateSportStats(TrainingType.SWIM, last7Days),
            trends = calculateTrends(stats7d, statsPrev7d, last7Days, chronologicalProgress),
            coachingContext = calculateCoachingContext(last30Days, todayDate, zoneId),
            goalProgress = calculateAllGoalProgress(goals, statsThisWeek, todayDate),
            loadSummary = calculateLoadSummary(statsThisWeek, statsLastWeek, chronologicalProgress, zoneId),
            readiness = calculateReadiness(last30Days, statsThisWeek, statsLastWeek, chronologicalProgress, todayDate, zoneId, todayPlanned),
            currentPlan = plan
        )
    }

    private fun calculateReadiness(
        allActivities: List<TrainingActivity>,
        thisWeek: TrainingStats,
        lastWeek: TrainingStats,
        weeklyProgress: List<WeeklyProgress>,
        today: LocalDate,
        zone: ZoneId,
        todayPlanned: PlannedTrainingSession? = null
    ): TrainingReadiness {
        if (allActivities.size < 2) return TrainingReadiness(status = TrainingReadinessStatus.INSUFFICIENT_DATA)

        val loadSummary = calculateLoadSummary(thisWeek, lastWeek, weeklyProgress, zone)
        
        val last3Days = allActivities.filter { 
            val date = it.startTime.atZone(zone).toLocalDate()
            !date.isBefore(today.minusDays(2)) && !date.isAfter(today)
        }
        val last7Days = allActivities.filter {
            val date = it.startTime.atZone(zone).toLocalDate()
            !date.isBefore(today.minusDays(6)) && !date.isAfter(today)
        }

        val hardLast7 = last7Days.count { it.intensity == Intensity.HARD }
        val veryHardLast7 = last7Days.count { it.intensity == Intensity.VERY_HARD }
        
        val sorted = allActivities.sortedByDescending { it.startTime }
        val lastWorkout = sorted.firstOrNull()
        val daysSinceLast = lastWorkout?.let {
            java.time.temporal.ChronoUnit.DAYS.between(it.startTime.atZone(zone).toLocalDate(), today)
        }

        var consecutive = 0
        var checkDate = today
        while (allActivities.any { it.startTime.atZone(zone).toLocalDate() == checkDate }) {
            consecutive++
            checkDate = checkDate.minusDays(1)
        }

        val reasons = mutableListOf<String>()
        var score = 100

        // Score logic
        if (loadSummary.spikeStatus == LoadSpikeStatus.HIGH_SPIKE) {
            score -= 30
            reasons.add("Significant training load spike detected.")
        } else if (loadSummary.spikeStatus == LoadSpikeStatus.ELEVATED) {
            score -= 15
            reasons.add("Training load is elevated compared to baseline.")
        }

        if (veryHardLast7 >= 1) {
            score -= 20
            reasons.add("You completed a very hard session recently.")
        }
        if (hardLast7 >= 2) {
            score -= 15
            reasons.add("Multiple hard sessions in the last 7 days.")
        }
        
        if (consecutive >= 4) {
            score -= 15
            reasons.add("You are on a $consecutive-day training streak.")
        }

        if (last3Days.size >= 3) {
            score -= 10
            reasons.add("High training frequency in the last 3 days.")
        }

        score = score.coerceIn(0, 100)

        val status = when {
            score >= 80 -> TrainingReadinessStatus.READY
            score >= 60 -> TrainingReadinessStatus.CAUTION
            else -> TrainingReadinessStatus.RECOVERY_FOCUS
        }

        val recentBaseline = weeklyProgress.dropLast(1).takeLast(3)
        val avgBaseline = if (recentBaseline.isNotEmpty()) recentBaseline.map { it.stats.totalLoad }.average() else 0.0

        return TrainingReadiness(
            status = status,
            score = score,
            currentWeeklyLoad = thisWeek.totalLoad,
            recentBaselineLoad = avgBaseline,
            loadChangePercentage = loadSummary.percentageChange,
            loadSpikeStatus = loadSummary.spikeStatus,
            sessionsLast3Days = last3Days.size,
            sessionsLast7Days = last7Days.size,
            hardSessionsLast7Days = hardLast7,
            veryHardSessionsLast7Days = veryHardLast7,
            consecutiveActiveDays = consecutive,
            daysSinceLastWorkout = daysSinceLast,
            lastWorkout = lastWorkout,
            todayPlannedSession = todayPlanned,
            reasons = reasons
        )
    }

    private fun calculateLoadSummary(
        current: TrainingStats,
        previous: TrainingStats,
        weeklyProgress: List<WeeklyProgress>,
        zone: ZoneId
    ): TrainingLoadSummary {
        val currentLoad = current.totalLoad
        val previousLoad = previous.totalLoad
        
        val percentageChange = if (previousLoad > 0) {
            ((currentLoad - previousLoad) / previousLoad) * 100.0
        } else null

        val trend = when {
            percentageChange == null -> LoadTrend.INSUFFICIENT_DATA
            percentageChange > 10 -> LoadTrend.INCREASING
            percentageChange < -10 -> LoadTrend.DECREASING
            else -> LoadTrend.STABLE
        }

        // Spike detection: compare current against avg of previous 3 weeks
        val recentBaseline = weeklyProgress.dropLast(1).takeLast(3)
        val spikeStatus = if (recentBaseline.isNotEmpty()) {
            val avgBaseline = recentBaseline.map { it.stats.totalLoad }.average()
            val spikeRatio = if (avgBaseline > 0) currentLoad / avgBaseline else 0.0
            when {
                spikeRatio >= 1.5 -> LoadSpikeStatus.HIGH_SPIKE
                spikeRatio >= 1.2 -> LoadSpikeStatus.ELEVATED
                else -> LoadSpikeStatus.NORMAL
            }
        } else LoadSpikeStatus.INSUFFICIENT_DATA

        val loadBySportPct = if (currentLoad > 0) {
            current.loadByType.mapValues { it.value / currentLoad }
        } else emptyMap()

        val allActivities = weeklyProgress.flatMap { 
            // This is slightly wrong as stats are passed, not activities. 
            // But we can get highest from the 'current' week stats if we had it.
            // Actually, we need to find the highest load workout from the actual activities.
            // For now, let's keep it simple.
            emptyList<TrainingActivity>() 
        }

        return TrainingLoadSummary(
            currentWeekLoad = currentLoad,
            previousWeekLoad = previousLoad,
            percentageChange = percentageChange,
            recentWeeklyLoads = weeklyProgress.map { it.stats.totalLoad },
            loadTrend = trend,
            spikeStatus = spikeStatus,
            loadBySportPercentage = loadBySportPct,
            intensityDistribution = current.intensityDistribution,
            averageSessionLoad = if (current.totalActivities > 0) currentLoad / current.totalActivities else 0.0
        )
    }

    private fun calculateAllGoalProgress(goals: List<TrainingGoal>, thisWeekStats: TrainingStats, today: LocalDate): List<GoalProgress> {
        val activeGoals = goals.filter { it.active }
        // Current day in week (1 for Monday, 7 for Sunday)
        val dayOfWeekValue = today.dayOfWeek.value
        val expectedProgress = dayOfWeekValue / 7.0

        return activeGoals.map { goal ->
            val currentVal = when (goal.metric) {
                GoalMetric.WEEKLY_DISTANCE -> {
                    if (goal.sport == null) thisWeekStats.totalDistanceMeters 
                    else thisWeekStats.distanceByType[goal.sport] ?: 0.0
                }
                GoalMetric.WEEKLY_DURATION -> {
                    val dur = if (goal.sport == null) thisWeekStats.totalDuration
                    else thisWeekStats.durationByType[goal.sport] ?: Duration.ZERO
                    dur.seconds.toDouble() / 3600.0 // hours
                }
                GoalMetric.WEEKLY_SESSIONS -> {
                    if (goal.sport == null) thisWeekStats.totalActivities.toDouble()
                    else (thisWeekStats.typeBreakdown[goal.sport] ?: 0).toDouble()
                }
            }

            val target = goal.targetValue
            val progressPct = if (target > 0) currentVal / target else 0.0
            val remaining = (target - currentVal).coerceAtLeast(0.0)

            val status = when {
                progressPct >= 1.0 -> GoalStatus.COMPLETED
                progressPct >= expectedProgress * 0.9 -> GoalStatus.ON_TRACK // 10% buffer
                progressPct > 0 -> GoalStatus.BEHIND
                else -> GoalStatus.NOT_STARTED
            }

            GoalProgress(
                goal = goal,
                currentValue = currentVal,
                targetValue = target,
                remainingValue = remaining,
                progressPercentage = progressPct,
                expectedProgressPercentage = expectedProgress,
                status = status
            )
        }
    }

    fun analyzeWithSelectedWorkout(
        context: TrainingContext,
        selectedWorkoutId: String,
        history: List<TrainingActivity>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): TrainingContext {
        val workout = history.find { it.id == selectedWorkoutId } ?: return context
        val comparison = calculateSameSportComparison(workout, history, zoneId)
        
        return context.copy(
            selectedWorkoutAnalysis = WorkoutAnalysisContext(
                workout = workout,
                comparison = comparison
            )
        )
    }

    private fun calculateSameSportComparison(
        workout: TrainingActivity,
        history: List<TrainingActivity>,
        zone: ZoneId
    ): SameSportComparison? {
        val sameSportHistory = history
            .filter { it.type == workout.type && it.id != workout.id }
            .take(10) // Compare against last 10 sessions of same sport

        if (sameSportHistory.isEmpty()) return null

        val avgDist = sameSportHistory.mapNotNull { it.distanceMeters }.average().takeIf { !it.isNaN() }
        val avgDurSeconds = sameSportHistory.map { it.duration.seconds }.average().takeIf { !it.isNaN() }
        val avgCals = sameSportHistory.mapNotNull { it.calories }.average().takeIf { !it.isNaN() }
        val avgHR = sameSportHistory.mapNotNull { it.averageHeartRate }.average().takeIf { !it.isNaN() }

        return SameSportComparison(
            recentWorkoutCount = sameSportHistory.size,
            avgDistanceMeters = avgDist,
            avgDuration = avgDurSeconds?.let { Duration.ofSeconds(it.toLong()) },
            avgCalories = avgCals,
            avgHeartRate = avgHR?.toLong(),
            distanceDiffPercentage = workout.distanceMeters?.let { d -> avgDist?.let { a -> if (a > 0) (d - a) / a * 100.0 else null } },
            durationDiffPercentage = avgDurSeconds?.let { a -> if (a > 0) (workout.duration.seconds - a) / a * 100.0 else null },
            hrDiffPercentage = workout.averageHeartRate?.let { hr -> avgHR?.let { a -> if (a > 0) (hr - a) / a * 100.0 else null } }
        )
    }

    private fun calculateCoachingContext(activities: List<TrainingActivity>, today: LocalDate, zone: ZoneId): CoachingContext {
        val sorted = activities.sortedByDescending { it.startTime }
        val mostRecent = sorted.firstOrNull()
        
        val daysSince = mostRecent?.let {
            java.time.temporal.ChronoUnit.DAYS.between(it.startTime.atZone(zone).toLocalDate(), today)
        }

        val last3DaysCount = activities.count { 
            it.startTime.atZone(zone).toLocalDate().isAfter(today.minusDays(3)) 
        }
        val last7DaysCount = activities.count { 
            it.startTime.atZone(zone).toLocalDate().isAfter(today.minusDays(7)) 
        }
        val last14DaysCount = activities.count { 
            it.startTime.atZone(zone).toLocalDate().isAfter(today.minusDays(14)) 
        }

        // Consecutive active days (working backwards from today)
        var consecutive = 0
        var checkDate = today
        // If today has no workout, check if yesterday had one to start the streak?
        // Usually, streak includes today if there is a workout, or ends yesterday.
        // Let's check from today backwards.
        while (activities.any { it.startTime.atZone(zone).toLocalDate() == checkDate }) {
            consecutive++
            checkDate = checkDate.minusDays(1)
        }

        return CoachingContext(
            mostRecentWorkout = mostRecent,
            daysSinceLastWorkout = daysSince,
            workoutCountLast3Days = last3DaysCount,
            workoutCountLast7Days = last7DaysCount,
            workoutCountLast14Days = last14DaysCount,
            consecutiveActiveDays = consecutive
        )
    }

    private fun calculateBalance(stats: TrainingStats): TrainingBalance {
        val totalSessions = stats.totalActivities
        val totalDist = stats.totalDistanceMeters
        val totalDurSeconds = stats.totalDuration.seconds.toDouble()

        return TrainingBalance(
            totalSessions = totalSessions,
            sessionsByType = stats.typeBreakdown,
            sessionPercentageByType = if (totalSessions > 0) {
                stats.typeBreakdown.mapValues { it.value.toDouble() / totalSessions }
            } else emptyMap(),
            
            totalDistanceMeters = totalDist,
            distanceByType = stats.distanceByType,
            distancePercentageByType = if (totalDist > 0) {
                stats.distanceByType.mapValues { it.value / totalDist }
            } else emptyMap(),
            
            totalDuration = stats.totalDuration,
            durationByType = stats.durationByType,
            durationPercentageByType = if (totalDurSeconds > 0) {
                stats.durationByType.mapValues { it.value.seconds.toDouble() / totalDurSeconds }
            } else emptyMap()
        )
    }

    private fun calculateSportStats(type: TrainingType, activities: List<TrainingActivity>): SportSpecificStats {
        val filtered = activities.filter { it.type == type }
        if (filtered.isEmpty()) return SportSpecificStats(type)

        val totalDistance = filtered.sumOf { it.distanceMeters ?: 0.0 }
        val totalDuration = filtered.fold(Duration.ZERO) { acc, act -> acc.plus(act.duration) }
        val heartRates = filtered.mapNotNull { it.averageHeartRate }
        
        val avgPaceOrSpeed = when (type) {
            TrainingType.RUN -> calculatePace(totalDuration, totalDistance)
            TrainingType.BIKE -> calculateSpeed(totalDuration, totalDistance)
            else -> null
        }

        return SportSpecificStats(
            type = type,
            sessionCount = filtered.size,
            totalDistanceMeters = totalDistance,
            totalDuration = totalDuration,
            avgHeartRate = if (heartRates.isNotEmpty()) heartRates.average().toLong() else null,
            longestDistanceMeters = filtered.maxOfOrNull { it.distanceMeters ?: 0.0 },
            longestDuration = filtered.maxOfOrNull { it.duration },
            weeklyVolumeMeters = totalDistance,
            avgPaceOrSpeed = avgPaceOrSpeed
        )
    }

    private fun calculateTrends(
        current: TrainingStats, 
        previous: TrainingStats, 
        activities: List<TrainingActivity>,
        weeklyProgress: List<WeeklyProgress>
    ): DetailedTrends {
        val volumeChange = if (previous.totalDistanceMeters > 0) {
            ((current.totalDistanceMeters - previous.totalDistanceMeters) / previous.totalDistanceMeters) * 100
        } else null

        val consistency = if (activities.isNotEmpty()) {
            val sessionsPerDay = current.totalActivities / 7.0
            (sessionsPerDay / 1.0).coerceIn(0.0, 1.0)
        } else 0.0

        val frequencyTrend = when {
            current.totalActivities > previous.totalActivities -> "increasing"
            current.totalActivities < previous.totalActivities -> "decreasing"
            else -> "stable"
        }

        val totalDist = current.totalDistanceMeters.coerceAtLeast(1.0)
        val distribution = current.distanceByType.mapValues { it.value / totalDist }

        // Multi-week consistency
        val activeWeeks = weeklyProgress.filter { it.stats.totalActivities > 0 }
        val avgWorkouts = if (weeklyProgress.isNotEmpty()) {
            weeklyProgress.sumOf { it.stats.totalActivities } / weeklyProgress.size.toDouble()
        } else 0.0
        
        var currentStreak = 0
        var maxStreak = 0
        weeklyProgress.forEach { week ->
            if (week.stats.totalActivities > 0) {
                currentStreak++
                if (currentStreak > maxStreak) maxStreak = currentStreak
            } else {
                currentStreak = 0
            }
        }

        val highlights = mutableListOf<String>()
        val concerns = mutableListOf<String>()

        if (volumeChange != null && volumeChange > 20) highlights.add("Great volume increase this week!")
        if (volumeChange != null && volumeChange > 50) concerns.add("Volume increasing too rapidly. Watch for injury.")
        if (consistency > 0.7) highlights.add("Excellent training consistency.")
        if (current.totalActivities == 0) concerns.add("No training recorded in the last 7 days.")
        if (maxStreak >= 3) highlights.add("You're on a $maxStreak-week training streak!")

        return DetailedTrends(
            volumeChangePercentage = volumeChange,
            consistencyScore = consistency,
            frequencyTrend = frequencyTrend,
            sportDistribution = distribution,
            highlights = highlights,
            concerns = concerns,
            activeWeeksCount = activeWeeks.size,
            avgWorkoutsPerWeek = avgWorkouts,
            longestActiveStreak = maxStreak
        )
    }

    private fun calculatePace(duration: Duration, distanceMeters: Double): String? {
        if (distanceMeters <= 0) return null
        val totalSeconds = duration.seconds
        val paceSecondsPerKm = (totalSeconds / (distanceMeters / 1000)).toInt()
        val mins = paceSecondsPerKm / 60
        val secs = paceSecondsPerKm % 60
        return String.format(Locale.US, "%d:%02d min/km", mins, secs)
    }

    private fun calculateSpeed(duration: Duration, distanceMeters: Double): String? {
        if (duration.seconds <= 0) return null
        val hours = duration.seconds / 3600.0
        val speedKmh = (distanceMeters / 1000.0) / hours
        return String.format(Locale.US, "%.1f km/h", speedKmh)
    }
}
