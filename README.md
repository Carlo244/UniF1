# UniF1: AI-Powered Triathlon Training Coach

UniF1 is an Android application that combines Health Connect integration, real-time training analytics, and Google Gemini AI to provide personalized Swim/Bike/Run coaching. Built with Jetpack Compose and MVVM architecture, it delivers intelligent training insights, goal tracking, and adaptive weekly planning.

## Features

### 🏋️ Training Analytics
- **Real-time Health Connect Integration** - Seamlessly reads exercise sessions, heart rate data, distance, and calories burned
- **Training Metrics** - Weekly/monthly statistics with sport-specific breakdowns
- **Intensity Calculation** - Classifies workouts as Easy, Moderate, Hard, or Very Hard
- **Training Load Modeling** - Tracks cumulative training stress and recovery status
- **Training Balance** - Visualizes Swim/Bike/Run distribution by count, distance, and duration

### 🎯 Goal Management
- Create and track sport-specific or overall training goals
- Supported metrics: Weekly Distance, Weekly Duration, Weekly Sessions
- Real-time progress tracking with visual status indicators
- Goal-based coaching recommendations

### 🤖 AI Coach (Powered by Google Gemini)
- **Interactive Chat** - Ask questions about your training in natural language
- **Smart Prompt Engineering** - Custom prompts guide Gemini to provide training-specific insights
- **Intelligent Context Detection** - Automatically identifies question type (workout analysis, weekly review, goal alignment, plan adherence, etc.)
- **Personalized Responses** - Gemini analyzes your training data using engineered prompts and provides actionable coaching insights
- **Conversation Memory** - Maintains context across multiple exchanges
- **Multi-Turn Dialogue** - Build on previous coaching discussions

> **Note:** UniF1 AI Coach uses **custom prompts with Google Gemini model** (not a custom-trained model). The coaching intelligence comes from carefully engineered system instructions and formatted training context sent to Gemini.

### 📅 Smart Training Planning
- **AI-Generated Plans** - Gemini creates personalized weekly training plans based on your history and goals
- **Plan Review Workflow** - Approve or discard AI-generated proposals
- **Session Tracking** - Mark sessions as Planned, Completed, Skipped, Missed, or Rest
- **Plan Adherence Metrics** - Measure how well you followed your training plan
- **Performance Comparison** - Track plan-to-actual alignment by sport

### 📊 Detailed Insights
- **Weekly Trends** - Track consistency, volume changes, highlights, and concerns
- **Individual Activity Analysis** - Deep-dive into specific workouts with same-sport comparisons
- **Training Readiness** - Calculates recovery status based on recent load and rest days
- **Plan Performance** - Visualizes adherence to current week's training plan

## Architecture

UniF1 follows clean architecture principles with clear separation of concerns:

```
UI Layer (Jetpack Compose)
    ↓
ViewModel (State Management)
    ↓
Repository Layer (Data Access)
    ↓
Data Sources (Health Connect, DataStore, Gemini AI)
```

### Key Layers

**Presentation Layer** (`ui/`)
- Built with Jetpack Compose and Material3
- Reactive state management via StateFlow
- Screens: Home, Metrics, Plan, Advisor (AI Coach), Goals, Settings, Details

**ViewModel** (`TrainingViewModel`)
- Central state hub managing all UI state
- Coordinates repositories and data flow
- Handles Health Connect permissions and data lifecycle
- Drives Gemini AI interactions

**Repository Layer** (`data/`)
- **TrainingRepository** - Calculates metrics and context from Health Connect
- **HealthConnectRepository** - Direct interface to Health Connect API
- **TrainingGoalRepository** - Persists goals to DataStore
- **TrainingPlanRepository** - Persists generated plans to DataStore

**Domain Layer** (`data/training/`)
- **TrainingAnalyzer** - Enriches activities with calculated metrics
- **PlannedVsActualAnalyzer** - Compares plans to actual execution
- **TrainingPlanAnalyzer** - Calculates goal alignment

**AI Integration** (`data/ai/`)
- **GeminiService** - Communicates with Google Gemini model via Firebase AI
- **TrainingDataFormatter** - Prepares training data for AI prompts based on context

## Tech Stack

### Core Frameworks
- **Jetpack Compose** - Modern declarative UI framework
- **Navigation3** - Type-safe navigation with serializable routes
- **Lifecycle & ViewModel** - MVVM state management
- **Coroutines** - Asynchronous and concurrent operations
- **DataStore** - Encrypted local data persistence

### Health & AI
- **Health Connect Client** - Reads exercise, heart rate, distance, calorie data
- **Firebase AI (Gemini)** - AI coaching and plan generation
- **Firebase App Check** - Security attestation for API calls

### Serialization & Networking
- **kotlinx.serialization** - JSON model serialization
- **Moshi** - JSON parsing with code generation (KSP)
- **Retrofit + OkHttp** - Networking infrastructure

### Design
- **Material3** - Modern Material Design 3 components
- **Compose Adaptive** - Responsive layouts for phones and tablets
- **Coil** - Image loading and caching

## Installation & Setup

### Prerequisites
- Android Studio (latest version)
- Android SDK 37+ (compileSdk)
- Min SDK: Android 8.0 (API 26)
- A Google AI API key from [Google AI Studio](https://aistudio.google.com/app/apikey)

### Step 1: Clone & Build
```bash
git clone https://github.com/Carlo244/UniF1.git
cd UniF1
./gradlew build
```

### Step 2: Add Google AI API Key
1. Open `app/src/main/res/values/strings.xml`
2. Replace `YOUR_GOOGLE_AI_API_KEY_HERE` with your actual API key:
```xml
<string name="google_ai_api_key">YOUR_API_KEY_HERE</string>
```

> ⚠️ **Security:** Never commit API keys to version control. Use GitHub Secrets or environment variables for CI/CD.

### Step 3: Firebase Configuration
- The project includes `google-services.json` (already configured)
- Firebase App Check is enabled with Debug token for development
- In production, deploy with PlayIntegrity attestation

### Step 4: Health Connect Permissions
The app requests the following Health Connect permissions:
- Read Exercise Sessions (Swim, Bike, Run)
- Read Heart Rate data
- Read Distance data
- Read Total Calories Burned

Users will be prompted to grant these permissions on first app launch.

### Step 5: Run the App
```bash
./gradlew installDebug
```

Or open in Android Studio and run on an emulator or physical device with Health Connect installed.

## Usage

### Home Screen
- View today's activities and recent workouts
- Quick access to goals, plan, and AI coach
- Summary statistics and readiness assessment

### Metrics Screen
- Browse historical training data (7, 14, 30 days)
- Analyze trends by sport and intensity
- Identify patterns in training volume and consistency

### Advisor (AI Coach)
1. Tap the **Advisor** tab
2. Type a question about your training, e.g.:
   - "How was my run this morning?"
   - "What's my training balance like?"
   - "Am I following my plan?"
3. The AI coach analyzes your data and responds with personalized insights

### Goals
- Create sport-specific or overall training goals
- Set targets for distance, duration, or session count
- Track progress in real-time

### Plan
- View your current week's training plan
- Update session status (Completed, Skipped, etc.)
- Generate new AI plans for upcoming weeks
- Review plan adherence metrics

### Training Readiness
- See your recovery status based on recent load
- Get recommendations for rest or intensity
- Track weekly progression

## Data Models

### TrainingActivity
Core record of a single workout session:
- Type: RUN, BIKE, or SWIM
- Duration, distance, calories, heart rate metrics
- Calculated intensity and training load

### TrainingStats
Aggregated metrics for a time period:
- Total activities, distance, duration, calories
