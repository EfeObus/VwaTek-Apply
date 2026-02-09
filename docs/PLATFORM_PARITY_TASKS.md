# VwaTek Apply - Platform Parity Task Schedule

## Overview

This document outlines the step-by-step tasks required to achieve feature parity across Web, Android, and iOS platforms. Tasks are organized by priority and grouped by feature area.

**Document Created:** February 8, 2026  
**Target Completion:** TBD  
**Platforms:** Web (Kotlin/JS), Android (Kotlin), iOS (Swift/SwiftUI)

---

## Task Status Legend

- `[ ]` - Not Started
- `[~]` - In Progress
- `[x]` - Completed
- `[!]` - Blocked

---

## Phase 1: Critical Navigation and Structure Fixes

### 1.1 Android - Add Resume Optimizer Navigation

**Priority:** Critical  
**Estimated Effort:** 2-4 hours  
**Dependencies:** None

| Task ID | Description | Status |
|---------|-------------|--------|
| 1.1.1 | Add `Optimizer` to `NavigationItem` enum in `VwaTekApp.kt` | [ ] |
| 1.1.2 | Create optimizer icon resources or use existing Material icons | [ ] |
| 1.1.3 | Update `NavigationBar` to include Optimizer item | [ ] |
| 1.1.4 | Update `NavigationRail` for tablet layout to include Optimizer | [ ] |
| 1.1.5 | Update `ScreenContent` composable to route to `OptimizerScreen` | [ ] |
| 1.1.6 | Remove `OptimizerScreen` from sub-navigation (AppDestination) | [ ] |
| 1.1.7 | Test navigation on phone and tablet layouts | [ ] |

**Files to Modify:**
- `androidApp/src/androidMain/kotlin/com/vwatek/apply/android/ui/VwaTekApp.kt`

---

### 1.2 Android - Add Cover Letter Navigation

**Priority:** Critical  
**Estimated Effort:** 2-4 hours  
**Dependencies:** None

| Task ID | Description | Status |
|---------|-------------|--------|
| 1.2.1 | Add `CoverLetter` to `NavigationItem` enum | [ ] |
| 1.2.2 | Update bottom navigation bar with Cover Letter item | [ ] |
| 1.2.3 | Update navigation rail for tablets | [ ] |
| 1.2.4 | Update `ScreenContent` to route to `CoverLetterScreen` | [ ] |
| 1.2.5 | Remove `CoverLetterScreen` from sub-navigation | [ ] |
| 1.2.6 | Adjust `CoverLetterScreen` to remove back navigation (now primary screen) | [ ] |
| 1.2.7 | Test navigation flow | [ ] |

**Files to Modify:**
- `androidApp/src/androidMain/kotlin/com/vwatek/apply/android/ui/VwaTekApp.kt`
- `androidApp/src/androidMain/kotlin/com/vwatek/apply/android/ui/screens/CoverLetterScreen.kt`

---

## Phase 2: Authentication Feature Parity

### 2.1 Android - Implement Google Sign-In

**Priority:** High  
**Estimated Effort:** 8-12 hours  
**Dependencies:** Google Cloud Console configuration

| Task ID | Description | Status |
|---------|-------------|--------|
| 2.1.1 | Add Google Sign-In dependencies to `androidApp/build.gradle.kts` | [ ] |
| 2.1.2 | Configure OAuth client ID in Google Cloud Console | [ ] |
| 2.1.3 | Add `google-services.json` to project | [ ] |
| 2.1.4 | Create `GoogleSignInHelper` utility class | [ ] |
| 2.1.5 | Update `AuthScreen` to include Google Sign-In button | [ ] |
| 2.1.6 | Implement sign-in flow with credential manager | [ ] |
| 2.1.7 | Connect to shared `AuthViewModel` via `GoogleSignIn` intent | [ ] |
| 2.1.8 | Handle sign-in errors and display to user | [ ] |
| 2.1.9 | Test complete flow on device | [ ] |

**Files to Modify:**
- `androidApp/build.gradle.kts`
- `androidApp/src/androidMain/kotlin/com/vwatek/apply/android/ui/screens/AuthScreen.kt`
- Create: `androidApp/src/androidMain/kotlin/com/vwatek/apply/android/util/GoogleSignInHelper.kt`

---

### 2.2 iOS - Implement Google Sign-In

**Priority:** High  
**Estimated Effort:** 6-8 hours  
**Dependencies:** Google Cloud Console configuration

| Task ID | Description | Status |
|---------|-------------|--------|
| 2.2.1 | Add GoogleSignIn SDK via SPM or CocoaPods | [ ] |
| 2.2.2 | Configure URL schemes in Info.plist | [ ] |
| 2.2.3 | Create `GoogleSignInManager` class | [ ] |
| 2.2.4 | Replace TODO in `SocialLoginButton` for Google | [ ] |
| 2.2.5 | Implement sign-in delegate methods | [ ] |
| 2.2.6 | Connect to `AuthViewModelWrapper` | [ ] |
| 2.2.7 | Test complete flow on simulator and device | [ ] |

**Files to Modify:**
- `iosApp/project.yml` or Xcode project settings
- `iosApp/iosApp/Info.plist`
- `iosApp/iosApp/Views/AuthView.swift`
- Create: `iosApp/iosApp/Managers/GoogleSignInManager.swift`

---

### 2.3 iOS - Implement Apple Sign-In

**Priority:** High  
**Estimated Effort:** 4-6 hours  
**Dependencies:** Apple Developer account configuration

| Task ID | Description | Status |
|---------|-------------|--------|
| 2.3.1 | Enable Sign in with Apple capability in Xcode | [ ] |
| 2.3.2 | Create `AppleSignInManager` class | [ ] |
| 2.3.3 | Replace TODO in `SocialLoginButton` for Apple | [ ] |
| 2.3.4 | Implement ASAuthorizationController delegate | [ ] |
| 2.3.5 | Handle credential response and create user | [ ] |
| 2.3.6 | Add `AppleSignIn` intent to shared AuthViewModel | [ ] |
| 2.3.7 | Test on physical device (required for Apple Sign-In) | [ ] |

**Files to Modify:**
- `iosApp/iosApp/Views/AuthView.swift`
- `shared/src/commonMain/kotlin/com/vwatek/apply/presentation/auth/AuthViewModel.kt`
- Create: `iosApp/iosApp/Managers/AppleSignInManager.swift`

---

### 2.4 Android - Implement LinkedIn Sign-In

**Priority:** Medium  
**Estimated Effort:** 6-8 hours  
**Dependencies:** LinkedIn Developer Console configuration

| Task ID | Description | Status |
|---------|-------------|--------|
| 2.4.1 | Register Android app in LinkedIn Developer Console | [ ] |
| 2.4.2 | Create `LinkedInAuthActivity` for OAuth callback | [ ] |
| 2.4.3 | Add LinkedIn sign-in button to `AuthScreen` | [ ] |
| 2.4.4 | Implement OAuth 2.0 flow using Custom Tabs | [ ] |
| 2.4.5 | Handle callback and extract authorization code | [ ] |
| 2.4.6 | Connect to shared ViewModel | [ ] |
| 2.4.7 | Test complete flow | [ ] |

**Files to Modify:**
- `androidApp/src/androidMain/AndroidManifest.xml`
- `androidApp/src/androidMain/kotlin/com/vwatek/apply/android/ui/screens/AuthScreen.kt`
- Create: `androidApp/src/androidMain/kotlin/com/vwatek/apply/android/auth/LinkedInAuthActivity.kt`

---

### 2.5 iOS - Implement LinkedIn Sign-In

**Priority:** Medium  
**Estimated Effort:** 6-8 hours  
**Dependencies:** LinkedIn Developer Console configuration

| Task ID | Description | Status |
|---------|-------------|--------|
| 2.5.1 | Register iOS app in LinkedIn Developer Console | [ ] |
| 2.5.2 | Configure URL scheme for callback | [ ] |
| 2.5.3 | Add LinkedIn button to AuthView | [ ] |
| 2.5.4 | Create `LinkedInAuthManager` using ASWebAuthenticationSession | [ ] |
| 2.5.5 | Handle OAuth callback | [ ] |
| 2.5.6 | Connect to AuthViewModelWrapper | [ ] |
| 2.5.7 | Test complete flow | [ ] |

**Files to Modify:**
- `iosApp/iosApp/Info.plist`
- `iosApp/iosApp/Views/AuthView.swift`
- Create: `iosApp/iosApp/Managers/LinkedInAuthManager.swift`

---

## Phase 3: Resume Feature Parity

### 3.1 Android - Implement LinkedIn Import

**Priority:** High  
**Estimated Effort:** 8-12 hours  
**Dependencies:** Phase 2.4 (LinkedIn Auth)

| Task ID | Description | Status |
|---------|-------------|--------|
| 3.1.1 | Add LinkedIn import button to Resume screen | [ ] |
| 3.1.2 | Create LinkedIn import dialog/sheet | [ ] |
| 3.1.3 | Implement OAuth flow for profile access | [ ] |
| 3.1.4 | Implement Android-specific `LinkedInRepository` | [ ] |
| 3.1.5 | Call `ImportLinkedInProfileUseCase` from shared module | [ ] |
| 3.1.6 | Display imported resume with LinkedIn badge | [ ] |
| 3.1.7 | Test import flow | [ ] |

**Files to Modify:**
- `androidApp/src/androidMain/kotlin/com/vwatek/apply/android/ui/screens/ResumeScreen.kt`
- `shared/src/androidMain/kotlin/com/vwatek/apply/data/repository/` (create Android implementation)

---

### 3.2 iOS - Implement LinkedIn Import

**Priority:** High  
**Estimated Effort:** 8-12 hours  
**Dependencies:** Phase 2.5 (LinkedIn Auth)

| Task ID | Description | Status |
|---------|-------------|--------|
| 3.2.1 | Add LinkedIn import button to ResumeView toolbar menu | [ ] |
| 3.2.2 | Create `LinkedInImportSheet` view | [ ] |
| 3.2.3 | Implement OAuth flow for profile access | [ ] |
| 3.2.4 | Implement iOS-specific `LinkedInRepository` | [ ] |
| 3.2.5 | Call shared use case via ViewModel wrapper | [ ] |
| 3.2.6 | Display imported resume appropriately | [ ] |
| 3.2.7 | Test import flow | [ ] |

**Files to Modify:**
- `iosApp/iosApp/Views/ResumeView.swift`
- `shared/src/iosMain/kotlin/com/vwatek/apply/data/repository/` (create iOS implementation)
- Create: `iosApp/iosApp/Views/LinkedInImportSheet.swift`

---

### 3.3 Android - Implement PDF Export

**Priority:** High  
**Estimated Effort:** 12-16 hours  
**Dependencies:** None

| Task ID | Description | Status |
|---------|-------------|--------|
| 3.3.1 | Research Android PDF generation libraries (iText, AndroidPdfWriter) | [ ] |
| 3.3.2 | Add PDF generation dependencies | [ ] |
| 3.3.3 | Create `PdfExportUtil` class | [ ] |
| 3.3.4 | Implement 4 resume templates (Professional, Modern, Classic, Minimal) | [ ] |
| 3.3.5 | Add export button to resume card actions | [ ] |
| 3.3.6 | Create template selection dialog | [ ] |
| 3.3.7 | Implement file saving using Storage Access Framework | [ ] |
| 3.3.8 | Add share intent for exported PDF | [ ] |
| 3.3.9 | Test export with all templates | [ ] |

**Files to Modify:**
- `androidApp/build.gradle.kts`
- `androidApp/src/androidMain/kotlin/com/vwatek/apply/android/ui/screens/ResumeScreen.kt`
- Create: `androidApp/src/androidMain/kotlin/com/vwatek/apply/android/util/PdfExportUtil.kt`

---

### 3.4 iOS - Implement PDF Export

**Priority:** High  
**Estimated Effort:** 12-16 hours  
**Dependencies:** None

| Task ID | Description | Status |
|---------|-------------|--------|
| 3.4.1 | Create `PDFGenerator` class using UIGraphicsPDFRenderer | [ ] |
| 3.4.2 | Implement 4 resume templates matching web version | [ ] |
| 3.4.3 | Add export button to ResumeRow context menu | [ ] |
| 3.4.4 | Create `PDFExportSheet` for template selection | [ ] |
| 3.4.5 | Implement PDF preview using PDFKit | [ ] |
| 3.4.6 | Add share sheet for exported PDF | [ ] |
| 3.4.7 | Test export with all templates | [ ] |

**Files to Modify:**
- `iosApp/iosApp/Views/ResumeView.swift`
- Create: `iosApp/iosApp/Utilities/PDFGenerator.swift`
- Create: `iosApp/iosApp/Views/PDFExportSheet.swift`

---

### 3.5 Android - Implement Version History

**Priority:** Medium  
**Estimated Effort:** 8-12 hours  
**Dependencies:** None

| Task ID | Description | Status |
|---------|-------------|--------|
| 3.5.1 | Add version history button to resume card | [ ] |
| 3.5.2 | Create `VersionHistorySheet` composable | [ ] |
| 3.5.3 | Implement version list UI with timestamps | [ ] |
| 3.5.4 | Add version preview functionality | [ ] |
| 3.5.5 | Implement restore confirmation dialog | [ ] |
| 3.5.6 | Connect to `RestoreResumeVersionUseCase` | [ ] |
| 3.5.7 | Test version history and restore flow | [ ] |

**Files to Modify:**
- `androidApp/src/androidMain/kotlin/com/vwatek/apply/android/ui/screens/ResumeScreen.kt`
- Create: `androidApp/src/androidMain/kotlin/com/vwatek/apply/android/ui/components/VersionHistorySheet.kt`

---

### 3.6 iOS - Implement Version History

**Priority:** Medium  
**Estimated Effort:** 8-12 hours  
**Dependencies:** None

| Task ID | Description | Status |
|---------|-------------|--------|
| 3.6.1 | Add version history option to ResumeRow menu | [ ] |
| 3.6.2 | Create `VersionHistorySheet` view | [ ] |
| 3.6.3 | Implement version list with timestamps | [ ] |
| 3.6.4 | Add version content preview | [ ] |
| 3.6.5 | Implement restore confirmation alert | [ ] |
| 3.6.6 | Add `restoreVersion` method to ResumeViewModelWrapper | [ ] |
| 3.6.7 | Test version history and restore flow | [ ] |

**Files to Modify:**
- `iosApp/iosApp/Views/ResumeView.swift`
- `iosApp/iosApp/ViewModels/ResumeViewModelWrapper.swift`
- Create: `iosApp/iosApp/Views/VersionHistorySheet.swift`

---

## Phase 4: Optimizer Feature Parity

### 4.1 Android - Implement Section Rewriter

**Priority:** High  
**Estimated Effort:** 8-12 hours  
**Dependencies:** Phase 1.1 (Optimizer Navigation)

| Task ID | Description | Status |
|---------|-------------|--------|
| 4.1.1 | Add tabbed interface to OptimizerScreen (ATS / Section Rewriter) | [ ] |
| 4.1.2 | Create `SectionRewriterContent` composable | [ ] |
| 4.1.3 | Implement section type dropdown (Summary, Experience, Skills, Education) | [ ] |
| 4.1.4 | Add section content text input | [ ] |
| 4.1.5 | Implement writing style selection (professional, confident, results-driven, innovative) | [ ] |
| 4.1.6 | Add target role and industry inputs | [ ] |
| 4.1.7 | Connect to shared `RewriteSectionUseCase` | [ ] |
| 4.1.8 | Display rewritten content with copy functionality | [ ] |
| 4.1.9 | Test all section types and styles | [ ] |

**Files to Modify:**
- `androidApp/src/androidMain/kotlin/com/vwatek/apply/android/ui/screens/OptimizerScreen.kt`

---

### 4.2 Android - Enhance ATS Analysis

**Priority:** Medium  
**Estimated Effort:** 4-6 hours  
**Dependencies:** Phase 1.1 (Optimizer Navigation)

| Task ID | Description | Status |
|---------|-------------|--------|
| 4.2.1 | Add target keywords input field | [ ] |
| 4.2.2 | Display score breakdown (Format, Keywords, Structure, Readability) | [ ] |
| 4.2.3 | Show formatting issues list | [ ] |
| 4.2.4 | Show structure issues list | [ ] |
| 4.2.5 | Display recommendations with severity indicators | [ ] |
| 4.2.6 | Add keyword density visualization | [ ] |
| 4.2.7 | Test with various resume formats | [ ] |

**Files to Modify:**
- `androidApp/src/androidMain/kotlin/com/vwatek/apply/android/ui/screens/OptimizerScreen.kt`

---

## Phase 5: Interview Prep Feature Parity

### 5.1 Android - Implement STAR Method Coaching

**Priority:** High  
**Estimated Effort:** 6-8 hours  
**Dependencies:** None

| Task ID | Description | Status |
|---------|-------------|--------|
| 5.1.1 | Add STAR Coaching button to InterviewScreen header | [ ] |
| 5.1.2 | Create `StarCoachingDialog` composable | [ ] |
| 5.1.3 | Implement experience input text field | [ ] |
| 5.1.4 | Implement job context input field | [ ] |
| 5.1.5 | Connect to shared `GetStarCoachingUseCase` | [ ] |
| 5.1.6 | Display STAR breakdown (Situation, Task, Action, Result) | [ ] |
| 5.1.7 | Display improvement suggestions | [ ] |
| 5.1.8 | Add copy functionality for generated content | [ ] |
| 5.1.9 | Test with various experience descriptions | [ ] |

**Files to Modify:**
- `androidApp/src/androidMain/kotlin/com/vwatek/apply/android/ui/screens/InterviewScreen.kt`
- Create: `androidApp/src/androidMain/kotlin/com/vwatek/apply/android/ui/components/StarCoachingDialog.kt`

---

### 5.2 iOS - Implement STAR Method Coaching

**Priority:** High  
**Estimated Effort:** 6-8 hours  
**Dependencies:** None

| Task ID | Description | Status |
|---------|-------------|--------|
| 5.2.1 | Add STAR Coaching button to InterviewSetupView | [ ] |
| 5.2.2 | Create `StarCoachingSheet` view | [ ] |
| 5.2.3 | Implement experience input TextEditor | [ ] |
| 5.2.4 | Implement job context input | [ ] |
| 5.2.5 | Add `getStarCoaching` method to InterviewViewModelWrapper | [ ] |
| 5.2.6 | Display STAR breakdown with card layout | [ ] |
| 5.2.7 | Display improvement suggestions list | [ ] |
| 5.2.8 | Add copy and share functionality | [ ] |
| 5.2.9 | Test with various inputs | [ ] |

**Files to Modify:**
- `iosApp/iosApp/Views/InterviewView.swift`
- `iosApp/iosApp/ViewModels/InterviewViewModelWrapper.swift`
- Create: `iosApp/iosApp/Views/StarCoachingSheet.swift`

---

### 5.3 Android - Add Resume Selection for Interviews

**Priority:** Low  
**Estimated Effort:** 2-3 hours  
**Dependencies:** None

| Task ID | Description | Status |
|---------|-------------|--------|
| 5.3.1 | Add resume dropdown to `InterviewSetupDialog` | [ ] |
| 5.3.2 | Load resumes from ResumeViewModel | [ ] |
| 5.3.3 | Pass selected resume content to StartSession intent | [ ] |
| 5.3.4 | Test interview with resume context | [ ] |

**Files to Modify:**
- `androidApp/src/androidMain/kotlin/com/vwatek/apply/android/ui/screens/InterviewScreen.kt`

---

## Phase 6: Cover Letter Feature Parity

### 6.1 iOS - Add Saved Cover Letters List

**Priority:** Medium  
**Estimated Effort:** 4-6 hours  
**Dependencies:** None

| Task ID | Description | Status |
|---------|-------------|--------|
| 6.1.1 | Create `CoverLetterListView` component | [ ] |
| 6.1.2 | Add saved letters section below generator form | [ ] |
| 6.1.3 | Implement cover letter row with job title and company | [ ] |
| 6.1.4 | Add swipe to delete functionality | [ ] |
| 6.1.5 | Create detail sheet for viewing saved letters | [ ] |
| 6.1.6 | Add copy and share actions | [ ] |
| 6.1.7 | Test list display and interactions | [ ] |

**Files to Modify:**
- `iosApp/iosApp/Views/CoverLetterView.swift`
- `iosApp/iosApp/ViewModels/CoverLetterViewModelWrapper.swift`

---

### 6.2 iOS - Expose Tone Selection

**Priority:** Low  
**Estimated Effort:** 2-3 hours  
**Dependencies:** None

| Task ID | Description | Status |
|---------|-------------|--------|
| 6.2.1 | Add tone picker below job description input | [ ] |
| 6.2.2 | Implement segmented control or picker for tones | [ ] |
| 6.2.3 | Pass selected tone to generate function | [ ] |
| 6.2.4 | Test generation with different tones | [ ] |

**Files to Modify:**
- `iosApp/iosApp/Views/CoverLetterView.swift`

---

### 6.3 Android - Add Copy to Clipboard

**Priority:** Low  
**Estimated Effort:** 1-2 hours  
**Dependencies:** None

| Task ID | Description | Status |
|---------|-------------|--------|
| 6.3.1 | Add copy button to cover letter detail sheet | [ ] |
| 6.3.2 | Implement clipboard functionality using ClipboardManager | [ ] |
| 6.3.3 | Show snackbar confirmation | [ ] |
| 6.3.4 | Test copy functionality | [ ] |

**Files to Modify:**
- `androidApp/src/androidMain/kotlin/com/vwatek/apply/android/ui/screens/CoverLetterScreen.kt`

---

## Phase 7: Profile and Settings Parity

### 7.1 Web - Add Change Password

**Priority:** Low  
**Estimated Effort:** 3-4 hours  
**Dependencies:** None

| Task ID | Description | Status |
|---------|-------------|--------|
| 7.1.1 | Add change password section to ProfileView | [ ] |
| 7.1.2 | Create password change form with validation | [ ] |
| 7.1.3 | Connect to shared ChangePassword intent | [ ] |
| 7.1.4 | Display success/error feedback | [ ] |
| 7.1.5 | Test password change flow | [ ] |

**Files to Modify:**
- `webApp/src/jsMain/kotlin/com/vwatek/apply/ui/screens/AuthScreen.kt`

---

### 7.2 Web - Add Notifications and Appearance Settings

**Priority:** Low  
**Estimated Effort:** 4-6 hours  
**Dependencies:** None

| Task ID | Description | Status |
|---------|-------------|--------|
| 7.2.1 | Add notifications section to Settings screen | [ ] |
| 7.2.2 | Implement notification preferences toggles | [ ] |
| 7.2.3 | Add appearance section with dark mode toggle | [ ] |
| 7.2.4 | Implement CSS theme switching | [ ] |
| 7.2.5 | Persist preferences in settings repository | [ ] |
| 7.2.6 | Test all preference toggles | [ ] |

**Files to Modify:**
- `webApp/src/jsMain/kotlin/com/vwatek/apply/ui/screens/SettingsScreen.kt`
- `webApp/src/jsMain/resources/styles.css`

---

## Phase 8: Dashboard/Home Feature Parity

### 8.1 Android - Add Getting Started Wizard

**Priority:** Low  
**Estimated Effort:** 4-6 hours  
**Dependencies:** None

| Task ID | Description | Status |
|---------|-------------|--------|
| 8.1.1 | Create `GettingStartedSection` composable | [ ] |
| 8.1.2 | Define 4 onboarding steps | [ ] |
| 8.1.3 | Implement step completion detection logic | [ ] |
| 8.1.4 | Add checkmark indicators for completed steps | [ ] |
| 8.1.5 | Make steps clickable to navigate to relevant screens | [ ] |
| 8.1.6 | Hide section when all steps complete (optional) | [ ] |
| 8.1.7 | Test step tracking | [ ] |

**Files to Modify:**
- `androidApp/src/androidMain/kotlin/com/vwatek/apply/android/ui/screens/HomeScreen.kt`

---

### 8.2 Web - Add Getting Started Wizard

**Priority:** Low  
**Estimated Effort:** 4-6 hours  
**Dependencies:** None

| Task ID | Description | Status |
|---------|-------------|--------|
| 8.2.1 | Create `GettingStartedSection` component | [ ] |
| 8.2.2 | Define 4 onboarding steps matching iOS | [ ] |
| 8.2.3 | Implement step completion detection | [ ] |
| 8.2.4 | Style progress indicators | [ ] |
| 8.2.5 | Add click navigation to relevant screens | [ ] |
| 8.2.6 | Test step tracking | [ ] |

**Files to Modify:**
- `webApp/src/jsMain/kotlin/com/vwatek/apply/ui/screens/DashboardScreen.kt`

---

## Phase 9: Testing and Quality Assurance

### 9.1 Cross-Platform Testing

| Task ID | Description | Status |
|---------|-------------|--------|
| 9.1.1 | Create test matrix document | [ ] |
| 9.1.2 | Test all authentication flows on each platform | [ ] |
| 9.1.3 | Test resume CRUD operations on each platform | [ ] |
| 9.1.4 | Test optimizer features on each platform | [ ] |
| 9.1.5 | Test cover letter features on each platform | [ ] |
| 9.1.6 | Test interview prep features on each platform | [ ] |
| 9.1.7 | Test profile/settings on each platform | [ ] |
| 9.1.8 | Test tablet/responsive layouts | [ ] |
| 9.1.9 | Document any remaining discrepancies | [ ] |

---

### 9.2 Regression Testing

| Task ID | Description | Status |
|---------|-------------|--------|
| 9.2.1 | Verify existing features still work after changes | [ ] |
| 9.2.2 | Test shared ViewModel behavior across platforms | [ ] |
| 9.2.3 | Verify data persistence and sync | [ ] |
| 9.2.4 | Test error handling and edge cases | [ ] |
| 9.2.5 | Performance testing on low-end devices | [ ] |

---

## Summary: Task Counts by Priority

| Priority | Task Count | Estimated Hours |
|----------|------------|-----------------|
| Critical | 14 tasks | 4-8 hours |
| High | 45 tasks | 60-90 hours |
| Medium | 28 tasks | 30-45 hours |
| Low | 25 tasks | 20-35 hours |
| **Total** | **112 tasks** | **114-178 hours** |

---

## Execution Order Recommendation

1. **Week 1-2:** Phase 1 (Navigation fixes) - Critical
2. **Week 2-3:** Phase 4.1 (Android Section Rewriter) - High impact
3. **Week 3-4:** Phase 5 (STAR Coaching) - High visibility feature
4. **Week 4-6:** Phase 2 (Authentication) - Parallel work possible
5. **Week 6-8:** Phase 3 (Resume features) - LinkedIn and PDF export
6. **Week 8-9:** Phase 6 (Cover Letter fixes) - Quick wins
7. **Week 9-10:** Phase 7-8 (Profile/Dashboard) - Lower priority
8. **Week 10-11:** Phase 9 (Testing) - Final verification

---

## Notes

- Tasks can be parallelized across team members where dependencies allow
- iOS and Android work on the same feature can often proceed in parallel
- Shared module changes should be coordinated to avoid conflicts
- Each completed phase should include a code review checkpoint
- Update this document as tasks are completed

---

## Changelog

| Date | Author | Changes |
|------|--------|---------|
| 2026-02-08 | - | Initial document creation |

