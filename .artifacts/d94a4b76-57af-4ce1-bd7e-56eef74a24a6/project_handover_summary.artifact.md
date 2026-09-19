# UniF1 Project Handover Summary

This document provides a comprehensive overview of the UniF1 application architecture, core logic, and design system to facilitate your move to VS Code.

## 1. Core Architecture
UniF1 is a modern Android application built with **Jetpack Compose** and **MVVM** architecture. It integrates with **Health Connect** for athletic data and **Firebase AI Logic (Gemini)** for coaching insights.

### Data Layer (`data/`)
- **`training/`**: The heart of the app's logic.
    - `TrainingActivity.kt`: Core data model for workouts (Run, Bike, Swim).
    - `TrainingAnalyzer.kt`: Contains the deterministic logic for calculating **Readiness Scores** and **Training Load**.
    - `TrainingRepository.kt`: Orchestrates data retrieval from Health Connect and enrichment via the analyzer.
    - `TrainingGoalRepository.kt` & `TrainingPlanRepository.kt`: Persistent storage for user objectives and AI-generated plans using **DataStore**.
- **`health/`**:
    - `HealthConnectRepository.kt`: Low-level integration with the Android Health Connect API.
- **`ai/`**:
    - `GeminiService.kt`: Integration with Firebase AI. Currently configured to use `gemini-1.5-flash` with the `googleAI()` backend.
    - `TrainingDataFormatter.kt`: Responsible for converting complex Kotlin training objects into clean text prompts for the AI.

### UI Layer (`ui/`)
- **`TrainingViewModel.kt`**: The single source of truth for UI state, coordinating repository updates and AI requests.
- **`UniF1App.kt`**: The root navigation container using a custom **Floating Pill Navigation Bar**.
- **`Screens.kt`**: Contains the primary feature screens (Home, Training/Metrics, Coach, Details).
- **`HomeComponents.kt`**: Reusable UI widgets including the **Readiness Index dial**, **Today's Sequence card**, and **Coach hub**.
- **Feature Screens**: `GoalsScreen.kt`, `TrainingPlanScreen.kt`, `PlanPerformanceScreen.kt`, `TrainingReadinessScreen.kt`.

---

## 2. Design System: "Glassmorphic Component Kit"
The app recently underwent a visual transformation to a **Soft Futuristic** aesthetic.

- **Theme (`ui/theme/`)**:
    - `Color.kt`: Defined by a palette of Peach, Sky Blue, and Violet. Uses `GlassWhite` for translucent surfaces.
    - `Theme.kt`: Implements both **Light** and **Dark** color schemes. Dark mode uses a premium deep charcoal (`#121212`) base.
    - `ThemePreferences.kt`: Manages user theme selection (Light/Dark/System) via DataStore.
- **Aesthetic Rules**:
    - Large rounded corners (up to 32dp).
    - Floating modular widgets with soft drop shadows.
    - Pill-shaped buttons and interactive elements.
    - Minimalist typography with high letter spacing.

---

## 3. External Integrations & Configuration
- **Firebase**: Used for AI (Vertex AI for Firebase) and App Check.
    - **App Check**: Requires a debug token to be registered in the Firebase Console for emulator/device access to Gemini.
- **Health Connect**: Requires standard Android health permissions.
- **Navigation**: Uses a custom `UniF1Route` sealed interface for type-safe navigation.

## 4. Key Dependencies (`libs.versions.toml`)
- **Compose**: Foundation of the UI.
- **Navigation3**: Modern, lightweight navigation.
- **Firebase AI**: Powers the coaching intelligence.
- **DataStore Preferences**: For theme and goal persistence.

---

## 5. Maintenance Notes
- **Testing**: Unit tests for all analyzers and the AI data formatter are located in `app/src/test/java/com/example/unif1/`.
- **AI Debugging**: Look for the `UniF1-Gemini` tag in Logcat to diagnose connectivity or token issues.
- **Build**: The app is currently targeting **Android 15 (SDK 37)** with a minimum SDK of 26.
