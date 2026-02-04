#!/bin/bash
# VwaTek Apply - Google Cloud Deployment Script
# Usage: ./deploy.sh [backend|frontend|all]

set -e

# Configuration
PROJECT_ID="vwatek-apply"
REGION="us-central1"
BACKEND_SERVICE="vwatek-backend"
FRONTEND_BUCKET="vwatek-apply-web"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  VwaTek Apply - Cloud Deployment${NC}"
echo -e "${GREEN}========================================${NC}"

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo -e "${RED}Error: gcloud CLI is not installed${NC}"
    echo "Install from: https://cloud.google.com/sdk/docs/install"
    exit 1
fi

# Set project
gcloud config set project $PROJECT_ID

deploy_backend() {
    echo -e "\n${YELLOW}Deploying Backend to Cloud Run...${NC}"
    
    # Build from project root using the root Dockerfile (which points to backend)
    echo "Building container image..."
    gcloud builds submit --tag gcr.io/$PROJECT_ID/$BACKEND_SERVICE .
    
    # Deploy to Cloud Run
    echo "Deploying to Cloud Run..."
    
    # Check if openai-api-key secret exists
    OPENAI_SECRET=""
    if gcloud secrets describe openai-api-key --project=$PROJECT_ID &>/dev/null; then
        OPENAI_SECRET=",OPENAI_API_KEY=openai-api-key:latest"
        echo "OpenAI API key secret found, will include in deployment"
    else
        echo -e "${YELLOW}Warning: openai-api-key secret not found, deploying with Gemini only${NC}"
    fi
    
    gcloud run deploy $BACKEND_SERVICE \
        --image gcr.io/$PROJECT_ID/$BACKEND_SERVICE \
        --platform managed \
        --region $REGION \
        --allow-unauthenticated \
        --add-cloudsql-instances vwatek-apply:us-central1:vwatekapply \
        --set-env-vars "CLOUD_SQL_DATABASE=Vwatek_Apply" \
        --set-secrets "CLOUD_SQL_USER=db-username:latest,CLOUD_SQL_PASSWORD=db-password:latest,GEMINI_API_KEY=gemini-api-key:latest${OPENAI_SECRET}" \
        --min-instances 0 \
        --max-instances 10 \
        --memory 1Gi \
        --cpu 2
    
    # Get the service URL
    BACKEND_URL=$(gcloud run services describe $BACKEND_SERVICE --region $REGION --format 'value(status.url)')
    echo -e "${GREEN}Backend deployed at: $BACKEND_URL${NC}"
}

deploy_frontend() {
    echo -e "\n${YELLOW}Deploying Frontend to Cloud Storage...${NC}"
    
    # Build the web app
    echo "Building web application..."
    ./gradlew :webApp:jsBrowserDistribution
    
    # Create bucket if not exists
    gsutil mb -p $PROJECT_ID -l $REGION gs://$FRONTEND_BUCKET 2>/dev/null || true
    
    # Make bucket public
    gsutil iam ch allUsers:objectViewer gs://$FRONTEND_BUCKET
    
    # Configure as website
    gsutil web set -m index.html -e index.html gs://$FRONTEND_BUCKET
    
    # Upload files
    echo "Uploading files..."
    gsutil -m rsync -r -d webApp/build/dist/js/productionExecutable/ gs://$FRONTEND_BUCKET
    
    # Set cache control
    gsutil -m setmeta -h "Cache-Control:public, max-age=3600" gs://$FRONTEND_BUCKET/**
    
    echo -e "${GREEN}Frontend deployed at: https://storage.googleapis.com/$FRONTEND_BUCKET/index.html${NC}"
    echo -e "${YELLOW}For custom domain, set up Cloud CDN or Firebase Hosting${NC}"
}

setup_secrets() {
    echo -e "\n${YELLOW}Setting up Secret Manager...${NC}"
    
    # Enable Secret Manager API
    gcloud services enable secretmanager.googleapis.com
    
    # Create secrets (you'll need to provide values)
    echo "Creating database secrets..."
    echo -n "root" | gcloud secrets create db-username --data-file=- 2>/dev/null || \
        echo -n "root" | gcloud secrets versions add db-username --data-file=-
    
    echo -e "${YELLOW}Please set the database password secret manually:${NC}"
    echo "gcloud secrets create db-password --data-file=-"
    echo "Then type your password and press Ctrl+D"
    
    echo -e "\n${YELLOW}Setting up AI API keys...${NC}"
    echo "Create the following secrets for AI features:"
    echo "  gcloud secrets create gemini-api-key --data-file=-"
    echo "  gcloud secrets create openai-api-key --data-file=-"
    echo "Then type your API key and press Ctrl+D"
}

setup_cloud_sql() {
    echo -e "\n${YELLOW}Cloud SQL Configuration...${NC}"
    
    # Your Cloud SQL is already set up, just need to configure VPC connector for Cloud Run
    echo "Creating VPC Connector for Cloud Run -> Cloud SQL..."
    
    gcloud compute networks vpc-access connectors create vwatek-connector \
        --region $REGION \
        --network default \
        --range 10.8.0.0/28 \
        2>/dev/null || echo "VPC Connector may already exist"
    
    echo -e "${GREEN}Cloud SQL is ready at: 34.134.196.247:3306${NC}"
}

case "$1" in
    backend)
        deploy_backend
        ;;
    frontend)
        deploy_frontend
        ;;
    secrets)
        setup_secrets
        ;;
    cloudsql)
        setup_cloud_sql
        ;;
    all)
        setup_secrets
        setup_cloud_sql
        deploy_backend
        deploy_frontend
        ;;
    *)
        echo "Usage: $0 {backend|frontend|secrets|cloudsql|all}"
        echo ""
        echo "Commands:"
        echo "  backend   - Deploy Ktor backend to Cloud Run"
        echo "  frontend  - Deploy web app to Cloud Storage"
        echo "  secrets   - Set up Secret Manager"
        echo "  cloudsql  - Configure Cloud SQL VPC access"
        echo "  all       - Deploy everything"
        exit 1
        ;;
esac

echo -e "\n${GREEN}Deployment complete!${NC}"
