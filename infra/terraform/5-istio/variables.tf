variable "namespace" {
  description = "Namespace for Istio control plane"
  type        = string
  default     = "istio-system"
}

variable "istio_version" {
  description = "Version of Istio to install"
  type        = string
  default     = "1.21.0"
}
