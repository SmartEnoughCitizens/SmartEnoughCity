
provider "helm" {
  kubernetes {
    config_path = "~/.kube/config"
  }
}

provider "kubernetes" {
  config_path = "~/.kube/config"
}

# Enable Istio injection on ingress namespace
resource "kubernetes_labels" "ingress_istio_injection" {
  api_version = "v1"
  kind        = "Namespace"
  metadata {
    name = var.namespace
  }

  labels = {
    "istio-injection" = "enabled"
  }
}

# Install ingress-nginx using Helm
resource "helm_release" "ingress_nginx" {
  name       = "ingress-nginx"
  repository = "https://kubernetes.github.io/ingress-nginx"
  chart      = "ingress-nginx"
  version    = var.ingress_nginx_version
  namespace  = var.namespace

  # Use values file for cleaner configuration
  values = [
    file("${path.module}/values.yaml")
  ]

  # Wait for ingress-nginx to be ready
  wait    = true
  timeout = 600

  depends_on = [kubernetes_labels.ingress_istio_injection]
}
