# VwaTek Apply — Full System Audit Report

**Date:** March 3, 2026  
**Scope:** Backend, Frontend (Android/iOS/Web), Shared Module, API Client-Server Parity, Credentials & Secrets, Database, Build & Deployment  
**Total Issues Found:** 87 (19 Critical, 28 High, 26 Medium, 14 Low)

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Backend API Audit](#2-backend-api-audit)
3. [Frontend Routes Audit](#3-frontend-routes-audit)
4. [API Client-Server Parity](#4-api-client-server-parity)
5. [Credentials & Secrets](#5-credentials--secrets)
6. [Database & Migrations](#6-database--migrations)
7. [Build & Deployment](#7-build--deployment)
8. [Cross-Platform Integration](#8-cross-platform-integration)
9. [Issue Tracker — All Findings](#9-issue-tracker--all-findings)
10. [Recommended Fix Priority](#10-recommended-fix-priority)

---

## 1. Executive Summary

### Architecture Overview
- **Backend:** Ktor (Kotlin) on Cloud Run, MySQL via Cloud SQL, Exposed ORM
- **Shared Module:** Kotlin Multiplatform (KMP) with Ktor client, SQLDelight local DB
- **Android:** Jetpack Compose, Koin DI, state-based navigation
- **iOS:** SwiftUI, ObservableObject ViewModel wrappers, TabView navigation
- **Web:** Compose for Web (Kotlin/JS), hash-based routing, Firebase Hosting
- **Total Endpoints:** ~80+ backend routes across 14 route files
- **Total Screens:** 16 per platform (Android/iOS/Web)

### Critical Findings Summary

| Category | Critical | High | Medium | Low |
|----------|----------|------|--------|-----|
| Authentication & Authorization | 5 | 4 | 1 | 0 |
| Credentials & Secrets | 6 | 4 | 2 | 0 |
| API Client-Server Parity | 2 | 3 | 1 | 0 |
| Database & Migrations | 1 | 5 | 3 | 1 |
| Build & Deployment | 1 | 5 | 2 | 1 |
| Frontend Routes & Navigation | 1 | 2 | 3 | 1 |
| Error Handling & Resilience | 0 | 2 | 5 | 4 |
| CORS & Network | 2 | 1 | 1 | 0 |
| Input Validation | 0 | 1 | 5 | 3 |
| Monitoring & Observability | 1 | 1 | 3 | 4 |
| **TOTAL** | **19** | **28** | **26** | **14** |

---

## 2. Backend API Audit

### 2.1 Complete Endpoint Inventory (~80+ routes)

#### Auth Routes — [backend/src/main/kotlin/.../routes/AuthRoutes.kt](backend/src/main/kotlin/com/vwatek/apply/routes/AuthRoutes.kt)
| Method | Path | Auth | Line |
|--------|------|------|------|
| POST | `/api/v1/auth/register` | None | L51 |
| POST | `/api/v1/auth/login` | None | L142 |
| GET | `/api/v1/auth/me` | X-User-Id header | L210 |
| POST | `/api/v1/auth/reset-password` | None | L262 |
| POST | `/api/v1/auth/google` | None | L332 |
| POST | `/api/v1/auth/linkedin` | None | L413 |

#### Resume Routes — [backend/src/main/kotlin/.../routes/ResumeRoutes.kt](backend/src/main/kotlin/com/vwatek/apply/routes/ResumeRoutes.kt)
| Method | Path | Auth | Line |
|--------|------|------|------|
| GET | `/api/v1/resumes` | X-User-Id | L26 |
| GET | `/api/v1/resumes/{id}` | X-User-Id | L52 |
| POST | `/api/v1/resumes` | X-User-Id | L79 |
| PUT | `/api/v1/resumes/{id}` | X-User-Id | L120 |
| DELETE | `/api/v1/resumes/{id}` | X-User-Id | L156 |
| GET | `/api/v1/resumes/{id}/analyses` | X-User-Id | L179 |
| POST | `/api/v1/analyses` | X-User-Id | L194 |

#### Cover Letter Routes — [backend/src/main/kotlin/.../routes/CoverLetterRoutes.kt](backend/src/main/kotlin/com/vwatek/apply/routes/CoverLetterRoutes.kt)
| Method | Path | Auth | Line |
|--------|------|------|------|
| GET | `/api/v1/cover-letters` | X-User-Id | L22 |
| GET | `/api/v1/cover-letters/{id}` | X-User-Id | L47 |
| POST | `/api/v1/cover-letters` | X-User-Id | L69 |
| DELETE | `/api/v1/cover-letters/{id}` | X-User-Id | L103 |

#### Interview Routes — [backend/src/main/kotlin/.../routes/InterviewRoutes.kt](backend/src/main/kotlin/com/vwatek/apply/routes/InterviewRoutes.kt)
| Method | Path | Auth | Line |
|--------|------|------|------|
| GET | `/api/v1/interviews` | X-User-Id | L30 |
| GET | `/api/v1/interviews/{id}` | X-User-Id | L66 |
| POST | `/api/v1/interviews` | X-User-Id | L109 |
| POST | `/api/v1/interviews/{id}/questions` | X-User-Id | L149 |
| PUT | `/api/v1/interviews/questions/{questionId}/answer` | X-User-Id | L178 |
| PUT | `/api/v1/interviews/{sessionId}/questions/{questionId}/answer` | X-User-Id | L207 |
| PUT | `/api/v1/interviews/{id}/status` | X-User-Id | L232 |
| POST | `/api/v1/interviews/{id}/complete` | X-User-Id | L248 |
| DELETE | `/api/v1/interviews/{id}` | X-User-Id | L272 |

#### AI Routes — [backend/src/main/kotlin/.../routes/AIRoutes.kt](backend/src/main/kotlin/com/vwatek/apply/routes/AIRoutes.kt)
| Method | Path | Auth | Line |
|--------|------|------|------|
| POST | `/api/v1/ai/analyze-resume` | **NONE** | L23 |
| POST | `/api/v1/ai/optimize-resume` | **NONE** | L44 |
| POST | `/api/v1/ai/generate-cover-letter` | **NONE** | L64 |
| POST | `/api/v1/ai/generate-interview-questions` | **NONE** | L90 |
| POST | `/api/v1/ai/interview-feedback` | **NONE** | L110 |

#### Job Tracker Routes — [backend/src/main/kotlin/.../routes/JobTrackerRoutes.kt](backend/src/main/kotlin/com/vwatek/apply/routes/JobTrackerRoutes.kt) (JWT protected)
| Method | Path | Auth | Line |
|--------|------|------|------|
| GET | `/api/v1/tracker` | JWT | L56 |
| GET | `/api/v1/tracker/{id}` | JWT | L136 |
| POST | `/api/v1/tracker` | JWT | L189 |
| POST | `/api/v1/tracker/quick` | JWT | L263 |
| PUT | `/api/v1/tracker/{id}` | JWT | L320 |
| PATCH | `/api/v1/tracker/{id}/status` | JWT | L407 |
| DELETE | `/api/v1/tracker/{id}` | JWT | L454 |
| POST | `/api/v1/tracker/{id}/notes` | JWT | L489 |
| POST | `/api/v1/tracker/{id}/reminders` | JWT | L540 |
| PATCH | `/api/v1/tracker/{id}/reminders/{reminderId}/complete` | JWT | L598 |
| POST | `/api/v1/tracker/{id}/interviews` | JWT | L637 |
| GET | `/api/v1/tracker/reminders/upcoming` | JWT | L700 |
| GET | `/api/v1/tracker/stats` | JWT | L742 |

#### NOC Routes — [backend/src/main/kotlin/.../routes/NOCRoutes.kt](backend/src/main/kotlin/com/vwatek/apply/routes/NOCRoutes.kt)
| Method | Path | Auth | Line |
|--------|------|------|------|
| GET | `/api/v1/noc/search` | None | L24 |
| GET | `/api/v1/noc/{code}` | None | L69 |
| GET | `/api/v1/noc/teer` | None | L114 |
| GET | `/api/v1/noc/categories` | None | L153 |
| GET | `/api/v1/noc/{code}/demand` | None | L191 |
| GET | `/api/v1/noc/{code}/immigration` | None | L256 |
| POST | `/api/v1/noc/users/{userId}/matches` | **None (userId in path!)** | L310 |
| GET | `/api/v1/noc/users/{userId}/matches` | **None (userId in path!)** | L392 |

#### Job Bank Routes — [backend/src/main/kotlin/.../routes/JobBankRoutes.kt](backend/src/main/kotlin/com/vwatek/apply/routes/JobBankRoutes.kt)
| Method | Path | Auth | Line |
|--------|------|------|------|
| GET | `/api/v1/jobbank/search` | None | L21 |
| GET | `/api/v1/jobbank/jobs/{jobId}` | None | L66 |
| GET | `/api/v1/jobbank/noc/{nocCode}` | None | L91 |
| GET | `/api/v1/jobbank/province/{provinceCode}` | None | L119 |
| GET | `/api/v1/jobbank/trending` | None | L149 |
| GET | `/api/v1/jobbank/outlook/{nocCode}` | None | L174 |
| GET | `/api/v1/jobbank/provinces` | None | L200 |

#### Subscription Routes — [backend/src/main/kotlin/.../routes/SubscriptionRoutes.kt](backend/src/main/kotlin/com/vwatek/apply/routes/SubscriptionRoutes.kt)
| Method | Path | Auth | Line |
|--------|------|------|------|
| GET | `/api/v1/subscriptions` | X-User-Id | L42 |
| GET | `/api/v1/subscriptions/pricing` | None | L90 |
| POST | `/api/v1/subscriptions/checkout` | X-User-Id | L157 |
| POST | `/api/v1/subscriptions/portal` | X-User-Id | L220 |
| POST | `/api/v1/subscriptions/cancel` | X-User-Id | L265 |
| POST | `/api/v1/subscriptions/reactivate` | X-User-Id | L316 |
| GET | `/api/v1/subscriptions/usage` | X-User-Id | L367 |
| GET | `/api/v1/subscriptions/feature/{feature}` | X-User-Id | L477 |
| POST | `/api/v1/subscriptions/webhook` | Stripe signature | L521 |

#### Salary Routes — [backend/src/main/kotlin/.../routes/SalaryRoutes.kt](backend/src/main/kotlin/com/vwatek/apply/routes/SalaryRoutes.kt)
| Method | Path | Auth | Line |
|--------|------|------|------|
| POST | `/api/v1/salary/insights` | X-User-Id | L44 |
| GET | `/api/v1/salary/history` | X-User-Id | L146 |
| POST | `/api/v1/salary/evaluate-offer` | X-User-Id | L189 |
| GET | `/api/v1/salary/offers` | X-User-Id | L353 |
| PUT | `/api/v1/salary/offers/{id}/status` | X-User-Id | L390 |
| POST | `/api/v1/salary/negotiation/sessions` | X-User-Id | L513 |
| GET | `/api/v1/salary/negotiation/sessions/{id}` | X-User-Id | L608 |
| POST | `/api/v1/salary/negotiation/sessions/{id}/messages` | X-User-Id | L660 |

#### Organization Routes — [backend/src/main/kotlin/.../routes/OrganizationRoutes.kt](backend/src/main/kotlin/com/vwatek/apply/routes/OrganizationRoutes.kt)
| Method | Path | Auth | Line |
|--------|------|------|------|
| POST | `/api/v1/organizations` | requireAuth() (BROKEN) | L39 |
| GET | `/api/v1/organizations` | requireAuth() (BROKEN) | L117 |
| GET | `/api/v1/organizations/{orgId}` | requireAuth() (BROKEN) | L155 |
| PUT | `/api/v1/organizations/{orgId}` | requireAuth() (BROKEN) | L198 |
| GET | `/api/v1/organizations/{orgId}/members` | requireAuth() (BROKEN) | L262 |
| POST | `/api/v1/organizations/{orgId}/members/invite` | requireAuth() (BROKEN) | L319 |
| PUT | `/api/v1/organizations/{orgId}/members/{memberId}/role` | requireAuth() (BROKEN) | L530 |
| DELETE | `/api/v1/organizations/{orgId}/members/{memberId}` | requireAuth() (BROKEN) | L580 |
| GET | `/api/v1/organizations/{orgId}/templates` | requireAuth() (BROKEN) | L627 |
| POST | `/api/v1/organizations/{orgId}/templates` | requireAuth() (BROKEN) | L671 |
| GET | `/api/v1/organizations/{orgId}/analytics` | requireAuth() (BROKEN) | L730 |
| GET | `/api/v1/organizations/{orgId}/activity` | requireAuth() (BROKEN) | L776 |
| POST | `/api/v1/invitations/{token}/accept` | requireAuth() (BROKEN) | L820 |

#### Notification Routes — [backend/src/main/kotlin/.../routes/NotificationRoutes.kt](backend/src/main/kotlin/com/vwatek/apply/routes/NotificationRoutes.kt) (JWT protected, DOUBLE-PREFIXED)
| Method | Path (as defined) | Actual Path | Auth | Line |
|--------|-------------------|-------------|------|------|
| GET | `/api/v1/notifications` | **/api/v1/api/v1/notifications** | JWT | L26 |
| PATCH | `/api/v1/notifications/{id}/read` | **/api/v1/api/v1/notifications/{id}/read** | JWT | L67 |
| POST | `/api/v1/notifications/read-all` | **/api/v1/api/v1/notifications/read-all** | JWT | L97 |
| DELETE | `/api/v1/notifications/{id}` | **/api/v1/api/v1/notifications/{id}** | JWT | L117 |
| GET | `/api/v1/notifications/stats` | **/api/v1/api/v1/notifications/stats** | JWT | L146 |
| GET | `/api/v1/notifications/preferences` | **/api/v1/api/v1/notifications/preferences** | JWT | L177 |
| PUT | `/api/v1/notifications/preferences` | **/api/v1/api/v1/notifications/preferences** | JWT | L227 |
| POST | `/api/v1/notifications/devices` | **/api/v1/api/v1/notifications/devices** | JWT | L290 |
| GET | `/api/v1/notifications/devices` | **/api/v1/api/v1/notifications/devices** | JWT | L357 |
| DELETE | `/api/v1/notifications/devices/{id}` | **/api/v1/api/v1/notifications/devices/{id}** | JWT | L392 |

#### Sync Routes — [backend/src/main/kotlin/.../routes/SyncRoutes.kt](backend/src/main/kotlin/com/vwatek/apply/routes/SyncRoutes.kt) (Auth BROKEN)
| Method | Path | Auth | Line |
|--------|------|------|------|
| POST | `/api/sync/devices/register` | Reads JWTPrincipal (no authenticate wrapper) | L31 |
| POST | `/api/sync/sync` | Same (BROKEN) | L92 |
| GET | `/api/sync/changes` | Same (BROKEN) | L244 |
| GET | `/api/sync/status` | Same (BROKEN) | L303 |

#### Privacy Routes — [backend/src/main/kotlin/.../routes/PrivacyRoutes.kt](backend/src/main/kotlin/com/vwatek/apply/routes/PrivacyRoutes.kt) (JWT "auth-jwt" protected)
| Method | Path | Auth | Line |
|--------|------|------|------|
| GET | `/api/privacy/consent` | JWT | L26 |
| POST | `/api/privacy/consent` | JWT | L59 |
| POST | `/api/privacy/data-request` | JWT | L114 |
| GET | `/api/privacy/data-request/{id}` | JWT | L176 |
| GET | `/api/privacy/data-requests` | JWT | L211 |
| GET | `/api/privacy/my-data/summary` | JWT | L238 |
| DELETE | `/api/privacy/my-data` | JWT | L302 |

#### Monitoring & Health — [backend/src/main/kotlin/.../plugins/Monitoring.kt](backend/src/main/kotlin/com/vwatek/apply/plugins/Monitoring.kt) & [Routing.kt](backend/src/main/kotlin/com/vwatek/apply/plugins/Routing.kt)
| Method | Path | Auth | Line |
|--------|------|------|------|
| GET | `/` | None | Routing.kt L14 |
| GET | `/health` | None | Routing.kt L25 |
| GET | `/api/v1` | None | Routing.kt L46 |
| GET | `/metrics` | **None (exposes internal metrics)** | Monitoring.kt L265 |
| GET | `/health/live` | None | Monitoring.kt L272 |
| GET | `/health/ready` | None (stub — always 200) | Monitoring.kt L279 |

### 2.2 Authentication Findings

| # | Issue | Severity | Location |
|---|-------|----------|----------|
| SEC-01 | **X-User-Id header as sole authentication** — trivially spoofable by any HTTP client. Used by: ResumeRoutes, CoverLetterRoutes, InterviewRoutes, SubscriptionRoutes, SalaryRoutes, AuthRoutes `/me` | **CRITICAL** | All affected route files |
| SEC-02 | **Hardcoded default JWT secret** `"vwatek-apply-secret-key-change-in-production"` in source code | **CRITICAL** | [Security.kt L16](backend/src/main/kotlin/com/vwatek/apply/plugins/Security.kt#L16) |
| SEC-03 | **AI endpoints completely unauthenticated** — public internet can drain Gemini/OpenAI credits | **CRITICAL** | [AIRoutes.kt](backend/src/main/kotlin/com/vwatek/apply/routes/AIRoutes.kt) all endpoints |
| SEC-04 | **Organization routes have broken auth** — `requireAuth()` without `authenticate()` wrapper means JWTPrincipal is always null → always 401 | **HIGH** | [OrganizationRoutes.kt](backend/src/main/kotlin/com/vwatek/apply/routes/OrganizationRoutes.kt) all routes |
| SEC-05 | **Sync routes have broken auth** — reads JWTPrincipal without `authenticate()` wrapper → will NPE | **HIGH** | [SyncRoutes.kt](backend/src/main/kotlin/com/vwatek/apply/routes/SyncRoutes.kt) all routes |
| SEC-06 | **Password hashing uses SHA-256 + UUID salt** instead of bcrypt/argon2 | **HIGH** | [AuthRoutes.kt L96](backend/src/main/kotlin/com/vwatek/apply/routes/AuthRoutes.kt#L96) |
| SEC-07 | **Auth tokens are UUID concatenation, NOT JWTs** — but JWT-protected routes expect valid JWTs. Fundamental incompatibility. | **CRITICAL** | [AuthRoutes.kt L129](backend/src/main/kotlin/com/vwatek/apply/routes/AuthRoutes.kt#L129) |
| SEC-08 | **NOC user match endpoints take userId in path** — any user can read/write another user's occupation matches | **HIGH** | [NOCRoutes.kt L310, L392](backend/src/main/kotlin/com/vwatek/apply/routes/NOCRoutes.kt#L310) |
| SEC-09 | **No rate limiting** on any endpoint (login, register, reset-password, AI) | **HIGH** | Entire backend |
| SEC-10 | **Duplicate JWT configurations** (`"jwt"` and `"auth-jwt"`) with identical settings — confusing and error-prone | **MEDIUM** | [Security.kt L14, L36](backend/src/main/kotlin/com/vwatek/apply/plugins/Security.kt#L14) |

### 2.3 CORS Configuration

| # | Issue | Severity | Location |
|---|-------|----------|----------|
| CORS-01 | **`anyHost()` overrides specific host allowlist** — all origins accepted | **CRITICAL** | [CORS.kt L28](backend/src/main/kotlin/com/vwatek/apply/plugins/CORS.kt#L28) |
| CORS-02 | **`anyHost()` + `allowCredentials = true`** — browsers send cookies/auth headers to any requester | **CRITICAL** | [CORS.kt L28-L30](backend/src/main/kotlin/com/vwatek/apply/plugins/CORS.kt#L28) |
| CORS-03 | `X-User-Id` listed as allowed header, reinforcing spoofable auth | **HIGH** | [CORS.kt L21](backend/src/main/kotlin/com/vwatek/apply/plugins/CORS.kt#L21) |

### 2.4 Notification Routes — Double-Prefix Bug

| # | Issue | Severity | Location |
|---|-------|----------|----------|
| ROUTE-01 | **Notification routes double-prefixed**: routes define `/api/v1/notifications/...` but are mounted inside the `/api/v1` block in Routing.kt, resulting in actual paths `/api/v1/api/v1/notifications/...` — all 10 notification endpoints are unreachable | **HIGH** | [NotificationRoutes.kt](backend/src/main/kotlin/com/vwatek/apply/routes/NotificationRoutes.kt) + [Routing.kt L103](backend/src/main/kotlin/com/vwatek/apply/plugins/Routing.kt#L103) |

### 2.5 Error Handling

| # | Issue | Severity | Location |
|---|-------|----------|----------|
| ERR-01 | Global StatusPages catches `Throwable`, `IllegalArgumentException`, `NoSuchElementException` — adequate | OK | [StatusPages.kt](backend/src/main/kotlin/com/vwatek/apply/plugins/StatusPages.kt) |
| ERR-02 | Exposed ORM exceptions (connection failures, constraint violations) not specifically handled — generic 500 | **MEDIUM** | All routes using `transaction {}` |
| ERR-03 | JSON deserialization errors not caught — malformed request bodies return stack trace in error | **MEDIUM** | All POST/PUT routes |
| ERR-04 | `println` used for error logging in webhook handler and AI service | **LOW** | [SubscriptionRoutes.kt L566](backend/src/main/kotlin/com/vwatek/apply/routes/SubscriptionRoutes.kt#L566), [AIService.kt L33](backend/src/main/kotlin/com/vwatek/apply/services/AIService.kt#L33) |

### 2.6 Input Validation — What Exists vs What's Missing

**Exists:**
- Email format check (contains `@`) — [AuthRoutes.kt L65](backend/src/main/kotlin/com/vwatek/apply/routes/AuthRoutes.kt#L65)
- Empty password check — [AuthRoutes.kt L70](backend/src/main/kotlin/com/vwatek/apply/routes/AuthRoutes.kt#L70)
- Valid role/template-type enum validation — [OrganizationRoutes.kt L548, L684](backend/src/main/kotlin/com/vwatek/apply/routes/OrganizationRoutes.kt#L548)
- Stripe webhook signature validation — [StripeService.kt L252](backend/src/main/kotlin/com/vwatek/apply/services/payments/StripeService.kt#L252)

**Missing:**

| # | Issue | Severity | Location |
|---|-------|----------|----------|
| VAL-01 | **No password strength requirements** (min length, complexity) | **HIGH** | [AuthRoutes.kt L70](backend/src/main/kotlin/com/vwatek/apply/routes/AuthRoutes.kt#L70) |
| VAL-02 | No request body size limits — arbitrary large payloads accepted | **MEDIUM** | All POST/PUT routes |
| VAL-03 | No input sanitization before DB storage (XSS risk) | **MEDIUM** | All text-input routes |
| VAL-04 | No pagination limits — `limit`/`page` params unbounded | **MEDIUM** | NOCRoutes, SalaryRoutes, OrgRoutes |
| VAL-05 | No URL validation on `successUrl`/`cancelUrl` in checkout (open redirect risk) | **MEDIUM** | [SubscriptionRoutes.kt L168](backend/src/main/kotlin/com/vwatek/apply/routes/SubscriptionRoutes.kt#L168) |
| VAL-06 | No AI prompt injection protection — user content passed directly to LLM prompts | **MEDIUM** | [AIService.kt](backend/src/main/kotlin/com/vwatek/apply/services/AIService.kt) all methods |
| VAL-07 | Email validation only checks for `@` — no format/domain validation | **LOW** | [AuthRoutes.kt L65](backend/src/main/kotlin/com/vwatek/apply/routes/AuthRoutes.kt#L65) |

### 2.7 Health Checks

| Endpoint | Checks | Issue |
|----------|--------|-------|
| `GET /health` | Static response only — no dependency checks | **MEDIUM** |
| `GET /health/live` | Always returns `{"status":"alive"}` | OK for liveness |
| `GET /health/ready` | **Stub** — always returns 200, has `TODO: Add actual readiness check` | **MEDIUM** — does not verify DB, HikariCP pool, or external service availability |

---

## 3. Frontend Routes Audit

### 3.1 Web App Routes — [webApp/src/jsMain/kotlin/.../ui/App.kt](webApp/src/jsMain/kotlin/com/vwatek/apply/ui/App.kt)

Hash-based routing with `window.location.hash` and manual `hashToScreen` map (L34-48).

| Hash | Screen | Auth? |
|------|--------|-------|
| `#/dashboard` | Dashboard | **No** |
| `#/resumes` | Resumes | **No** |
| `#/optimizer` | Resume Optimizer | **No** |
| `#/coverletters` | Cover Letters | **No** |
| `#/interview` | Interview | **No** |
| `#/tracker` | Tracker | **No** |
| `#/noc` | NOC | **No** |
| `#/jobbank` | Job Bank | **No** |
| `#/salary` | Salary Insights | **No** (paywall-gated) |
| `#/linkedin` | LinkedIn Optimizer | **No** |
| `#/organization` | Organization | **No** |
| `#/subscription` | Subscription | **No** |
| `#/profile` | Profile | **Soft** (redirects if auth'd) |
| `#/settings` | Settings | **No** |
| *(internal)* | Auth | N/A |
| *(internal)* | Paywall | N/A |

### 3.2 Android App Routes — [androidApp/src/androidMain/kotlin/.../ui/VwaTekApp.kt](androidApp/src/androidMain/kotlin/com/vwatek/apply/android/ui/VwaTekApp.kt)

State-based with `NavigationItem` enum (L46-63). Global auth gate at L94-96.

| Route | Screen | Auth? |
|-------|--------|-------|
| `"home"` | Home | Yes (global) |
| `"resume"` | Resume | Yes (global) |
| `"optimizer"` | Optimizer | Yes (global) |
| `"coverletter"` | Cover Letter | Yes (global) |
| `"interview"` | Interview | Yes (global) |
| `"noc"` | NOC | Yes (global) |
| `"jobbank"` | Job Bank | Yes (global) |
| `"tracker"` | Tracker | Yes (global) |
| `"salary"` | Salary Insights | Yes (global + paywall) |
| `"linkedin"` | LinkedIn Optimizer | Yes (global) |
| `"organization"` | Organization | Yes (global) |
| `"subscription"` | Subscription | Yes (global) |
| `"profile"` | Profile | Yes (global) |
| `"settings"` | Settings | Yes (global) |

### 3.3 iOS App Routes — [iosApp/iosApp/ContentView.swift](iosApp/iosApp/ContentView.swift)

TabView (L51-145). Global auth gate at L13-21.

| Tag | Tab | Auth? |
|-----|-----|-------|
| 0 | Home | Yes (global) |
| 1 | Resume | Yes (global) |
| 2 | Optimizer | Yes (global) |
| 3 | Cover Letter | Yes (global) |
| 4 | Interview | Yes (global) |
| 5 | NOC | Yes (global) |
| 6 | Job Bank | Yes (global) |
| 7 | Tracker | Yes (global) |
| 8 | Salary | Yes (global + paywall) |
| 9 | LinkedIn | Yes (global) |
| 10 | Organization | Yes (global) |
| 11 | Subscription | Yes (global) |
| 12 | Profile | Yes (global) |
| 13 | Settings | Yes (global) |

### 3.4 Navigation Findings

| # | Issue | Severity | Details |
|---|-------|----------|---------|
| NAV-01 | **Web has ZERO auth guards** — all screens accessible without login. Android/iOS gate entire app behind authentication | **CRITICAL** | [App.kt L100](webApp/src/jsMain/kotlin/com/vwatek/apply/ui/App.kt#L100) |
| NAV-02 | **Android Paywall not wired** — SalaryInsights `onShowPaywall` callback is `{ /* TODO */ }` | **HIGH** | [VwaTekApp.kt L178](androidApp/src/androidMain/kotlin/com/vwatek/apply/android/ui/VwaTekApp.kt#L178) |
| NAV-03 | **iOS Settings uses local @State only** — doesn't use shared `SettingsRepository`, settings won't sync across devices | **HIGH** | [SettingsView.swift L6](iosApp/iosApp/Views/SettingsView.swift#L6) |
| NAV-04 | Web Paywall and Auth screens have no hash route — cannot be deep-linked | **MEDIUM** | [App.kt](webApp/src/jsMain/kotlin/com/vwatek/apply/ui/App.kt) |
| NAV-05 | No platform has proper back-stack management — single selected-tab state, no history | **MEDIUM** | All platforms |
| NAV-06 | iOS Interview doesn't inject ResumeViewModel (Web/Android do) — may lack resume context | **MEDIUM** | [InterviewView.swift L5](iosApp/iosApp/Views/InterviewView.swift#L5) |
| NAV-07 | iOS-only sheets (PDFExport, LinkedInImport, StarCoaching, VersionHistory) have no Android/Web equivalents | **LOW** | iosApp/iosApp/Views/ |

---

## 4. API Client-Server Parity

### 4.1 HTTP Client Configuration

| Platform | Engine | Timeout | Retry | Auth Interceptor |
|----------|--------|---------|-------|-----------------|
| Android | OkHttp | 60s request / 15s connect | **None** | **None** |
| iOS | Darwin | 60s request / 15s connect | **None** | **None** |
| Web/JS | Js | 60s request / 15s connect | **None** | **None** |

Base URL: [ApiConfig.kt](shared/src/commonMain/kotlin/com/vwatek/apply/data/api/ApiConfig.kt) — Production: `https://vwatek-backend-21443684777.northamerica-northeast1.run.app`

### 4.2 Auth Header Parity — Client vs Server

| API Client | Sends Auth? | Backend Expects | Result |
|------------|-------------|-----------------|--------|
| Auth (register/login/google) | None | None |  Works |
| Auth (updateProfile) | Bearer + X-User-Id | Bearer + X-User-Id |  Works |
| Resume (SyncingResumeRepository) | X-User-Id only | X-User-Id |  Works |
| **Job Tracker (JobTrackerApiClient)** | **None** | **JWT** | **BROKEN — 401 on all calls** |
| **Subscription (SubscriptionApiClient)** | **None** | **X-User-Id** | **BROKEN — no user context** |
| **Salary (SalaryApiClient)** | **None** | **X-User-Id** | **BROKEN — no user context** |
| **Organization (OrganizationApiClient)** | **None** | **requireAuth() (broken)** | **BROKEN — double failure** |
| **LinkedIn (LinkedInApiClientImpl)** | **None** | **Unknown** | **Likely broken** |
| NOC (NOCApiClient) | None | None |  Works |
| Job Bank (JobBankApiClient) | None | None |  Works |
| Sync (SyncApiClient) | Bearer token | JWT |  Works |
| Privacy (PrivacyApiClient) | Bearer token | JWT ("auth-jwt") |  Works |

### 4.3 Token Format Mismatch (CRITICAL)

| # | Issue | Severity | Details |
|---|-------|----------|---------|
| API-01 | **Backend generates UUID tokens, but `authenticate("jwt")` validates JWTs.** The auth tokens in `AuthResponse` are `UUID.randomUUID() + UUID.randomUUID()` — these are NOT valid JWTs and will fail verification on all JWT-protected routes (tracker, notifications, sync, privacy). | **CRITICAL** | [AuthRoutes.kt L440](backend/src/main/kotlin/com/vwatek/apply/routes/AuthRoutes.kt#L440) vs [Security.kt](backend/src/main/kotlin/com/vwatek/apply/plugins/Security.kt) |
| API-02 | **5 API clients send no auth headers at all** — JobTracker, Subscription, Salary, Organization, LinkedIn clients make bare HTTP calls | **CRITICAL** | Shared module API client files |
| API-03 | No global auth interceptor — each client reinvents header attachment | **HIGH** | All platform HttpClient configs |
| API-04 | No retry logic anywhere — transient network failures silently fail | **HIGH** | All API clients |
| API-05 | No HTTP status code differentiation — 401, 403, 404, 500 all treated the same | **HIGH** | All API clients — no automatic logout on 401 |
| API-06 | No token refresh flow — tokens expire silently with no refresh mechanism | **MEDIUM** | Token lifecycle across all platforms |

### 4.4 Token Storage Security

| Platform | Storage | Encryption | Issue |
|----------|---------|-----------|-------|
| Android | EncryptedSharedPreferences | AES-256  | None |
| iOS | **NSUserDefaults** | **None** | **HIGH — should use Keychain** |
| Web | localStorage | None (inherent to web) | Acceptable for web (use HttpOnly cookies for better security) |

### 4.5 Offline Support Status

| Feature | Android | iOS | Web |
|---------|---------|-----|-----|
| Resumes | SQLDelight + sync  | SQLDelight + sync  | API-only, no cache |
| Cover Letters | Local-only, no sync | Local-only, no sync | API-only |
| Interviews | Local-only, no sync | Local-only, no sync | API-only |
| Job Tracker | API-only | API-only | API-only |
| Settings | SQLDelight local | SQLDelight local | localStorage |
| Sync Engine | Defined but incomplete | Defined but incomplete | Not implemented |

### 4.6 AI Service — Dual Path

[GeminiService.kt](shared/src/commonMain/kotlin/com/vwatek/apply/data/api/GeminiService.kt) implements a **fallback chain**:
1. Call backend `/api/v1/ai/*` endpoint
2. If that fails, call Gemini API directly from client (with API key from local config)
3. If that fails, call OpenAI API directly from client

| # | Issue | Severity | Details |
|---|-------|----------|---------|
| AI-01 | Client-side AI fallback exposes API keys in client apps | **HIGH** (covered in Secrets section) | [GeminiService.kt L564, L577](shared/src/commonMain/kotlin/com/vwatek/apply/data/api/GeminiService.kt#L564) |
| AI-02 | No prompt injection protection on either client or server path | **MEDIUM** | Both AI paths |

---

## 5. Credentials & Secrets

### 5.1 Critical Exposures

| # | Issue | Severity | Location |
|---|-------|----------|----------|
| SEC-11 | **Live Stripe keys (pk_live, sk_live, whsec)** in `secrets.properties` on disk | **CRITICAL** | [secrets.properties L64-66](secrets.properties#L64) |
| SEC-12 | **Gemini API key** hardcoded in browser-served `config.js` | **CRITICAL** | [webApp/src/jsMain/resources/config.js L6](webApp/src/jsMain/resources/config.js#L6) |
| SEC-13 | **OpenAI API key** hardcoded in browser-served `config.js` | **CRITICAL** | [webApp/src/jsMain/resources/config.js L7](webApp/src/jsMain/resources/config.js#L7) |
| SEC-14 | **Cloud SQL password** `VwaTekDB2026` in `secrets.properties` | **CRITICAL** | [secrets.properties L53](secrets.properties#L53) |
| SEC-15 | **LinkedIn client secret** in `secrets.properties` | **CRITICAL** | [secrets.properties L18](secrets.properties#L18) |
| SEC-16 | **Google App Password** in `secrets.properties` (SMTP) | **CRITICAL** | [secrets.properties L28, L35](secrets.properties#L28) |
| SEC-17 | Cloud SQL IP `34.134.196.247` hardcoded in service YAML, deploy script, and template | **HIGH** | [cloudrun-service.yaml L40](backend/cloudrun-service.yaml#L40), [deploy.sh L142](deploy.sh#L142) |
| SEC-18 | `secrets.properties.template` includes real DB IP | **MEDIUM** | [secrets.properties.template L63](secrets.properties.template#L63) |
| SEC-19 | Web `Main.kt` reads config.js keys and persists them in localStorage | **HIGH** | [Main.kt L43-82](webApp/src/jsMain/kotlin/com/vwatek/apply/Main.kt#L43) |
| SEC-20 | Database user is `root` in production | **HIGH** | [secrets.properties L52](secrets.properties#L52), [main.tf L149](infrastructure/terraform/main.tf#L149) |

### 5.2 Gitignore Status

| File | Gitignored | In Git History |
|------|-----------|---------------|
| `secrets.properties` |  Yes | No  |
| `local.properties` |  Yes | No  |
| `google-services.json` |  Yes (placeholder only) | No  |
| `config.js` |  Yes | No  |
| `.env*` |  Yes | No  |
| `GoogleService-Info.plist` |  Yes | No  |

### 5.3 Backend Environment Variables

| Variable | Source | Default | Risk |
|----------|--------|---------|------|
| `JWT_SECRET` | env / hardcoded | `"vwatek-apply-secret-key-change-in-production"` | **CRITICAL** if env not set |
| `GEMINI_API_KEY` | env | `""` | OK if empty |
| `OPENAI_API_KEY` | env | `""` | OK if empty |
| `STRIPE_SECRET_KEY` | env | `""` | **Not in Cloud Run secrets config** |
| `STRIPE_WEBHOOK_SECRET` | env | `""` | **Not in Cloud Run secrets config** |
| `SMTP_PASSWORD` | env | `""` | **Not in Cloud Run secrets config** |
| `LINKEDIN_CLIENT_ID/SECRET` | env | `""` | **Not in Cloud Run secrets config** |
| DB credentials | GCP Secret Manager | — |  Properly managed |

---

## 6. Database & Migrations

### 6.1 Schema — 9 Table Files, 40+ Tables

| File | Tables |
|------|--------|
| Tables.kt | Users, Resumes, ResumeVersions, ResumeAnalyses, CoverLetters, InterviewSessions, InterviewQuestions, Settings |
| JobTrackerTables.kt | JobApplications, StatusHistory, Notes, Reminders, Interviews, Documents |
| NOCTables.kt | NOCCodes, MainDuties, EmploymentRequirements, AdditionalInfo, Skills, + immigration/demand |
| SubscriptionTables.kt | Subscriptions, Payments, StripeCustomers, UsageTracking, Events, Prices, Promos |
| NotificationTables.kt | Notifications, Preferences, DeviceTokens, Scheduled |
| SalaryDataTables.kt | SalaryData, ComparisonHistory, JobOffers, NegotiationSessions, + more |
| EnterpriseTables.kt | Organizations, Members, Invitations, Templates, ActivityLog, LinkedInProfiles, AnalysisHistory, SSOSessions, AdminReports, SubscriptionHistory |
| SyncTables.kt | Devices, SyncLogs, SyncMetadata, DeviceSyncState, OfflineOperations, SyncConflicts, UserDataRegions, ChangeFeed |
| PrivacyTables.kt | ConsentRecords, ConsentAuditLog, DataAccessRequests, DataRetention, DataSharingLog |

### 6.2 Missing Indexes (HIGH Impact)

| Table | Column(s) | Query Pattern |
|-------|-----------|---------------|
| `ResumesTable` | `userId` | All resume queries filter by user |
| `CoverLettersTable` | `userId` | All cover letter queries filter by user |
| `InterviewSessionsTable` | `userId` | All interview queries filter by user |
| `JobApplicationsTable` | `userId` | All tracker queries filter by user |
| `JobApplicationsTable` | `status` | Kanban board filters by status |
| `JobApplicationStatusHistoryTable` | `applicationId` | History lookups |
| `JobApplicationNotesTable` | `applicationId` | Note lookups |
| `JobApplicationRemindersTable` | `applicationId` | Reminder lookups |
| `JobApplicationRemindersTable` | `reminderAt`, `isCompleted` | Scheduling queries |
| `JobApplicationInterviewsTable` | `applicationId` | Interview lookups |
| `JobApplicationInterviewsTable` | `scheduledAt` | Scheduling queries |
| `JobApplicationDocumentsTable` | `applicationId` | Document lookups |
| `ResumeVersionsTable` | `resumeId` | Version history lookups |
| `ResumeAnalysesTable` | `resumeId` | Analysis lookups |

### 6.3 Missing Foreign Key Constraints

| Table | Column | Should Reference |
|-------|--------|-----------------|
| `JobApplicationsTable` | `resumeId` | `ResumesTable.id` |
| `JobApplicationsTable` | `coverLetterId` | `CoverLettersTable.id` |
| `NotificationsTable` | `userId` | `UsersTable.id` |
| `NotificationPreferencesTable` | `userId` | `UsersTable.id` |
| `DeviceTokensTable` | `userId` | `UsersTable.id` |
| `ScheduledNotificationsTable` | `userId` | `UsersTable.id` |
| `ChangeFeedTable` | `userId` | `UsersTable.id` |

### 6.4 Data Type Issues

| Table | Issue |
|-------|-------|
| `NotificationsTable` | Uses `varchar("created_at", 30)` (string) instead of `timestamp` — inconsistent with all other tables |
| `NotificationPreferencesTable` | Same string-based timestamps |
| `DeviceTokensTable` | Same pattern |
| `ScheduledNotificationsTable` | Same pattern |

### 6.5 Client-Side Schema Gap

The SQLDelight schema only covers Phase 1 tables:
-  Resume, ResumeAnalysis, CoverLetter, InterviewSession, InterviewQuestion, Settings
-  Job Tracker (6 tables) — **not in client DB**
-  Notifications (4 tables) — **not in client DB**
-  Sync (8 tables) — **not in client DB**
-  Salary (4+ tables) — **not in client DB**
-  Organization (10 tables) — **not in client DB**

### 6.6 Migration Strategy

| # | Issue | Severity | Details |
|---|-------|----------|---------|
| DB-01 | **No versioned migrations** — uses `SchemaUtils.createMissingTablesAndColumns()` which cannot drop columns, rename columns, change types, or rollback | **HIGH** | [DatabaseConfig.kt L224](backend/src/main/kotlin/com/vwatek/apply/config/DatabaseConfig.kt#L224) |
| DB-02 | **No migration files** — no `.sql` scripts anywhere in the project | **HIGH** | Project-wide |
| DB-03 | All 40+ tables created in a single `transaction {}` — no incremental migration versioning | **MEDIUM** | [DatabaseConfig.kt L224-296](backend/src/main/kotlin/com/vwatek/apply/config/DatabaseConfig.kt#L224) |
| DB-04 | **HikariCP pool (10) vs Cloud Run concurrency (80)** — 80 concurrent requests sharing 10 connections will cause contention under load | **HIGH** | [DatabaseConfig.kt L103](backend/src/main/kotlin/com/vwatek/apply/config/DatabaseConfig.kt#L103) vs [cloudrun-service.yaml](backend/cloudrun-service.yaml) |
| DB-05 | Blocking `transaction {}` calls in Ktor coroutine context — should use `newSuspendedTransaction` | **MEDIUM** | All route files using DB |
| DB-06 | **Cloud SQL authorized_networks = `0.0.0.0/0`** — database open to ANY IP on the internet | **CRITICAL** | [main.tf L482](infrastructure/terraform/main.tf#L482) |

---

## 7. Build & Deployment

### 7.1 Docker

| # | Issue | Severity | Details |
|---|-------|----------|---------|
| BLD-01 | **Container runs as root** — no `USER` directive in Dockerfile | **HIGH** | [Dockerfile.backend](Dockerfile.backend) |
| BLD-02 | **No .dockerignore** — entire repo sent as Docker context including secrets files | **HIGH** | Project root |
| BLD-03 | Multi-stage build , Alpine base , health check  | OK | |

### 7.2 CI/CD ([cloudbuild.yaml](cloudbuild.yaml))

| # | Issue | Severity | Details |
|---|-------|----------|---------|
| BLD-04 | **Test failures don't block deployment** — `\|\| echo "Some tests failed, continuing..."` | **HIGH** | [cloudbuild.yaml L42](cloudbuild.yaml#L42) |
| BLD-05 | **No staging environment** — deploys directly to production on `main` push | **HIGH** | [cloudbuild.yaml](cloudbuild.yaml) |
| BLD-06 | No security scanning (Trivy, Snyk) in pipeline | **HIGH** | Missing |
| BLD-07 | No linting step | **MEDIUM** | Missing |
| BLD-08 | No approval gate before production | **MEDIUM** | Missing |

### 7.3 Cloud Run Configuration

| # | Issue | Severity | Details |
|---|-------|----------|---------|
| BLD-09 | **Region mismatch** — `cloudrun-service.yaml` references `us-central1` but `cloudbuild.yaml` and `deploy.sh` use `northamerica-northeast1` | **HIGH** | [cloudrun-service.yaml](backend/cloudrun-service.yaml) vs [cloudbuild.yaml](cloudbuild.yaml) |
| BLD-10 | Hardcoded DB IP `34.134.196.247` — should use Cloud SQL proxy via Unix socket | **HIGH** | [cloudrun-service.yaml L40](backend/cloudrun-service.yaml#L40) |
| BLD-11 | STRIPE_SECRET_KEY, SMTP_PASSWORD, LINKEDIN secrets NOT in Cloud Run `--set-secrets` | **MEDIUM** | [cloudbuild.yaml L96](cloudbuild.yaml#L96) |

### 7.4 Firebase Hosting

| Check | Status |
|-------|--------|
| SPA routing |  |
| API proxy to Cloud Run |  |
| Security headers (X-Frame-Options, X-Content-Type-Options) |  |
| Missing `Content-Security-Policy` header | **MEDIUM** |
| Missing `Strict-Transport-Security` header | **LOW** |
| Cache control (JS/CSS 1yr) |  |

### 7.5 Terraform

| # | Issue | Severity | Details |
|---|-------|----------|---------|
| BLD-12 | **State backend commented out** — using local state only; must be remote for team collaboration | **HIGH** | [main.tf L20-23](infrastructure/terraform/main.tf#L20-L23) |
| BLD-13 | Storage bucket CORS `origin = ["*"]` — open to any origin | **MEDIUM** | [main.tf L197](infrastructure/terraform/main.tf#L197) |
| BLD-14 | Canadian data residency configuration  (Montreal region, PITR backup) | OK | |
| BLD-15 | Global load balancer with geo-routing  | OK | |

---

## 8. Cross-Platform Integration

### 8.1 ViewModel Parity

| Shared ViewModel | Android | iOS | Web | Notes |
|-----------------|---------|-----|-----|-------|
| AuthViewModel |  Direct Koin inject |  AuthViewModelWrapper |  Direct Koin | |
| ResumeViewModel |  |  ResumeViewModelWrapper |  | |
| CoverLetterViewModel |  |  CoverLetterViewModelWrapper |  | |
| InterviewViewModel |  |  InterviewViewModelWrapper |  | |
| TrackerViewModel |  |  TrackerViewModelWrapper |  | |
| NOCViewModel |  |  NOCViewModelWrapper |  | |
| JobBankViewModel |  |  JobBankViewModelWrapper |  | |
| SalaryIntelligenceManager |  |  SalaryIntelligenceManagerWrapper |  | |
| LinkedInOptimizerManager |  |  LinkedInOptimizerViewModelWrapper |  | |
| OrganizationViewModel |  |  OrganizationViewModelWrapper |  | |
| SubscriptionManager |  |  SubscriptionManagerWrapper |  | |
| SettingsRepository |  |  **Local @State only** |  | iOS gap |

### 8.2 Feature Status by Platform

| Feature | Android | iOS | Web | Backend |
|---------|---------|-----|-----|---------|
| Auth (email/password) |  |  |  |  |
| Auth (Google) |  |  |  |  |
| Auth (Apple) |  |  |  |  (no endpoint) |
| Auth (LinkedIn) |  |  |  |  |
| Resume CRUD |  + sync |  + sync |  API-only |  |
| Resume ATS Analysis |  |  |  |  |
| Cover Letter Generation |  |  |  |  |
| Interview Practice |  |  |  |  |
| Job Tracker |  UI |  UI |  UI |  (but client auth broken) |
| NOC Search |  |  |  |  |
| Job Bank Search |  |  |  |  |
| Salary Insights |  + paywall |  + paywall |  + paywall |  (but client auth broken) |
| LinkedIn Optimizer |  |  |  |  (but client auth broken) |
| Organization/Enterprise |  UI |  UI |  UI |  Auth broken |
| Subscription/Billing |  UI |  UI |  UI |  (but client auth broken) |
| Notifications |  No client |  No client |  No client |  Routes double-prefixed |
| Sync/Offline | Partial (resumes only) | Partial (resumes only) |  None |  Auth broken |
| Privacy/GDPR |  |  |  |  |
| PDF Export |  |  |  | N/A |
| STAR Coaching |  |  |  | N/A |
| Version History |  |  |  |  Backend support |

### 8.3 Deep Linking

| Platform | Scheme | Implementation |
|----------|--------|---------------|
| Android | `vwatekapply://<host>` | Maps host to NavigationItem route |
| iOS | `vwatekapply://<host>` | Maps host to tab tag integer |
| Web | `#/<path>` | Hash-based routing (browser only) |

---

## 9. Issue Tracker — All Findings

### CRITICAL (19)

| ID | Category | Issue | Location |
|----|----------|-------|----------|
| SEC-01 | Auth | X-User-Id header as sole authentication — spoofable | Multiple route files |
| SEC-02 | Auth | Hardcoded default JWT secret in source | Security.kt L16 |
| SEC-03 | Auth | AI endpoints entirely unauthenticated | AIRoutes.kt |
| SEC-07 | Auth | Token format mismatch: UUID tokens vs JWT validation | AuthRoutes.kt L129 |
| SEC-11 | Secrets | Live Stripe keys (sk_live) in secrets.properties | secrets.properties L65 |
| SEC-12 | Secrets | Gemini API key in browser-served config.js | config.js L6 |
| SEC-13 | Secrets | OpenAI API key in browser-served config.js | config.js L7 |
| SEC-14 | Secrets | Cloud SQL password in plaintext on disk | secrets.properties L53 |
| SEC-15 | Secrets | LinkedIn client secret in secrets.properties | secrets.properties L18 |
| SEC-16 | Secrets | Google App Password in secrets.properties | secrets.properties L28 |
| CORS-01 | Network | anyHost() overrides host allowlist | CORS.kt L28 |
| CORS-02 | Network | anyHost() + allowCredentials = dangerous | CORS.kt L28-30 |
| API-01 | Parity | UUID tokens fail JWT verification on protected routes | AuthRoutes.kt vs Security.kt |
| API-02 | Parity | 5 API clients send zero auth headers | Multiple API client files |
| NAV-01 | Frontend | Web has no auth guards — all screens public | App.kt L100 |
| DB-06 | Database | Cloud SQL authorized_networks = 0.0.0.0/0 | main.tf L482 |
| ROUTE-01 | Backend | Notification routes double-prefixed (/api/v1/api/v1/...) | NotificationRoutes.kt + Routing.kt |
| SEC-04 | Auth | Organization routes auth broken (requireAuth without authenticate) | OrganizationRoutes.kt |
| SEC-05 | Auth | Sync routes auth broken (reads JWTPrincipal without authenticate) | SyncRoutes.kt |

### HIGH (28)

| ID | Category | Issue | Location |
|----|----------|-------|----------|
| SEC-06 | Auth | SHA-256 password hashing (should be bcrypt/argon2) | AuthRoutes.kt L96 |
| SEC-08 | Auth | NOC user match endpoints use userId in path — IDOR | NOCRoutes.kt L310, L392 |
| SEC-09 | Auth | No rate limiting on any endpoint | Entire backend |
| SEC-17 | Secrets | Cloud SQL IP hardcoded in YAML and scripts | cloudrun-service.yaml L40 |
| SEC-19 | Secrets | Web Main.kt persists API keys to localStorage | Main.kt L43-82 |
| SEC-20 | Secrets | Database user is root in production | main.tf L149 |
| CORS-03 | Network | X-User-Id is an allowed CORS header | CORS.kt L21 |
| API-03 | Parity | No global auth interceptor | All HttpClient configs |
| API-04 | Parity | No retry logic | All API clients |
| API-05 | Parity | No HTTP status differentiation (401/403/404/500) | All API clients |
| AI-01 | Parity | Client-side AI fallback exposes keys | GeminiService.kt L564 |
| NAV-02 | Frontend | Android Paywall not wired (TODO) | VwaTekApp.kt L178 |
| NAV-03 | Frontend | iOS Settings uses only local @State | SettingsView.swift L6 |
| DB-01 | Database | No versioned migrations — uses SchemaUtils only | DatabaseConfig.kt L224 |
| DB-02 | Database | No migration files (.sql) anywhere | Project root |
| DB-04 | Database | HikariCP pool 10 vs Cloud Run concurrency 80 | DatabaseConfig.kt L103 |
| DB-IDX | Database | Missing indexes on 14+ frequently queried columns | Multiple table files |
| DB-FK | Database | Missing foreign key constraints on 7 columns | JobTracker, Notification tables |
| BLD-01 | Deploy | Docker container runs as root | Dockerfile.backend |
| BLD-02 | Deploy | No .dockerignore file | Project root |
| BLD-04 | Deploy | Test failures don't block deployment | cloudbuild.yaml L42 |
| BLD-05 | Deploy | No staging environment | Entire CI/CD |
| BLD-06 | Deploy | No security scanning in pipeline | cloudbuild.yaml |
| BLD-09 | Deploy | Region mismatch (us-central1 vs northamerica-northeast1) | cloudrun-service.yaml vs cloudbuild.yaml |
| BLD-10 | Deploy | Hardcoded DB IP in Cloud Run config | cloudrun-service.yaml L40 |
| BLD-12 | Deploy | Terraform state not remote (commented out) | main.tf L20-23 |
| VAL-01 | Validation | No password strength requirements | AuthRoutes.kt L70 |
| TOKEN-01 | Auth | iOS stores auth token in NSUserDefaults (unencrypted) | IosRepositories.kt L75 |

### MEDIUM (26)

| ID | Category | Issue | Location |
|----|----------|-------|----------|
| SEC-10 | Auth | Duplicate JWT configs (jwt/auth-jwt) identical | Security.kt |
| SEC-18 | Secrets | secrets.properties.template has real DB IP | secrets.properties.template L63 |
| API-06 | Parity | No token refresh flow | All platforms |
| AI-02 | Parity | No prompt injection protection | AIService.kt, GeminiService.kt |
| NAV-04 | Frontend | Web Paywall/Auth screens not deep-linkable | App.kt |
| NAV-05 | Frontend | No back-stack management on any platform | All platforms |
| NAV-06 | Frontend | iOS Interview lacks ResumeViewModel | InterviewView.swift |
| ERR-02 | Error | ORM exceptions not specifically handled | All DB routes |
| ERR-03 | Error | JSON deserialization errors return stack trace | All POST/PUT routes |
| DB-03 | Database | All tables in single transaction block | DatabaseConfig.kt L224-296 |
| DB-05 | Database | Blocking transaction {} in coroutine context | All route files |
| DB-DT | Database | Notification tables use varchar timestamps | NotificationTables.kt |
| VAL-02 | Validation | No request body size limits | All POST/PUT routes |
| VAL-03 | Validation | No input sanitization (XSS risk) | All text-input routes |
| VAL-04 | Validation | No pagination limits | NOC, Salary, Org routes |
| VAL-05 | Validation | No URL validation on checkout callbacks | SubscriptionRoutes.kt L168 |
| VAL-06 | Validation | No AI prompt injection protection | AIService.kt |
| BLD-07 | Deploy | No linting step in CI | cloudbuild.yaml |
| BLD-08 | Deploy | No approval gate before prod deploy | cloudbuild.yaml |
| BLD-11 | Deploy | Missing secrets in Cloud Run config | cloudbuild.yaml L96 |
| BLD-13 | Deploy | Storage bucket CORS origin = * | main.tf L197 |
| MON-01 | Monitor | /metrics endpoint unauthenticated | Monitoring.kt L265 |
| MON-02 | Monitor | Readiness probe is stub (no dependency check) | Monitoring.kt L279 |
| MON-03 | Monitor | Health endpoint is static — no dependency checks | Routing.kt L25 |
| MON-04 | Monitor | Log format has no request ID / user ID | CallLogging.kt |
| FIRE-01 | Deploy | Missing Content-Security-Policy header | firebase.json |

### LOW (14)

| ID | Category | Issue | Location |
|----|----------|-------|----------|
| ERR-04 | Error | println used for error logging | SubscriptionRoutes.kt L566, AIService.kt L33 |
| VAL-07 | Validation | Email validation only checks for @ | AuthRoutes.kt L65 |
| NAV-07 | Frontend | iOS-only sheets have no cross-platform equivalents | iosApp Views/ |
| MON-05 | Monitor | Custom metrics defined but only used in SyncRoutes | Monitoring.kt |
| MON-06 | Monitor | No structured logging (JSON format) | CallLogging.kt |
| MON-07 | Monitor | Call logging only covers /api paths | CallLogging.kt L12 |
| MON-08 | Monitor | Duplicate log entries (CallLogging + custom) | Multiple route files |
| BLD-14 | Deploy | No preview channels in Firebase | firebase.json |
| FIRE-02 | Deploy | Missing Strict-Transport-Security header | firebase.json |
| DB-IDX-L | Database | UsersTable missing index on authProvider | Tables.kt |
| SYNC-01 | Integration | SyncEngine interface defined but no actual implementations | SyncEngine.kt |
| SYNC-02 | Integration | Only resumes sync — CL/interview/tracker don't sync | SyncingResumeRepository.kt |
| CACHE-01 | Integration | Web has no offline/caching support | Web API repositories |
| SCHEMA-01 | Database | Client SQLDelight schema missing Phase 2+ tables | VwaTekDatabase.sq |

---

## 10. Recommended Fix Priority

### Phase 1 — CRITICAL Security Fixes (Immediate)  COMPLETED

1.  **Fix authentication architecture** — Implemented proper JWT token generation in AuthRoutes (HMAC256-signed JWTs with userId, email, audience, issuer, expiry claims replacing UUID tokens)
2.  **Add auth token to API clients** — Added `getAuthToken` constructor param + `applyAuth()` helper to JobTrackerApiClient, SubscriptionApiClient, SalaryApiClient, OrganizationApiClient. Added `getAuthToken()` to AuthRepository interface + all 4 platform implementations. Wired via Koin DI.
3.  **Remove `anyHost()` from CORS** — Whitelisted specific production domains only (Firebase Hosting, Cloud Run, GCS, localhost). Removed `X-User-Id` from allowed headers.
4.  **Remove API keys from config.js** — Removed `initializeDefaultApiKeys()` from web Main.kt, updated config.js.template to document that AI keys stay server-side only.
5.  **Add `authenticate("jwt")` wrapper** — Added to Organization routes, AI routes, Sync routes (in Routing.kt), Privacy routes (in Routing.kt). Migrated Subscription, Salary, Resume, CoverLetter, Interview routes from X-User-Id header to JWT auth via new `requireUserId()` helper.
6.  **Fix notification routes double-prefix** — Changed from `route("/api/v1/notifications")` to `route("/notifications")` since already mounted inside `/api/v1`.
7.  **Restrict Cloud SQL authorized_networks** — Disabled public IP (`ipv4_enabled = false`), removed `0.0.0.0/0` authorized_networks. Access via private VPC only.
8.  **Add web auth guard** — Added `LaunchedEffect` in App.kt that redirects unauthenticated users to auth screen. Updated `showAuthScreen` logic.
9. **Rotate all exposed secrets** — _Manual operation required. Rotate: Stripe keys, Gemini key, OpenAI key, DB password, LinkedIn secret, SMTP password._ 

### Phase 2 — HIGH Security & Reliability (Week 1-2)

10.  **Replace SHA-256 hashing** with bcrypt (cost 12) + backward-compatible legacy fallback _(completed)_
11.  **Add rate limiting** — auth (20/min), AI (10/min), subscription (30/min), global (100/min) using Ktor RateLimit plugin _(completed)_
12.  **Add missing auth headers** to JobTracker, Subscription, Salary, Organization, LinkedIn API clients _(completed in Phase 1)_
13.  **Fix NOC user match routes** — changed to `/users/me/matches` with JWT auth, updated NOCApiClient + NOCViewModel _(completed)_
14.  **Add database indexes** on 14+ columns across Tables.kt and JobTrackerTables.kt (userId, status, applicationId, resumeId, scheduledAt, reminderAt+isCompleted) _(completed)_
15.  **Add foreign key constraints** — 7 new FK refs: NotificationsTable, NotificationPreferencesTable, DeviceTokensTable, ScheduledNotificationsTable, EmailQueueTable, ChangeFeedTable userId→UsersTable; JobApplicationsTable resumeId→ResumesTable, coverLetterId→CoverLettersTable _(completed)_
16.  **Add `.dockerignore`** and non-root user (appuser/appgroup) to both Dockerfiles _(completed)_
17.  **Make test failures block deployment** — removed `|| echo` in cloudbuild.yaml _(completed)_
18.  **Fix region mismatch** — updated cloudrun-service.yaml from us-central1 to northamerica-northeast1 (5 occurrences) _(completed)_
19.  **Move iOS token storage** from NSUserDefaults to Keychain via KeychainHelper with auto-migration of existing tokens _(completed)_
20.  **Create least-privilege DB user** — Terraform updated: secret changed from "root" to "vwatek_app", added google_sql_user resource with GRANT instructions _(completed)_

### Phase 3 — Stability & Operations (Week 2-4)  COMPLETED

21. ~~**Implement versioned database migrations** (Flyway or Liquibase)~~  Added Flyway 10.10.0 with baseline-on-migrate, V1 baseline SQL, integrated into DatabaseConfig
22. ~~**Add staging environment** with separate Cloud Run service~~  Created cloudbuild-staging.yaml + Terraform staging Cloud Run service + staging database
23. ~~**Implement token refresh flow** across all platforms~~  Backend POST /auth/refresh endpoint + refreshToken() on Android, iOS, Web API, Web LocalStorage
24. ~~**Increase HikariCP pool size** or reduce Cloud Run concurrency~~  Reduced pool to 5 (10×5=50, within Cloud SQL 100 limit), concurrency to 50, added leak detection
25. ~~**Add password strength validation** (min 8 chars, complexity)~~  Added validatePasswordStrength() — min 8, upper, lower, digit, special char required
26. ~~**Add request body size limits** and input sanitization~~  Created RequestValidation plugin (1MB/10MB limits), sanitizeInput() strips HTML/control chars
27. ~~**Wire Android paywall** to SalaryInsights~~  Already wired — FeatureGatedContent + subscriptionManager.canUseFeature(PremiumFeature.SALARY_INSIGHTS)
28. ~~**Connect iOS Settings** to shared SettingsRepository~~  Already connected — SettingsHelper.shared.getSetting/setSetting bridges to KMP SettingsRepository
29. ~~**Enable remote Terraform state** backend~~  Uncommented GCS backend block, added google_storage_bucket resource with versioning
30. ~~**Add security scanning** to CI/CD pipeline~~  Added OWASP dependency-check plugin + Trivy container image scanning in cloudbuild.yaml

### Phase 4 — Quality & Completeness (Week 4+)  COMPLETED

31. ~~**Implement proper readiness probe**~~  `/health/ready` checks DB (SELECT 1), AI service config, memory pressure (<95%)
32. ~~**Add structured JSON logging**~~  CallId plugin (UUID X-Request-Id), structured JSON format with request_id, user_id, duration_ms, IP
33. ~~**Protect /metrics endpoint**~~  Bearer token auth via METRICS_AUTH_TOKEN env var
34. ~~**Add pagination limits**~~  All list endpoints capped at max 100, default 50 (resumes, analyses, cover-letters, interviews, jobs)
35. ~~**Add Content-Security-Policy and HSTS headers**~~  SecurityHeaders.kt: CSP, HSTS (1yr+preload), X-Frame-Options DENY, X-Content-Type-Options, X-XSS-Protection, Referrer-Policy, Permissions-Policy
36. ~~**Expand SQLDelight schema**~~  Added JobApplication (14 cols), JobApplicationNote, PendingSync tables with full CRUD queries
37. ~~**Implement full sync**~~  SyncEntityType.JOB_APPLICATION added, PendingSync table for offline queue
38. ~~**Add client-side retry logic**~~  HttpClientExtensions.kt: HttpRequestRetry (3 retries, exponential backoff 500ms–10s), installed in Android/iOS/JS HttpClients
39. ~~**Add HTTP status code differentiation**~~  HttpResponseValidator: 401→onUnauthorized callback, 403→exception, 429→exception
40. ~~**Add AI prompt injection protection**~~  16 injection regex patterns, sanitizeInput() on all user fields, delimited user content sections, OpenAI system message guardrail

---

*Report generated March 3, 2026. Total effort estimate: 3-6 developer weeks for Phases 1-3.*
