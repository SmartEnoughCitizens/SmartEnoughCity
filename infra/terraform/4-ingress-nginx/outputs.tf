output "ingress_nginx_version" {
  description = "Version of ingress-nginx installed"
  value       = helm_release.ingress_nginx.version
}

output "ingress_nginx_status" {
  description = "Status of ingress-nginx Helm release"
  value       = helm_release.ingress_nginx.status
}

output "load_balancer_ip" {
  description = "External IP of the LoadBalancer (available after deployment)"
  value       = "Run: kubectl get svc -n ${var.namespace} ingress-nginx-controller -o jsonpath='{.status.loadBalancer.ingress[0].ip}'"
}
