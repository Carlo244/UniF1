package com.example.unif1.data.training

import java.time.Duration

class TrainingPlanAnalyzer {

    fun calculateGoalAlignment(plan: TrainingPlan, goals: List<TrainingGoal>): List<PlanGoalAlignment> {
        val activeGoals = goals.filter { it.active }
        
        return activeGoals.map { goal ->
            val plannedValue = when (goal.metric) {
                GoalMetric.WEEKLY_DISTANCE -> {
                    plan.sessions
                        .filter { it.sport == goal.sport || goal.sport == null }
                        .sumOf { it.targetDistanceMeters ?: 0.0 }
                }
                GoalMetric.WEEKLY_DURATION -> {
                    plan.sessions
                        .filter { it.sport == goal.sport || goal.sport == null }
                        .sumOf { it.targetDurationSeconds ?: 0L }
                        .toDouble() / 3600.0 // hours
                }
                GoalMetric.WEEKLY_SESSIONS -> {
                    plan.sessions
                        .count { it.sport == goal.sport || goal.sport == null }
                        .toDouble()
                }
            }

            val alignment = if (goal.targetValue > 0) plannedValue / goal.targetValue else 0.0
            
            PlanGoalAlignment(
                goal = goal,
                plannedValue = plannedValue,
                alignmentPercentage = alignment
            )
        }
    }

    fun calculatePlanStats(plan: TrainingPlan): TrainingStats {
        val totalSessions = plan.sessions.count { it.status != SessionStatus.REST }
        val totalDistance = plan.sessions.sumOf { it.targetDistanceMeters ?: 0.0 }
        val totalDurationSeconds = plan.sessions.sumOf { it.targetDurationSeconds ?: 0L }
        
        val typeGroups = plan.sessions.groupBy { it.sport }
        val typeBreakdown = typeGroups.mapValues { it.value.count { s -> s.status != SessionStatus.REST } }
        val distanceByType = typeGroups.mapValues { (_, sessions) -> 
            sessions.sumOf { it.targetDistanceMeters ?: 0.0 } 
        }
        val durationByType = typeGroups.mapValues { (_, sessions) ->
            Duration.ofSeconds(sessions.sumOf { it.targetDurationSeconds ?: 0L })
        }

        return TrainingStats(
            totalActivities = totalSessions,
            totalDistanceMeters = totalDistance,
            totalDuration = Duration.ofSeconds(totalDurationSeconds),
            totalCalories = 0.0, // AI won't usually plan calories
            avgHeartRate = null,
            typeBreakdown = typeBreakdown,
            distanceByType = distanceByType,
            durationByType = durationByType
        )
    }
}
