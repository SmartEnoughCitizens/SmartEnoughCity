#!/bin/bash
# Apply all security policies: NetworkPolicies, AuthorizationPolicies, and STRICT mTLS

set -euo pipefail

echo "======================================"
echo "Applying Security Hardening"
echo "======================================"

# 1. Apply NetworkPolicies
echo ""
echo "1/3 Applying NetworkPolicies..."
kubectl apply -f network-policies/

# 2. Apply AuthorizationPolicies
echo ""
echo "2/3 Applying Istio AuthorizationPolicies..."
kubectl apply -f istio/authorization-policies-strict.yaml

# 3. Enable STRICT mTLS
echo ""
echo "3/3 Enabling STRICT mTLS..."
# First delete any existing PERMISSIVE policies
kubectl delete peerauthentication default-permissive -n dev 2>/dev/null || true
kubectl delete peerauthentication frontend-permissive -n dev 2>/dev/null || true
# Apply STRICT
kubectl apply -f istio/strict-mtls.yaml

echo ""
echo "======================================"
echo "Security Hardening Complete!"
echo "======================================"
echo ""
echo "Verifying..."
kubectl get networkpolicy -n dev
echo ""
kubectl get authorizationpolicy -n dev
echo ""
kubectl get peerauthentication -n dev

echo ""
echo "Test your website: https://dev.citycontrol.me/"
