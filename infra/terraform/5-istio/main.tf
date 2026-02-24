
provider "helm" {
  kubernetes {
    config_path = "~/.kube/config"
  }
}

provider "kubernetes" {
  config_path = "~/.kube/config"
}

# Install Istio Base (CRDs and cluster-wide resources)
resource "helm_release" "istio_base" {
  name       = "istio-base"
  repository = "https://istio-release.storage.googleapis.com/charts"
  chart      = "base"
  version    = var.istio_version
  namespace  = var.namespace

  wait    = true
  timeout = 300
}

# Install Istiod (Control Plane)
resource "helm_release" "istiod" {
  name       = "istiod"
  repository = "https://istio-release.storage.googleapis.com/charts"
  chart      = "istiod"
  version    = var.istio_version
  namespace  = var.namespace

  # Optimized sidecar resource limits
  set {
    name  = "global.proxy.resources.requests.cpu"
    value = "10m"
  }

  set {
    name  = "global.proxy.resources.requests.memory"
    value = "64Mi"
  }

  set {
    name  = "global.proxy.resources.limits.cpu"
    value = "100m"
  }

  set {
    name  = "global.proxy.resources.limits.memory"
    value = "128Mi"
  }

  # Enable CNI for sidecar injection
  set {
    name  = "istio_cni.enabled"
    value = "false"
  }

  # Control plane resources
  set {
    name  = "pilot.resources.requests.cpu"
    value = "100m"
  }

  set {
    name  = "pilot.resources.requests.memory"
    value = "128Mi"
  }

  # Wait for istiod to be ready
  wait    = true
  timeout = 300

  depends_on = [helm_release.istio_base]
}

# Wait for Istio CRDs to be ready
resource "null_resource" "wait_for_istio_crds" {
  triggers = {
    istiod_version = helm_release.istiod.version
    always_run     = timestamp()
  }

  provisioner "local-exec" {
    command = <<-EOT
      echo "Waiting for Istio CRDs to be installed..."

      # Wait up to 5 minutes for the CRD to appear
      for i in {1..60}; do
        if kubectl get crd peerauthentications.security.istio.io >/dev/null 2>&1; then
          echo "PeerAuthentication CRD found!"
          break
        fi
        echo "Waiting for PeerAuthentication CRD... (attempt $i/60)"
        sleep 5
      done

      # Now wait for it to be established
      echo "Waiting for CRD to be established..."
      kubectl wait --for=condition=established --timeout=120s crd/peerauthentications.security.istio.io

      # Clear kubectl discovery cache to force API refresh
      echo "Clearing kubectl discovery cache..."
      rm -rf ~/.kube/cache/discovery/ ~/.kube/http-cache/ 2>/dev/null || true

      # Wait for API server to recognize the new resource type
      echo "Waiting for API server to recognize PeerAuthentication resource..."
      for i in {1..30}; do
        if kubectl api-resources --api-group=security.istio.io 2>/dev/null | grep -q PeerAuthentication; then
          echo "PeerAuthentication API resource is now available!"
          break
        fi
        echo "Waiting for API server to refresh... (attempt $i/30)"
        sleep 2
      done

      # Final stabilization wait
      echo "Waiting for Istio controller to be fully ready..."
      sleep 20
    EOT
  }

  depends_on = [helm_release.istiod]
}

# Enable Istio injection on dev namespace
resource "kubernetes_labels" "dev_istio_injection" {
  api_version = "v1"
  kind        = "Namespace"
  metadata {
    name = "dev"
  }

  labels = {
    "istio-injection" = "enabled"
  }

  depends_on = [null_resource.wait_for_istio_crds]
}

# Note: PeerAuthentication policies removed from Terraform due to kubectl cache issues
# These can be applied manually after infrastructure is up:
#
# kubectl apply -f - <<EOF
# apiVersion: security.istio.io/v1
# kind: PeerAuthentication
# metadata:
#   name: default-permissive
#   namespace: dev
# spec:
#   mtls:
#     mode: PERMISSIVE
# ---
# apiVersion: security.istio.io/v1
# kind: PeerAuthentication
# metadata:
#   name: postgresql-disable
#   namespace: dev
# spec:
#   selector:
#     matchLabels:
#       app.kubernetes.io/name: postgresql
#   mtls:
#     mode: DISABLE
# EOF
