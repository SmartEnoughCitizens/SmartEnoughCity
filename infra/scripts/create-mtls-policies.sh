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

# Create PERMISSIVE mTLS for dev namespace
echo ""
echo -e "${BLUE}Creating default-permissive PeerAuthentication...${NC}"
kubectl apply -f - <<EOF
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default-permissive
  namespace: dev
spec:
  mtls:
    mode: PERMISSIVE
EOF

echo ""
echo -e "${BLUE}Creating postgresql-disable PeerAuthentication...${NC}"
kubectl apply -f - <<EOF
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: postgresql-disable
  namespace: dev
spec:
  selector:
    matchLabels:
      app.kubernetes.io/name: postgresql
  mtls:
    mode: DISABLE
EOF

echo ""
echo -e "${GREEN}=== mTLS policies created successfully! ===${NC}"
echo ""
echo "Verify with: kubectl get peerauthentications -n dev"
