output "cert_manager_version" {
  description = "Version of cert-manager installed"
  value       = helm_release.cert_manager.version
}

output "cert_manager_status" {
  description = "Status of cert-manager Helm release"
  value       = helm_release.cert_manager.status
}

output "cluster_issuers" {
  description = "List of ClusterIssuers created"
  value = [
    "letsencrypt-staging",
    "letsencrypt-prod"
  ]
}
