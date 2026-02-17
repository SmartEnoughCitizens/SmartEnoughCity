#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== SmartEnoughCity Cluster Setup ===${NC}"
echo ""

# Check prerequisites
command -v terraform >/dev/null 2>&1 || { echo -e "${RED}terraform is required but not installed${NC}"; exit 1; }
command -v kubectl >/dev/null 2>&1 || { echo -e "${RED}kubectl is required but not installed${NC}"; exit 1; }
command -v helm >/dev/null 2>&1 || { echo -e "${RED}helm is required but not installed${NC}"; exit 1; }
command -v gcloud >/dev/null 2>&1 || { echo -e "${RED}gcloud is required but not installed${NC}"; exit 1; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$SCRIPT_DIR/../terraform"

# Step 1: Apply all Terraform configurations
echo -e "${BLUE}Step 1: Applying Terraform configurations${NC}"
"$SCRIPT_DIR/apply-all-terraform.sh"

# Step 2: Wait for cluster to be ready
echo -e "${BLUE}Step 2: Waiting for cluster to be ready${NC}"
PROJECT_ID=$(cd "$TERRAFORM_DIR/1-gke-cluster" && terraform output -raw project_id 2>/dev/null || echo "")
CLUSTER_NAME=$(cd "$TERRAFORM_DIR/1-gke-cluster" && terraform output -raw cluster_name 2>/dev/null || echo "smart-enough-city")
CLUSTER_LOCATION=$(cd "$TERRAFORM_DIR/1-gke-cluster" && terraform output -raw cluster_location 2>/dev/null || echo "europe-west1-b")

if [ -z "$PROJECT_ID" ]; then
  echo -e "${YELLOW}Warning: Could not determine project ID from Terraform output${NC}"
  echo -e "${YELLOW}Please set PROJECT_ID manually if needed${NC}"
else
  echo -e "${GREEN}Configuring kubectl for cluster: $CLUSTER_NAME${NC}"
  gcloud container clusters get-credentials "$CLUSTER_NAME" \
    --zone "$CLUSTER_LOCATION" \
    --project "$PROJECT_ID"
fi

# Wait for nodes to be ready
echo -e "${BLUE}Waiting for nodes to be ready...${NC}"
kubectl wait --for=condition=Ready nodes --all --timeout=300s

# Step 3: Verify all components
echo -e "${BLUE}Step 3: Verifying infrastructure${NC}"
"$SCRIPT_DIR/verify-infrastructure.sh"

echo ""
echo -e "${GREEN}=== Cluster is ready! ===${NC}"
#echo ""
#echo -e "${BLUE}Next steps:${NC}"
#echo "  1. Deploy your applications using Helm charts"
#echo "  2. Configure DNS to point to LoadBalancer IP"
#echo "  3. Apply ingress resources for HTTPS"
#echo ""
#echo "Get LoadBalancer IP:"
#echo "  kubectl get svc -n ingress ingress-nginx-controller -o jsonpath='{.status.loadBalancer.ingress[0].ip}'"
#echo ""
