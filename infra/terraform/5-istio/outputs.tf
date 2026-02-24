output "istio_version" {
  description = "Version of Istio installed"
  value       = var.istio_version
}

output "istio_base_status" {
  description = "Status of Istio base Helm release"
  value       = helm_release.istio_base.status
}

output "istiod_status" {
  description = "Status of istiod Helm release"
  value       = helm_release.istiod.status
}

output "mtls_mode" {
  description = "mTLS mode configured"
  value       = "PERMISSIVE"
}

output "sidecar_resources" {
  description = "Sidecar resource configuration"
  value = {
    requests = {
      cpu    = "10m"
      memory = "64Mi"
    }
    limits = {
      cpu    = "100m"
      memory = "128Mi"
    }
  }
}
