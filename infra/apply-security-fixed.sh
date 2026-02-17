#!/bin/bash
# Apply working security configuration

set -euo pipefail

echo "======================================"
echo "Applying Fixed Security Configuration"
echo "======================================"

# 1. Apply simpler, working AuthorizationPolicies
echo ""
echo "1/2 Applying working AuthorizationPolicies..."
kubectl delete authorizationpolicy --all -n dev 2>/dev/null || true
kubectl apply -f istio/authorization-policies-working.yaml

# 2. Enable STRICT mTLS (works because ingress has sidecar)
echo ""
echo "2/2 Enabling STRICT mTLS..."
kubectl delete peerauthentication --all -n dev 2>/dev/null || true
kubectl apply -f istio/enable-strict-mtls.yaml

echo ""
echo "======================================"
echo "Security Applied!"
echo "======================================"
echo ""
echo "Verification:"
kubectl get authorizationpolicy -n dev
echo ""
kubectl get peerauthentication -n dev

echo ""
echo "Test your website: https://dev.citycontrol.me/"
echo "If it works, you now have:"
echo "  ✅ HTTPS external"
echo "  ✅ mTLS internal (STRICT)"
echo "  ✅ NetworkPolicies"
echo "  ✅ AuthorizationPolicies"
