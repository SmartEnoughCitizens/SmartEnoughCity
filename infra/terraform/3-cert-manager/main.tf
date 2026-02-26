
# Configure Helm provider
provider "helm" {
  kubernetes {
    config_path = "~/.kube/config"
  }
}

provider "kubernetes" {
  config_path = "~/.kube/config"
}

# Install cert-manager using Helm
resource "helm_release" "cert_manager" {
  name       = "cert-manager"
  repository = "https://charts.jetstack.io"
  chart      = "cert-manager"
  version    = var.cert_manager_version
  namespace  = var.namespace

  # Install CRDs
  set {
    name  = "installCRDs"
    value = "true"
  }

  # Enable webhook
  set {
    name  = "webhook.enabled"
    value = "true"
  }

  # Resource limits for cert-manager controller
  set {
    name  = "resources.requests.cpu"
    value = "10m"
  }

  set {
    name  = "resources.requests.memory"
    value = "32Mi"
  }

  set {
    name  = "resources.limits.cpu"
    value = "100m"
  }

  set {
    name  = "resources.limits.memory"
    value = "128Mi"
  }

  # Webhook resources
  set {
    name  = "webhook.resources.requests.cpu"
    value = "10m"
  }

  set {
    name  = "webhook.resources.requests.memory"
    value = "32Mi"
  }

  # CA injector resources
  set {
    name  = "cainjector.resources.requests.cpu"
    value = "10m"
  }

  set {
    name  = "cainjector.resources.requests.memory"
    value = "32Mi"
  }

  # Wait for cert-manager to be ready
  wait    = true
  timeout = 300
}

# Wait for cert-manager CRDs to be ready
resource "null_resource" "wait_for_cert_manager_crds" {
  provisioner "local-exec" {
    command = <<-EOT
      echo "Waiting for cert-manager CRDs to be ready..."
      kubectl wait --for=condition=established --timeout=300s crd/clusterissuers.cert-manager.io || true
      sleep 5
    EOT
  }

  depends_on = [helm_release.cert_manager]
}

# Let's Encrypt Staging ClusterIssuer
resource "null_resource" "letsencrypt_staging" {
  provisioner "local-exec" {
    command = <<-EOT
      kubectl apply -f - <<EOF
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-staging
spec:
  acme:
    server: https://acme-staging-v02.api.letsencrypt.org/directory
    email: ${var.letsencrypt_email}
    privateKeySecretRef:
      name: letsencrypt-staging
    solvers:
    - http01:
        ingress:
          class: nginx
EOF
    EOT
  }

  depends_on = [null_resource.wait_for_cert_manager_crds]
}

# Let's Encrypt Production ClusterIssuer
resource "null_resource" "letsencrypt_prod" {
  provisioner "local-exec" {
    command = <<-EOT
      kubectl apply -f - <<EOF
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: ${var.letsencrypt_email}
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
EOF
    EOT
  }

  depends_on = [null_resource.wait_for_cert_manager_crds]
}
