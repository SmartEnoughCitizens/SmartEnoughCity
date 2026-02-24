variable "project_id" {
  description = "GCP Project ID"
  type        = string
}

variable "region" {
  description = "GCP Region"
  type        = string
  default     = "europe-west1"
}

variable "zone" {
  description = "GCP Zone for the cluster"
  type        = string
  default     = "europe-west1-b"
}

variable "cluster_name" {
  description = "Name of the GKE cluster"
  type        = string
  default     = "smart-enough-city"
}

variable "machine_type" {
  description = "Machine type for cluster nodes"
  type        = string
  default     = "e2-standard-4"
}

variable "environment" {
  description = "Environment (dev, staging, prod)"
  type        = string
  default     = "dev"
}

variable "use_preemptible" {
  description = "Use preemptible nodes for cost savings"
  type        = bool
  default     = false
}
