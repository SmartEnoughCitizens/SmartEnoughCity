# Configure Kubernetes provider
# Loads configuration from kubeconfig
provider "kubernetes" {
  config_path = "~/.kube/config"
}

# Namespace: dev
resource "kubernetes_namespace" "dev" {
  metadata {
    name = "dev"
    labels = {
      name        = "dev"
      environment = "development"
      managed-by  = "terraform"
      # Istio injection will be enabled later
    }
  }
}

# Namespace: ingress
resource "kubernetes_namespace" "ingress" {
  metadata {
    name = "ingress"
    labels = {
      name       = "ingress"
      managed-by = "terraform"
    }
  }
}

# Namespace: cert-manager
resource "kubernetes_namespace" "cert_manager" {
  metadata {
    name = "cert-manager"
    labels = {
      name       = "cert-manager"
      managed-by = "terraform"
    }
  }
}

# Namespace: istio-system
resource "kubernetes_namespace" "istio_system" {
  metadata {
    name = "istio-system"
    labels = {
      name       = "istio-system"
      managed-by = "terraform"
    }
  }
}

# ResourceQuota for dev namespace
resource "kubernetes_resource_quota" "dev_quota" {
  metadata {
    name      = "dev-quota"
    namespace = kubernetes_namespace.dev.metadata[0].name
  }

  spec {
    hard = {
      "requests.cpu"    = var.dev_cpu_requests
      "requests.memory" = var.dev_memory_requests
      "limits.cpu"      = var.dev_cpu_limits
      "limits.memory"   = var.dev_memory_limits
      "pods"            = var.dev_max_pods
    }
  }
}

# LimitRange for dev namespace (default limits for pods without resource specs)
resource "kubernetes_limit_range" "dev_limits" {
  metadata {
    name      = "dev-limits"
    namespace = kubernetes_namespace.dev.metadata[0].name
  }

  spec {
    limit {
      type = "Container"

      default = {
        cpu    = "500m"
        memory = "512Mi"
      }

      default_request = {
        cpu    = "100m"
        memory = "128Mi"
      }
    }
  }
}
