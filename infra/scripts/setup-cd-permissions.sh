#!/bin/bash
set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}=== Setup CD Permissions for GCP ===${NC}"
echo ""

# Configuration
read -p "Enter your GCP Project ID [smart-ecity-dev]: " PROJECT_ID
PROJECT_ID=${PROJECT_ID:-smart-ecity-dev}

read -p "Enter your GitHub repository (owner/repo) [smartenoughcitizens/smartenoughcity]: " GITHUB_REPO
GITHUB_REPO=${GITHUB_REPO:-smartenoughcitizens/smartenoughcity}

SERVICE_ACCOUNT="github-deployer"

echo ""
echo -e "${BLUE}Configuration:${NC}"
echo "  Project: $PROJECT_ID"
echo "  GitHub Repo: $GITHUB_REPO"
echo "  Service Account: $SERVICE_ACCOUNT"
echo ""

# Enable APIs
echo -e "${BLUE}Step 1: Enabling required APIs...${NC}"
gcloud services enable \
  iamcredentials.googleapis.com \
  cloudresourcemanager.googleapis.com \
  sts.googleapis.com \
  --project="$PROJECT_ID"
echo -e "${GREEN}✅ APIs enabled${NC}"

# Create Workload Identity Pool
echo ""
echo -e "${BLUE}Step 2: Creating Workload Identity Pool...${NC}"
if gcloud iam workload-identity-pools describe github-pool \
  --location="global" \
  --project="$PROJECT_ID" &>/dev/null; then
  echo -e "${YELLOW}Pool 'github-pool' already exists${NC}"
else
  gcloud iam workload-identity-pools create github-pool \
    --location="global" \
    --project="$PROJECT_ID" \
    --display-name="GitHub Actions Pool"
  echo -e "${GREEN}✅ Pool created${NC}"
fi

# Create Provider
echo ""
echo -e "${BLUE}Step 3: Creating OIDC Provider...${NC}"
if gcloud iam workload-identity-pools providers describe github-provider \
  --location="global" \
  --workload-identity-pool="github-pool" \
  --project="$PROJECT_ID" &>/dev/null; then
  echo -e "${YELLOW}Provider 'github-provider' already exists${NC}"
else
  gcloud iam workload-identity-pools providers create-oidc github-provider \
    --location="global" \
    --workload-identity-pool="github-pool" \
    --project="$PROJECT_ID" \
    --issuer-uri="https://token.actions.githubusercontent.com" \
    --attribute-mapping="google.subject=assertion.sub,attribute.actor=assertion.actor,attribute.repository=assertion.repository" \
    --attribute-condition="assertion.repository=='$GITHUB_REPO'"
  echo -e "${GREEN}✅ Provider created${NC}"
fi

# Create Service Account
echo ""
echo -e "${BLUE}Step 4: Creating Service Account...${NC}"
if gcloud iam service-accounts describe "${SERVICE_ACCOUNT}@${PROJECT_ID}.iam.gserviceaccount.com" \
  --project="$PROJECT_ID" &>/dev/null; then
  echo -e "${YELLOW}Service account already exists${NC}"
else
  gcloud iam service-accounts create "$SERVICE_ACCOUNT" \
    --project="$PROJECT_ID" \
    --display-name="GitHub Actions Deployer"
  echo -e "${GREEN}✅ Service account created${NC}"
fi

# Grant permissions
echo ""
echo -e "${BLUE}Step 5: Granting GKE permissions...${NC}"
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${SERVICE_ACCOUNT}@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/container.developer" \
  --condition=None
echo -e "${GREEN}✅ Permissions granted${NC}"

# Bind Workload Identity
echo ""
echo -e "${BLUE}Step 6: Binding Workload Identity...${NC}"
PROJECT_NUMBER=$(gcloud projects describe "$PROJECT_ID" --format="value(projectNumber)")

gcloud iam service-accounts add-iam-policy-binding \
  "${SERVICE_ACCOUNT}@${PROJECT_ID}.iam.gserviceaccount.com" \
  --project="$PROJECT_ID" \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/github-pool/attribute.repository/${GITHUB_REPO}"

echo -e "${GREEN}✅ Workload Identity bound${NC}"

# Get provider name
PROVIDER_NAME=$(gcloud iam workload-identity-pools providers describe github-provider \
  --location="global" \
  --workload-identity-pool="github-pool" \
  --project="$PROJECT_ID" \
  --format="value(name)")

# Summary
echo ""
echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}✅ CD Permissions Setup Complete!${NC}"
echo -e "${GREEN}=========================================${NC}"
echo ""
echo -e "${BLUE}Update .github/workflows/cd.yml with:${NC}"
echo ""
echo -e "${YELLOW}WORKLOAD_IDENTITY_PROVIDER:${NC} $PROVIDER_NAME"
echo -e "${YELLOW}GCP_SERVICE_ACCOUNT:${NC} ${SERVICE_ACCOUNT}@${PROJECT_ID}.iam.gserviceaccount.com"