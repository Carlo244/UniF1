package com.example.unif1.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface UniF1Route : NavKey {
    @Serializable
    data object Home : UniF1Route
    @Serializable
    data object Trends : UniF1Route
    @Serializable
    data object AI : UniF1Route
    @Serializable
    data object Goals : UniF1Route
    @Serializable
    data object Plan : UniF1Route
    @Serializable
    data object PlanPerformance : UniF1Route
    @Serializable
    data object TrainingReadiness : UniF1Route
    @Serializable
    data object Settings : UniF1Route
    @Serializable
    data class TrainingDetail(val activityId: String) : UniF1Route
}
