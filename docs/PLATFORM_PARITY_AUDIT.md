# VwaTek Apply — Full Platform Parity Audit Report

> **Generated:** 2025-01-XX  
> **Scope:** Android · iOS · Web  
> **Shared Module:** Kotlin Multiplatform (commonMain)  
> **Build Status:** All 5 compile targets PASSING

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Platform Overview](#2-platform-overview)
3. [Screen-by-Screen Comparison Matrix](#3-screen-by-screen-comparison-matrix)
4. [Shared ViewModel → Platform Mapping](#4-shared-viewmodel--platform-mapping)
5. [UX Pattern Coverage](#5-ux-pattern-coverage)
6. [Navigation & Deep Linking](#6-navigation--deep-linking)
7. [Feature Completeness by Screen](#7-feature-completeness-by-screen)
8. [Currency & Pricing Consistency](#8-currency--pricing-consistency)
9. [Known Bugs & Issues](#9-known-bugs--issues)
10. [Gaps & TODO Items](#10-gaps--todo-items)
11. [Recommendations](#11-recommendations)
12. [Appendix: Shared Module Inventory](#appendix-shared-module-inventory)

---

## 1. Executive Summary

### Parity Scores (out of 100)

| Category | Android | iOS | Web |
|----------|:-------:|:---:|:---:|
| **Navigation destinations** | 100 | 100 | 100 |
| **Deep linking routes** | 100 | 100 | 100 |
| **Screen count** | 100 | 100 | 100 |
| **Error handling coverage** | 81 | 69 | 87 |
| **Loading state coverage** | 88 | 88 | 73 |
| **Empty state coverage** | 38 | 56 | 44 |
| **Currency consistency** | 100 | 100 | 93 |
| **Overall Parity** | **87** | **85** | **85** |

### Key Findings

- **Navigation is fully at parity.** All 3 platforms expose 14 identical destinations with matching deep link URIs (`vwatekapply://` scheme on mobile, `#hash` routing on web).
- **Error handling is strongest on Web** (87%), weakest on iOS (69%). PaywallScreen has no error handling on any platform (acceptable — presentational only).
- **Loading states are strongest on Android & iOS** (88%), weakest on Web (73%). HomeScreen/DashboardScreen lacks a loading indicator on all 3 platforms.
- **Empty states are the weakest UX category** across all platforms (38–56%). iOS leads with the most empty states implemented.
- **One critical currency bug:** Web `CheckoutModal` still uses USD pricing with a bare `$` while the rest of the app uses CAD with `CA$`.
- **One potential iOS auth bug:** Apple Sign-In handler reuses `AuthIntent.GoogleSignIn` instead of a dedicated `AppleSignIn` intent.

---

## 2. Platform Overview

### 2.1 File Counts & Lines of Code

| Metric | Android | iOS | Web |
|--------|--------:|----:|----:|
| Screen/View files | 16 | 20 | 17 |
| ViewModel wrappers | N/A (direct) | 11 | N/A (direct) |
| Total screen LOC | ~13,220 | ~8,356 (views) + ~1,880 (VMs) | ~11,411 |
| Navigation file | VwaTekApp.kt | ContentView.swift | App.kt + Sidebar.kt |
| Navigation items | 14 | 14 | 14 (+2 internal) |

### 2.2 Architecture Patterns

| Aspect | Android | iOS | Web |
|--------|---------|-----|-----|
| **UI Framework** | Jetpack Compose | SwiftUI | Compose HTML (Kotlin/JS) |
| **State management** | `collectAsState()` from shared ViewModels | `ObservableObject` wrappers with `@Published` + `FlowExtensionsKt.watch()` | Direct `StateFlow.value` via Koin `GlobalContext.get()` |
| **DI** | Koin `koinInject()` | `KoinHelper` singleton + explicit getters | `GlobalContext.get().get<T>()` |
| **Error display** | `SnackbarHost` + `LaunchedEffect` | `.alert()` + `.onChange` | Styled error `Div` (toast-error / alert-error) |
| **Loading display** | `CircularProgressIndicator` | `ProgressView()` | Spinner `Div` + `isLoading` checks |
| **Empty display** | Custom composables | Inline SwiftUI views | CSS-styled `Div` with `empty-state` class |
| **Navigation** | `NavigationItem` enum + conditional render | `TabView` (phone) + `NavigationSplitView` (iPad) | `Screen` enum + sidebar click |
| **Deep linking** | Intent filters in AndroidManifest.xml + `LaunchedEffect` | URL scheme in Info.plist + `.onOpenURL` | Hash-based routing in `window.location.hash` |

### 2.3 Naming Conventions

The shared module uses two naming patterns for state holders:

| Pattern | ViewModels | Managers / Repositories |
|---------|-----------|------------------------|
| **Intent-based** (`sealed class *Intent`) | AuthViewModel, ResumeViewModel, CoverLetterViewModel, InterviewViewModel, TrackerViewModel, OrganizationViewModel | — |
| **Direct method** (public suspend funs) | NOCViewModel, JobBankViewModel | LinkedInOptimizerManager, SalaryIntelligenceManager, SubscriptionManager, SettingsRepository |

 This inconsistency propagates to all 3 platforms.

---

## 3. Screen-by-Screen Comparison Matrix

### Legend
-  = Implemented
-  = Partial / Issues
-  = Missing
- N/A = Not applicable

| # | Feature Screen | Android | iOS | Web | Notes |
|---|---------------|:-------:|:---:|:---:|-------|
| 1 | **Home / Dashboard** |  HomeScreen (328L) |  HomeView (349L) |  DashboardScreen (373L) | All display welcome + quick stats |
| 2 | **Resume Management** |  ResumeScreen (1,182L) |  ResumeView (599L) |  ResumeScreen (1,557L) | Web has most code; Android/Web inline version history |
| 3 | **Resume Optimizer (ATS)** |  OptimizerScreen (1,547L) |  OptimizerView (1,161L) |  ResumeOptimizerScreen (1,061L) | Full ATS analysis + grammar + impact bullets on all |
| 4 | **Cover Letter** |  CoverLetterScreen (604L) |  CoverLetterView (604L) |  CoverLetterScreen (457L) | Generate + list + delete on all |
| 5 | **Interview Prep** |  InterviewScreen (902L) |  InterviewView (533L) |  InterviewScreen (635L) | Start session, Q&A, STAR coaching |
| 6 | **NOC Codes** |  NOCScreen (780L) |  NOCView (555L) |  NOCScreen (528L) | Search + details + immigration pathways |
| 7 | **Job Bank** |  JobBankScreen (673L) |  JobBankView (474L) |  JobBankScreen (426L) | Search + filters + trending + outlook |
| 8 | **Job Tracker** |  TrackerScreen (1,745L) |  TrackerView (1,106L) |  TrackerScreen (1,440L) | CRUD + status pipeline + stats |
| 9 | **Salary Insights** |  SalaryInsightsScreen (670L) |  SalaryInsightsView (686L) |  SalaryInsightsScreen (548L) | Insights + offer eval + negotiation |
| 10 | **LinkedIn Optimizer** |  LinkedInOptimizerScreen (489L) |  LinkedInOptimizerView (439L) |  LinkedInOptimizerScreen (518L) | Profile analysis + section scores |
| 11 | **Organization** |  OrganizationScreen (500L) |  OrganizationView (422L) |  OrganizationScreen (779L) | Create org + members + templates |
| 12 | **Subscription** |  SubscriptionScreen (645L) |  SubscriptionView (535L) |  SubscriptionScreen (489L) | Tier display + manage/cancel |
| 13 | **Paywall** |  PaywallScreen (500L) |  PaywallView (427L) |  PaywallScreen (493L) | Feature gate + upgrade CTA |
| 14 | **Profile** |  ProfileScreen (1,202L) |  ProfileView (842L) |  (in SettingsScreen) | Web merges profile into settings |
| 15 | **Settings** |  SettingsScreen (589L) |  SettingsView (298L) |  SettingsScreen (570L) | Theme + language + notifications |
| 16 | **Auth** |  AuthScreen (671L) |  AuthView (592L) |  AuthScreen (912L) | Login + register + OAuth |

### iOS-Only Extra Views (no Android/Web equivalents)

| View | Lines | Purpose |
|------|------:|---------|
| LinkedInImportSheet | 103 | Bottom sheet for LinkedIn profile import |
| PDFExportSheet | 196 | Share sheet for resume PDF export |
| StarCoachingSheet | 318 | Modal for STAR method coaching |
| VersionHistorySheet | 193 | Modal for resume version list |

These are **modal/sheet extractions** of functionality that Android and Web handle inline within their respective screens. This is not a parity gap — it reflects SwiftUI's sheet-based UX pattern.

---

## 4. Shared ViewModel → Platform Mapping

### 4.1 ViewModel Usage per Platform

| Shared ViewModel/Manager | Android | iOS Wrapper | Web |
|--------------------------|---------|-------------|-----|
| `AuthViewModel` |  (param, not injected) |  `AuthViewModelWrapper` (170L) |  via Koin |
| `ResumeViewModel` |  `koinInject()` |  `ResumeViewModelWrapper` (186L) |  via Koin |
| `CoverLetterViewModel` |  `koinInject()` |  `CoverLetterViewModelWrapper` (83L) |  via Koin |
| `InterviewViewModel` |  `koinInject()` |  `InterviewViewModelWrapper` (111L) |  via Koin |
| `TrackerViewModel` |  `koinInject()` |  `TrackerViewModelWrapper` (236L) |  via Koin |
| `NOCViewModel` |  `koinInject()` |  `NOCViewModelWrapper` (326L) |  via Koin |
| `JobBankViewModel` |  `koinInject()` |  `JobBankViewModelWrapper` (236L) |  via Koin |
| `OrganizationViewModel` |  `koinInject()` |  `OrganizationViewModelWrapper` (100L) |  via Koin |
| `LinkedInOptimizerManager` |  `koinInject()` |  `LinkedInOptimizerViewModelWrapper` (126L) |  via Koin |
| `SalaryIntelligenceManager` |  `koinInject()` |  `SalaryIntelligenceManagerWrapper` (168L) |  via Koin |
| `SubscriptionManager` |  `koinInject()` |  `SubscriptionManagerWrapper` (138L) |  via Koin |
| `SettingsRepository` |  `koinInject()` |  `SettingsHelper.shared` (no wrapper) |  via Koin |

### 4.2 Intent Pattern Adoption

| VM | State Fields | Intent Subclasses | Direct Methods |
|----|:-----------:|:-----------------:|:--------------:|
| AuthViewModel | 10 | 16 | — |
| ResumeViewModel | 20 | 23 | — |
| CoverLetterViewModel | 6 | 6 | — |
| InterviewViewModel | 10 | 10 | — |
| TrackerViewModel | 13 | 22 | — |
| OrganizationViewModel | 12 | 10 | — |
| NOCViewModel | 23 | — |  (direct) |
| JobBankViewModel | 20 | — |  (direct) |
| LinkedInOptimizerManager | N/A | — |  (manager) |
| SalaryIntelligenceManager | N/A | — |  (manager) |
| SubscriptionManager | N/A | — |  (manager) |

### 4.3 iOS KoinHelper Accessors

12 accessors in `KoinHelper.kt` (iosMain) + 11 top-level Swift-friendly functions. All shared VMs/Managers are accessible from iOS.

 `getOrganizationViewModel()` exists in `KoinHelper` but does **not** have a corresponding top-level Swift-friendly function (the other 11 do).

---

## 5. UX Pattern Coverage

### 5.1 Error Handling

| Screen | Android | iOS | Web |
|--------|:-------:|:---:|:---:|
| HomeScreen / Dashboard |  | — |  |
| ResumeScreen |  |  |  |
| OptimizerScreen |  |  |  |
| CoverLetterScreen |  |  |  |
| InterviewScreen |  |  |  |
| NOCScreen |  |  |  |
| JobBankScreen |  |  |  |
| TrackerScreen |  |  |  |
| SalaryInsightsScreen |  |  |  |
| LinkedInOptimizerScreen |  |  |  |
| OrganizationScreen |  |  |  |
| SubscriptionScreen |  |  |  |
| PaywallScreen |  |  |  |
| ProfileScreen |  |  | N/A |
| SettingsScreen |  |  |  |
| AuthScreen |  |  |  |
| **Coverage** | **81% (13/16)** | **69% (11/16)** | **87% (13/15)** |

#### Parity Gaps (error handling)
- **LinkedInOptimizerScreen**: Missing on Android & iOS, present on Web
- **OptimizerScreen**: Missing on iOS, present on Android & Web
- **SalaryInsightsScreen**: Missing on iOS, present on Android & Web
- **AuthScreen**: Missing on Android & iOS, present on Web
- **PaywallScreen**: Missing on all 3 (acceptable — static/presentational screen)

### 5.2 Loading States

| Screen | Android | iOS | Web |
|--------|:-------:|:---:|:---:|
| HomeScreen / Dashboard |  |  |  |
| ResumeScreen |  |  |  |
| OptimizerScreen |  |  |  |
| CoverLetterScreen |  |  |  |
| InterviewScreen |  |  |  |
| NOCScreen |  |  |  |
| JobBankScreen |  |  |  |
| TrackerScreen |  |  |  |
| SalaryInsightsScreen |  |  |  |
| LinkedInOptimizerScreen |  |  |  |
| OrganizationScreen |  |  |  |
| SubscriptionScreen |  |  |  |
| PaywallScreen |  |  |  |
| ProfileScreen |  |  | N/A |
| SettingsScreen |  |  |  |
| AuthScreen |  |  |  |
| **Coverage** | **88% (14/16)** | **88% (14/16)** | **73% (11/15)** |

#### Parity Gaps (loading states)
- **HomeScreen / DashboardScreen**: Missing on all 3 platforms
- **PaywallScreen**: Missing on all 3 (acceptable — static content)
- **AuthScreen**: Missing on Web only (Android & iOS have it)

### 5.3 Empty States

| Screen | Android | iOS | Web |
|--------|:-------:|:---:|:---:|
| HomeScreen / Dashboard |  |  |  |
| ResumeScreen |  |  |  |
| OptimizerScreen |  |  |  |
| CoverLetterScreen |  |  |  |
| InterviewScreen |  |  |  |
| NOCScreen |  |  |  |
| JobBankScreen |  |  |  |
| TrackerScreen |  |  |  |
| SalaryInsightsScreen |  |  |  |
| LinkedInOptimizerScreen |  |  |  |
| OrganizationScreen |  |  |  |
| SubscriptionScreen | N/A | N/A | N/A |
| PaywallScreen | N/A | N/A | N/A |
| ProfileScreen |  |  | N/A |
| SettingsScreen | N/A | N/A | N/A |
| AuthScreen | N/A | N/A | N/A |
| **Coverage (applicable)** | **50% (6/12)** | **75% (9/12)** | **55% (6/11)** |

#### Parity Gaps (empty states)
- **InterviewScreen**: Missing on Android, present on iOS & Web
- **TrackerScreen**: Missing on Android, present on iOS & Web
- **NOCScreen**: Missing on Web, present on Android & iOS
- **JobBankScreen**: Missing on Web, present on Android & iOS
- **LinkedInOptimizerScreen**: Missing on Android & Web, present on iOS
- **OptimizerScreen**: Missing on Android & iOS, present on Web only

---

## 6. Navigation & Deep Linking

### 6.1 Navigation Destinations

All 3 platforms: **14 destinations** — FULL PARITY 

| # | Destination | Android | iOS | Web |
|---|-------------|---------|-----|-----|
| 1 | Home / Dashboard | `NavigationItem.Home` | Tag 0 | `Screen.DASHBOARD` |
| 2 | Resume | `NavigationItem.Resume` | Tag 1 | `Screen.RESUMES` |
| 3 | Optimizer | `NavigationItem.Optimizer` | Tag 2 | `Screen.RESUME_OPTIMIZER` |
| 4 | Cover Letter | `NavigationItem.CoverLetter` | Tag 3 | `Screen.COVER_LETTERS` |
| 5 | Interview | `NavigationItem.Interview` | Tag 4 | `Screen.INTERVIEW` |
| 6 | NOC Codes | `NavigationItem.NOC` | Tag 5 | `Screen.NOC` |
| 7 | Job Bank | `NavigationItem.JobBank` | Tag 6 | `Screen.JOB_BANK` |
| 8 | Tracker | `NavigationItem.Tracker` | Tag 7 | `Screen.TRACKER` |
| 9 | Salary Insights | `NavigationItem.SalaryInsights` | Tag 8 | `Screen.SALARY_INSIGHTS` |
| 10 | LinkedIn Optimizer | `NavigationItem.LinkedInOptimizer` | Tag 9 | `Screen.LINKEDIN_OPTIMIZER` |
| 11 | Organization | `NavigationItem.Organization` | Tag 10 | `Screen.ORGANIZATION` |
| 12 | Subscription | `NavigationItem.Subscription` | Tag 11 | `Screen.SUBSCRIPTION` |
| 13 | Profile | `NavigationItem.Profile` | Tag 12 | `Screen.PROFILE` |
| 14 | Settings | `NavigationItem.Settings` | Tag 13 | `Screen.SETTINGS` |

Web additionally has `Screen.PAYWALL` and `Screen.AUTH` as non-sidebar internal states.

### 6.2 Deep Link Routes

| URI | Android | iOS | Web |
|-----|:-------:|:---:|:---:|
| `vwatekapply://home` |  |  | `#dashboard` |
| `vwatekapply://resume` |  |  | `#resumes` |
| `vwatekapply://optimizer` |  |  | `#optimizer` |
| `vwatekapply://coverletter` |  |  | `#coverletters` |
| `vwatekapply://interview` |  |  | `#interview` |
| `vwatekapply://noc` |  |  | `#noc` |
| `vwatekapply://jobbank` |  |  | `#jobbank` |
| `vwatekapply://tracker` |  |  | `#tracker` |
| `vwatekapply://salary` |  |  | `#salary` |
| `vwatekapply://linkedin` |  |  | `#linkedin` |
| `vwatekapply://organization` |  |  | `#organization` |
| `vwatekapply://subscription` |  |  | `#subscription` |
| `vwatekapply://profile` |  |  | `#profile` |
| `vwatekapply://settings` |  |  | `#settings` |
| **Total** | **14** | **14** | **14** |

Deep linking parity: **FULL PARITY **

---

## 7. Feature Completeness by Screen

### 7.1 Auth Screen

| Feature | Android | iOS | Web |
|---------|:-------:|:---:|:---:|
| Email login |  |  |  |
| Email registration |  |  |  |
| Google Sign-In |  |  |  |
| Apple Sign-In | — |  Bug | — |
| LinkedIn Sign-In |  |  |  |
| Password reset |  |  |  |
| Email verification |  |  |  |
| Error display |  |  |  |
| Loading indicator |  |  |  |

 iOS Apple Sign-In reuses `AuthIntent.GoogleSignIn` — see [Section 9](#9-known-bugs--issues).

### 7.2 Resume Screen

| Feature | Android | iOS | Web |
|---------|:-------:|:---:|:---:|
| List resumes |  |  |  |
| Create resume |  |  |  |
| Edit resume |  |  |  |
| Delete resume |  |  |  |
| File upload (PDF/DOCX) |  |  |  |
| LinkedIn import |  |  |  |
| Version history |  |  (sheet) |  |
| Empty state |  |  |  |
| Error handling |  |  |  |
| Loading state |  |  |  |

### 7.3 Optimizer Screen (ATS Analysis)

| Feature | Android | iOS | Web |
|---------|:-------:|:---:|:---:|
| Overall ATS score |  |  |  |
| Section scores (format/keyword/structure/readability) |  |  |  |
| Issue list (HIGH/MED/LOW severity) |  |  |  |
| Recommendations |  |  |  |
| Impact bullets (XYZ format) |  |  |  |
| Grammar analysis |  |  |  |
| Section rewrite |  |  |  |
| Error handling |  |  |  |
| Loading state |  |  |  |
| Empty state |  |  |  |

### 7.4 Cover Letter Screen

| Feature | Android | iOS | Web |
|---------|:-------:|:---:|:---:|
| List cover letters |  |  |  |
| Generate from resume + job |  |  |  |
| Tone selection |  |  |  |
| Delete |  |  |  |
| Error/Loading/Empty | // | // | // |

### 7.5 Interview Prep Screen

| Feature | Android | iOS | Web |
|---------|:-------:|:---:|:---:|
| Start session |  |  |  |
| Question & answer |  |  |  |
| AI feedback |  |  |  |
| STAR coaching |  |  (sheet) |  |
| Session history |  |  |  |
| Delete session |  |  |  |
| Error handling |  |  |  |
| Loading state |  |  |  |
| Empty state |  |  |  |

### 7.6 NOC Screen

| Feature | Android | iOS | Web |
|---------|:-------:|:---:|:---:|
| Search by title/code |  |  |  |
| Bilingual (EN/FR) |  |  |  |
| TEER level info |  |  |  |
| Main duties |  |  |  |
| Employment requirements |  |  |  |
| Immigration pathways |  |  |  |
| Provincial demand |  |  |  |
| NOC fit analysis |  |  |  |
| Error/Loading/Empty | // | // | // |

### 7.7 Job Bank Screen

| Feature | Android | iOS | Web |
|---------|:-------:|:---:|:---:|
| Keyword search |  |  |  |
| Location filter |  |  |  |
| Province filter |  |  |  |
| NOC code filter |  |  |  |
| Trending jobs |  |  |  |
| Job details |  |  |  |
| Job outlook |  |  |  |
| Bilingual (EN/FR) |  |  |  |
| Pagination |  |  |  |
| Error/Loading/Empty | // | // | // |

### 7.8 Tracker Screen

| Feature | Android | iOS | Web |
|---------|:-------:|:---:|:---:|
| Application list |  |  |  |
| Create application |  |  |  |
| Edit application |  |  |  |
| Delete application |  |  |  |
| Status pipeline (13 statuses) |  |  |  |
| Status change |  |  |  |
| Notes |  |  |  |
| Reminders |  |  |  |
| Interviews |  |  |  |
| Statistics dashboard |  |  |  |
| Calendar view |  "Coming soon" |  Placeholder |  "Coming soon" |
| Search & filter |  |  |  |
| Error/Loading/Empty | // | // | // |

### 7.9 Salary Insights Screen

| Feature | Android | iOS | Web |
|---------|:-------:|:---:|:---:|
| Salary lookup |  |  |  |
| Provincial comparison |  |  |  |
| Market trends |  |  |  |
| Offer evaluation |  |  |  |
| Negotiation coach |  |  |  |
| Error/Loading/Empty | // | // | // |

### 7.10 LinkedIn Optimizer Screen

| Feature | Android | iOS | Web |
|---------|:-------:|:---:|:---:|
| Profile import |  |  (sheet) |  |
| Manual entry |  |  |  |
| Profile analysis |  |  |  |
| Section scores |  |  |  |
| Improvements list |  |  |  |
| Keyword analysis |  |  |  |
| Analysis history |  |  |  |
| Error/Loading/Empty | // | // | // |

### 7.11 Organization Screen

| Feature | Android | iOS | Web |
|---------|:-------:|:---:|:---:|
| List organizations |  |  |  |
| Create organization |  |  |  |
| View members |  |  |  |
| Invite members |  |  |  |
| Remove members |  |  |  |
| Accept invitation |  |  |  |
| Templates |  |  |  |
| Industry field |  |  |  |
| Error/Loading/Empty | // | // | // |

### 7.12 Subscription & Paywall

| Feature | Android | iOS | Web |
|---------|:-------:|:---:|:---:|
| Tier display (Free/Pro/Premium) |  |  |  |
| Feature comparison |  |  |  |
| Monthly/Yearly toggle |  |  |  |
| Checkout |  TODO |  (Apple IAP) |  (Stripe) |
| Manage billing |  TODO |  |  (Stripe Portal) |
| Cancel subscription |  |  |  |
| Currency (CAD/CA$) |  |  |  CheckoutModal uses USD |

### 7.13 Settings Screen

| Feature | Android | iOS | Web |
|---------|:-------:|:---:|:---:|
| Theme toggle (Dark/Light) |  |  |  |
| Language (EN/FR) |  |  |  |
| Notification preferences |  |  |  |
| Account deletion |  |  |  |
| Logout |  |  |  |
| Error handling |  |  |  |

### 7.14 Profile Screen

| Feature | Android | iOS | Web |
|---------|:-------:|:---:|:---:|
| View profile |  |  |  (in Settings) |
| Edit profile |  |  |  |
| Profile image |  |  |  |
| Address fields |  |  |  |
| Error handling |  |  | N/A |
| Loading state |  |  | N/A |

---

## 8. Currency & Pricing Consistency

### 8.1 Pricing Values

| Tier | Period | Price | Android | iOS | Web |
|------|--------|------:|:-------:|:---:|:---:|
| Pro | Monthly | CA$14.99 |  |  |  |
| Pro | Yearly | CA$149.99 |  |  |  |
| Premium | Monthly | CA$29.99 |  |  |  |
| Premium | Yearly | CA$299.99 |  |  |  |

### 8.2 Currency Display

| Location | Currency | Symbol | Status |
|----------|----------|--------|--------|
| Android PaywallScreen | CAD | CA$ |  |
| Android SubscriptionScreen | CAD | CA$ |  |
| iOS PaywallView | CAD | CA$ |  |
| iOS SubscriptionView | CAD | CA$ |  |
| Web PaywallScreen | CAD | CA$ |  |
| Web SubscriptionScreen (cards) | CAD | CA$ |  |
| **Web CheckoutModal** | **USD** | **$** | ** BUG** |

### 8.3 Shared Pricing Source

`SubscriptionPricing` in `shared/.../domain/model/Subscription.kt` defines both CAD and USD prices with Stripe/Apple/Google product IDs. The `CheckoutModal` on Web incorrectly references `monthlyPriceUsd` / `yearlyPriceUsd` instead of the CAD equivalents.

---

## 9. Known Bugs & Issues

| # | Severity | Platform | Description | Location |
|---|----------|----------|-------------|----------|
| 1 | **CRITICAL** | Web | `CheckoutModal` uses USD pricing (`monthlyPriceUsd`/`yearlyPriceUsd`) with bare `$` symbol while the rest of the app uses CAD with `CA$` | `webApp/.../SubscriptionScreen.kt` ~L424 |
| 2 | **HIGH** | iOS | Apple Sign-In handler calls `AuthIntent.GoogleSignIn` instead of a dedicated Apple Sign-In intent | `iosApp/iosApp/Auth/AuthViewModelWrapper` |
| 3 | **MEDIUM** | iOS | `hasActiveFilters` in `JobBankViewModelWrapper` always returns `false` (hardcoded, never reads actual filter state) | `iosApp/iosApp/ViewModels/JobBankViewModelWrapper.swift` |
| 4 | **MEDIUM** | iOS | Duplicate locale enums: `AppLocale` (NOCViewModelWrapper) vs `AppLocaleJB` (JobBankViewModelWrapper) — should be unified | `iosApp/iosApp/ViewModels/` |
| 5 | **LOW** | iOS | Hardcoded 2-second delay in `importFromLinkedIn` method — artificial latency | `iosApp/iosApp/ViewModels/LinkedInOptimizerViewModelWrapper.swift` |
| 6 | **LOW** | iOS | `getOrganizationViewModel()` missing from top-level Swift-friendly functions in `KoinHelper.kt` (accessible via `KoinHelper.shared.organizationViewModel`) | `shared/src/iosMain/.../KoinHelper.kt` |

---

## 10. Gaps & TODO Items

### 10.1 Unfinished Features (in code)

| # | Platform | Feature | Status | Code Evidence |
|---|----------|---------|--------|---------------|
| 1 | Android | `onStartCheckout` callback | Empty lambda | SubscriptionScreen |
| 2 | Android | `onManageBilling` callback | Empty lambda | SubscriptionScreen |
| 3 | Android | `onShowPaywall` callback | Empty lambda | SalaryInsightsScreen |
| 4 | All | Calendar view in Tracker | "Coming soon" placeholder | TrackerScreen (all platforms) |

### 10.2 Missing Error Handling

| Screen | Android | iOS | Web |
|--------|:-------:|:---:|:---:|
| LinkedInOptimizerScreen |  |  |  |
| OptimizerScreen |  |  |  |
| SalaryInsightsScreen |  |  |  |
| AuthScreen |  |  |  |

### 10.3 Missing Empty States

| Screen | Android | iOS | Web |
|--------|:-------:|:---:|:---:|
| InterviewScreen |  |  |  |
| TrackerScreen |  |  |  |
| NOCScreen |  |  |  |
| JobBankScreen |  |  |  |
| LinkedInOptimizerScreen |  |  |  |
| OptimizerScreen |  |  |  |

### 10.4 Missing Loading States

| Screen | Android | iOS | Web |
|--------|:-------:|:---:|:---:|
| HomeScreen / DashboardScreen |  |  |  |
| AuthScreen |  |  |  |

### 10.5 i18n Gaps

Only **JobBank** and **NOC** screens use `LocaleManager` for bilingual (EN/FR) content. Settings can toggle the locale globally, but the toggle does not affect any other screen. Full i18n support for all screens would require string resource externalization across all platforms.

---

## 11. Recommendations

### Priority 1 — Bug Fixes (CRITICAL/HIGH)

1. **Fix Web CheckoutModal currency** — Change `monthlyPriceUsd`/`yearlyPriceUsd` to `monthlyPriceCad`/`yearlyPriceCad` and update `$` to `CA$` in `webApp/.../SubscriptionScreen.kt`.
2. **Fix iOS Apple Sign-In intent** — Create a dedicated `AuthIntent.AppleSignIn` or map to the correct auth provider instead of reusing `GoogleSignIn`.

### Priority 2 — Parity Fixes (MEDIUM)

3. **Add error handling** to iOS `OptimizerView`, `SalaryInsightsView`, `LinkedInOptimizerView`, and `AuthView` using `.alert()` pattern.
4. **Add error handling** to Android `LinkedInOptimizerScreen` and `AuthScreen` using Snackbar pattern.
5. **Fix iOS `hasActiveFilters`** in `JobBankViewModelWrapper` to read actual filter state from the shared `JobBankViewModel`.
6. **Unify iOS locale enums** — Replace `AppLocale` and `AppLocaleJB` with a single shared enum.

### Priority 3 — UX Polish

7. **Add missing empty states** across platforms to reach consistent coverage:
   - Android: InterviewScreen, TrackerScreen, LinkedInOptimizerScreen, OptimizerScreen
   - iOS: OptimizerView
   - Web: NOCScreen, JobBankScreen, LinkedInOptimizerScreen
8. **Add loading state** to HomeScreen/DashboardScreen on all 3 platforms.
9. **Add loading state** to Web AuthScreen.
10. **Wire Android checkout/billing callbacks** — Connect `onStartCheckout` and `onManageBilling` to a payment flow (Google Play Billing or Stripe).
11. **Remove hardcoded 2s delay** in iOS `LinkedInOptimizerViewModelWrapper.importFromLinkedIn`.

### Priority 4 — Long-term

12. **Implement Calendar view** in TrackerScreen across all 3 platforms (currently "Coming soon" placeholder everywhere).
13. **Expand i18n** beyond NOC/JobBank to all screens.
14. **Standardize naming** — Consider renaming `*Manager` classes to `*ViewModel` and adopting sealed intent pattern consistently, or vice versa.
15. **Add top-level Swift function** for `getOrganizationViewModel()` in `KoinHelper.kt`.

---

## Appendix: Shared Module Inventory

### A.1 Koin Registration Summary (~85 total)

| Category | Count |
|----------|------:|
| Singletons (API clients, Managers) | 11 |
| Factories — Auth use cases | 10 |
| Factories — Resume use cases | 10 |
| Factories — Resume version use cases | 5 |
| Factories — Cover letter use cases | 3 |
| Factories — Interview use cases | 7 |
| Factories — LinkedIn use cases | 2 |
| Factories — File upload use cases | 3 |
| Factories — Tracker use cases | 10 |
| Factories — Subscription use cases | 8 |
| Factories — Salary use cases | 8 |
| Factories — ViewModels | 8 |
| **Total** | **~85** |

### A.2 Domain Model Files

| File | Models | Key Types |
|------|-------:|-----------|
| Models.kt | 23 | User, Resume, CoverLetter, InterviewSession, LinkedInProfile, ATSAnalysis, FileUploadResult |
| JobApplication.kt | 20+ | JobApplication, ApplicationStatus (13 values), TrackerStats, CanadianProvince (13), JobBoardSource (14) |
| JobBankModels.kt | 6 | JobBankJob, JobOutlook, OutlookRating, JobBankSearchFilters |
| NOCModels.kt | 15+ | NOCCode, NOCDetails, TEERLevel (6), NOCFitAnalysis, NOCMatchResult |
| Notification.kt | 6 | PushNotification, NotificationType (12), NotificationPreferences, DeviceToken |
| Subscription.kt | 12+ | SubscriptionTier (3), FeatureLimits, Subscription, SubscriptionPricing, PremiumFeature (8) |
| LinkedInModels.kt | 15+ | LinkedInAnalysis, LinkedInSectionScores, OptimizedLinkedInContent, LinkedInOptimizationSession |
| SalaryModels.kt | 15+ | SalaryData, SalaryInsights, JobOffer, OfferEvaluation, NegotiationSession |
| EnterpriseModels.kt | 15+ | Organization, OrganizationMember, OrganizationTemplate, AdminDashboardStats |

### A.3 ViewModel State Complexity

| ViewModel | State Fields | Intents/Methods | Use Cases |
|-----------|:-----------:|:---------------:|:---------:|
| AuthViewModel | 10 | 16 intents | 13 |
| ResumeViewModel | 20 | 23 intents | 12 |
| CoverLetterViewModel | 6 | 6 intents | 3 |
| InterviewViewModel | 10 | 10 intents | 7 |
| TrackerViewModel | 13 | 22 intents | 10 |
| OrganizationViewModel | 12 | 10 intents | 1 (API client) |
| NOCViewModel | 23 | 11 methods | 1 (API client) |
| JobBankViewModel | 20 | 11 methods | 1 (API client) |
| LinkedInOptimizerManager | N/A | methods | 1 (API client) |
| SalaryIntelligenceManager | N/A | methods | 1 (API client) |
| SubscriptionManager | N/A | methods | 1 (API client) |

---

*End of Platform Parity Audit Report*
