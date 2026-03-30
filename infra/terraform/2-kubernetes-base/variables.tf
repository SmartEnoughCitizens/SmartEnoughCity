variable "dev_cpu_requests" {
  description = "Total CPU requests for dev namespace"
  type        = string
  default     = "10"
}

variable "dev_cpu_limits" {
  description = "Total CPU limits for dev namespace"
  type        = string
  default     = "36"
}

variable "dev_memory_requests" {
  description = "Total memory requests for dev namespace"
  type        = string
  default     = "20Gi"
}

variable "dev_memory_limits" {
  description = "Total memory limits for dev namespace"
  type        = string
  default     = "40Gi"
}

variable "dev_max_pods" {
  description = "Maximum number of pods in dev namespace"
  type        = string
  default     = "30"
}
