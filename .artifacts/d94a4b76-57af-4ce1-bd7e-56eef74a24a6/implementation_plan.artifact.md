# UniF1 UI Redesign Implementation Plan

Modernize the visual design and Compose implementation of UniF1 to create a premium, analytical, and athletic training intelligence experience using Material 3 Expressive principles.

## User Review Required

> [!IMPORTANT]
> This is a **UI-only redesign**. No changes will be made to business logic, repositories, Health Connect integration, or navigation architecture.

> [!NOTE]
> We will prioritize information hierarchy and athletic telemetry over generic dashboard patterns.

## Proposed Changes

### Design System & Theming

Update the foundational design system to support "Expressive" styling and a more professional athletic palette.

#### [MODIFY] [Color.kt](file:///C:/Users/user/AndroidStudioProjects/UniF1/app/src/main/java/com/example/unif1/ui/theme/Color.kt)
- Refine the status colors (`ReadyColor`, `CautionColor`, `RecoveryColor`) to be more analytical and less "neon".
- Ensure support for Material 3 tonal surface containers (`surfaceContainer`, `surfaceContainerLow`, etc.).

#### [MODIFY] [Type.kt](file:///C:/Users/user/AndroidStudioProjects/UniF1/app/src/main/java/com/example/unif1/ui/theme/Type.kt)
- Adjust headline and display typography to better support large athletic metrics.
- Refine label styles for metadata and units.

---

### Home Dashboard

Redesign the primary entry point to feel like a high-end training dashboard.

#### [MODIFY] [Screens.kt](file:///C:/Users/user/AndroidStudioProjects/UniF1/app/src/main/java/com/example/unif1/ui/Screens.kt)
- **HomeScreen:** Modernize the header with a lightweight athletic schedule focus.
- **TrainingScreen:** Transition the "Training Load" card into a more analytical telemetry summary.

#### [MODIFY] [HomeComponents.kt](file:///C:/Users/user/AndroidStudioProjects/UniF1/app/src/main/java/com/example/unif1/ui/HomeComponents.kt)
- **ReadinessHero:** Redesign as a professional diagnostic anchor. Remove oversized borders; use tonal depth.
- **TodayPlanCard:** Redesign as an actionable schedule item.
- **WeeklyOverview:** Group metrics into a telemetry grid instead of independent boxes.
- **TrainingActivityRow:** Refine for high-density scannability.
- **GoalProgressCard:** Integrate progress indicators directly into the layout.

---

### Goals & Analytics

Enhance the scannability of training goals and analytical screens.

#### [MODIFY] [GoalsScreen.kt](file:///C:/Users/user/AndroidStudioProjects/UniF1/app/src/main/java/com/example/unif1/ui/GoalsScreen.kt)
- **RedesignedGoalItem:** Use structured rows instead of giant cards. Improve progress visualization.
- **AddGoalDialog:** Modernize the form layout with Material 3 Expressive inputs.

#### [MODIFY] [TrainingReadinessScreen.kt](file:///C:/Users/user/AndroidStudioProjects/UniF1/app/src/main/java/com/example/unif1/ui/TrainingReadinessScreen.kt)
- Prioritize the Readiness score as a central anchor.
- Redesign "Contributing Factors" into a clean analytical list.

#### [MODIFY] [PlanPerformanceScreen.kt](file:///C:/Users/user/AndroidStudioProjects/UniF1/app/src/main/java/com/example/unif1/ui/PlanPerformanceScreen.kt)
- Use side-by-side "Planned vs Actual" comparisons for distance and duration.
- Implement a cohesive telemetry summary for sport-specific adherence.

---

### Training Plan

Professionalize the weekly training calendar.

#### [MODIFY] [TrainingPlanScreen.kt](file:///C:/Users/user/AndroidStudioProjects/UniF1/app/src/main/java/com/example/unif1/ui/TrainingPlanScreen.kt)
- Redesign the weekly list as a timeline-style calendar.
- Use subtle status markers for Completed, Skipped, and Rest days.

## Verification Plan

### Automated Tests
- Run existing unit tests to ensure no business logic regression: `./gradlew test`
- Run existing UI tests: `./gradlew :app:connectedDebugAndroidTest` (if device is available) or `./gradlew :app:compileDebugKotlin` to ensure no syntax/signature errors.

### Manual Verification
- Deploy to device/emulator and verify:
    - Adaptive layouts on phone vs. large screen (using `NavigationSuiteScaffold`).
    - Light and Dark theme consistency.
    - Scannability of metrics and status colors.
    - Navigation flow remains identical to current implementation.
