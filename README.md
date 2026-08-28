# Gamak AI (गमक) — Context-Aware Multilingual Voice Assistant for Android

**Gamak AI** is a production-grade, privacy-conscious voice and text assistant for Android designed for seamless multilingual communication across **Hindi (हिंदी), Nepali (नेपाली), Hinglish, and English**.

Built with Jetpack Compose, Material 3, Kotlin Coroutines/Flow, Room Database, and an intelligent hybrid NLU pipeline combining low-latency on-device intent parsing with Google Gemini LLM reasoning.

---

## 🌟 Key Capabilities

### 1. Hybrid Multilingual NLU & AI Engine
- **On-Device Local NLU (`LocalNluEngine`)**: Instant, zero-latency parsing for common device actions, app navigation, system controls, alarms, calls, and WhatsApp messages in Hindi, Nepali, Hinglish, and English without requiring internet connectivity.
- **Cloud Gemini Reasoning (`GeminiClient`)**: Deep contextual understanding, question answering, multi-step intent breakdown, and rich conversational interactions powered by Google Gemini via server-side/client-side integrations.
- **Contextual Memory & Pronoun Resolution**: Remembers recent conversation entities (contacts, apps, time, locations) allowing natural follow-ups like *"उसे call करो"* or *"वो app खोलो"*.
- **Multi-Step Execution Pipeline**: Chains sequential commands with conjunctions (e.g., *"सुबह 7 बजे का अलार्म लगाओ और फिर YouTube खोलो"*).

### 2. Deep Device & System Tool Integrations
- **Communications**: Intelligent contact lookup with fuzzy matching, contact disambiguation, direct dialer intents, SMS drafts, and direct WhatsApp messaging flows.
- **App Management**: Dynamic package launcher resolving local aliases (YouTube, Spotify, Camera, Gallery, Settings, Maps, DeskClock, etc.).
- **Productivity & Time**: Alarm scheduling, count-down timers with system clock integration, and local notification reminders with broadcast receivers (`ReminderBroadcastReceiver`).
- **Media & Entertainment**: Direct search and playback launches on YouTube and audio streaming apps.
- **Navigation & Environment**: Location querying, Google Maps directions, and weather lookup.

### 3. Voice & Audio Architecture
- **Speech-to-Text (STT)**: Android platform SpeechRecognizer with real-time waveform level monitoring and language tagging.
- **Text-to-Speech (TTS)**: Dynamic pitch, speed adjustment, and locale selection with automatic Hindi/Nepali/English voice fallback.
- **Wake-Word System (`WakeWordService`)**: Foreground service featuring microphone lifecycle coordination, audio focus management, and acoustic energy / syllable-cadence detection.

### 4. Personas, Theming & Memory Persistence
- **Personas**: Switch seamlessly between *Gamak (Default)*, *Chhavi (Helpful Guide)*, *Aarav (Tech Companion)*, or a custom assistant name.
- **Local Persistence (`GamakDatabase`)**: Encrypted on-device Room SQLite database storing conversational memories and key-value preferences.
- **Material 3 Design**: Expressive animations, pulsating voice orb, dynamic themes (Light/Dark), and adaptive edge-to-edge layout.

---

## 🏗️ Architecture & Project Structure

The project strictly follows MVVM (Model-View-ViewModel) and Clean Architecture principles:

```
com.example/
├── GamakApplication.kt        # Application lifecycle & Room DB initialization
├── MainActivity.kt            # Edge-to-edge Compose host & Permissions orchestrator
├── ai/                        # AI & NLU Engine
│   ├── GeminiClient.kt        # Gemini Generative AI client
│   └── LocalNluEngine.kt      # Multilingual zero-latency regex & semantic parser
├── data/                      # Data layer & Assistant engine
│   └── AssistantEngine.kt     # Central coordinator for Speech, NLU, Memory & TTS
├── memory/                    # Room Database persistence
│   ├── MemoryDao.kt           # Memory data access object
│   ├── MemoryEntity.kt        # SQLite entity for user preferences & facts
│   └── GamakDatabase.kt       # Room database definition
├── model/                     # Core domain models
│   ├── ActionModels.kt        # ActionRequest, ActionResult, AiPlanResult
│   ├── AssistantPersona.kt    # Persona presets and tone definitions
│   ├── AssistantState.kt      # State machine (IDLE, LISTENING, THINKING, SPEAKING)
│   ├── ConversationContext.kt # Entity tracking & pronoun resolution
│   └── UserSettings.kt        # User configuration state
├── navigation/                # Compose navigation graphs & routes
├── planner/                   # Tool dispatching & Execution
│   ├── ActionExecutor.kt      # Android Intent & tool dispatcher with transaction locks
│   └── Planner.kt             # Multi-step task decomposition
├── receiver/                  # System broadcast receivers
│   └── ReminderBroadcastReceiver.kt # Notification reminders
├── ui/                        # Jetpack Compose Presentation Layer
│   ├── components/            # VoiceOrb, WaveformVisualizer, QuickActionPills
│   ├── screens/               # MainAssistantScreen, SettingsScreen, AboutScreen
│   └── theme/                 # Material 3 Color Schemes, Typography, Shapes
└── voice/                     # Audio, Speech & Wake-Word services
    ├── speech/                # SpeechRecognizer & TextToSpeech managers
    └── wakeword/              # WakeWordService, WakeWordEngine, Acoustic Detector
```

---

## 🔒 Security & Privacy Notice

### Client-Side API Keys Warning
This repository configures the **Secrets Gradle Plugin** to read `GEMINI_API_KEY` from `.env` or the environment during local development.

> **Production Recommendation:**
> For production applications deployed to public app stores, **never embed API keys directly in the client APK**. Instead, route requests through a secure backend proxy:
> ```
> Android Client  ──(Authenticated HTTPS/gRPC)──>  Your Secure Backend  ──>  Google Gemini API
> ```

### Privacy Safeguards
- **Transparent Microphone Usage**: Microphone recording only activates when explicitly listening or when the user enables the background wake service.
- **Opt-in Memory**: Conversation memory storage is fully user-controlled and can be cleared with a single tap in the Settings screen.
- **No Unsolicited Telemetry**: Audio data is processed on-device for local NLU and is never logged or transmitted to third parties.

---

## 🎙️ Wake-Word Engine Capability & Limitations

### Current Implementation
The built-in `WakeWordEngine` implementation uses an **on-device acoustic syllable and energy cadence detector** (`EnergyPatternWakeEngine`) running inside an Android `ForegroundService` with `FOREGROUND_SERVICE_MICROPHONE` permissions.

- **Capabilities**: Detects vocal energy bursts and cadence triggers matching pre-configured patterns for assistant names (*"Gamak"*, *"हे गमक"*, *"Hey Gamak"*).
- **Honest Limitations**: Syllable-energy heuristics do not perform full deep neural keyword spotting (KWS). Custom arbitrary names use speech recognition fallback when tapped.
- **Extensibility**: The modular `WakeWordEngine` interface is engineered for drop-in integration with production neural KWS SDKs (e.g., Picovoice Porcupine, OpenWakeWord, or ONNX/TFLite models).

---

## 🛠️ Build & Installation

### Prerequisites
- **Android Studio**: Ladybug (2024.2.1+) or newer
- **JDK**: Java 17 (Eclipse Temurin or OpenJDK recommended)
- **Android SDK**: API 36 (Minimum SDK: API 24 / Android 7.0)

### 1. Clone & Setup Secrets
```bash
git clone https://github.com/your-repo/gamak-ai.git
cd gamak-ai

# Copy example environment configuration
cp .env.example .env
```
Edit `.env` to include your Google AI Studio Gemini API Key:
```properties
GEMINI_API_KEY=AIzaSy...YourKeyHere
```

### 2. Build via Command Line
```bash
# Make gradlew executable
chmod +x gradlew

# Run Unit and Robolectric tests
./gradlew testDebugUnitTest

# Assemble Debug APK
./gradlew assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🧪 Testing Suite

Run all local JVM and Robolectric unit tests:
```bash
./gradlew testDebugUnitTest
```

Verified Test Suites:
- `NluPlannerTest`: Multilingual parsing, multi-action orchestration, entity extraction, and pronoun resolution.
- `Phase5WakeWordTest`: Persona configurations, active display names, and wake engine state transitions.
- `ExampleRobolectricTest`: Activity lifecycle, service binding, and Room database migrations.

---

## 🚀 Release Signing Configuration

To build a release-ready APK:
1. Create your release keystore:
   ```bash
   keytool -genkey -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
   ```
2. Export your signing environment variables:
   ```bash
   export KEYSTORE_PATH="/path/to/my-upload-key.jks"
   export STORE_PASSWORD="your-keystore-password"
   export KEY_PASSWORD="your-key-password"
   ```
3. Run the release build:
   ```bash
   ./gradlew assembleRelease
   ```

---

## 📄 License
This project is licensed under the Apache License 2.0.
