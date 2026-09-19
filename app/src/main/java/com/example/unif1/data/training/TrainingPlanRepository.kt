package com.example.unif1.data.training

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

private val Context.planDataStore by preferencesDataStore(name = "training_plans")

class TrainingPlanRepository(private val context: Context) {

    private val PLANS_KEY = stringPreferencesKey("plans_json")

    val plans: Flow<List<TrainingPlan>> = context.planDataStore.data.map { preferences ->
        val json = preferences[PLANS_KEY] ?: "[]"
        try {
            Json.decodeFromString<List<TrainingPlan>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun savePlan(plan: TrainingPlan) {
        updatePlans { it.filter { p -> p.weekStartDate != plan.weekStartDate } + plan }
    }

    suspend fun deletePlan(planId: String) {
        updatePlans { it.filter { p -> p.id != planId } }
    }

    suspend fun updateSession(planId: String, updatedSession: PlannedTrainingSession) {
        updatePlans { plans ->
            plans.map { plan ->
                if (plan.id == planId) {
                    plan.copy(sessions = plan.sessions.map { 
                        if (it.id == updatedSession.id) updatedSession else it 
                    })
                } else plan
            }
        }
    }

    suspend fun getPlanForWeek(weekStart: LocalDate): Flow<TrainingPlan?> {
        return plans.map { plans ->
            plans.find { it.weekStartDate == weekStart }
        }
    }

    private suspend fun updatePlans(transform: (List<TrainingPlan>) -> List<TrainingPlan>) {
        context.planDataStore.edit { preferences ->
            val currentJson = preferences[PLANS_KEY] ?: "[]"
            val currentPlans = try {
                Json.decodeFromString<List<TrainingPlan>>(currentJson)
            } catch (e: Exception) {
                emptyList<TrainingPlan>()
            }
            val updatedPlans = transform(currentPlans)
            preferences[PLANS_KEY] = Json.encodeToString(updatedPlans)
        }
    }
}
