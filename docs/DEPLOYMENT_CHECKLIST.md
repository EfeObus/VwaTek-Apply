# VwaTek Apply - Deployment Guide

## Overview

VwaTek Apply backend is deployed on **Railway** with automatic deployments triggered by pushes to the `main` branch on GitHub.

---

## Railway Setup

### Project Configuration

| Item | Status | Details |
|------|--------|---------|
| Project | ✅ Complete | `vwatek-apply-production` |
| Region | ✅ Complete | US West (Railway managed) |
| Auto-Deploy | ✅ Enabled | Triggers on push to `main` |

### Environment Variables

The following environment variables are configured in Railway:

| Variable | Description |
|----------|-------------|
| `DATABASE_URL` | PostgreSQL connection string (auto-provisioned) |
| `GEMINI_API_KEY` | Gemini AI API key |
| `JWT_SECRET` | JWT signing secret |
| `STRIPE_SECRET_KEY` | Stripe payment processing |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook verification |
| `STRIPE_PRICE_PRO_MONTHLY` | Stripe Pro monthly price ID |
| `STRIPE_PRICE_PRO_YEARLY` | Stripe Pro yearly price ID |
| `STRIPE_PRICE_PREMIUM_MONTHLY` | Stripe Premium monthly price ID |
| `STRIPE_PRICE_PREMIUM_YEARLY` | Stripe Premium yearly price ID |
| `ENVIRONMENT` | `production` |

### Database

| Item | Status | Details |
|------|--------|---------|
| PostgreSQL | ✅ Provisioned | Railway managed PostgreSQL |
| Auto-Migration | ✅ Enabled | Schema migrations run on startup |
| SSL | ✅ Required | Connections encrypted |

---

## CI/CD Configuration

### GitHub Actions

| Workflow | Purpose | Trigger |
|----------|---------|---------|
| `ci.yml` | Continuous Integration | Pull requests, push to main |
| `release.yml` | Release Management | Tags |

### Railway Auto-Deploy

Railway automatically builds and deploys when:
1. Code is pushed to `main` branch
2. Environment variables are updated

Build process:
1. Railway detects `Dockerfile` in `backend/`
2. Builds Docker image using multi-stage build
3. Deploys to production environment
4. Health check verifies deployment

---

## Backend Configuration

### Dockerfile (`backend/Dockerfile`)

The backend uses a multi-stage Docker build:
- **Build stage**: Gradle 8.5 with JDK 17 builds the shadow JAR
- **Runtime stage**: Eclipse Temurin 17 JRE Alpine for minimal image size

### Build Features

- **Conditional Web Assets**: The `copyWebAssets` task is conditional and skips when building backend-only (Docker builds)
- **Shadow JAR**: Creates a fat JAR with all dependencies bundled
- **Non-root User**: Runs as `appuser` for security

---

## URLs

| Service | URL |
|---------|-----|
| Backend API | `https://vwatek-apply-production.up.railway.app` |
| Health Check | `https://vwatek-apply-production.up.railway.app/health` |
| Metrics | `https://vwatek-apply-production.up.railway.app/metrics` |
| Railway Dashboard | `https://railway.app/dashboard` |

---

## Deployment Commands

### Manual Deployment (Local)

```bash
# Build backend locally
./gradlew :backend:shadowJar

# Run locally
java -jar backend/build/libs/backend-all.jar
```

### Build Web Frontend

```bash
./gradlew :webApp:jsBrowserProductionWebpack
```

### Build Android App

```bash
./gradlew :androidApp:assembleRelease
```

### Build iOS App

```bash
cd iosApp
xcodebuild -scheme iosApp -configuration Release
```

---

## API Endpoints Verification

After deployment, verify endpoints:

1. **Health Check**
```bash
curl https://vwatek-apply-production.up.railway.app/health
```

2. **Metrics Endpoint**
```bash
curl https://vwatek-apply-production.up.railway.app/metrics
```

3. **API Status**
```bash
curl https://vwatek-apply-production.up.railway.app/api/v1/pricing
```

4. **Auth Endpoints**
```bash
curl -X POST https://vwatek-apply-production.up.railway.app/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "password"}'
```

---

## Rollback

Railway maintains deployment history. To rollback:

1. Go to Railway Dashboard
2. Select the project
3. Navigate to Deployments
4. Click on a previous successful deployment
5. Select "Rollback to this deployment"

---

## Monitoring

### Backend Logs

View logs in Railway Dashboard or via CLI:
```bash
railway logs
```

### Metrics

Prometheus metrics available at `/metrics` endpoint:
- JVM metrics
- HTTP request metrics
- Custom application metrics

### Error Tracking

- **Mobile**: Firebase Crashlytics (Android/iOS)
- **Web**: Sentry for error tracking

---

## Support

- **Railway Docs**: https://docs.railway.app
- **Railway Status**: https://status.railway.app
- **GitHub Issues**: https://github.com/EfeObus/VwaTek-Apply/issues
