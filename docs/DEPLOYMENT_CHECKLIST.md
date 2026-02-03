# VwaTek Apply - Deployment Checklist

## Pre-Deployment Checklist Status

### ✅ Google Cloud Setup

| Item | Status | Details |
|------|--------|---------|
| GCP Project | ✅ Complete | `vwatek-apply` (Project ID: 21443684777) |
| Authentication | ✅ Complete | Logged in as `talk2efeprogress@gmail.com` |
| APIs Enabled | ✅ Complete | All required APIs enabled |

### ✅ APIs Enabled

| API | Status |
|-----|--------|
| Cloud Run | ✅ Enabled |
| Cloud Build | ✅ Enabled |
| Secret Manager | ✅ Enabled |
| Cloud SQL Admin | ✅ Enabled |
| Container Registry | ✅ Enabled |
| Resource Manager | ✅ Enabled |
| IAM | ✅ Enabled |
| VPC Access | ✅ Enabled |
| Compute Engine | ✅ Enabled |

### ✅ Secret Manager Secrets

| Secret Name | Status | Description |
|-------------|--------|-------------|
| `db-username` | ✅ Created | Database username (root) |
| `db-password` | ✅ Created | Database password |
| `gemini-api-key` | ✅ Created | Gemini AI API key |

### ✅ Service Accounts

| Service Account | Status | Purpose |
|-----------------|--------|---------|
| `vwatek-cloudrun-sa` | ✅ Created | Cloud Run service runtime |
| `vwatek-cicd-sa` | ✅ Created | CI/CD pipeline automation |

### ✅ IAM Roles Assigned

#### vwatek-cloudrun-sa (Cloud Run Service)
- ✅ `roles/cloudsql.client` - Access Cloud SQL
- ✅ `roles/secretmanager.secretAccessor` - Read secrets

#### vwatek-cicd-sa (CI/CD Pipeline)
- ✅ `roles/run.admin` - Deploy Cloud Run services
- ✅ `roles/storage.admin` - Manage Cloud Storage
- ✅ `roles/cloudbuild.builds.builder` - Run Cloud Build
- ✅ `roles/iam.serviceAccountUser` - Use service accounts
- ✅ `roles/secretmanager.secretAccessor` - Read secrets
- ✅ `roles/cloudsql.client` - Access Cloud SQL

### ✅ Network Configuration

| Item | Status | Details |
|------|--------|---------|
| VPC Network | ✅ Using `default` | Standard GCP VPC |
| VPC Connector | ✅ Created | `vwatek-connector` (us-central1) |
| IP Range | ✅ Configured | `10.8.0.0/28` |
| Cloud SQL Auth Network | ✅ Configured | `142.114.123.165/32` |

### ✅ Cloud Storage

| Bucket | Status | Purpose |
|--------|--------|---------|
| `vwatek-apply-web` | ✅ Created | Frontend static hosting |

### ✅ Cloud SQL Database

| Item | Status | Details |
|------|--------|---------|
| Instance | ✅ Running | `vwatekapply` |
| Region | ✅ Configured | `us-central1` |
| IP Address | ✅ Assigned | `34.134.196.247` |
| Database | ✅ Created | `Vwatek_Apply` |
| SSL | ✅ Required | SSL mode enabled |
| Tables | ✅ Migrated | 8 tables created |

### ✅ CI/CD Configuration Files

| File | Status | Purpose |
|------|--------|---------|
| `.github/workflows/ci.yml` | ✅ Created | Continuous Integration |
| `.github/workflows/cd.yml` | ✅ Created | Continuous Deployment |
| `.github/workflows/release.yml` | ✅ Created | Release Management |
| `cloudbuild.yaml` | ✅ Created | Google Cloud Build |
| `backend/Dockerfile` | ✅ Created | Container image build |
| `backend/cloudrun-service.yaml` | ✅ Updated | Cloud Run service config |
| `deploy.sh` | ✅ Created | Manual deployment script |

### ✅ Infrastructure as Code

| File | Status | Purpose |
|------|--------|---------|
| `infrastructure/terraform/main.tf` | ✅ Created | Terraform IaC |
| `infrastructure/terraform/terraform.tfvars.example` | ✅ Created | Variable template |

---

## ⚠️ Manual Steps Required

### 1. GitHub Repository Secrets

You need to add the following secrets to your GitHub repository:

1. Go to: **Repository → Settings → Secrets and variables → Actions**

2. Add these secrets:

| Secret Name | Source |
|-------------|--------|
| `GCP_SA_KEY` | Contents of `gcp-sa-key.json` (base64 encoded) |
| `GCP_PROJECT_ID` | `vwatek-apply` |

To encode the key:
```bash
cat gcp-sa-key.json | base64 -w 0  # Linux
cat gcp-sa-key.json | base64       # macOS
```

### 2. Delete Local Service Account Key

After adding to GitHub Secrets, delete the local key file:
```bash
rm /Users/efeobukohwo/Desktop/VwaTek/gcp-sa-key.json
```

### 3. Connect Cloud Build to GitHub

1. Go to: [Cloud Build Triggers](https://console.cloud.google.com/cloud-build/triggers?project=vwatek-apply)
2. Click **Connect Repository**
3. Select **GitHub** and authorize
4. Choose the `VwaTek` repository
5. Create trigger for `main` branch

---

## Deployment Commands

### Manual Deployment

```bash
# Deploy backend to Cloud Run
cd /Users/efeobukohwo/Desktop/VwaTek
./deploy.sh

# Or step by step:
# 1. Build and push Docker image
gcloud builds submit --tag gcr.io/vwatek-apply/vwatek-backend backend/

# 2. Deploy to Cloud Run
gcloud run deploy vwatek-backend \
  --image gcr.io/vwatek-apply/vwatek-backend \
  --platform managed \
  --region us-central1 \
  --service-account vwatek-cloudrun-sa@vwatek-apply.iam.gserviceaccount.com \
  --add-cloudsql-instances vwatek-apply:us-central1:vwatekapply \
  --vpc-connector vwatek-connector \
  --allow-unauthenticated \
  --set-secrets="CLOUD_SQL_USER=db-username:latest,CLOUD_SQL_PASSWORD=db-password:latest,GEMINI_API_KEY=gemini-api-key:latest"
```

### Build Frontend

```bash
./gradlew :webApp:jsBrowserProductionWebpack
```

### Deploy Frontend

```bash
gsutil -m rsync -r webApp/build/kotlin-webpack/js/productionExecutable gs://vwatek-apply-web/
gsutil web set -m index.html -e 404.html gs://vwatek-apply-web
```

---

## URLs

| Service | URL |
|---------|-----|
| Backend API | `https://vwatek-backend-[hash]-uc.a.run.app` |
| Frontend Web | `https://storage.googleapis.com/vwatek-apply-web/index.html` |
| Cloud Console | `https://console.cloud.google.com/run?project=vwatek-apply` |
| Build History | `https://console.cloud.google.com/cloud-build/builds?project=vwatek-apply` |

---

## Verification

After deployment, verify:

1. **Backend Health Check**
```bash
curl https://vwatek-backend-[hash]-uc.a.run.app/health
```

2. **Database Connection**
```bash
curl https://vwatek-backend-[hash]-uc.a.run.app/api/status
```

3. **Frontend Accessibility**
Open the frontend URL in a browser

---

## Rollback

If deployment fails:

```bash
# List revisions
gcloud run revisions list --service vwatek-backend --region us-central1

# Rollback to previous revision
gcloud run services update-traffic vwatek-backend \
  --to-revisions [PREVIOUS_REVISION]=100 \
  --region us-central1
```

---

## Support

- **Cloud Run Logs**: `gcloud logs read --service vwatek-backend --region us-central1`
- **Build Logs**: Available in Cloud Build console
- **Error Investigation**: Check Cloud Logging for detailed traces
