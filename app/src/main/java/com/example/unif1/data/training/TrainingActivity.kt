package com.example.unif1.data.training
import java.time.Instant
import java.time.Duration
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}

object DurationSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Duration", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Duration) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): Duration = Duration.parse(decoder.decodeString())
}

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}

@Serializable
enum class TrainingType {
    SWIM,
    BIKE,
    RUN
}

enum class Intensity {
    EASY,
    MODERATE,
    HARD,
    VERY_HARD
}

data class TrainingActivity(
    val id: String,
    val type: TrainingType,
    val startTime: Instant,
    val endTime: Instant,
    val duration: Duration,
    val distanceMeters: Double? = null,
    val calories: Double? = null,
    val averageHeartRate: Long? = null,
    val maxHeartRate: Long? = null,
    val calculatedLoad: Double = 0.0,
    val intensity: Intensity = Intensity.EASY
)

data class HeartRateSample(
    val time: Instant,
    val bpm: Long
)

data class TrainingStats(
    val totalActivities: Int = 0,
    val totalDistanceMeters: Double = 0.0,
    val totalDuration: Duration = Duration.ZERO,
    val totalCalories: Double = 0.0,
    val avgHeartRate: Long? = null,
    val totalLoad: Double = 0.0,
    val typeBreakdown: Map<TrainingType, Int> = emptyMap(),
    val distanceByType: Map<TrainingType, Double> = emptyMap(),
    val durationByType: Map<TrainingType, Duration> = emptyMap(),
    val loadByType: Map<TrainingType, Double> = emptyMap(),
    val intensityDistribution: Map<Intensity, Int> = emptyMap()
)

data class WeeklyProgress(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val stats: TrainingStats,
    val balance: TrainingBalance
)

data class TrainingBalance(
    val totalSessions: Int = 0,
    val sessionsByType: Map<TrainingType, Int> = emptyMap(),
    val sessionPercentageByType: Map<TrainingType, Double> = emptyMap(),
    
    val totalDistanceMeters: Double = 0.0,
    val distanceByType: Map<TrainingType, Double> = emptyMap(),
    val distancePercentageByType: Map<TrainingType, Double> = emptyMap(),
    
    val totalDuration: Duration = Duration.ZERO,
    val durationByType: Map<TrainingType, Duration> = emptyMap(),
    val durationPercentageByType: Map<TrainingType, Double> = emptyMap()
)

@Serializable
enum class GoalMetric {
    WEEKLY_DISTANCE,
    WEEKLY_DURATION,
    WEEKLY_SESSIONS
}

@Serializable
data class TrainingGoal(
    val id: String,
    val metric: GoalMetric,
    val sport: TrainingType?, // null means ALL
    val targetValue: Double,
    val active: Boolean = true,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant = Instant.now()
)

enum class GoalStatus {
    NOT_STARTED,
    ON_TRACK,
    BEHIND,
    COMPLETED
}

data class GoalProgress(
    val goal: TrainingGoal,
    val currentValue: Double,
    val targetValue: Double,
    val remainingValue: Double,
    val progressPercentage: Double,
    val expectedProgressPercentage: Double,
    val status: GoalStatus
)

@Serializable
data class TrainingPlan(
    val id: String,
    @Serializable(with = LocalDateSerializer::class)
    val weekStartDate: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val weekEndDate: LocalDate,
    val sessions: List<PlannedTrainingSession>,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant = Instant.now()
)

@Serializable
data class PlannedTrainingSession(
    val id: String,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val sport: TrainingType,
    val title: String,
    val description: String,
    val targetDistanceMeters: Double? = null,
    val targetDurationSeconds: Long? = null,
    val intensity: String? = null,
    val status: SessionStatus = SessionStatus.PLANNED,
    val notes: String? = null
)

@Serializable
enum class SessionStatus {
    PLANNED,
    COMPLETED,
    SKIPPED,
    MISSED,
    REST
}

data class PlannedVsActualSummary(
    val weekStartDate: LocalDate,
    val weekEndDate: LocalDate,
    val plannedSessionCount: Int,
    val completedSessionCount: Int,
    val skippedSessionCount: Int,
    val missedSessionCount: Int,
    val adherencePercentage: Double,
    val plannedDistanceMeters: Double,
    val actualDistanceMeters: Double,
    val distanceAdherencePercentage: Double,
    val plannedDurationSeconds: Long,
    val actualDurationSeconds: Long,
    val durationAdherencePercentage: Double,
    val sportComparisons: Map<TrainingType, SportPerformanceComparison>
)

data class SportPerformanceComparison(
    val type: TrainingType,
    val plannedSessions: Int,
    val actualSessions: Int,
    val plannedDistance: Double,
    val actualDistance: Double,
    val distanceAdherence: Double,
    val plannedDuration: Duration,
    val actualDuration: Duration,
    val durationAdherence: Double,
    val sessionAdherence: Double
)

data class PlanGoalAlignment(
    val goal: TrainingGoal,
    val plannedValue: Double,
    val alignmentPercentage: Double
)

enum class LoadTrend {
    INCREASING,
    DECREASING,
    STABLE,
    INSUFFICIENT_DATA
}

enum class LoadSpikeStatus {
    NORMAL,
    ELEVATED,
    HIGH_SPIKE,
    INSUFFICIENT_DATA
}

data class TrainingLoadSummary(
    val currentWeekLoad: Double = 0.0,
    val previousWeekLoad: Double = 0.0,
    val percentageChange: Double? = null,
    val recentWeeklyLoads: List<Double> = emptyList(),
    val loadTrend: LoadTrend = LoadTrend.INSUFFICIENT_DATA,
    val spikeStatus: LoadSpikeStatus = LoadSpikeStatus.INSUFFICIENT_DATA,
    val loadBySportPercentage: Map<TrainingType, Double> = emptyMap(),
    val intensityDistribution: Map<Intensity, Int> = emptyMap(),
    val highestLoadWorkout: TrainingActivity? = null,
    val averageSessionLoad: Double = 0.0
)

enum class TrainingReadinessStatus {
    READY,
    CAUTION,
    RECOVERY_FOCUS,
    INSUFFICIENT_DATA
}

data class TrainingReadiness(
    val status: TrainingReadinessStatus = TrainingReadinessStatus.INSUFFICIENT_DATA,
    val score: Int = 0,
    val currentWeeklyLoad: Double = 0.0,
    val recentBaselineLoad: Double = 0.0,
    val loadChangePercentage: Double? = null,
    val loadSpikeStatus: LoadSpikeStatus = LoadSpikeStatus.INSUFFICIENT_DATA,
    val sessionsLast3Days: Int = 0,
    val sessionsLast7Days: Int = 0,
    val hardSessionsLast7Days: Int = 0,
    val veryHardSessionsLast7Days: Int = 0,
    val consecutiveActiveDays: Int = 0,
    val daysSinceLastWorkout: Long? = null,
    val lastWorkout: TrainingActivity? = null,
    val todayPlannedSession: PlannedTrainingSession? = null,
    val reasons: List<String> = emptyList()
)
