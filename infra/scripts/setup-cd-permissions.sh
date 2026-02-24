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

# FIXED: Use correct case for GitHub org/repo
read -p "Enter your GitHub repository owner [SmartEnoughCitizens]: " GITHUB_OWNER
GITHUB_OWNER=${GITHUB_OWNER:-SmartEnoughCitizens}

read -p "Enter your GitHub repository name [SmartEnoughCity]: " GITHUB_REPO_NAME
GITHUB_REPO_NAME=${GITHUB_REPO_NAME:-SmartEnoughCity}

GITHUB_REPO="${GITHUB_OWNER}/${GITHUB_REPO_NAME}"
SERVICE_ACCOUNT="github-deployer"

echo ""
echo -e "${BLUE}Configuration:${NC}"
echo "  Project: $PROJECT_ID"
echo "  GitHub Owner: $GITHUB_OWNER"
echo "  GitHub Repo: $GITHUB_REPO"
echo "  Service Account: $SERVICE_ACCOUNT"
echo ""
echo -e "${YELLOW}️  Make sure the owner/repo match the exact case in GitHub!${NC}"
echo ""
read -p "Continue? (y/n): " CONFIRM
if [[ ! $CONFIRM =~ ^[Yy]$ ]]; then
  echo "Aborted."
  exit 1
fi

# Enable APIs
echo ""
echo -e "${BLUE}Step 1: Enabling required APIs...${NC}"
gcloud services enable \
  iamcredentials.googleapis.com \
  cloudresourcemanager.googleapis.com \
  sts.googleapis.com \
  --project="$PROJECT_ID"
echo -e "${GREEN} APIs enabled${NC}"

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
  echo -e "${GREEN} Pool created${NC}"
fi

# Get project number
PROJECT_NUMBER=$(gcloud projects describe "$PROJECT_ID" --format="value(projectNumber)")

# Create Provider
echo ""
echo -e "${BLUE}Step 3: Creating OIDC Provider...${NC}"
PROVIDER_EXISTS=false
if gcloud iam workload-identity-pools providers describe github-provider-v2 \
  --location="global" \
  --workload-identity-pool="github-pool" \
  --project="$PROJECT_ID" &>/dev/null; then

  PROVIDER_STATE=$(gcloud iam workload-identity-pools providers describe github-provider-v2 \
    --location="global" \
    --workload-identity-pool="github-pool" \
    --project="$PROJECT_ID" \
    --format="value(state)")

  if [[ "$PROVIDER_STATE" == "DELETED" ]]; then
    echo -e "${YELLOW}Provider 'github-provider-v2' exists but is DELETED. Using it anyway.${NC}"
  else
    echo -e "${YELLOW}Provider 'github-provider-v2' already exists${NC}"
    PROVIDER_EXISTS=true
  fi
fi

if [[ "$PROVIDER_EXISTS" == "false" ]]; then
  # FIXED: Added repository_owner to attribute mapping, use owner-based condition
  gcloud iam workload-identity-pools providers create-oidc github-provider-v2 \
    --location="global" \
    --workload-identity-pool="github-pool" \
    --project="$PROJECT_ID" \
    --issuer-uri="https://token.actions.githubusercontent.com" \
    --allowed-audiences="https://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/github-pool/providers/github-provider-v2" \
    --attribute-mapping="google.subject=assertion.sub,attribute.actor=assertion.actor,attribute.repository=assertion.repository,attribute.repository_owner=assertion.repository_owner" \
    --attribute-condition="assertion.repository_owner=='${GITHUB_OWNER}'"
  echo -e "${GREEN} Provider created${NC}"
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
  echo -e "${GREEN} Service account created${NC}"
fi

# Grant GKE permissions
echo ""
echo -e "${BLUE}Step 5: Granting GKE permissions...${NC}"
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${SERVICE_ACCOUNT}@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/container.developer" \
  --condition=None 2>/dev/null || echo -e "${YELLOW}Role may already be bound${NC}"
echo -e "${GREEN} GKE permissions granted${NC}"

# FIXED: Add Service Account Token Creator role
echo ""
echo -e "${BLUE}Step 6: Granting Token Creator permission...${NC}"
gcloud iam service-accounts add-iam-policy-binding \
  "${SERVICE_ACCOUNT}@${PROJECT_ID}.iam.gserviceaccount.com" \
  --project="$PROJECT_ID" \
  --role="roles/iam.serviceAccountTokenCreator" \
  --member="serviceAccount:${SERVICE_ACCOUNT}@${PROJECT_ID}.iam.gserviceaccount.com" 2>/dev/null || echo -e "${YELLOW}Role may already be bound${NC}"
echo -e "${GREEN} Token Creator permission granted${NC}"

# Bind Workload Identity
echo ""
echo -e "${BLUE}Step 7: Binding Workload Identity...${NC}"

# FIXED: Use exact case for repository in binding
gcloud iam service-accounts add-iam-policy-binding \
  "${SERVICE_ACCOUNT}@${PROJECT_ID}.iam.gserviceaccount.com" \
  --project="$PROJECT_ID" \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/github-pool/attribute.repository/${GITHUB_REPO}" 2>/dev/null || echo -e "${YELLOW}Binding may already exist${NC}"

echo -e "${GREEN} Workload Identity bound${NC}"

# Get provider name
PROVIDER_NAME="projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/github-pool/providers/github-provider-v2"

# Summary
echo ""
echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN} CD Permissions Setup Complete!${NC}"
echo -e "${GREEN}=========================================${NC}"
echo ""
echo -e "${BLUE}Add these GitHub Variables:${NC}"
echo -e "${YELLOW}(GitHub repo → Settings → Secrets and variables → Actions → Variables)${NC}"
echo ""
echo -e "${YELLOW}GCP_PROJECT_ID:${NC} $PROJECT_ID"
echo -e "${YELLOW}GKE_CLUSTER:${NC} smart-enough-city"
echo -e "${YELLOW}GKE_LOCATION:${NC} europe-west1-b"
echo -e "${YELLOW}WORKLOAD_IDENTITY_PROVIDER:${NC} $PROVIDER_NAME"
echo -e "${YELLOW}GCP_SERVICE_ACCOUNT:${NC} ${SERVICE_ACCOUNT}@${PROJECT_ID}.iam.gserviceaccount.com"
echo ""
echo -e "${GREEN}Done! Your GitHub Actions CD pipeline is ready to use.${NC}"
