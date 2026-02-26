output "network_policies" {
  description = "List of NetworkPolicies created"
  value = [
    "default-deny-all",
    "allow-dns",
    "allow-https-egress",
    "allow-istio-system",
    "allow-from-ingress",
    "allow-internal-dev",
    "allow-cert-manager-http01"
  ]
}

output "namespace" {
  description = "Namespace where NetworkPolicies are applied"
  value       = var.namespace
}
