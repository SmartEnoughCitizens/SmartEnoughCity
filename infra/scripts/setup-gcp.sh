#!/bin/bash
set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}=== SmartEnoughCity GCP Setup ===${NC}"
echo ""

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo -e "${RED}❌ gcloud CLI not found.${NC}"
    echo "Install it first: https://cloud.google.com/sdk/docs/install"
    exit 1
fi

# Get project ID
read -p "Enter your GCP project ID: " PROJECT_ID
if [ -z "$PROJECT_ID" ]; then
    echo -e "${RED}❌ Project ID is required${NC}"
    exit 1
fi

echo ""
echo -e "${BLUE}Project ID: $PROJECT_ID${NC}"
echo ""

# Check if project exists
echo "Checking if project exists..."
if ! gcloud projects describe "$PROJECT_ID" &> /dev/null; then
    echo -e "${YELLOW}⚠️  Project '$PROJECT_ID' does not exist${NC}"
    read -p "Create project '$PROJECT_ID'? (yes/no): " CREATE_PROJECT

    if [[ $CREATE_PROJECT =~ ^[Yy][Ee][Ss]$ ]]; then
        echo "Creating project..."
        gcloud projects create "$PROJECT_ID" --name="SmartEnoughCity"
        echo -e "${GREEN}✅ Project created${NC}"
    else
        echo -e "${RED}❌ Project required. Exiting.${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✅ Project exists${NC}"
fi

# Set default project
echo ""
echo "Setting default project..."
gcloud config set project "$PROJECT_ID"

# Check billing
echo ""
echo "Checking billing..."

# First, enable billing API if not already enabled
gcloud services enable cloudbilling.googleapis.com --project="$PROJECT_ID" 2>/dev/null || true

# Try to get billing info (with timeout)
BILLING_CHECK=$(timeout 10s gcloud projects get-iam-policy "$PROJECT_ID" 2>/dev/null || echo "skip")

if [ "$BILLING_CHECK" = "skip" ] || [ -z "$BILLING_CHECK" ]; then
    echo -e "${YELLOW}⚠️  Could not verify billing status automatically${NC}"
    echo ""
    read -p "Do you need to link a billing account? (yes/no): " NEED_BILLING

    if [[ $NEED_BILLING =~ ^[Yy][Ee][Ss]$ ]]; then
        echo ""
        echo "Available billing accounts:"
        gcloud billing accounts list
        echo ""
        read -p "Enter billing account ID (format: 0X0X0X-0X0X0X-0X0X0X): " BILLING_ACCOUNT

        if [ -n "$BILLING_ACCOUNT" ]; then
            echo "Linking billing account..."
            gcloud billing projects link "$PROJECT_ID" --billing-account="$BILLING_ACCOUNT"
            echo -e "${GREEN}✅ Billing linked${NC}"
        else
            echo -e "${RED}❌ Billing account ID required. Exiting.${NC}"
            exit 1
        fi
    else
        echo -e "${GREEN}✅ Skipping billing (assuming already configured)${NC}"
    fi
else
    echo -e "${GREEN}✅ Project accessible (billing likely configured)${NC}"
fi

# Enable APIs
echo ""
echo -e "${BLUE}Enabling required APIs...${NC}"
echo "This takes 2-3 minutes, please wait..."

APIS=(
  "container.googleapis.com"
  "compute.googleapis.com"
  "servicenetworking.googleapis.com"
  "cloudresourcemanager.googleapis.com"
  "iam.googleapis.com"
  "logging.googleapis.com"
  "monitoring.googleapis.com"
  "storage-api.googleapis.com"
  "storage-component.googleapis.com"
)

for api in "${APIS[@]}"; do
  echo -n "  Enabling $api... "
  if gcloud services enable "$api" 2>&1 | grep -q "already enabled"; then
    echo -e "${GREEN}already enabled${NC}"
  else
    echo -e "${GREEN}done${NC}"
  fi
done

echo ""
echo -e "${GREEN}✅ All APIs enabled${NC}"

# Create Terraform state bucket
echo ""
BUCKET_NAME="${PROJECT_ID}-terraform-state"
echo "Checking Terraform state bucket..."

if ! gsutil ls -b "gs://${BUCKET_NAME}" &> /dev/null; then
    echo "Creating Terraform state bucket: gs://${BUCKET_NAME}"
    gsutil mb -p "$PROJECT_ID" -l europe-west1 "gs://${BUCKET_NAME}"
    gsutil versioning set on "gs://${BUCKET_NAME}"
    echo -e "${GREEN}✅ Bucket created${NC}"
else
    echo -e "${GREEN}✅ Bucket already exists${NC}"
fi

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$(dirname "$SCRIPT_DIR")"

# Update Terraform variables for GKE cluster
echo ""
echo "Updating Terraform configuration..."

TFVARS_FILE="$INFRA_DIR/terraform/1-gke-cluster/terraform.tfvars"
cat > "$TFVARS_FILE" <<EOF
# GCP Project Configuration
project_id = "$PROJECT_ID"

# Cluster Configuration
cluster_name = "smart-enough-city"
region       = "europe-west1"
zone         = "europe-west1-b"

# Node Configuration
machine_type = "e2-standard-4"  # 4 cores, 16GB RAM

# Environment
environment = "dev"

# Cost Optimization
use_preemptible = false
EOF

echo -e "${GREEN}✅ Updated $TFVARS_FILE${NC}"

# Prompt for cert-manager email
echo ""
read -p "Enter email for Let's Encrypt certificates: " CERT_EMAIL
if [ -n "$CERT_EMAIL" ]; then
    CERT_TFVARS="$INFRA_DIR/terraform/3-cert-manager/terraform.tfvars"
    cat > "$CERT_TFVARS" <<EOF
# Let's Encrypt Configuration
letsencrypt_email = "$CERT_EMAIL"

# Cert-manager Configuration
namespace            = "cert-manager"
cert_manager_version = "v1.14.4"
EOF
    echo -e "${GREEN}✅ Updated $CERT_TFVARS${NC}"
fi

# Optional: Configure backend
echo ""
read -p "Configure Terraform remote state in GCS bucket? (recommended for teams) (yes/no): " CONFIG_BACKEND

if [[ $CONFIG_BACKEND =~ ^[Yy][Ee][Ss]$ ]]; then
    # Create backend config for each module
    MODULES=("1-gke-cluster" "2-kubernetes-base" "3-cert-manager" "4-ingress-nginx" "5-istio" "6-network-policies")

    for module in "${MODULES[@]}"; do
        BACKEND_FILE="$INFRA_DIR/terraform/$module/backend.tf"
        cat > "$BACKEND_FILE" <<EOF
terraform {
  backend "gcs" {
    bucket = "$BUCKET_NAME"
    prefix = "smartenoughcity/$module"
  }
}
EOF
        echo -e "${GREEN}✅ Created backend config for $module${NC}"
    done
fi

# Authenticate if needed
echo ""
echo "Checking authentication..."
if ! gcloud auth application-default print-access-token &> /dev/null; then
    echo -e "${YELLOW}⚠️  Application default credentials not found${NC}"
    read -p "Authenticate now? (required for Terraform) (yes/no): " DO_AUTH

    if [[ $DO_AUTH =~ ^[Yy][Ee][Ss]$ ]]; then
        gcloud auth application-default login
        echo -e "${GREEN}✅ Authenticated${NC}"
    else
        echo -e "${YELLOW}⚠️  You'll need to run: gcloud auth application-default login${NC}"
    fi
else
    echo -e "${GREEN}✅ Already authenticated${NC}"
fi

# Summary
echo ""
echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}✅ GCP Setup Complete!${NC}"
echo -e "${GREEN}=========================================${NC}"
echo ""
echo -e "${BLUE}Configuration:${NC}"
echo "  Project ID: $PROJECT_ID"
echo "  Region: europe-west1"
echo "  Zone: europe-west1-b"
echo "  Terraform state: gs://${BUCKET_NAME}"
echo "  Email: ${CERT_EMAIL:-'(not set)'}"
echo ""
echo -e "${BLUE}APIs Enabled:${NC}"
for api in "${APIS[@]}"; do
  echo "  ✓ $api"
done
echo ""
echo -e "${BLUE}Next Steps:${NC}"
echo "  1. Review Terraform variables:"
echo "     ${YELLOW}cat $TFVARS_FILE${NC}"
echo ""
echo "  2. Deploy infrastructure (~10-15 minutes):"
echo "     ${YELLOW}cd $INFRA_DIR${NC}"
echo "     ${YELLOW}./scripts/cluster-up.sh${NC}"
echo ""
echo "  3. Deploy your application:"
echo "     ${YELLOW}cd helm-charts${NC}"
echo "     ${YELLOW}helm upgrade --install smartenoughcity . -n dev --create-namespace --wait${NC}"
echo ""
echo -e "${BLUE}Cost Estimate:${NC}"
echo "  • Development (1-3 nodes): ~$50-150/month"
echo "  • Use ./scripts/cluster-down.sh when not in use to save 60-70%"
echo ""
