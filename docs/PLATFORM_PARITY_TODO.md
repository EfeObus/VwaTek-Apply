# Platform Parity TODO

> Generated from a full audit of Android, iOS, and Web platforms.
> Goal: bring all three platforms to feature parity.

---

## Legend

| Symbol | Meaning |
|--------|---------|
| **P0** | Critical — screen/feature is built but unreachable |
| **P1** | High — feature exists on one platform but missing on others |
| **P2** | Medium — partial implementation or mock data |
| **P3** | Low — polish / UX consistency |

---

## Phase 1 — Wire Orphaned Screens into Navigation (P0)

Three fully-built screens exist on Android and iOS but are **not reachable** from navigation. Web already has all three wired.

### 1.1 Android — Add Tracker, Salary Insights, Subscription to navigation

- [ ] **Add `Tracker` to `NavigationItem` enum** in `VwaTekApp.kt`
  - Icon: `Icons.Filled.ViewKanban` / `Icons.Outlined.ViewKanban` (or similar)
  - Label: "Tracker", Route: "tracker"
- [ ] **Add `SalaryInsights` to `NavigationItem` enum**
  - Icon: `Icons.Filled.TrendingUp` / `Icons.Outlined.TrendingUp`
  - Label: "Salary", Route: "salary"
- [ ] **Add `Subscription` to `NavigationItem` enum**
  - Icon: `Icons.Filled.Star` / `Icons.Outlined.Star`
  - Label: "Premium", Route: "subscription"
- [ ] **Add `when` cases in `ScreenContent`** for `Tracker`, `SalaryInsights`, `Subscription`
  - `NavigationItem.Tracker` → `TrackerScreen()`
  - `NavigationItem.SalaryInsights` → `SalaryInsightsScreen(onNavigateBack = {}, onShowPaywall = {})`
  - `NavigationItem.Subscription` → `SubscriptionScreen(onNavigateBack = {}, ...)`
- [ ] **Consider a "More" pattern** — 12 bottom tabs is too many for phone. Options:
  - Use a "More" overflow menu for low-frequency items (Settings, Subscription)
  - Move Tracker/Salary to Home screen quick actions instead of top-level nav
  - Use NavigationDrawer on phone (already have NavigationRail for tablet)

### 1.2 iOS — Add Tracker, Salary Insights, Subscription to navigation

- [ ] **Add `TrackerView` tab to `MainTabView`** in `ContentView.swift`
  - Tag: `9`, Icon: `list.clipboard.fill`, Label: "Tracker"
  - Inject `TrackerViewModelWrapper` as `@StateObject`
- [ ] **Add `SalaryInsightsView` tab to `MainTabView`**
  - Tag: `10`, Icon: `chart.line.uptrend.xyaxis`, Label: "Salary"
- [ ] **Add `SubscriptionView` tab to `MainTabView`**
  - Tag: `11`, Icon: `star.fill`, Label: "Premium"
- [ ] **Add corresponding sections to `iPadMainView`** sidebar as well
- [ ] **Consider using `TabSection` or overflow** — 12 tabs exceed Apple HIG (max ~5 visible + "More")
  - Implement a `UITabBarController`-style "More" tab, or
  - Move some tabs to NavigationLink from Home, or
  - Use a sidebar-based layout (like iPadMainView) on all devices

### 1.3 iOS — Activate iPadMainView

- [ ] **Wire `iPadMainView` in `ContentView`** — currently dead code
  - Add horizontal size class detection: `@Environment(\.horizontalSizeClass) var sizeClass`
  - `if sizeClass == .regular { iPadMainView(...) } else { MainTabView(...) }`
- [ ] **Add the 3 missing sections** (Tracker, SalaryInsights, Subscription) to `iPadMainView` too

---

## Phase 2 — Register Missing Shared Components in DI (P0)

UseCases and Managers exist in the shared module but are **not registered** in Koin, so no platform can inject them.

### 2.1 Register API Clients in `Modules.kt`

- [ ] `single { SubscriptionApiClient(get()) }` — needed for subscription features
- [ ] `single { SalaryApiClient(get()) }` — needed for salary insights
- [ ] `single { OrganizationApiClient(get()) }` — needed for enterprise features
- [ ] `single { SyncApiClient(get()) }` — needed for cross-device sync
- [ ] `single { PrivacyApiClient(get()) }` — needed for data export/deletion

### 2.2 Register Subscription UseCases & Manager

- [ ] `factory { GetSubscriptionUseCase(get()) }`
- [ ] `factory { GetPricingUseCase(get()) }`
- [ ] `factory { CreateCheckoutSessionUseCase(get()) }`
- [ ] `factory { CreatePortalSessionUseCase(get()) }`
- [ ] `factory { CancelSubscriptionUseCase(get()) }`
- [ ] `factory { ReactivateSubscriptionUseCase(get()) }`
- [ ] `factory { GetUsageLimitsUseCase(get()) }`
- [ ] `factory { CheckFeatureAccessUseCase(get()) }`
- [ ] `single { SubscriptionManager(get(), get(), ...) }` — singleton for reactive state

### 2.3 Register Salary Intelligence UseCases & Manager

- [ ] `factory { GetSalaryInsightsUseCase(get()) }`
- [ ] `factory { GetSalaryHistoryUseCase(get()) }`
- [ ] `factory { EvaluateOfferUseCase(get()) }`
- [ ] `factory { GetSavedOffersUseCase(get()) }`
- [ ] `factory { UpdateOfferStatusUseCase(get()) }`
- [ ] `factory { StartNegotiationSessionUseCase(get()) }`
- [ ] `factory { GetNegotiationSessionUseCase(get()) }`
- [ ] `factory { SendNegotiationMessageUseCase(get()) }`
- [ ] `single { SalaryIntelligenceManager(get(), get(), ...) }`

### 2.4 Register LinkedIn Optimizer UseCases & Manager

- [ ] `factory { AnalyzeLinkedInProfileUseCase(get()) }`
- [ ] `factory { GetOptimizedLinkedInContentUseCase(get()) }`
- [ ] `factory { SaveLinkedInProfileUseCase(get()) }`
- [ ] `factory { GetLinkedInHistoryUseCase(get()) }`
- [ ] `factory { GenerateLinkedInHeadlineUseCase(get()) }`
- [ ] `factory { GenerateLinkedInSummaryUseCase(get()) }`
- [ ] `single { LinkedInOptimizerManager(get(), get(), ...) }`

### 2.5 Export new components to iOS via KoinHelper

- [ ] Add `getSubscriptionManager()` to `KoinHelper.kt` (iosMain)
- [ ] Add `getSalaryIntelligenceManager()` to `KoinHelper.kt`
- [ ] Add `getLinkedInOptimizerManager()` to `KoinHelper.kt`

---

## Phase 3 — Salary Insights: Replace Simulated Data with Real API (P1)

Android already uses `SalaryIntelligenceManager` — iOS and Web use hardcoded mock data.

### 3.1 iOS — Wire SalaryInsightsView to real data

- [ ] Create `SalaryIntelligenceManagerWrapper.swift` ViewModel wrapper
  - `@Published var insightsState`, `offersState`, `negotiationState`
  - Bridge `SalaryIntelligenceManager` from shared module via `KoinHelper`
- [ ] Replace hardcoded salary data in `SalaryInsightsView.swift` with live API calls
- [ ] Add offer evaluation section (Android has it, iOS doesn't)

### 3.2 Web — Wire SalaryInsightsScreen to real data

- [ ] Replace `delay(1500)` + hardcoded salary data with `SalaryIntelligenceManager` (Koin inject)
- [ ] Replace `hasAccess = false` hardcode with `SubscriptionManager.canUseFeature()`
- [ ] Add offer evaluation section
- [ ] Add offer list view
- [ ] Add negotiation session UI

### 3.3 All Platforms — Add missing salary features

- [ ] **Trend charts / salary history** — no platform has this yet, but `GetSalaryHistoryUseCase` exists
- [ ] **Negotiation coach screen** — referenced in paywall copy, backend supports sessions, but no dedicated UI on any platform

---

## Phase 4 — Subscription & Payment Integration (P1)

### 4.1 iOS — Complete StoreKit integration

- [ ] Wire `SubscriptionView` to navigation (Phase 1.2 above)
- [ ] Implement `Product.purchase()` flow (products are loaded but purchase not triggered)
- [ ] Handle transaction verification and webhook to backend
- [ ] Wire `SubscriptionManager` from shared module for feature gating

### 4.2 Android — Add Play Billing or Stripe

- [ ] Wire `SubscriptionScreen` to navigation (Phase 1.1 above)
- [ ] Decide: Google Play Billing Library vs Stripe mobile SDK
- [ ] Implement checkout flow (currently `onStartCheckout` is a no-op callback)
- [ ] Wire `SubscriptionManager` from shared module for feature gating

### 4.3 Web — Complete Stripe integration

- [ ] Replace `isDemoMode = true` with actual Stripe checkout session creation
- [ ] Implement `CreateCheckoutSessionUseCase` call in `SubscriptionScreen`
- [ ] Implement "Manage billing" → `CreatePortalSessionUseCase` → redirect to Stripe portal
- [ ] Wire `SubscriptionManager` from shared module for feature gating

### 4.4 All Platforms — Feature gating consistency

- [ ] Use `SubscriptionManager.canUseFeature(PremiumFeature.*)` for all premium checks
- [ ] Currently gated features: Salary Insights, Negotiation Coach, LinkedIn Optimizer
- [ ] Replace all hardcoded `hasAccess = false` with real subscription checks
- [ ] Ensure `PaywallScreen`/`PaywallView`/`PaywallModal` all show consistent pricing

---

## Phase 5 — Profile Screen Parity (P1)

### 5.1 Web — Create standalone ProfileScreen

- [ ] Create `webApp/.../screens/ProfileScreen.kt` as a dedicated screen
- [ ] Add `PROFILE` to the `Screen` enum in `App.kt`
- [ ] Add Profile to sidebar (between Settings and the spacer, or as a user-section click)
- [ ] Port features from Android/iOS:
  - [ ] Edit profile form (name, phone, address)
  - [ ] Email verified badge
  - [ ] Connected accounts (Google, LinkedIn) display
  - [ ] Help Center, Feedback, About sections
  - [ ] API Keys section (currently only in Settings)

### 5.2 Android — Add missing profile features

- [ ] **Connected Accounts** section (Google/LinkedIn status) — iOS has it
- [ ] **Language preference** selector — iOS has it

### 5.3 iOS — Add missing profile features

- [ ] Change password **sheet** — listed but no sheet implementation visible
- [ ] Verify "Connected Accounts" shows accurate status from `AuthViewState`

---

## Phase 6 — Home/Dashboard Screen Parity (P2)

### 6.1 iOS — Add quick stats

- [ ] Add stats row (Resume count, Cover Letter count, Interview count — like Android)
- [ ] Use `ResumeViewModelWrapper`, `CoverLetterViewModelWrapper`, `InterviewViewModelWrapper` 

### 6.2 Web — Add quick stats

- [ ] Add stats row (Resume count, Cover Letter count, Interview count — like Android)
- [ ] Already injects the 3 ViewModels — just need the UI cards

### 6.3 Web — Add Optimizer quick action

- [ ] Add "Resume Optimizer" card to the quick actions grid (currently only 3: Resume, Cover Letter, Interview)

### 6.4 All — Align remaining dashboard content

- [ ] Add Pro Tip card to Web (Android and iOS have it)
- [ ] Add personalized greeting to Android and Web (iOS has `userName`)
- [ ] Move Web's "Key Features" section to Android/iOS _or_ remove it from Web for consistency

---

## Phase 7 — Tracker Screen Parity (P2)

### 7.1 iOS — Add reminder creation

- [ ] Add "Add Reminder" action in application detail sheet
- [ ] Call `TrackerViewModelWrapper` → `TrackerIntent.AddReminder`

### 7.2 Web — Add reminder creation

- [ ] Add "Add Reminder" UI in application detail modal
- [ ] Call `TrackerViewModel.onIntent(TrackerIntent.AddReminder(...))`

### 7.3 Web — Add Offer Rate stat

- [ ] Already shows Offer Rate — add to Android and iOS for consistency (or remove from Web)

### 7.4 All — Implement Calendar view

- [ ] All three platforms show "Calendar coming soon" placeholder
- [ ] Implement calendar grid showing applications by date/deadline
- [ ] Android: Use `AndroidView` with `CalendarView` or compose-calendar library
- [ ] iOS: Use SwiftUI `DatePicker` or custom calendar grid
- [ ] Web: Use HTML/CSS calendar grid

---

## Phase 8 — Settings Screen Parity (P2)

### 8.1 Web — Add missing settings

- [ ] **Language toggle** (English/French) — Android and iOS have it
- [ ] **Analytics toggle** — Android and iOS have it
- [ ] **Crash reporting toggle** — Android and iOS have it (controls Sentry on Web)
- [ ] **Privacy Policy** link
- [ ] **Terms of Service** link
- [ ] **Help & Support** link

### 8.2 Android/iOS — Add missing settings

- [ ] **Compact mode toggle** — Web has it (applies CSS class)
  - Android: could reduce padding/font sizes
  - iOS: could use `.dynamicTypeSize` controls
- [ ] **API key status indicators** — Web shows green/red dots

---

## Phase 9 — Resume Screen Parity (P2)

### 9.1 Android — Add per-resume ATS analysis

- [ ] Add "ATS Check" / "Match Score" button on each resume card
- [ ] Show analysis inline (or bottom sheet) without navigating to Optimizer

### 9.2 Android/iOS — Add resume source badges

- [ ] Show "Uploaded" / "LinkedIn" / "Created" badge on resume cards (Web has this)

### 9.3 Android/iOS — Add content preview

- [ ] Show first ~100 chars of resume content on list cards (Web has this)

### 9.4 Android/Web — Add pull-to-refresh

- [ ] Android: `PullRefreshIndicator` / `pullToRefresh` modifier
- [ ] Web: pull-to-refresh is unusual on web — consider a manual "Refresh" button instead

---

## Phase 10 — Optimizer Screen Parity (P3)

### 10.1 iOS — Add keyword density display

- [ ] Show keyword density breakdown in ATS analysis results (Android and Web have it)

### 10.2 iOS — Add copy-to-clipboard for rewrite results

- [ ] Add "Copy" button to section rewrite results (Android uses ClipboardManager, Web uses browser API)

### 10.3 Web — Add "How it works" instructions card

- [ ] iOS shows instruction text for first-time users — add to Web

### 10.4 All — Implement standalone grammar check

- [ ] `AnalyzeGrammarUseCase` exists in shared module but no platform has a dedicated grammar tab
- [ ] Consider adding a "Grammar" tab alongside ATS / Section Rewriter

### 10.5 All — Implement standalone impact bullets generator

- [ ] `GenerateImpactBulletsUseCase` exists in shared module but no platform has a dedicated UI
- [ ] Consider adding to Optimizer or as a sub-feature

---

## Phase 11 — Auth Flow Parity (P2)

### 11.1 Web — Complete LinkedIn token exchange

- [ ] In `AuthScreen.kt` line 148 and `ResumeScreen.kt` line 834: exchange auth code for tokens via backend
- [ ] Currently logs the code but never calls `LoginWithLinkedInUseCase`

### 11.2 iOS — Add Apple Sign-In to Android

- [ ] Apple Sign-In is iOS-only (expected), but consider web support via Apple JS SDK

### 11.3 Android — Replace icon placeholders

- [ ] `AuthScreen.kt` line 314: Replace `Text("G")` with actual Google "G" icon
- [ ] `AuthScreen.kt` line 344: Replace `Text("in")` with actual LinkedIn icon

---

## Phase 12 — LinkedIn Optimizer Screen (P2)

No platform has a LinkedIn Optimizer screen, but the shared module has full UseCases and a Manager.

- [ ] **Create shared ViewModel** for `LinkedInOptimizerManager` (or use Manager directly)
- [ ] **Android:** Create `LinkedInOptimizerScreen.kt` — profile analysis, headline suggestions, summary optimization
- [ ] **iOS:** Create `LinkedInOptimizerView.swift` + `LinkedInOptimizerViewModelWrapper.swift`
- [ ] **Web:** Create `LinkedInOptimizerScreen.kt` for webApp
- [ ] **Wire into navigation** on all three platforms
- [ ] **Feature gate** behind premium subscription

---

## Phase 13 — Organization/Enterprise Features (P3)

Backend has full `OrganizationRoutes` and `OrganizationApiClient` exists in shared, but no platform has a UI.

- [ ] Design Organization management screens
- [ ] Create invite/accept flow UI
- [ ] Create team member management UI
- [ ] Create shared template management UI
- [ ] Wire into all three platforms

---

## Phase 14 — Cross-Platform UX Polish (P3)

### 14.1 Consistent error handling

- [ ] Ensure all screens show error states the same way (snackbar/toast/banner)
- [ ] Android: Snackbar via `SnackbarHost`
- [ ] iOS: `.alert()` or banner
- [ ] Web: Toast notification component

### 14.2 Consistent loading states

- [ ] Ensure all screens show shimmer/skeleton or spinner during loading
- [ ] Verify all ViewModels' `isLoading` state is observed and rendered

### 14.3 Consistent empty states

- [ ] "No resumes yet", "No applications yet" etc. — verify all three platforms show these
- [ ] Use consistent illustration + call-to-action pattern

### 14.4 Subscription/Paywall pricing consistency

- [ ] Android and iOS show USD (`$`) — Web shows CAD
- [ ] Align to show user's currency OR both CAD/USD based on locale
- [ ] Use `PricingResponse` from backend for dynamic pricing on all platforms

### 14.5 Deep linking

- [ ] Android: Add intent filters for `/tracker`, `/salary`, `/subscription` screens
- [ ] iOS: Add URL scheme handlers for screen navigation
- [ ] Web: Implement hash-based or history-based routing (`#/tracker`, etc.)

---

## Loading State Audit (P3)

> Audit of which screens show loading indicators when data is loading, and which silently wait.
> Key: ✅ = has loading indicator, ❌ = missing loading indicator, ➖ = screen does not exist on this platform, 🔶 = has `isLoading` variable but no visible indicator.

### Screens WITH Loading States

| Screen | Android | iOS | Web | Method / Notes |
|--------|---------|-----|-----|----------------|
| **Tracker** | ✅ `CircularProgressIndicator` on `state.isLoading` + `state.isLoadingDetail` | ✅ `ProgressView()` on `viewModel.isLoading` + `viewModel.isLoadingDetail` | ✅ `Div.spinner` on `state.isLoading` + `state.isLoadingDetail` | Shared `TrackerState.isLoading` consumed on all 3 |
| **Resume** | ✅ `CircularProgressIndicator` on `state.isLoading` + `state.isLoadingVersions` | ✅ `ProgressView("Loading resumes...")` on `viewModel.isLoading` | ✅ `Div.spinner` on `state.isLoading` | Shared `ResumeState.isLoading` consumed on all 3 |
| **Optimizer** | ✅ `CircularProgressIndicator` + `LinearProgressIndicator` (button/analysis states) | ✅ `ProgressView()` (button/analysis states) | ✅ `Div.spinner` + `loading-state` class | No shared VM — each platform manages locally |
| **CoverLetter** | ✅ `CircularProgressIndicator` on `coverLetterState.isLoading` + generate button | ❌ **Missing initial list loading** (only shows `ProgressView` on generate button, not `viewModel.isLoading`) | ✅ `Div.spinner` on `state.isLoading` | Shared `CoverLetterState.isLoading` NOT consumed on iOS for list loading |
| **Interview** | ✅ `CircularProgressIndicator` on `state.isLoading` + progress bar + answer submission | ❌ **Missing sessions list loading** (shows `ProgressView` only on answer submit + progress bar, not `viewModel.isLoading`) | ✅ `Div.spinner` on `state.isLoading` | Shared `InterviewState.isLoading` NOT consumed on iOS for session list |
| **NOC** | ✅ `CircularProgressIndicator` on `state.isSearching` + `state.isLoadingMore` + `state.isLoadingDetails` | ✅ `ProgressView()` on `viewModel.isSearching` + `viewModel.isLoadingMore` + `viewModel.isLoadingDetails` | ✅ `Div.spinner` on `state.isSearching` + `state.isLoadingMore` + `state.isLoadingDetails` | Uses `isSearching` pattern (not `isLoading`) — consistent on all 3 |
| **JobBank** | ✅ `CircularProgressIndicator` on search + `state.isLoadingMore` + `state.isLoadingTrending` + `state.isLoadingDetails` | ✅ `ProgressView()` on `viewModel.isLoadingMore` + `viewModel.isLoadingDetails` | ✅ `Span.spinner` on search + `state.isLoadingMore` + `state.isLoadingTrending` + `state.isLoadingDetails` | Comprehensive loading across all 3 |
| **SalaryInsights** | ✅ `CircularProgressIndicator` on `SalaryInsightsState.Loading` | ✅ `ProgressView("Loading salary data...")` on `viewModel.isLoading` | ✅ `Div.spinner` on `SalaryInsightsState.Loading` | No shared VM — uses sealed state pattern |
| **Subscription** | ✅ `CircularProgressIndicator` on `isLoading` + `SubscriptionState.Loading` | ✅ `ProgressView("Processing purchase...")` on purchase flow | ✅ `isLoading` on `SubscriptionState.Loading` + checkout button disabled | No shared VM — platform-specific billing |
| **Settings** | ✅ `CircularProgressIndicator` on local `isLoading` | ✅ `ProgressView()` on local `isLoading` | 🔶 Has `isLoading` variable but **no spinner shown** during load | No shared VM — settings load locally |
| **LinkedInOptimizer** | ✅ `CircularProgressIndicator` + `LinearProgressIndicator` (analysis actions) | ✅ `ProgressView()` + `ProgressView("Loading history...")` on `viewModel.isLoadingHistory` | ✅ `LinkedInHistoryState.Loading` + `"Loading history..."` text | No shared VM — uses manager pattern |
| **Organization** | ✅ `CircularProgressIndicator` on `state.isLoading` + `state.isLoadingMembers` | ✅ `ProgressView("Loading organizations...")` on `viewModel.isLoading` + `viewModel.isLoadingMembers` | ✅ `"Loading organizations..."` + `"Loading members..."` on `state.isLoading` + `state.isLoadingMembers` | Shared `OrganizationState.isLoading` consumed on all 3 |
| **Profile** | ✅ `CircularProgressIndicator` on local `isLoading` (in edit sheet) | ❌ **No loading state** | ➖ No standalone ProfileScreen | Uses `AuthViewModel` — iOS never checks `isLoading` |

### Screens MISSING Loading States

| Platform | Screen | Issue | Shared VM `isLoading` Field | Fix Recommended |
|----------|--------|-------|----------------------------|-----------------|
| **iOS** | CoverLetterView | No loading indicator when cover letters list is initially loading | `CoverLetterState.isLoading` (defaults `true`) | Add `if viewModel.isLoading { ProgressView("Loading cover letters...") }` before list |
| **iOS** | InterviewView | No loading indicator when sessions list is initially loading | `InterviewState.isLoading` (defaults `true`) | Add `if viewModel.isLoading { ProgressView() }` before session list / setup view |
| **iOS** | ProfileView | No loading indicator at all | `AuthViewState.isLoading` available on wrapper | Add `ProgressView()` guard while profile data loads |
| **Web** | SettingsScreen | Has `isLoading` variable but renders full form immediately with no visual indicator | N/A (local state only) | Add `if (isLoading) { Div { Div(attrs = { classes("spinner") }) } }` guard |
| **All** | Home/Dashboard | No loading indicator on any platform | No shared VM exists | Low priority — screen is static cards/navigation; no async data to load |

### Notes

- **Home/Dashboard** is static content (feature cards, links) — no ViewModel with `isLoading` exists. A loading state is unnecessary unless dynamic data (stats, recent activity) is added later.
- **Web has no ProfileScreen** — profile info is in Settings or accessed through the auth flow sidebar.
- **Optimizer** and **SalaryInsights** use platform-specific managers (not shared ViewModels) but all three platforms handle their loading states correctly.
- **NOC** uses `isSearching` instead of `isLoading` — this is semantically correct since the screen is search-driven.

---

## Summary — Priority Order

| Phase | Priority | Effort | Description |
|-------|----------|--------|-------------|
| 1 | P0 | Low | Wire 3 orphaned screens into Android & iOS navigation |
| 2 | P0 | Medium | Register missing UseCases/Managers in Koin DI |
| 3 | P1 | Medium | Replace simulated salary data with real API on iOS & Web |
| 4 | P1 | High | Complete payment integration (StoreKit, Play Billing, Stripe) |
| 5 | P1 | Medium | Profile screen parity (Web standalone, Android/iOS features) |
| 6 | P2 | Low | Dashboard/Home parity (stats, quick actions) |
| 7 | P2 | Low-Med | Tracker parity (reminders, calendar, offer rate) |
| 8 | P2 | Low | Settings screen parity (language, analytics, legal links) |
| 9 | P2 | Low | Resume screen parity (ATS, badges, preview) |
| 10 | P3 | Low | Optimizer parity (keyword density, copy, grammar) |
| 11 | P2 | Low | Auth flow completion (LinkedIn exchange, icon placeholders) |
| 12 | P2 | High | LinkedIn Optimizer screen (new feature, all platforms) |
| 13 | P3 | High | Organization/Enterprise UI (new feature, all platforms) |
| 14 | P3 | Medium | Cross-platform UX polish |
| 15 | P3 | Low | Loading state parity — 4 screens missing indicators (iOS CoverLetter, iOS Interview, iOS Profile, Web Settings) |
