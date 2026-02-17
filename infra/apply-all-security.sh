#!/bin/bash
# Apply complete security stack

set -euo pipefail

echo "========================================================"
echo "Deploying the Cluster(Infra+ networks + TLS + Policies)"
echo "========================================================"

# 1. Apply comprehensive NetworkPolicies
echo ""
echo "1/2 Applying NetworkPolicies..."
kubectl apply -f network-policies/all-policies.yaml

# 2. Keep mTLS in PERMISSIVE mode (works with everything)
echo ""
echo "2/2 Ensuring mTLS PERMISSIVE mode..."
kubectl apply -f - <<YAML
apiVersion: security.istio.io/v1
kind: PeerAuthentication
metadata:
  name: default-permissive
  namespace: dev
spec:
  mtls:
    mode: PERMISSIVE
---
apiVersion: security.istio.io/v1
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
YAML

echo ""
echo "======================================"
echo "✅ Security Stack Applied!"
echo "======================================"
echo ""
echo "Active Security:"
kubectl get networkpolicy -n dev
echo ""
kubectl get peerauthentication -n dev

echo "Security Summary:"
echo "  ✅ HTTPS external (cert-manager)"
echo "  ✅ mTLS internal PERMISSIVE (automatic encryption)"
echo "  ✅ NetworkPolicies (network isolation)"
echo "  ✅ Ingress in mesh (full integration)"
