# Changelog

All notable changes to VwaTek Apply will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - 2026-02-11

### Added - Phase 2: Core Feature Expansion

#### Job Application Tracker
- Comprehensive job tracker with Kanban, List, and Calendar views
- JobTrackerApiClient for backend communication (`/api/v1/jobs/*`)
- 13 application statuses: SAVED → APPLIED → INTERVIEW stages → OFFER → ACCEPTED/REJECTED
- Canadian-specific tracking: Province selection, NOC codes, LMIA/work permit support
- Job source tracking: LinkedIn, Indeed, Glassdoor, Job Bank Canada, Monster, etc.
- TrackerViewModel with full MVI state management
- TrackerUseCases with 10 use case implementations bridging ViewModel to API
- Full platform coverage:
  - Android: `TrackerScreen.kt` with Compose Material 3
  - iOS: `TrackerView.swift` with SwiftUI + `TrackerViewModelWrapper.swift`
  - Web: `TrackerScreen.kt` with Compose for Web

#### Tracker Statistics
- Real-time statistics: Total applications, applied count, interview count, offer count
- Calculated metrics: Interview rate, offer rate
- Response time tracking

#### Application Details
- Notes with types: GENERAL, FOLLOW_UP, INTERVIEW_FEEDBACK, RESEARCH
- Reminders: FOLLOW_UP, INTERVIEW, DEADLINE, ASSESSMENT
- Interview tracking: PHONE, VIDEO, ONSITE, TECHNICAL, BEHAVIORAL, PANEL, FINAL
- Status history with transition audit trail

#### Notification System
- Push notification infrastructure
- Device token management
- Notification database tables
- Notification API routes

#### Chrome Extension
- Content scripts for LinkedIn, Indeed, Glassdoor, Job Bank Canada
- One-click job saving from job boards
- Modern popup UI with authentication
- Background sync to backend API

### Changed
- Updated Modules.kt with JobTrackerApiClient, TrackerUseCases, and TrackerViewModel DI bindings
- Updated KoinHelper.kt for iOS to expose TrackerViewModel
- Chrome Extension API endpoints aligned to `/api/v1/jobs/*`

### Fixed
- Chrome Extension API endpoint mismatches corrected
- DTO mapper issues in TrackerUseCases (enum-to-String, missing parameters)
- Added missing `GET /api/v1/jobs/reminders/upcoming` endpoint

---

## [1.1.0] - 2026-02-11

### Added - Phase 1: Foundation & Infrastructure

#### Observability & Monitoring
- Firebase Crashlytics integration for Android and iOS crash reporting
- Sentry integration for web error tracking and performance monitoring
- Shared analytics framework with platform-specific implementations
- Backend APM with Micrometer and Prometheus metrics
- `/metrics` endpoint for Prometheus scraping
- `/health` endpoint for health checks

#### Cross-Device Sync Engine
- Sync database tables (sync_operations, sync_conflicts)
- SyncEngine with platform-specific implementations (Android, iOS, Web)
- SyncApiClient for backend communication
- Automatic sync on network availability
- Conflict detection and resolution
- NetworkMonitor for real-time connectivity status

#### Canadian Data Residency
- Infrastructure deployed to northamerica-northeast1 (Montreal)
- Terraform configuration for Canadian region
- PIPEDA-compliant data handling

#### Privacy & Compliance (PIPEDA)
- ConsentManager with platform-specific storage
- Consent preferences (analytics, data sharing, marketing)
- PrivacyApiClient for backend communication
- Data export endpoint (POST /api/v1/privacy/export)
- Data deletion endpoint (POST /api/v1/privacy/delete)
- Consent audit trail with timestamps and IP logging
- Privacy database tables (consent_records, data_export_requests, deletion_requests)

#### API Infrastructure
- Centralized ApiConfig for URL management
- SyncApiClient with full CRUD operations
- PrivacyApiClient for consent and data requests
- Backend routes: /api/v1/sync/*, /api/v1/privacy/*

### Changed
- AndroidAuthRepository now calls backend API (previously local-only)
- All API URLs now use Canadian region endpoints
- Updated documentation for Phase 1 features

### Security
- PIPEDA compliance with explicit consent collection
- Quebec Law 25 readiness
- Consent withdrawal mechanism
- 72-hour data export fulfillment
- 30-day account deletion processing

---

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
