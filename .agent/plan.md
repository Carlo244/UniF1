# Project Plan

Extend UniF1 to read and display historical exercise and running data from Health Connect. 

Key Features:
- Read exercise sessions (Running, Walking, etc.) from Health Connect.
- Retrieve session details: type, time, duration, distance, calories, avg/max HR.
- Add "Recent Runs" section to the Trends screen with a clean list/card layout.
- Implement a simple Run Detail view for selected sessions.
- Provide historical running statistics (Total runs, distance, time, avg distance, avg pace) for 7/30/90 days.
- Handle all data states (loading, no data, errors, missing fields).

Technical Constraints:
- Use existing architecture (Kotlin, Compose, Coroutines, Health Connect SDK, Navigation 3, Adaptive UI).
- No database (Room), AI (Gemini), or cloud integration.
- Read-only implementation using real Health Connect records.
- Do not break existing Home dashboard functionality.

## Project Brief

# UniF1 Health Extension Project Brief

This brief outlines the plan to extend the UniF1 application to integrate historical exercise and running data from Health Connect.

## Features (MVP)

*   **Health Connect Data Integration**: Implementation of a read-only integration with the Health Connect SDK to securely fetch historical exercise sessions (e.g., Running, Walking).
*   **Fitness Trends Dashboard**: A summary view that calculates and displays aggregated statistics, including total runs, distance, time, and average pace across 7, 30, and 90-day intervals.
*   **Recent Activities Feed**: A card-based list integrated into the Trends screen, providing a quick-glance view of recent exercise sessions.
*   **Activity Detail View**: A dedicated detail screen for individual sessions that displays comprehensive metrics such as duration, calories burned, distance, and heart rate data.
*   **Robust Data State Handling**: A consistent UI strategy for managing loading states, empty data sets, missing fields, and permission/error scenarios.

## High-Level Technical Stack

*   **Kotlin**: Primary programming language.
*   **Jetpack Compose**: Declarative UI framework for building modern Android interfaces.
*   **Health Connect SDK**: Official Android API for accessing on-device health and fitness records.
*   **Kotlin Coroutines & Flow**: Asynchronous programming model for reactive data streaming and UI updates.
*   **Jetpack Navigation 3**: State-driven navigation architecture for seamless transitions between screens.
*   **Compose Material Adaptive**: Library for implementing responsive and adaptive layouts (e.g., list-detail) that scale across different form factors.

> [!NOTE]
> This implementation is read-only and does not require local persistence (Room) or cloud synchronization, relying entirely on real-time queries to the Health Connect provider.

## Implementation Steps

### Task_1_Setup_Navigation_Adaptive: Initialize the project with necessary dependencies (Navigation 3, Compose Material Adaptive) and implement the adaptive navigation scaffold (BottomBar for phones, Navigation Rail for larger screens).
- **Status:** COMPLETED
- **Updates:** Initialized project with Jetpack Compose, Navigation 3, and Material 3 Adaptive dependencies. Created a responsive navigation scaffold that adapts between BottomBar and NavigationRail. Set up the UniF1 theme and placeholder screens for Home, Trends, AI, and Settings.
- **Acceptance Criteria:**
  - Project builds successfully
  - Navigation 3 state-driven system implemented
  - Adaptive scaffold switches between BottomBar and Navigation Rail based on screen size

### Task_2_Home_Dashboard_UI: Implement the Home screen featuring the Daily Health Dashboard with rounded cards for Steps, Sleep, Heart Rate, and SpO2, plus Activity placeholders.
- **Status:** COMPLETED
- **Updates:** Implemented the Home screen with a professional health-tech aesthetic. Created reusable components for health metrics (Steps, Sleep, HR, SpO2), Activity, and Trends. Added a prominent "Ask UniF1" button. The layout is responsive and supports edge-to-edge. Used placeholder data as requested.
- **Acceptance Criteria:**
  - Home screen UI matches modern, clean design
  - Metric cards (Steps, Sleep, HR, SpO2) are visible with placeholder data
  - Responsive layout for different window sizes

### Task_3_Secondary_Screens_and_Assets: Implement the Trends and Settings screens, create the AI Health Assistant portal entry point, and generate the app icon and theme assets.
- **Status:** COMPLETED
- **Updates:** Implemented Trends screen with health insight placeholders. Created a conversational AI Insights screen with chat bubbles and input bar. Built a structured Settings screen. Generated a modern adaptive app icon with a teal background and stylized F1/heartbeat logo. Integrated all screens into the navigation system.
- **Acceptance Criteria:**
  - Trends and Settings screens are accessible via navigation
  - AI Health Assistant entry point is clearly visible
  - App icon is generated and displayed

### Task_4_Run_and_Verify: Perform a final run of the UniF1 app to verify stability, requirement alignment, and UI consistency.
- **Status:** COMPLETED
- **Updates:** Project completed successfully. All UI components, navigation, and assets are implemented and verified via successful build. Final report provided to the user.
- **Acceptance Criteria:**
  - Build pass
  - App does not crash
  - Make sure all existing tests pass
  - Critic_agent verifies stability (no crashes), confirms alignment with user requirements, and reports critical UI issues

### Task_5_Health_Connect_Integration: Integrate Health Connect SDK, implement availability checks, and manage permissions for Steps, Sleep, Heart Rate (Avg/Resting), and SpO2. Develop a repository to fetch today's records.
- **Status:** COMPLETED
- **Updates:** Integrated Health Connect SDK. Added permissions to Manifest. Implemented HealthConnectRepository with availability checks and data fetching for Steps, Sleep, HR, Resting HR, and SpO2 using Coroutines. Created HealthDataState models. Project builds successfully.
- **Acceptance Criteria:**
  - Health Connect SDK integrated into build
  - Permission flow handles availability (not installed/update required) and user grant/denial
  - Repository successfully fetches today's health metrics using Coroutines

### Task_6_UI_Binding_and_Final_Verification: Replace placeholder data in Home screen cards with live Health Connect data, implement Loading/Error/No Data UI states, and perform final stability verification.
- **Status:** COMPLETED
- **Updates:** Implemented HomeViewModel to manage HealthDataState. Updated Home screen UI to bind live Health Connect data and handle Loading, Permission Required, Unavailable, No Data, and Error states. Integrated permission request flow. Preserved existing visual design and adaptive layout. Project builds successfully.
- **Acceptance Criteria:**
  - Home dashboard displays live data from Health Connect
  - Shimmer loading, 'No Data', and 'Permission Denied' UI states are functional
  - Build pass, app does not crash, and all existing tests pass
  - Critic_agent verifies stability and alignment with health data requirements

### Task_7_Historical_Exercise_Data_Fetch: Extend HealthConnectRepository to fetch historical ExerciseSessionRecords (Running, Walking) and implement logic to aggregate statistics (total runs, distance, time, avg pace) for 7, 30, and 90-day windows.
- **Status:** COMPLETED
- **Updates:** Extended HealthConnectRepository to fetch historical exercise sessions and calculate running statistics. Added READ_EXERCISE permission. Implemented TrendsViewModel to expose data for 7, 30, and 90-day ranges. Created ExerciseSession and RunningStats data models. Project builds successfully.
- **Acceptance Criteria:**
  - Repository fetches exercise sessions from Health Connect
  - Statistics aggregation logic correctly calculates distance, time, and pace for specified intervals
  - Data models support loading and error states for historical data

### Task_8_Trends_Update_and_Run_Detail: Update the Trends screen to display aggregated statistics and a 'Recent Activities' feed. Implement the Run Detail view for individual sessions using Navigation 3 and Adaptive UI. Perform final run and verification.
- **Status:** COMPLETED
- **Updates:** Updated Trends screen with real RunningStats and a 'Recent Activities' list. Implemented RunDetailScreen to show comprehensive session metrics. Integrated new screen and navigation logic using Navigation 3. Added RunDetailViewModel for session data fetching. Ensured adaptive layout support and consistent visual design. Project builds successfully.
- **Acceptance Criteria:**
  - Trends screen displays summary stats and recent runs list
  - Run Detail screen shows full metrics (calories, HR, distance, duration) for a session
  - Navigation between Trends and Detail view is functional
  - Build pass, app does not crash, and all existing tests pass
  - Critic_agent verifies stability and alignment with health extension requirements
- **Duration:** N/A

