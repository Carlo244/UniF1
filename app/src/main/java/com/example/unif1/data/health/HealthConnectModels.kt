package com.example.unif1.data.health

sealed class HealthAvailability {
    object Installed : HealthAvailability()
    object NotInstalled : HealthAvailability()
    object UpdateRequired : HealthAvailability()
}

sealed class HealthPermissionState {
    object Granted : HealthPermissionState()
    object NotGranted : HealthPermissionState()
}
