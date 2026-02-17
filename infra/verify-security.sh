#!/bin/bash
# Comprehensive Security Verification Script

echo "======================================"
echo "Security Verification Report"
echo "======================================"
echo ""

echo "1️⃣  NetworkPolicies Status:"
echo "----------------------------"
kubectl get networkpolicy -n dev
echo ""

echo "2️⃣  Authorization Policies Status:"
echo "-----------------------------------"
kubectl get authorizationpolicy -n dev
echo ""

echo "3️⃣  mTLS Configuration:"
echo "----------------------"
kubectl get peerauthentication -n dev
echo ""

echo "4️⃣  Pod Sidecar Status (should be 2/2 except postgresql 1/1):"
echo "-------------------------------------------------------------"
kubectl get pods -n dev -o custom-columns='NAME:.metadata.name,READY:.status.containerStatuses[*].ready,CONTAINERS:.spec.containers[*].name' | head -10
echo ""

echo "5️⃣  Istio Proxy Status (all should be SYNCED):"
echo "----------------------------------------------"
istioctl proxy-status | grep dev
echo ""

echo "6️⃣  mTLS Verification for Frontend:"
echo "----------------------------------"
FRONTEND_POD=$(kubectl get pod -n dev -l app.kubernetes.io/name=frontend -o jsonpath='{.items[0].metadata.name}')
echo "Pod: $FRONTEND_POD"
istioctl x describe pod $FRONTEND_POD -n dev | grep -A 2 "PeerAuthentication"
echo ""

echo "7️⃣  Check Certificates:"
echo "----------------------"
kubectl exec -n dev $FRONTEND_POD -c istio-proxy -- pilot-agent request GET certs | grep -E "Cert Chain|URI"
echo ""

echo "8️⃣  External Access Test:"
echo "------------------------"
echo "Testing https://dev.citycontrol.me/"
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" https://dev.citycontrol.me/ || echo "❌ Failed to reach website"
echo ""

echo "======================================"
echo "Summary:"
echo "======================================"
echo "✅ If NetworkPolicies exist → Network security active"
echo "✅ If AuthorizationPolicies exist → Access control active"
echo "✅ If PeerAuthentication shows STRICT → mTLS enforced"
echo "✅ If pods show 2/2 → Sidecars injected"
echo "✅ If proxy-status shows SYNCED → Istio working"
echo "✅ If certs show → mTLS certificates issued"
echo "✅ If HTTP 200/302 → Website accessible"
echo ""
echo "🎉 All checks passed = Full security stack operational!"
