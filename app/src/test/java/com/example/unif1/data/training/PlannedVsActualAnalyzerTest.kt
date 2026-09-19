package com.example.unif1.data.training

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PlannedVsActualAnalyzerTest {

    private val analyzer = PlannedVsActualAnalyzer()
    private val zone = ZoneId.systemDefault()

    @Test
    fun `analyze calculates perfect adherence correctly`() {
        val today = LocalDate.of(2026, 9, 8) // Tuesday
        val weekStart = LocalDate.of(2026, 9, 7)
        
        val sessions = listOf(
            PlannedTrainingSession("s1", weekStart, TrainingType.RUN, "T1", "D1", 5000.0, 1800),
            PlannedTrainingSession("s2", today, TrainingType.BIKE, "T2", "D2", 20000.0, 3600)
        )
        val plan = TrainingPlan("p1", weekStart, weekStart.plusDays(6), sessions)
        
        val actuals = listOf(
            createActivity(5000.0, TrainingType.RUN, weekStart.atStartOfDay(zone).toInstant()),
            createActivity(20000.0, TrainingType.BIKE, today.atStartOfDay(zone).toInstant())
        )
        
        val summary = analyzer.analyze(plan, actuals, today, zone)
        
        assertEquals(1.0, summary.adherencePercentage, 0.01)
        assertEquals(2, summary.completedSessionCount)
        assertEquals(0, summary.missedSessionCount)
    }

    @Test
    fun `analyze detects missed past session`() {
        val today = LocalDate.of(2026, 9, 8) // Tuesday
        val weekStart = LocalDate.of(2026, 9, 7)
        
        val sessions = listOf(
            PlannedTrainingSession("s1", weekStart, TrainingType.RUN, "T1", "D1", 5000.0, 1800), // Monday
            PlannedTrainingSession("s2", today, TrainingType.BIKE, "T2", "D2", 20000.0, 3600) // Tuesday
        )
        val plan = TrainingPlan("p1", weekStart, weekStart.plusDays(6), sessions)
        
        // No actuals
        val summary = analyzer.analyze(plan, emptyList(), today, zone)
        
        assertEquals(1, summary.missedSessionCount) // Monday is missed
        assertEquals(1, summary.sportComparisons[TrainingType.RUN]?.plannedSessions)
        assertEquals(0, summary.sportComparisons[TrainingType.RUN]?.actualSessions)
    }

    @Test
    fun `analyze handles multiple sessions on same day correctly`() {
        val today = LocalDate.of(2026, 9, 8) // Tuesday
        val weekStart = LocalDate.of(2026, 9, 7)
        
        // Two run sessions on Monday
        val sessions = listOf(
            PlannedTrainingSession("s1", weekStart, TrainingType.RUN, "T1", "D1", 5000.0, 1800),
            PlannedTrainingSession("s2", weekStart, TrainingType.RUN, "T2", "D2", 10000.0, 3600)
        )
        val plan = TrainingPlan("p1", weekStart, weekStart.plusDays(6), sessions)
        
        // Only one actual run on Monday
        val actuals = listOf(
            createActivity(5000.0, TrainingType.RUN, weekStart.atStartOfDay(zone).toInstant())
        )
        
        val summary = analyzer.analyze(plan, actuals, today, zone)
        
        assertEquals(2, summary.plannedSessionCount)
        assertEquals(1, summary.completedSessionCount)
        assertEquals(1, summary.missedSessionCount)
    }

    private fun createActivity(distance: Double, type: TrainingType, time: Instant): TrainingActivity {
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
