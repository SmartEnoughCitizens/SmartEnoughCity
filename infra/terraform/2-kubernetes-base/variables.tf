variable "dev_cpu_requests" {
  description = "Total CPU requests for dev namespace"
  type        = string
  default     = "4"
}

variable "dev_cpu_limits" {
  description = "Total CPU limits for dev namespace"
  type        = string
  default     = "12"
}

variable "dev_memory_requests" {
  description = "Total memory requests for dev namespace"
  type        = string
  default     = "4Gi"
}

variable "dev_memory_limits" {
  description = "Total memory limits for dev namespace"
  type        = string
  default     = "12Gi"
}

variable "dev_max_pods" {
  description = "Maximum number of pods in dev namespace"
  type        = string
  default     = "30"
}
