package com.example.unif1.data.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.unif1.data.training.TrainingActivity
import com.example.unif1.data.training.TrainingType
import com.example.unif1.data.training.HeartRateSample
import java.time.Duration
import java.time.Instant
import android.content.Context

open class HealthConnectRepository(
    private val context: Context?
) {

    private val healthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context!!)
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    fun checkAvailability(): HealthAvailability {
        if (context == null) return HealthAvailability.NotInstalled
        return when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthAvailability.Installed
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthAvailability.UpdateRequired
            else -> HealthAvailability.NotInstalled
        }
    }

    suspend fun hasAllPermissions(): Boolean {
        if (checkAvailability() != HealthAvailability.Installed) return false
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    open suspend fun fetchTrainingActivities(
        startTime: Instant,
        endTime: Instant
    ): List<TrainingActivity> {

        val request = ReadRecordsRequest(
            recordType = ExerciseSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(
                startTime,
                endTime
            )
        )

        val response = healthConnectClient.readRecords(request)

        println("UNIF1: Health Connect returned ${response.records.size} records")

        response.records.forEach { record ->
            println(
                "UNIF1 RECORD | " +
                        "id=${record.metadata.id} | " +
                        "type=${record.exerciseType} | " +
                        "start=${record.startTime} | " +
                        "end=${record.endTime}"
            )
        }

        val trainingRecords = response.records
            .filter { record ->
                mapExerciseType(record.exerciseType) != null
            }
            .distinctBy { record ->
                record.metadata.id
            }

        println(
            "UNIF1: After ID deduplication = " +
                    "${trainingRecords.size} records"
        )

        return trainingRecords
            .map { record ->

                val type = mapExerciseType(record.exerciseType)!!

                val metrics = aggregateSessionMetrics(
                    record.startTime,
                    record.endTime
                )

                TrainingActivity(
                    id = record.metadata.id,
                    type = type,
                    startTime = record.startTime,
                    endTime = record.endTime,
                    duration = Duration.between(
                        record.startTime,
                        record.endTime
                    ),
                    distanceMeters = metrics.distanceMeters,
                    calories = metrics.calories,
                    averageHeartRate = metrics.averageHeartRate,
                    maxHeartRate = metrics.maxHeartRate
                )
            }
            .sortedByDescending { activity ->
                activity.startTime
            }
    }

    private suspend fun aggregateSessionMetrics(
        startTime: Instant,
        endTime: Instant
    ): SessionMetrics {
        val response = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(
                    DistanceRecord.DISTANCE_TOTAL,
                    TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                    HeartRateRecord.BPM_AVG,
                    HeartRateRecord.BPM_MAX
                ),
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
        )

        return SessionMetrics(
            distanceMeters = response[DistanceRecord.DISTANCE_TOTAL]?.inMeters,
            calories = response[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories,
            averageHeartRate = response[HeartRateRecord.BPM_AVG],
            maxHeartRate = response[HeartRateRecord.BPM_MAX]
        )
    }

    suspend fun fetchHeartRateSamples(
        startTime: Instant,
        endTime: Instant
    ): List<HeartRateSample> {
        val records = healthConnectClient.readRecords(
            ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
        ).records

        return records
            .flatMap { record ->
                record.samples.map { sample ->
                    HeartRateSample(
                        time = sample.time,
                        bpm = sample.beatsPerMinute
                    )
                }
            }
            .sortedBy { it.time }
    }

    private fun mapExerciseType(exerciseType: Int): TrainingType? {
        return when (exerciseType) {
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> TrainingType.RUN
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> TrainingType.BIKE
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> TrainingType.SWIM
            else -> null
        }
    }

    private data class SessionMetrics(
        val distanceMeters: Double?,
        val calories: Double?,
        val averageHeartRate: Long?,
        val maxHeartRate: Long?
    )
}
