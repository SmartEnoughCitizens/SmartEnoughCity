#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${YELLOW}=== SmartEnoughCity Cluster Teardown ===${NC}"
echo ""

# Check prerequisites
command -v terraform >/dev/null 2>&1 || { echo -e "${RED}terraform is required but not installed${NC}"; exit 1; }
command -v kubectl >/dev/null 2>&1 || { echo -e "${RED}kubectl is required but not installed${NC}"; exit 1; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TERRAFORM_DIR="$SCRIPT_DIR/../terraform"

# Get cluster info before deletion
PROJECT_ID=$(cd "$TERRAFORM_DIR/1-gke-cluster" && terraform output -raw project_id 2>/dev/null || echo "")
CLUSTER_NAME=$(cd "$TERRAFORM_DIR/1-gke-cluster" && terraform output -raw cluster_name 2>/dev/null || echo "smart-enough-city")
CLUSTER_LOCATION=$(cd "$TERRAFORM_DIR/1-gke-cluster" && terraform output -raw cluster_location 2>/dev/null || echo "europe-west1-b")

echo -e "${YELLOW}This will delete the GKE cluster: $CLUSTER_NAME${NC}"
echo -e "${YELLOW}Location: $CLUSTER_LOCATION${NC}"
echo -e "${YELLOW}Project: ${PROJECT_ID:-'(unknown)'}${NC}"
echo ""
echo -e "${RED}WARNING: This action will:${NC}"
echo "  - Delete the GKE cluster and all nodes"
echo "  - Delete all running workloads"
echo "  - Preserve Terraform state for future recreation"
echo ""
read -p "Are you sure you want to proceed? (yes/no): " -r
echo ""

if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
  echo -e "${GREEN}Aborted. Cluster preserved.${NC}"
  exit 0
fi

# Step 1: Delete the cluster via Terraform
echo -e "${BLUE}Deleting GKE cluster...${NC}"
cd "$TERRAFORM_DIR/1-gke-cluster"
terraform destroy -target=google_container_node_pool.primary_nodes -auto-approve
terraform destroy -target=google_container_cluster.primary -auto-approve

echo ""
echo -e "${GREEN}=== Cluster deleted successfully! ===${NC}"
echo ""
echo -e "${BLUE}Cost savings:${NC}"
echo "  - Nodes: $0 (no running nodes)"
echo "  - You're now saving ~60-70% compared to 24/7 uptime"
echo ""
echo -e "${BLUE}To recreate the cluster:${NC}"
echo "  ./cluster-up.sh"
echo ""
echo -e "${YELLOW}Note:${NC}"
echo "  - Terraform state is preserved"
echo "  - You can recreate the cluster anytime"
echo "  - Static IPs and DNS records remain unchanged"
echo ""
