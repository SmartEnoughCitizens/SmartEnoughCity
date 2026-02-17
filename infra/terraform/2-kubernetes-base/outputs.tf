output "namespaces" {
  description = "List of created namespaces"
  value = {
    dev          = kubernetes_namespace.dev.metadata[0].name
    ingress      = kubernetes_namespace.ingress.metadata[0].name
    cert_manager = kubernetes_namespace.cert_manager.metadata[0].name
    istio_system = kubernetes_namespace.istio_system.metadata[0].name
  }
}

output "dev_namespace" {
  description = "Name of the dev namespace"
  value       = kubernetes_namespace.dev.metadata[0].name
}

output "dev_quota" {
  description = "ResourceQuota applied to dev namespace"
  value = {
    cpu_requests    = var.dev_cpu_requests
    cpu_limits      = var.dev_cpu_limits
    memory_requests = var.dev_memory_requests
    memory_limits   = var.dev_memory_limits
    max_pods        = var.dev_max_pods
  }
}
