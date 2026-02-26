#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Infrastructure Health Check ===${NC}"
echo ""

# Function to check and report status
check_status() {
  local name=$1
  local command=$2

  echo -n "Checking $name... "
  if eval "$command" > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC}"
    return 0
  else
    echo -e "${RED}✗${NC}"
    return 1
  fi
}

FAILED_CHECKS=0

# 1. Cluster connectivity
echo -e "${BLUE}1. Cluster Connectivity${NC}"
if ! kubectl cluster-info > /dev/null 2>&1; then
  echo -e "${RED}Cannot connect to cluster${NC}"
  exit 1
fi
echo -e "${GREEN}✓ Connected to cluster${NC}"
echo ""

# 2. Namespaces
echo -e "${BLUE}2. Namespaces${NC}"
check_status "dev namespace" "kubectl get namespace dev" || ((FAILED_CHECKS++))
check_status "ingress namespace" "kubectl get namespace ingress" || ((FAILED_CHECKS++))
check_status "cert-manager namespace" "kubectl get namespace cert-manager" || ((FAILED_CHECKS++))
check_status "istio-system namespace" "kubectl get namespace istio-system" || ((FAILED_CHECKS++))
echo ""

# 3. Cert-manager
echo -e "${BLUE}3. Cert-manager${NC}"
check_status "cert-manager pods" "kubectl get pods -n cert-manager -l app.kubernetes.io/name=cert-manager --field-selector=status.phase=Running | grep -q Running" || ((FAILED_CHECKS++))
check_status "cert-manager webhook" "kubectl get pods -n cert-manager -l app.kubernetes.io/name=webhook --field-selector=status.phase=Running | grep -q Running" || ((FAILED_CHECKS++))
check_status "letsencrypt-staging issuer" "kubectl get clusterissuer letsencrypt-staging" || ((FAILED_CHECKS++))
check_status "letsencrypt-prod issuer" "kubectl get clusterissuer letsencrypt-prod" || ((FAILED_CHECKS++))
echo ""

# 4. Ingress-nginx
echo -e "${BLUE}4. Ingress-nginx${NC}"
check_status "ingress-nginx controller" "kubectl get pods -n ingress -l app.kubernetes.io/name=ingress-nginx --field-selector=status.phase=Running | grep -q Running" || ((FAILED_CHECKS++))
check_status "LoadBalancer service" "kubectl get svc -n ingress ingress-nginx-controller -o jsonpath='{.status.loadBalancer.ingress[0].ip}' | grep -E '^[0-9]'" || ((FAILED_CHECKS++))
echo ""

# 5. Istio
echo -e "${BLUE}5. Istio Service Mesh${NC}"
check_status "istiod control plane" "kubectl get pods -n istio-system -l app=istiod --field-selector=status.phase=Running | grep -q Running" || ((FAILED_CHECKS++))
check_status "istio-injection on dev" "kubectl get namespace dev -o jsonpath='{.metadata.labels.istio-injection}' | grep -q enabled" || ((FAILED_CHECKS++))
check_status "istio-injection on ingress" "kubectl get namespace ingress -o jsonpath='{.metadata.labels.istio-injection}' | grep -q enabled" || ((FAILED_CHECKS++))
echo ""

# 6. mTLS Configuration
echo -e "${BLUE}6. mTLS Configuration${NC}"
check_status "PERMISSIVE mTLS policy" "kubectl get peerauthentication -n dev default-permissive" || ((FAILED_CHECKS++))
check_status "PostgreSQL mTLS disabled" "kubectl get peerauthentication -n dev postgresql-disable" || ((FAILED_CHECKS++))
echo ""

# 7. NetworkPolicies
echo -e "${BLUE}7. NetworkPolicies${NC}"
check_status "default-deny-all" "kubectl get networkpolicy -n dev default-deny-all" || ((FAILED_CHECKS++))
check_status "allow-dns" "kubectl get networkpolicy -n dev allow-dns" || ((FAILED_CHECKS++))
check_status "allow-https-egress" "kubectl get networkpolicy -n dev allow-https-egress" || ((FAILED_CHECKS++))
check_status "allow-istio-system" "kubectl get networkpolicy -n dev allow-istio-system" || ((FAILED_CHECKS++))
check_status "allow-from-ingress" "kubectl get networkpolicy -n dev allow-from-ingress" || ((FAILED_CHECKS++))
check_status "allow-internal-dev" "kubectl get networkpolicy -n dev allow-internal-dev" || ((FAILED_CHECKS++))
echo ""

# 8. Node Status
echo -e "${BLUE}8. Node Status${NC}"
NODE_COUNT=$(kubectl get nodes --no-headers | wc -l)
READY_NODES=$(kubectl get nodes --no-headers | grep -c " Ready " || true)
echo "Total nodes: $NODE_COUNT"
echo "Ready nodes: $READY_NODES"
if [ "$NODE_COUNT" -eq "$READY_NODES" ] && [ "$NODE_COUNT" -gt 0 ]; then
  echo -e "${GREEN}✓ All nodes ready${NC}"
else
  echo -e "${RED}✗ Some nodes not ready${NC}"
  ((FAILED_CHECKS++))
fi
echo ""

# Summary
echo -e "${BLUE}=== Summary ===${NC}"
if [ $FAILED_CHECKS -eq 0 ]; then
  echo -e "${GREEN}All checks passed! Infrastructure is healthy.${NC}"
  exit 0
else
  echo -e "${YELLOW}$FAILED_CHECKS checks failed. Please review the output above.${NC}"
  exit 1
fi
