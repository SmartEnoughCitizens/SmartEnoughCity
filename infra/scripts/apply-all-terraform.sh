#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Applying All Terraform Configurations ===${NC}"
echo ""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$SCRIPT_DIR/../terraform"

# Array of Terraform directories in dependency order
TERRAFORM_MODULES=(
  "1-gke-cluster"
  "2-kubernetes-base"
  "3-cert-manager"
  "5-istio"
  "4-ingress-nginx"
  "6-network-policies"
)

# Function to apply Terraform in a directory
apply_terraform() {
  local dir=$1
  local module_path="$TERRAFORM_DIR/$dir"

  if [ ! -d "$module_path" ]; then
    echo -e "${YELLOW}Warning: Directory $module_path does not exist, skipping...${NC}"
    return
  fi

  echo -e "${BLUE}Applying: $dir${NC}"
  cd "$module_path"

  # Initialize Terraform
  echo "  - Running terraform init..."
  terraform init -upgrade > /dev/null

  # Plan
  echo "  - Running terraform plan..."
  terraform plan -out=tfplan > /dev/null

  # Apply
  echo "  - Running terraform apply..."
  terraform apply tfplan
  rm -f tfplan

  echo -e "${GREEN}  ✓ $dir complete${NC}"
  echo ""
}

# Apply each module in order
for module in "${TERRAFORM_MODULES[@]}"; do
  apply_terraform "$module"

  # Special wait times for certain modules
  case "$module" in
    "1-gke-cluster")
      echo -e "${BLUE}Configuring kubectl for new cluster...${NC}"
      # Get cluster credentials
      PROJECT_ID=$(cd "$TERRAFORM_DIR/1-gke-cluster" && terraform output -raw project_id 2>/dev/null || echo "smart-enough-city")
      CLUSTER_NAME=$(cd "$TERRAFORM_DIR/1-gke-cluster" && terraform output -raw cluster_name 2>/dev/null || echo "smart-enough-city")
      CLUSTER_LOCATION=$(cd "$TERRAFORM_DIR/1-gke-cluster" && terraform output -raw cluster_location 2>/dev/null || echo "europe-west1-b")

      gcloud container clusters get-credentials "$CLUSTER_NAME" \
        --zone "$CLUSTER_LOCATION" \
        --project "$PROJECT_ID"

      echo -e "${BLUE}Waiting 30s for cluster to stabilize...${NC}"
      sleep 30
      ;;
    "3-cert-manager")
      echo -e "${BLUE}Waiting 15s for cert-manager webhooks...${NC}"
      sleep 15
      ;;
    "5-istio")
      echo -e "${BLUE}Waiting 30s for Istio control plane...${NC}"
      sleep 30

      echo -e "${YELLOW}Note: mTLS PeerAuthentication policies will be created after all modules are deployed${NC}"
      ;;
  esac
done

echo -e "${GREEN}=== All Terraform modules applied successfully! ===${NC}"
echo ""
echo -e "${YELLOW}Note: mTLS PeerAuthentication policies should be created separately${NC}"
echo -e "${BLUE}Run: ./scripts/create-mtls-policies.sh (wait 5-10 minutes after cluster-up completes)${NC}"
echo ""
