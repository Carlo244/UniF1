# Implementation Plan - Training Activity Duplication Fix

Investigate and fix the duplication of training activities in both the data layer (Health Connect records) and the UI layer (Home Screen sections).

## User Review Required

> [!IMPORTANT]
> The deduplication logic in the data layer will now group activities by their start time and type, rather than just by their Health Connect ID. This assumes that activities starting at the exact same time (or within a very small threshold) and of the same type are duplicates from different sources.

## Proposed Changes

### Data Layer

#### [MODIFY] [HealthConnectRepository.kt](file:///C:/Users/user/AndroidStudioProjects/UniF1/app/src/main/java/com/example/unif1/data/health/HealthConnectRepository.kt)
- Update `fetchTrainingActivities` to deduplicate records based on a combination of `startTime` and `exerciseType`.
- Implement a small time window (e.g., 1 minute) to identify duplicates that might have slightly different start times due to different apps' reporting.

### UI Layer

#### [MODIFY] [TrainingViewModel.kt](file:///C:/Users/user/AndroidStudioProjects/UniF1/app/src/main/java/com/example/unif1/ui/TrainingViewModel.kt)
- Optimize `refreshData` to fetch data once and derive `todayTraining` and `recentTraining` from the same result.
- Ensure `recentTraining` excludes activities that are already displayed in `todayTraining`.

#### [MODIFY] [Screens.kt](file:///C:/Users/user/AndroidStudioProjects/UniF1/app/src/main/java/com/example/unif1/ui/Screens.kt)
- Adjust the "Recent Training" section to be more descriptive or ensure it doesn't overlap with "Today's Training". (Actually, the ViewModel change should be enough to fix the overlap).

## Verification Plan

### Automated Tests
- Create a new unit test `TrainingRepositoryTest` to verify that activities starting at the same time are correctly deduplicated.
- Create a new unit test `TrainingViewModelTest` to verify that `recentTraining` does not contain activities from `todayTraining`.

### Manual Verification
- Run the app and check the Home screen.
- Verify that a training session completed "today" appears only under "Today's Training" and not under "Recent Training".
