package com.example.unif1.data.training

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TrainingPlanAnalyzerTest {

    private val analyzer = TrainingPlanAnalyzer()

    @Test
    fun `calculateGoalAlignment handles distance goal correctly`() {
        val weekStart = LocalDate.of(2026, 9, 7)
        val sessions = listOf(
            PlannedTrainingSession("s1", weekStart.plusDays(1), TrainingType.RUN, "T1", "D1", 5000.0, 1800, "Easy"),
            PlannedTrainingSession("s2", weekStart.plusDays(3), TrainingType.RUN, "T2", "D2", 7000.0, 2400, "Moderate")
        )
        val plan = TrainingPlan("p1", weekStart, weekStart.plusDays(6), sessions)
        
        val goal = TrainingGoal("g1", GoalMetric.WEEKLY_DISTANCE, TrainingType.RUN, 20000.0)
        
        val alignment = analyzer.calculateGoalAlignment(plan, listOf(goal))
        
        assertEquals(1, alignment.size)
        assertEquals(12000.0, alignment[0].plannedValue, 0.1)
        assertEquals(0.6, alignment[0].alignmentPercentage, 0.01)
    }

    @Test
    fun `calculatePlanStats summarizes sports correctly`() {
        val weekStart = LocalDate.of(2026, 9, 7)
        val sessions = listOf(
            PlannedTrainingSession("s1", weekStart.plusDays(1), TrainingType.RUN, "T1", "D1", 5000.0, 1800, "Easy"),
            PlannedTrainingSession("s2", weekStart.plusDays(2), TrainingType.BIKE, "T2", "D2", 20000.0, 3600, "Moderate")
        )
        val plan = TrainingPlan("p1", weekStart, weekStart.plusDays(6), sessions)
        
        val stats = analyzer.calculatePlanStats(plan)
        
        assertEquals(2, stats.totalActivities)
        assertEquals(25000.0, stats.totalDistanceMeters, 0.1)
        assertEquals(5000.0, stats.distanceByType[TrainingType.RUN]!!, 0.1)
        assertEquals(20000.0, stats.distanceByType[TrainingType.BIKE]!!, 0.1)
    }
}
