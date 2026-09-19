package com.example.unif1.data.training

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "training_goals")

class TrainingGoalRepository(private val context: Context) {

    private val GOALS_KEY = stringPreferencesKey("goals_json")

    val goals: Flow<List<TrainingGoal>> = context.dataStore.data.map { preferences ->
        val json = preferences[GOALS_KEY] ?: "[]"
        try {
            Json.decodeFromString<List<TrainingGoal>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addGoal(metric: GoalMetric, sport: TrainingType?, targetValue: Double) {
        val newGoal = TrainingGoal(
            id = UUID.randomUUID().toString(),
            metric = metric,
            sport = sport,
            targetValue = targetValue
        )
        updateGoals { it + newGoal }
    }

    suspend fun deleteGoal(goalId: String) {
        updateGoals { it.filter { goal -> goal.id != goalId } }
    }

    suspend fun updateGoal(updatedGoal: TrainingGoal) {
        updateGoals { it.map { goal -> if (goal.id == updatedGoal.id) updatedGoal else goal } }
    }

    private suspend fun updateGoals(transform: (List<TrainingGoal>) -> List<TrainingGoal>) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[GOALS_KEY] ?: "[]"
            val currentGoals = try {
                Json.decodeFromString<List<TrainingGoal>>(currentJson)
            } catch (e: Exception) {
                emptyList<TrainingGoal>()
            }
            val updatedGoals = transform(currentGoals)
            preferences[GOALS_KEY] = Json.encodeToString(updatedGoals)
        }
    }
}
