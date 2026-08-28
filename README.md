# Gamak AI (गमक) — Production-Grade Multilingual Voice & AI Assistant for Android

**Gamak AI** is a privacy-conscious, multilingual AI and device automation assistant for Android designed for seamless communication across **Hindi (हिंदी), Nepali (नेपाली), Hinglish, and English**.

Built with modern **Jetpack Compose**, **Material 3**, **Kotlin Coroutines/Flow**, **Room Database**, and a **hybrid NLU pipeline** combining zero-latency on-device intent parsing with cloud **Google Gemini AI** reasoning.

---

## 🌟 Key Capabilities & Features

### 1. Hybrid Multilingual NLU & AI Engine
- **On-Device Local NLU (`LocalNluEngine`)**: Instant, zero-latency parsing for common device actions, app navigation, system controls, alarms, timers, calls, and WhatsApp messages in Hindi, Nepali, Hinglish, and English without requiring internet connectivity.
- **Cloud Gemini Reasoning (`GeminiAiClient`)**: Deep contextual understanding, question answering, multi-step intent decomposition, and rich conversational intelligence powered by Google Gemini.
- **Conversational Context & Pronoun Resolution (`ConversationContext`)**: Remembers recently mentioned contacts, apps, times, and locations across multi-turn interactions (e.g., *"उसे call करो"* or *"वो app खोलो"*).
- **Multi-Step Execution Pipeline**: Chains sequential commands seamlessly (e.g., *"सुबह 7 बजे का अलार्म लगाओ और फिर YouTube खोलो"*).

### 2. Device & System Automation Tools
- **Communications**: Intelligent contact lookup with fuzzy matching, contact disambiguation, direct dialer intents, SMS drafts, and WhatsApp messaging flows.
- **App Management**: Dynamic package launcher resolving local aliases (YouTube, Spotify, Camera, Gallery, Settings, Maps, DeskClock, etc.).
- **Productivity & Time**: Alarm scheduling, count-down timers with system clock integration, and local notification reminders with broadcast receivers (`ReminderBroadcastReceiver`).
- **Media & Entertainment**: Direct search and playback launches on YouTube and audio streaming apps.
- **Navigation & Environment**: Location querying, Google Maps directions, and weather lookup.

### 3. Voice & Audio Architecture
- **Speech-to-Text (STT)**: Android platform SpeechRecognizer with real-time waveform level monitoring and language tagging.
- **Text-to-Speech (TTS)**: Dynamic pitch, speed adjustment, and locale selection with automatic Hindi/Nepali/English voice fallback and tap-to-interrupt capability.
- **Wake-Word System (`WakeWordService`)**: Foreground service featuring microphone lifecycle coordination, audio focus management, and acoustic energy / syllable-cadence detection.

### 4. Personas, Theming & Memory Persistence
- **Personas**: Switch seamlessly between *Gamak (Default)*, *Maya (Helpful Guide)*, *Sathi (Friendly Companion)*, *Mitra (Assistant)*, or configure a *Custom Assistant Name*.
- **Local Persistence (`GamakDatabase`)**: On-device Room SQLite database storing conversational memories and key-value preferences with complete user opt-in and memory wipe controls.
- **Material 3 Design**: Expressive animations, pulsating AI orb, dynamic themes (Light/Dark/System), and adaptive edge-to-edge layout.

---

## 🔒 Security & Privacy Architecture

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
- `NluPlannerTest`: Multilingual parsing, multi-action orchestration, entity extraction, pronoun resolution, retry, and confirmation flows.
- `Phase5WakeWordTest`: Persona configurations, active display names, and wake engine state transitions.
- `ExampleRobolectricTest`: Activity lifecycle, service binding, and Room database migrations.

---

## 🚀 Release Configuration & Signing

Refer to [RELEASE_BUILD.md](RELEASE_BUILD.md) for detailed instructions on generating keystores, configuring signing variables, and assembling production Release APKs and AAB bundles.

---

## 📄 License
This project is licensed under the Apache License 2.0.
