# VwaTek Apply

<p align="center">
  <img src="assets/logo.png" alt="VwaTek Apply Logo" width="200"/>
</p>

<p align="center">
  <strong>Professional AI Career Suite</strong><br>
  Transform your job hunt from a manual grind into an automated, data-driven strategy
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-iOS%20%7C%20Android%20%7C%20Web-blue?style=flat-square" alt="iOS | Android | Web"/>
  <img src="https://img.shields.io/badge/Language-Kotlin-purple?style=flat-square&logo=kotlin" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/UI-Compose%20Multiplatform-green?style=flat-square" alt="Compose"/>
  <img src="https://img.shields.io/badge/AI-Gemini%202.0%20Flash-orange?style=flat-square&logo=google" alt="Gemini"/>
</p>

---

## Overview

VwaTek Apply is a high-performance job application assistant built with **Kotlin Multiplatform**, delivering a native experience for **iOS, Android, and Web**. Powered by **Gemini 2.0 Flash**, it doesn't just write for you—it analyzes, optimizes, and coaches you through every stage of the hiring pipeline, from the initial "Apply" button to the final interview.

## Ground Rules

| Rule | Description |
|------|-------------|
| **No Emojis** | Emojis are not permitted in the application UI or codebase |
| **Clipart Allowed** | Vector icons and clipart graphics may be used for visual elements |

## Core Features

### 1. Resume Review & ATS Optimization

| Feature | Description |
|---------|-------------|
| **Match Scoring** | Compare your resume against specific job descriptions to get a compatibility percentage with detailed breakdown |
| **Keyword Gap Analysis** | Identify critical hard skills, soft skills, and industry-specific terms missing from your profile |
| **ATS Formatting Analysis** | Check resume formatting compliance with Applicant Tracking Systems including structure, length, and keyword density |
| **X-Y-Z Impact Framework** | Transform experience bullets using the "Accomplished [X] as measured by [Y], by doing [Z]" format with metrics |
| **Grammar & Tone Polishing** | AI-powered analysis of grammar issues, passive voice, weak verbs, and professional tone |
| **Section-Specific Rewriting** | Rewrite individual resume sections (Summary, Experience, Skills, Education) tailored to target roles |

### 2. Intelligent Document Generation

| Feature | Description |
|---------|-------------|
| **Contextual Rewriting** | Automatically adjust your resume's professional summary and bullet points to mirror job requirements |
| **Tailored Cover Letters** | Generate unique, non-generic cover letters highlighting the intersection of your skills and company needs |
| **Multi-Version Management** | Maintain a library of resumes for different industries and roles |
| **Resume Version Control** | Track changes with full version history, preview previous versions, and restore to any point |

### 3. AI Interview Preparation

| Feature | Description |
|---------|-------------|
| **Tough Recruiter Simulation** | Engage in realistic mock interviews with an AI acting as a skeptical recruiter |
| **STAR Method Coaching** | Structure your experiences into Situation, Task, Action, and Result format |
| **Feedback Loop** | Input drafted answers and receive AI coaching on tone, clarity, and keyword inclusion |

### 4. Native Mobile & Web Experience

| Feature | Description |
|---------|-------------|
| **PDF Export** | Professional formatting with 4 template styles (Professional, Modern, Classic, Minimal) and instant export |
| **On-Device Security** | SQLDelight with encryption ensures your personal work history stays private |
| **Real-time Streaming** | View AI suggestions word-by-word for a responsive, modern UI experience |
| **Browser Print-to-PDF** | Web version supports native browser print dialog for PDF generation |

## Technical Stack

```
+-------------------------------------------------------------+
|                      VwaTek Apply                           |
+-------------------------------------------------------------+
|  Language             |  Kotlin 100%                        |
|  UI Framework         |  Compose Multiplatform              |
|  Target Platforms     |  iOS, Android, Web                  |
|  AI Engine            |  Gemini 2.0 Flash                   |
|  Data Persistence     |  SQLDelight (Local Database)        |
|  Networking           |  Ktor                               |
|  Dependency Injection |  Koin                               |
+-------------------------------------------------------------+
```

## Getting Started

### Prerequisites

- **Xcode 16+** (for iOS development)
- **Android Studio** or **JetBrains Fleet** (for Kotlin Multiplatform development)
- **JDK 17+** (for Kotlin compilation)
- **Node.js 18+** (for Web development)
- **Gemini API Key** (Obtained from [Google AI Studio](https://aistudio.google.com/))

### Installation

1. **Clone the Repository**
   ```bash
   git clone https://github.com/your-username/vwatek-apply.git
   cd vwatek-apply
   ```

2. **Add API Credentials**
   
   Create a `secrets.properties` file in the root directory:
   ```properties
   GEMINI_API_KEY=your_actual_key_here
   ```

3. **Build and Run**
   
   **For iOS:**
   Open the project in your IDE, select the `iosApp` target, and run it on a simulator or physical iPhone.
   
   **For Android:**
   Open the project in Android Studio, select the `androidApp` target, and run it on an emulator or physical device.
   
   **For Web:**
   Run the following command to start the development server:
   ```bash
   ./gradlew :webApp:jsBrowserDevelopmentRun
   ```
   Access the application at `http://localhost:8080`

## Project Structure

```
vwatek-apply/
|-- shared/                          # Shared Kotlin Multiplatform code
|   |-- src/
|   |   |-- commonMain/              # Common business logic
|   |   |   |-- kotlin/
|   |   |   |   |-- data/            # Data layer (repositories, models)
|   |   |   |   |-- domain/          # Domain layer (use cases)
|   |   |   |   |-- presentation/    # Presentation layer (ViewModels)
|   |   |   |   +-- di/              # Dependency injection modules
|   |   |   +-- sqldelight/          # Database schema definitions
|   |   |-- androidMain/             # Android-specific implementations
|   |   |-- iosMain/                 # iOS-specific implementations
|   |   |-- jsMain/                  # Web/JS-specific implementations
|   |   +-- commonTest/              # Shared unit tests
|   +-- build.gradle.kts
|-- androidApp/                      # Android application
|   |-- src/
|   |   +-- main/
|   |       |-- kotlin/              # Android-specific UI code
|   |       +-- res/                 # Android resources
|   +-- build.gradle.kts
|-- iosApp/                          # iOS application
|   |-- iosApp/
|   |   |-- ContentView.swift        # SwiftUI wrapper
|   |   +-- iOSApp.swift             # App entry point
|   +-- iosApp.xcodeproj
|-- webApp/                          # Web application
|   |-- src/
|   |   +-- jsMain/
|   |       |-- kotlin/              # Web-specific UI code
|   |       +-- resources/           # Web resources (index.html, CSS)
|   +-- build.gradle.kts
|-- docs/                            # Documentation
|-- assets/                          # Images and resources
|-- secrets.properties               # API credentials (not committed)
|-- build.gradle.kts                 # Root build configuration
|-- settings.gradle.kts              # Project settings
+-- README.md
```

## Configuration

### Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `GEMINI_API_KEY` | Your Gemini API key from Google AI Studio | Yes |

### Build Variants

| Variant | Description |
|---------|-------------|
| `debug` | Development build with logging enabled |
| `release` | Production build with optimizations |

## Documentation

| Document | Description |
|----------|-------------|
| [Architecture Guide](docs/ARCHITECTURE.md) | System architecture and design patterns |
| [API Reference](docs/API.md) | Gemini API integration documentation |
| [iOS Platform Guide](docs/IOS_PLATFORM.md) | iOS-specific implementation details |
| [Android Platform Guide](docs/ANDROID_PLATFORM.md) | Android-specific implementation details |
| [Web Platform Guide](docs/WEB_PLATFORM.md) | Web-specific implementation details |
| [Code Style Guide](docs/CODE_STYLE.md) | Coding standards and conventions |
| [Contributing Guide](docs/CONTRIBUTING.md) | How to contribute to the project |
| [Security Policy](docs/SECURITY.md) | Security guidelines and practices |
| [FAQ](docs/FAQ.md) | Frequently asked questions |
| [Changelog](CHANGELOG.md) | Version history and release notes |

## Roadmap

- [ ] **Voice Mock Interviews** - Real-time audio practice for interview prep using Gemini Live
- [ ] **LinkedIn Integration** - One-click import for work history and profile data
- [ ] **Application Tracker** - Kanban-style board to track the status of every job application

## Contributing

We welcome contributions! Please see our [Contributing Guide](docs/CONTRIBUTING.md) for details.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Google Gemini](https://ai.google.dev/)
- [SQLDelight](https://cashapp.github.io/sqldelight/)
- [Ktor](https://ktor.io/)
- [Koin](https://insert-koin.io/)

---

<p align="center">
  Made by the VwaTek Team
</p>
