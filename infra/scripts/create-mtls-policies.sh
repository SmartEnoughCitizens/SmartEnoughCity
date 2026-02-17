#!/bin/bash
set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Creating mTLS PeerAuthentication Policies ===${NC}"
echo ""

# Wait for Istio to be fully ready
echo -e "${BLUE}Waiting for Istio to be fully stabilized (60s)...${NC}"
sleep 60

# Clear kubectl cache
echo "Clearing kubectl discovery cache..."
rm -rf ~/.kube/cache/discovery/ ~/.kube/http-cache/ 2>/dev/null || true

# Verify API is available
echo "Verifying PeerAuthentication API availability..."
for i in {1..30}; do
  if kubectl api-resources --api-group=security.istio.io 2>/dev/null | grep -q PeerAuthentication; then
    echo -e "${GREEN}API confirmed available${NC}"
    sleep 10  # Extra buffer
    break
  fi
  echo "Waiting for API... ($i/30)"
  sleep 3
done

# Apply PERMISSIVE mTLS policies from file
echo ""
echo -e "${BLUE}Applying mTLS policies from istio/permissive-mtls.yaml...${NC}"

# Get script directory to find the YAML file
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ISTIO_DIR="$(dirname "$SCRIPT_DIR")/istio"

if [ ! -f "$ISTIO_DIR/permissive-mtls.yaml" ]; then
  echo -e "${YELLOW}⚠️  Warning: $ISTIO_DIR/permissive-mtls.yaml not found${NC}"
  echo "Expected location: $ISTIO_DIR/permissive-mtls.yaml"
  exit 1
fi

kubectl apply -f "$ISTIO_DIR/permissive-mtls.yaml"

echo ""
echo -e "${GREEN}=== mTLS policies created successfully! ===${NC}"
echo ""
echo "Applied: PERMISSIVE mTLS mode (from istio/permissive-mtls.yaml)"
echo ""
echo "Verify: kubectl get peerauthentications -n dev"
echo ""
echo -e "${BLUE}To switch to STRICT mode:${NC}"
echo "  kubectl apply -f $ISTIO_DIR/strict-mtls.yaml"
