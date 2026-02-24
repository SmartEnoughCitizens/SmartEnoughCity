variable "namespace" {
  description = "Namespace for ingress-nginx"
  type        = string
  default     = "ingress"
}

variable "ingress_nginx_version" {
  description = "Version of ingress-nginx Helm chart"
  type        = string
  default     = "4.10.0"
}
