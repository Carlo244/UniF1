package com.example.unif1.data.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import com.example.unif1.data.health.HealthConnectRepository

class TrainingAnalyzerTest {

    private val repository = TrainingRepository(HealthConnectRepository(null))
    private val analyzer = TrainingAnalyzer(repository)

    private val fixedToday = LocalDate.of(2026, 9, 3)

    @Test
    fun `analyze calculates volume change correctly`() {
        val last7Days = listOf(
            createActivity(1000.0, TrainingType.RUN)
        )
        val previous7Days = listOf(
            createActivity(500.0, TrainingType.RUN)
        )

        val context = analyzer.analyze(emptyList(), last7Days, previous7Days, emptyList(), emptyList(), null, ZoneId.systemDefault(), fixedToday)

        assertEquals(100.0, context.trends.volumeChangePercentage!!, 0.01)
    }

    @Test
    fun `analyze detects sport imbalance`() {
        val last7Days = listOf(
            createActivity(1000.0, TrainingType.RUN),
            createActivity(2000.0, TrainingType.RUN)
        )

        val context = analyzer.analyze(emptyList(), last7Days, emptyList(), emptyList(), emptyList(), null, ZoneId.systemDefault(), fixedToday)

        assertTrue(context.trends.sportDistribution[TrainingType.RUN] == 1.0)
        assertTrue(context.runStats.sessionCount == 2)
        assertTrue(context.bikeStats.sessionCount == 0)
    }

    @Test
    fun `analyze calculates multi-week progress correctly`() {
        val zone = ZoneId.systemDefault()
        val now = fixedToday.atTime(LocalTime.NOON).atZone(zone).toInstant()
        
        // Activity this week
        val act1 = createActivity(1000.0, TrainingType.RUN, now)
        
        // Activity last week (8 days ago)
        val act2 = createActivity(2000.0, TrainingType.BIKE, now.minus(Duration.ofDays(8)))
        
        val last30Days = listOf(act1, act2)
        
        val context = analyzer.analyze(emptyList(), emptyList(), emptyList(), last30Days, emptyList(), null, zone, fixedToday)
        
        assertEquals(4, context.weeklyProgress.size)
        assertEquals(1000.0, context.weeklyProgress[3].stats.totalDistanceMeters, 0.1)
        assertEquals(2000.0, context.weeklyProgress[2].stats.totalDistanceMeters, 0.1)
    }

    @Test
    fun `analyze calculates training balance correctly`() {
        val zone = ZoneId.systemDefault()
        val now = fixedToday.atTime(LocalTime.NOON).atZone(zone).toInstant()
        
        val last30Days = listOf(
            createActivity(1000.0, TrainingType.RUN, now), // 30 min
            createActivity(2000.0, TrainingType.BIKE, now.minus(Duration.ofMinutes(40))), // 30 min
            createActivity(500.0, TrainingType.SWIM, now.minus(Duration.ofMinutes(80))) // 30 min
        )
        
        val context = analyzer.analyze(emptyList(), emptyList(), emptyList(), last30Days, emptyList(), null, zone, fixedToday)
        
        val balance = context.overallBalance
        assertEquals(3, balance.totalSessions)
        assertEquals(1.0/3.0, balance.sessionPercentageByType[TrainingType.RUN]!!, 0.01)
        assertEquals(1.0/3.0, balance.sessionPercentageByType[TrainingType.BIKE]!!, 0.01)
        assertEquals(1.0/3.0, balance.sessionPercentageByType[TrainingType.SWIM]!!, 0.01)
    }

    @Test
    fun `analyze calculates coaching context correctly`() {
        val zone = ZoneId.systemDefault()
        val now = fixedToday.atTime(LocalTime.NOON).atZone(zone).toInstant()
        
        val last30Days = listOf(
            createActivity(1000.0, TrainingType.RUN, now), // Today
            createActivity(1000.0, TrainingType.RUN, now.minus(Duration.ofDays(1))), // Yesterday
            createActivity(1000.0, TrainingType.RUN, now.minus(Duration.ofDays(2))) // 2 days ago
        )
        
        val context = analyzer.analyze(emptyList(), emptyList(), emptyList(), last30Days, emptyList(), null, zone, fixedToday)
        
        val cc = context.coachingContext
        assertEquals(TrainingType.RUN, cc.mostRecentWorkout?.type)
        assertEquals(0L, cc.daysSinceLastWorkout)
        assertEquals(3, cc.workoutCountLast3Days)
        assertEquals(3, cc.workoutCountLast7Days)
        assertEquals(3, cc.consecutiveActiveDays)
    }

    @Test
    fun `analyze calculates distance goal correctly`() {
        val zone = ZoneId.systemDefault()
        val now = fixedToday.atTime(LocalTime.NOON).atZone(zone).toInstant()
        
        // 10km run this week
        val last7Days = listOf(
            createActivity(10000.0, TrainingType.RUN, now)
        )
        
        // Goal: 20km run per week
        val goal = TrainingGoal(
            id = "g1",
            metric = GoalMetric.WEEKLY_DISTANCE,
            sport = TrainingType.RUN,
            targetValue = 20000.0
        )
        
        val context = analyzer.analyze(emptyList(), last7Days, emptyList(), last7Days, listOf(goal), null, zone, fixedToday)
        
        assertEquals(1, context.goalProgress.size)
        val progress = context.goalProgress[0]
        assertEquals(10000.0, progress.currentValue, 0.1)
        assertEquals(0.5, progress.progressPercentage, 0.01)
        assertEquals(10000.0, progress.remainingValue, 0.1)
    }

    @Test
    fun `analyze calculates session goal correctly`() {
        val zone = ZoneId.systemDefault()
        val now = fixedToday.atTime(LocalTime.NOON).atZone(zone).toInstant()
        
        // 2 runs this week
        val last7Days = listOf(
            createActivity(1000.0, TrainingType.RUN, now),
            createActivity(1000.0, TrainingType.RUN, now.minus(Duration.ofHours(1)))
        )
        
        // Goal: 3 runs per week
        val goal = TrainingGoal(
            id = "g2",
            metric = GoalMetric.WEEKLY_SESSIONS,
            sport = TrainingType.RUN,
            targetValue = 3.0
        )
        
        val context = analyzer.analyze(emptyList(), last7Days, emptyList(), last7Days, listOf(goal), null, zone, fixedToday)
        
        assertEquals(1, context.goalProgress.size)
        val progress = context.goalProgress[0]
        assertEquals(2.0, progress.currentValue, 0.1)
        assertEquals(0.66, progress.progressPercentage, 0.01)
        assertEquals(1.0, progress.remainingValue, 0.1)
    }

    @Test
    fun `analyze calculates readiness correctly for high recent load`() {
        val zone = ZoneId.systemDefault()
        val now = fixedToday.atTime(LocalTime.NOON).atZone(zone).toInstant()
        
        // This week: 5 sessions, high load (multiplied by very hard)
        val thisWeek = (0 until 5).map { i ->
            createActivity(5000.0, TrainingType.RUN, now.minus(Duration.ofDays(i.toLong())))
                .copy(duration = Duration.ofMinutes(60), averageHeartRate = 180L)
        }.map { analyzer.enrichActivity(it) }
        
        // Previous week: 1 session, low load
        val lastWeek = listOf(
            createActivity(1000.0, TrainingType.RUN, now.minus(Duration.ofDays(8)))
                .copy(duration = Duration.ofMinutes(20), averageHeartRate = 110L)
        ).map { analyzer.enrichActivity(it) }
        
        val last30Days = thisWeek + lastWeek
        
        val context = analyzer.analyze(emptyList(), thisWeek, lastWeek, last30Days, emptyList(), null, zone, fixedToday)
        
        val tr = context.readiness
        assertEquals(TrainingReadinessStatus.RECOVERY_FOCUS, tr.status)
        assertTrue(tr.score < 60)
        assertTrue(tr.reasons.any { it.contains("Significant training load spike") })
        assertTrue(tr.reasons.any { it.contains("very hard session") })
        assertTrue(tr.reasons.any { it.contains("training streak") })
    }

    @Test
    fun `analyze calculates readiness correctly for normal training`() {
        val zone = ZoneId.systemDefault()
        // Use a fixed Friday so that Day 0, -2, -4 are in the same calendar week starting Monday
        val friday = LocalDate.of(2026, 9, 4) 
        val now = friday.atTime(LocalTime.NOON).atZone(zone).toInstant()
        
        // Steady training
        val last30Days = (0 until 10).map { i ->
            createActivity(5000.0, TrainingType.RUN, now.minus(Duration.ofDays(i.toLong() * 2)))
                .copy(duration = Duration.ofMinutes(30), averageHeartRate = 130L)
        }.map { analyzer.enrichActivity(it) }
        
        val thisWeek = last30Days.take(3)
        val lastWeek = last30Days.drop(3).take(3)
        
        val context = analyzer.analyze(emptyList(), thisWeek, lastWeek, last30Days, emptyList(), null, zone, friday)
        
        val tr = context.readiness
        assertEquals(TrainingReadinessStatus.READY, tr.status)
        assertTrue(tr.score >= 80)
        assertTrue(tr.reasons.isEmpty())
    }

    @Test
    fun `analyze calculates readiness correctly for insufficient data`() {
        val zone = ZoneId.systemDefault()
        val now = fixedToday.atTime(LocalTime.NOON).atZone(zone).toInstant()
        
        // Only 1 activity
        val act = createActivity(5000.0, TrainingType.RUN, now)
        val last30Days = listOf(analyzer.enrichActivity(act))
        
        val context = analyzer.analyze(emptyList(), last30Days, emptyList(), last30Days, emptyList(), null, zone, fixedToday)
        
        assertEquals(TrainingReadinessStatus.INSUFFICIENT_DATA, context.readiness.status)
    }

    @Test
    fun `analyze calculates workout comparison correctly`() {
        val zone = ZoneId.systemDefault()
        val now = fixedToday.atTime(LocalTime.NOON).atZone(zone).toInstant()
        
        // Target workout: 10km run
        val target = createActivity(10000.0, TrainingType.RUN, now)
        
        // History: 5km run average
        val history = listOf(
            target,
            createActivity(5000.0, TrainingType.RUN, now.minus(Duration.ofDays(1))),
            createActivity(5000.0, TrainingType.RUN, now.minus(Duration.ofDays(2))),
            createActivity(2000.0, TrainingType.BIKE, now.minus(Duration.ofDays(3))) // Should be ignored
        )
        
        val initialContext = analyzer.analyze(emptyList(), emptyList(), emptyList(), history, emptyList(), null, zone, fixedToday)
        val resultContext = analyzer.analyzeWithSelectedWorkout(initialContext, target.id, history, zone)
        
        val comparison = resultContext.selectedWorkoutAnalysis?.comparison
        assertTrue(comparison != null)
        assertEquals(2, comparison!!.recentWorkoutCount)
        assertEquals(5000.0, comparison.avgDistanceMeters!!, 0.1)
        assertEquals(100.0, comparison.distanceDiffPercentage!!, 0.1) // 10k vs 5k = +100%
    }

    @Test
    fun `enrichActivity calculates intensity and load correctly`() {
        val activity = createActivity(5000.0, TrainingType.RUN, fixedToday.atTime(LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant())
            .copy(duration = Duration.ofMinutes(30), averageHeartRate = 160L)
            
        val enriched = analyzer.enrichActivity(activity)
        
        // HR 160 -> HARD (146-170)
        assertEquals(Intensity.HARD, enriched.intensity)
        // Duration 30m, Intensity HARD (multiplier 4) -> 30 * 4 = 120
        assertEquals(120.0, enriched.calculatedLoad, 0.1)
    }

    @Test
    fun `analyze calculates load trend and spike correctly`() {
        val zone = ZoneId.systemDefault()
        val now = fixedToday.atTime(LocalTime.NOON).atZone(zone).toInstant()
        
        // This week: 2 sessions, load 240
        val thisWeek = listOf(
            createActivity(5000.0, TrainingType.RUN, now).copy(duration = Duration.ofMinutes(30), averageHeartRate = 160L),
            createActivity(5000.0, TrainingType.RUN, now.minus(Duration.ofHours(1))).copy(duration = Duration.ofMinutes(30), averageHeartRate = 160L)
        ).map { analyzer.enrichActivity(it) }
        
        // Previous week: 1 session, load 120
        val lastWeek = listOf(
            createActivity(5000.0, TrainingType.RUN, now.minus(Duration.ofDays(8))).copy(duration = Duration.ofMinutes(30), averageHeartRate = 160L)
        ).map { analyzer.enrichActivity(it) }
        
        val last30Days = thisWeek + lastWeek
        
        val context = analyzer.analyze(emptyList(), thisWeek, lastWeek, last30Days, emptyList(), null, zone, fixedToday)
        
        val ls = context.loadSummary
        assertEquals(240.0, ls.currentWeekLoad, 0.1)
        assertEquals(120.0, ls.previousWeekLoad, 0.1)
        assertEquals(100.0, ls.percentageChange!!, 0.1)
        assertEquals(LoadTrend.INCREASING, ls.loadTrend)
        assertEquals(LoadSpikeStatus.HIGH_SPIKE, ls.spikeStatus)
    }

    private fun createActivity(distance: Double, type: TrainingType, time: Instant = Instant.now()): TrainingActivity {
        return TrainingActivity(
            id = "test-${time.toEpochMilli()}",
            type = type,
            startTime = time,
            endTime = time.plus(Duration.ofMinutes(30)),
            duration = Duration.ofMinutes(30),
            distanceMeters = distance
        )
    }
}
