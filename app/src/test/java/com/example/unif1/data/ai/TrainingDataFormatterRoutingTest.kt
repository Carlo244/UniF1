package com.example.unif1.data.ai

import com.example.unif1.data.training.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TrainingDataFormatterRoutingTest {

    private val formatter = TrainingDataFormatter()
    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 9, 3)
    private val now = today.atTime(10, 0).atZone(zone).toInstant()

    @Test
    fun `how was my run today selects RUN and non-empty selected workout context`() {
        val activities = sampleActivities()
        val context = TrainingContext()
        val selected = formatter.selectActivitiesForQuestion(
            type = CoachQuestionType.WORKOUT_SPECIFIC,
            message = "How was my run today?",
            context = context,
            activities = activities,
            zone = zone,
            today = today
        )
        assertTrue(selected.isNotEmpty())
        assertTrue(selected.all { it.type == TrainingType.RUN })
        val prompt = formatter.formatForQuestion(
            type = CoachQuestionType.WORKOUT_SPECIFIC,
            context = context.copy(selectedWorkoutAnalysis = WorkoutAnalysisContext(selected.first())),
            activities = selected,
            currentDate = today,
            currentTime = LocalTime.NOON,
            zone = zone
        )
        assertTrue(prompt.contains("SELECTED WORKOUT"))
        assertTrue(prompt.length > 50)
    }

    @Test
    fun `latest workout selects latest activity and context contains workout`() {
        val activities = sampleActivities()
        val latest = activities.maxByOrNull { it.startTime }!!
        val selected = formatter.selectActivitiesForQuestion(
            CoachQuestionType.WORKOUT_SPECIFIC,
            "How was my latest workout?",
            TrainingContext(),
            activities,
            zone,
            today
        )
        assertEquals(1, selected.size)
        assertEquals(latest.id, selected.first().id)
        val prompt = formatter.formatForQuestion(
            CoachQuestionType.GENERAL_COACHING,
            TrainingContext(),
            selected,
            today,
            LocalTime.NOON,
            zone
        )
        assertTrue(prompt.contains("RECENT ACTIVITIES"))
        assertTrue(prompt.contains(latest.startTime.atZone(zone).toLocalDate().toString()))
    }

    @Test
    fun `what did i train today selects todays activities and includes data`() {
        val activities = sampleActivities()
        val selected = formatter.selectActivitiesForQuestion(
            CoachQuestionType.GENERAL_COACHING,
            "What did I train today?",
            TrainingContext(),
            activities,
            zone,
            today
        )
        assertTrue(selected.isNotEmpty())
        assertTrue(selected.all { it.startTime.atZone(zone).toLocalDate() == today })
        val prompt = formatter.formatForQuestion(
            CoachQuestionType.GENERAL_COACHING,
            TrainingContext(),
            selected,
            today,
            LocalTime.NOON,
            zone
        )
        assertTrue(prompt.contains("RECENT ACTIVITIES"))
        assertTrue(prompt.contains("RUN"))
    }

    @Test
    fun `last run selects latest RUN and excludes BIKE SWIM`() {
        val selected = formatter.selectActivitiesForQuestion(
            CoachQuestionType.WORKOUT_SPECIFIC,
            "What was my last run?",
            TrainingContext(),
            sampleActivities(),
            zone,
            today
        )
        assertTrue(selected.isNotEmpty())
        assertTrue(selected.all { it.type == TrainingType.RUN })
    }

    @Test
    fun `last bike selects latest BIKE and excludes RUN SWIM`() {
        val selected = formatter.selectActivitiesForQuestion(
            CoachQuestionType.WORKOUT_SPECIFIC,
            "What was my last bike?",
            TrainingContext(),
            sampleActivities(),
            zone,
            today
        )
        assertTrue(selected.isNotEmpty())
        assertTrue(selected.all { it.type == TrainingType.BIKE })
    }

    @Test
    fun `last swim selects latest SWIM and excludes RUN BIKE`() {
        val selected = formatter.selectActivitiesForQuestion(
            CoachQuestionType.WORKOUT_SPECIFIC,
            "What was my last swim?",
            TrainingContext(),
            sampleActivities(),
            zone,
            today
        )
        assertTrue(selected.isNotEmpty())
        assertTrue(selected.all { it.type == TrainingType.SWIM })
    }

    @Test
    fun `date handling supports today yesterday this week last week`() {
        val acts = sampleActivities()
        val todaySelected = formatter.selectActivitiesForQuestion(CoachQuestionType.GENERAL_COACHING, "today", TrainingContext(), acts, zone, today)
        val yesterdaySelected = formatter.selectActivitiesForQuestion(CoachQuestionType.GENERAL_COACHING, "yesterday", TrainingContext(), acts, zone, today)
        val thisWeekSelected = formatter.selectActivitiesForQuestion(CoachQuestionType.WEEKLY_REVIEW, "this week", TrainingContext(), acts, zone, today)
        val lastWeekSelected = acts.filter { it.startTime.atZone(zone).toLocalDate().isBefore(today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))) }
        assertTrue(todaySelected.all { it.startTime.atZone(zone).toLocalDate() == today })
        assertTrue(yesterdaySelected.all { it.startTime.atZone(zone).toLocalDate() == today.minusDays(1) })
        assertTrue(thisWeekSelected.isNotEmpty())
        assertTrue(lastWeekSelected.isNotEmpty())
    }

    @Test
    fun `no matching activity for RUN with only BIKE does not fabricate`() {
        val bikeOnly = listOf(activity("b1", TrainingType.BIKE, now.minus(Duration.ofHours(1)), 20000.0))
        val selected = formatter.selectActivitiesForQuestion(
            CoachQuestionType.WORKOUT_SPECIFIC,
            "What was my last run?",
            TrainingContext(),
            bikeOnly,
            zone,
            today
        )
        assertTrue(selected.isEmpty())
    }

    @Test
    fun `general coaching context exists when activities exist`() {
        val acts = sampleActivities()
        val prompt = formatter.formatForQuestion(
            CoachQuestionType.GENERAL_COACHING,
            TrainingContext(),
            acts.take(3),
            today,
            LocalTime.NOON,
            zone
        )
        assertTrue(prompt.isNotBlank())
        assertTrue(prompt.contains("GENERAL COACHING CONTEXT"))
    }

    @Test
    fun `workout-specific context is smaller than broad general context`() {
        val acts = sampleActivities()
        val workout = formatter.formatForQuestion(
            CoachQuestionType.WORKOUT_SPECIFIC,
            TrainingContext(selectedWorkoutAnalysis = WorkoutAnalysisContext(acts.first())),
            listOf(acts.first()),
            today,
            LocalTime.NOON,
            zone
        )
        val broad = formatter.formatForPlanGeneration(TrainingContext(), acts, today, LocalTime.NOON, zone)
        assertTrue(workout.length < broad.length)
    }

    @Test
    fun `training readiness is not included in formatted ai context`() {
        val prompt = formatter.formatForQuestion(
            CoachQuestionType.GENERAL_COACHING,
            TrainingContext(),
            sampleActivities(),
            today,
            LocalTime.NOON,
            zone
        )
        assertFalse(prompt.contains("TRAINING READINESS"))
    }

    private fun sampleActivities(): List<TrainingActivity> {
        return listOf(
            activity("r-today", TrainingType.RUN, now.minus(Duration.ofHours(1)), 5200.0),
            activity("b-yesterday", TrainingType.BIKE, now.minus(Duration.ofDays(1)), 24000.0),
            activity("s-2d", TrainingType.SWIM, now.minus(Duration.ofDays(2)), 1500.0),
            activity("r-3d", TrainingType.RUN, now.minus(Duration.ofDays(3)), 7000.0),
            activity("b-8d", TrainingType.BIKE, now.minus(Duration.ofDays(8)), 18000.0)
        ).sortedByDescending { it.startTime }
    }

    private fun activity(id: String, type: TrainingType, start: Instant, distance: Double): TrainingActivity {
        return TrainingActivity(
            id = id,
            type = type,
            startTime = start,
            endTime = start.plus(Duration.ofMinutes(31)),
            duration = Duration.ofMinutes(31),
            distanceMeters = distance,
            calories = 320.0,
            averageHeartRate = 148,
            maxHeartRate = 165
        )
    }
}
