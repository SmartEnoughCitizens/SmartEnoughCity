# Istio Security Policies

This folder contains Istio security configurations for the SmartEnoughCity platform.

## Files

### mTLS Policies

**`permissive-mtls.yaml`** - PERMISSIVE mode (default)
- Used by `create-mtls-policies.sh` during deployment
- Allows both encrypted and plain traffic
- Safe for development and initial setup
- **This is what you're currently using**

**`strict-mtls.yaml`** - STRICT mode (production hardening)
- Enforces encrypted mTLS for all traffic
- Use after validating PERMISSIVE mode works
- Apply with: `kubectl apply -f istio/strict-mtls.yaml`

**`enable-strict-mtls.yaml`** - Alternative STRICT mode config
- Similar to strict-mtls.yaml
- Includes PostgreSQL exclusion

### Authorization Policies

**`authorization-policies-working.yaml`** - Basic authorization
- Simple rules that work well
- Allows ingress → services
- Allows internal service communication

**`authorization-policies-strict.yaml`** - Strict authorization
- More granular rules
- Defines exact service-to-service permissions
- Use for production security hardening

## Usage

### During Deployment (Phase 4)

```bash
# Automatically applies permissive-mtls.yaml
./scripts/create-mtls-policies.sh
```

### To Switch to STRICT Mode

```bash
# Option 1: Apply directly
kubectl apply -f infra/istio/strict-mtls.yaml

# Option 2: Use helper script (if available)
./scripts/apply-security-fixed.sh
```

### To Add Authorization Policies

```bash
# Apply basic auth policies
kubectl apply -f infra/istio/authorization-policies-working.yaml

# Or apply strict policies
kubectl apply -f infra/istio/authorization-policies-strict.yaml
```

## Verify Applied Policies

```bash
# Check PeerAuthentication (mTLS mode)
kubectl get peerauthentication -n dev

# Check AuthorizationPolicies
kubectl get authorizationpolicy -n dev

# Detailed view
kubectl describe peerauthentication -n dev
```

## Switching Between Modes

### PERMISSIVE → STRICT

```bash
# Delete PERMISSIVE
kubectl delete -f infra/istio/permissive-mtls.yaml

# Apply STRICT
kubectl apply -f infra/istio/strict-mtls.yaml

# Verify
kubectl get peerauthentication -n dev
```

### STRICT → PERMISSIVE (rollback)

```bash
# Delete STRICT
kubectl delete -f infra/istio/strict-mtls.yaml

# Apply PERMISSIVE
kubectl apply -f infra/istio/permissive-mtls.yaml
```

## Policy Levels

**Level 1 (Current):** PERMISSIVE mTLS
- ✅ mTLS available but not enforced
- ✅ Works with all services
- ✅ Good for development

**Level 2:** PERMISSIVE mTLS + Basic Authorization
- ✅ mTLS available
- ✅ Basic access control
- ⚠️ Still allows some unencrypted traffic

**Level 3:** STRICT mTLS + Strict Authorization (Production)
- ✅ All traffic encrypted
- ✅ Fine-grained access control
- ✅ Production-ready security
- ⚠️ Requires all services to have Istio sidecars

## Troubleshooting

If services fail after applying policies:

```bash
# Check pod status
kubectl get pods -n dev

# Check Istio sidecar injection
kubectl get pod <pod-name> -n dev -o jsonpath='{.spec.containers[*].name}'
# Should show: <app-name> istio-proxy

# Check Istio proxy logs
kubectl logs <pod-name> -n dev -c istio-proxy

# Rollback to PERMISSIVE
kubectl apply -f infra/istio/permissive-mtls.yaml
```
