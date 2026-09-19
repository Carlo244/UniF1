package com.example.unif1.data.training

import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

class PlannedVsActualAnalyzer {

    fun analyze(
        plan: TrainingPlan,
        actualActivities: List<TrainingActivity>,
        today: LocalDate,
        zone: ZoneId = ZoneId.systemDefault()
    ): PlannedVsActualSummary {
        val usedActivityIds = mutableSetOf<String>()

        val sessionsWithActuals = plan.sessions.map { session ->
            val matchingActivity = if (session.status != SessionStatus.REST) {
                actualActivities.find { activity ->
                    val activityDate = activity.startTime.atZone(zone).toLocalDate()
                    activityDate == session.date && activity.type == session.sport && !usedActivityIds.contains(activity.id)
                }
            } else null
            
            matchingActivity?.let { usedActivityIds.add(it.id) }

            val effectiveStatus = when {
                session.status == SessionStatus.REST -> SessionStatus.REST
                session.status == SessionStatus.COMPLETED || matchingActivity != null -> SessionStatus.COMPLETED
                session.status == SessionStatus.SKIPPED -> SessionStatus.SKIPPED
                session.date.isBefore(today) -> SessionStatus.MISSED
                else -> SessionStatus.PLANNED
            }
            
            session to (matchingActivity to effectiveStatus)
        }

        val plannedTrainingSessions = sessionsWithActuals.filter { it.first.status != SessionStatus.REST }
        
        val plannedCount = plannedTrainingSessions.size
        val completedCount = plannedTrainingSessions.count { it.second.second == SessionStatus.COMPLETED }
        val skippedCount = plannedTrainingSessions.count { it.second.second == SessionStatus.SKIPPED }
        val missedCount = plannedTrainingSessions.count { it.second.second == SessionStatus.MISSED }

        val adherence = if (plannedCount > 0) completedCount.toDouble() / plannedCount else 0.0

        val plannedDist = plannedTrainingSessions.sumOf { it.first.targetDistanceMeters ?: 0.0 }
        val actualDist = plannedTrainingSessions.sumOf { it.second.first?.distanceMeters ?: 0.0 }
        val distAdherence = if (plannedDist > 0) actualDist / plannedDist else 0.0

        val plannedDur = plannedTrainingSessions.sumOf { it.first.targetDurationSeconds ?: 0L }
        val actualDur = plannedTrainingSessions.sumOf { it.second.first?.duration?.seconds ?: 0L }
        val durAdherence = if (plannedDur > 0) actualDur.toDouble() / plannedDur else 0.0

        val sportComparisons = TrainingType.entries.associateWith { type ->
            val sportSessions = plannedTrainingSessions.filter { it.first.sport == type }
            val sPlannedDist = sportSessions.sumOf { it.first.targetDistanceMeters ?: 0.0 }
            val sActualDist = sportSessions.sumOf { it.second.first?.distanceMeters ?: 0.0 }
            val sPlannedDur = sportSessions.sumOf { it.first.targetDurationSeconds ?: 0L }
            val sActualDur = sportSessions.sumOf { it.second.first?.duration?.seconds ?: 0L }
            
            SportPerformanceComparison(
                type = type,
                plannedSessions = sportSessions.size,
                actualSessions = sportSessions.count { it.second.second == SessionStatus.COMPLETED },
                plannedDistance = sPlannedDist,
                actualDistance = sActualDist,
                distanceAdherence = if (sPlannedDist > 0) sActualDist / sPlannedDist else 0.0,
                plannedDuration = Duration.ofSeconds(sPlannedDur),
                actualDuration = Duration.ofSeconds(sActualDur),
                durationAdherence = if (sPlannedDur > 0) sActualDur.toDouble() / sPlannedDur else 0.0,
                sessionAdherence = if (sportSessions.isNotEmpty()) {
                    sportSessions.count { it.second.second == SessionStatus.COMPLETED }.toDouble() / sportSessions.size
                } else 0.0
            )
        }

        return PlannedVsActualSummary(
            weekStartDate = plan.weekStartDate,
            weekEndDate = plan.weekEndDate,
            plannedSessionCount = plannedCount,
            completedSessionCount = completedCount,
            skippedSessionCount = skippedCount,
            missedSessionCount = missedCount,
            adherencePercentage = adherence,
            plannedDistanceMeters = plannedDist,
            actualDistanceMeters = actualDist,
            distanceAdherencePercentage = distAdherence,
            plannedDurationSeconds = plannedDur,
            actualDurationSeconds = actualDur,
            durationAdherencePercentage = durAdherence,
            sportComparisons = sportComparisons
        )
    }
}
