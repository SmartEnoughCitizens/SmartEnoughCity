output "project_id" {
  description = "GCP Project ID"
  value       = var.project_id
}

output "cluster_name" {
  description = "Name of the GKE cluster"
  value       = google_container_cluster.primary.name
}

output "cluster_endpoint" {
  description = "Endpoint for the GKE cluster"
  value       = google_container_cluster.primary.endpoint
  sensitive   = true
}

output "cluster_ca_certificate" {
  description = "CA certificate for the GKE cluster"
  value       = google_container_cluster.primary.master_auth[0].cluster_ca_certificate
  sensitive   = true
}

output "cluster_location" {
  description = "Location of the GKE cluster"
  value       = google_container_cluster.primary.location
}

output "node_pool_name" {
  description = "Name of the primary node pool"
  value       = google_container_node_pool.primary_nodes.name
}

output "service_account_email" {
  description = "Email of the node service account"
  value       = google_service_account.gke_nodes.email
}

output "kubeconfig_command" {
  description = "Command to configure kubectl"
  value       = "gcloud container clusters get-credentials ${google_container_cluster.primary.name} --zone ${google_container_cluster.primary.location} --project ${var.project_id}"
}
