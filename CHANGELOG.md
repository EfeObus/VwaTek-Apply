# Changelog

All notable changes to VwaTek Apply will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-02-02

### Added

#### Core Infrastructure
- Initial project setup with Kotlin Multiplatform (Kotlin 2.0.21)
- iOS, Android, and Web platform targets
- Compose Multiplatform 1.7.1 UI framework
- Koin dependency injection
- SQLDelight local database with encryption
- Ktor networking client
- MVI architecture with StateFlow

#### AI Integration
- Gemini 2.0 Flash API integration with streaming support
- OpenAI GPT-4o-mini fallback option
- Configurable API key via secrets.properties
- JSON response parsing with structured prompts

#### Resume Management
- Create, edit, and delete resumes
- Resume content parsing (Summary, Experience, Skills, Education sections)
- Industry and target role tagging
- LocalStorage persistence for web platform

#### ATS Analysis & Optimization
- Match score calculation against job descriptions
- Keyword gap analysis (hard skills, soft skills, industry terms)
- ATS formatting compliance checker
- Section-by-section breakdown scoring
- Overall resume quality metrics

#### X-Y-Z Impact Framework
- Automatic detection of weak experience bullets
- Transformation to "Accomplished [X] as measured by [Y], by doing [Z]" format
- Metric suggestions and impact quantification
- Confidence scoring for generated improvements

#### Grammar & Tone Analysis
- Grammar issue detection with suggestions
- Passive voice identification
- Weak verb replacement recommendations
- Professional tone consistency checking

#### Section-Specific Rewriting
- AI-powered rewriting for Summary, Experience, Skills, Education sections
- Target role and industry customization
- Writing style options (Professional, Conversational, Technical, Executive)
- Keyword integration from job descriptions
- Change tracking and improvement tips

#### PDF Export
- Four professional templates: Professional, Modern, Classic, Minimal
- Print-optimized CSS styling
- Browser print-to-PDF integration (Web)
- Section formatting with proper typography

#### Resume Version Control
- Automatic version creation on edits
- Version history with timestamps
- Version preview functionality
- Restore to previous versions
- Delete individual versions

#### Cover Letter Generation
- Contextual generation from resume + job description
- Company information integration
- Multiple tone options (Professional, Enthusiastic, Formal, Conversational)
- Key highlights extraction

#### Interview Preparation
- Mock interview simulation
- STAR method coaching
- Question generation based on role
- Feedback on response quality

### Security
- AES-256 encryption for local database
- Secure key storage (iOS Keychain, Android Keystore, Web Crypto API)
- TLS 1.3 for all API communications
- No server-side data storage

---

## Version History Format

### [X.Y.Z] - YYYY-MM-DD

#### Added
- New features

#### Changed
- Changes in existing functionality

#### Deprecated
- Soon-to-be removed features

#### Removed
- Removed features

#### Fixed
- Bug fixes

#### Security
- Security improvements or vulnerability fixes

---

## Release Types

| Type | Description |
|------|-------------|
| **Major (X.0.0)** | Breaking changes, major new features |
| **Minor (0.X.0)** | New features, backwards compatible |
| **Patch (0.0.X)** | Bug fixes, minor improvements |
