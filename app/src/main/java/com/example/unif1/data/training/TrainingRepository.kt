package com.example.unif1.data.training

import com.example.unif1.data.health.HealthConnectRepository
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

open class TrainingRepository(
    private val healthConnectRepository: HealthConnectRepository
) {
    val permissions = healthConnectRepository.permissions

    fun checkAvailability() = healthConnectRepository.checkAvailability()

    suspend fun hasAllPermissions() = healthConnectRepository.hasAllPermissions()

    open suspend fun getActivities(startTime: Instant, endTime: Instant): List<TrainingActivity> {
        return healthConnectRepository.fetchTrainingActivities(startTime, endTime)
    }

    open suspend fun fetchTodayTraining(): List<TrainingActivity> {
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val now = Instant.now()
        return getActivities(startOfDay, now)
    }

    open suspend fun fetchLast7DaysTraining(): List<TrainingActivity> {
        return fetchLastNDaysTraining(7)
    }

    open suspend fun fetchLastNDaysTraining(days: Int): List<TrainingActivity> {
        val zone = ZoneId.systemDefault()
        val startOfRange = LocalDate.now(zone).minusDays((days - 1).toLong()).atStartOfDay(zone).toInstant()
        val now = Instant.now()
        return getActivities(startOfRange, now)
    }

    open suspend fun fetchPreviousNDaysTraining(days: Int, offsetDays: Int): List<TrainingActivity> {
        val zone = ZoneId.systemDefault()
        val endOfRange = LocalDate.now(zone).minusDays(offsetDays.toLong()).atStartOfDay(zone).toInstant()
        val startOfRange = LocalDate.now(zone).minusDays((offsetDays + days).toLong()).atStartOfDay(zone).toInstant()
        return getActivities(startOfRange, endOfRange)
    }

    fun calculateStats(
        activities: List<TrainingActivity>
    ): TrainingStats {
        if (activities.isEmpty()) return TrainingStats()

        val totalDuration = activities
            .fold(Duration.ZERO) { acc, activity -> acc.plus(activity.duration) }
        val totalDistanceMeters = activities.sumOf { it.distanceMeters ?: 0.0 }
        val totalCalories = activities.sumOf { it.calories ?: 0.0 }

        val heartRates = activities.mapNotNull { it.averageHeartRate }
        val averageHeartRate = if (heartRates.isNotEmpty()) heartRates.average().toLong() else null

        val typeGroups = activities.groupBy { it.type }
        val typeBreakdown = typeGroups.mapValues { it.value.size }
        val distanceByType = typeGroups.mapValues { (_, activities) -> 
            activities.sumOf { it.distanceMeters ?: 0.0 } 
        }
        val durationByType = typeGroups.mapValues { (_, activities) ->
            activities.fold(Duration.ZERO) { acc, act -> acc.plus(act.duration) }
        }
        val loadByType = typeGroups.mapValues { (_, activities) ->
            activities.sumOf { it.calculatedLoad }
        }
        val totalLoad = activities.sumOf { it.calculatedLoad }
        val intensityDistribution = activities.groupBy { it.intensity }.mapValues { it.value.size }

        return TrainingStats(
            totalActivities = activities.size,
            totalDistanceMeters = totalDistanceMeters,
            totalDuration = totalDuration,
            totalCalories = totalCalories,
            avgHeartRate = averageHeartRate,
            totalLoad = totalLoad,
            typeBreakdown = typeBreakdown,
            distanceByType = distanceByType,
            durationByType = durationByType,
            loadByType = loadByType,
            intensityDistribution = intensityDistribution
        )
    }
}
