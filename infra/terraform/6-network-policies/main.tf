
provider "kubernetes" {
  config_path = "~/.kube/config"
}

# 1. Default deny all traffic
resource "kubernetes_manifest" "default_deny_all" {
  manifest = {
    apiVersion = "networking.k8s.io/v1"
    kind       = "NetworkPolicy"
    metadata = {
      name      = "default-deny-all"
      namespace = var.namespace
    }
    spec = {
      podSelector = {}
      policyTypes = ["Ingress", "Egress"]
    }
  }
}

# 2. Allow DNS queries
resource "kubernetes_manifest" "allow_dns" {
  manifest = {
    apiVersion = "networking.k8s.io/v1"
    kind       = "NetworkPolicy"
    metadata = {
      name      = "allow-dns"
      namespace = var.namespace
    }
    spec = {
      podSelector = {}
      policyTypes = ["Egress"]
      egress = [
        {
          ports = [
            {
              port     = 53
              protocol = "UDP"
            },
            {
              port     = 53
              protocol = "TCP"
            }
          ]
          to = [
            {
              namespaceSelector = {
                matchLabels = {
                  "kubernetes.io/metadata.name" = "kube-system"
                }
              }
              podSelector = {
                matchLabels = {
                  "k8s-app" = "kube-dns"
                }
              }
            }
          ]
        },
        {
          ports = [
            {
              port     = 53
              protocol = "UDP"
            },
            {
              port     = 53
              protocol = "TCP"
            }
          ]
          to = [
            {
              namespaceSelector = {
                matchLabels = {
                  "kubernetes.io/metadata.name" = "kube-system"
                }
              }
              podSelector = {
                matchLabels = {
                  "k8s-app" = "node-local-dns"
                }
              }
            }
          ]
        }
      ]
    }
  }
}

# 3. Allow HTTPS egress
resource "kubernetes_manifest" "allow_https_egress" {
  manifest = {
    apiVersion = "networking.k8s.io/v1"
    kind       = "NetworkPolicy"
    metadata = {
      name      = "allow-https-egress"
      namespace = var.namespace
    }
    spec = {
      podSelector = {}
      policyTypes = ["Egress"]
      egress = [
        {
          ports = [
            {
              port     = 443
              protocol = "TCP"
            }
          ]
          to = [
            {
              ipBlock = {
                cidr = "0.0.0.0/0"
              }
            }
          ]
        }
      ]
    }
  }
}

# 4. Allow Istio system communication
resource "kubernetes_manifest" "allow_istio_system" {
  manifest = {
    apiVersion = "networking.k8s.io/v1"
    kind       = "NetworkPolicy"
    metadata = {
      name      = "allow-istio-system"
      namespace = var.namespace
    }
    spec = {
      podSelector = {}
      policyTypes = ["Egress"]
      egress = [
        {
          to = [
            {
              namespaceSelector = {
                matchLabels = {
                  name = "istio-system"
                }
              }
            }
          ]
          ports = [
            {
              port     = 15012
              protocol = "TCP"
            },
            {
              port     = 15017
              protocol = "TCP"
            },
            {
              port     = 15021
              protocol = "TCP"
            }
          ]
        }
      ]
    }
  }
}

# 5. Allow ingress from ingress namespace
resource "kubernetes_manifest" "allow_from_ingress" {
  manifest = {
    apiVersion = "networking.k8s.io/v1"
    kind       = "NetworkPolicy"
    metadata = {
      name      = "allow-from-ingress"
      namespace = var.namespace
    }
    spec = {
      podSelector = {}
      policyTypes = ["Ingress"]
      ingress = [
        {
          from = [
            {
              namespaceSelector = {
                matchLabels = {
                  "kubernetes.io/metadata.name" = "ingress"
                }
              }
            }
          ]
          ports = [
            {
              port     = 8080
              protocol = "TCP"
            },
            {
              port     = 8000
              protocol = "TCP"
            },
            {
              port     = 8089
              protocol = "TCP"
            },
            {
              port     = 15020
              protocol = "TCP"
            },
            {
              port     = 15021
              protocol = "TCP"
            }
          ]
        }
      ]
    }
  }
}

# 6. Allow internal dev namespace communication
resource "kubernetes_manifest" "allow_internal_dev" {
  manifest = {
    apiVersion = "networking.k8s.io/v1"
    kind       = "NetworkPolicy"
    metadata = {
      name      = "allow-internal-dev"
      namespace = var.namespace
    }
    spec = {
      podSelector = {}
      policyTypes = ["Ingress", "Egress"]
      ingress = [
        {
          from = [
            {
              podSelector = {}
            }
          ]
        }
      ]
      egress = [
        {
          to = [
            {
              podSelector = {}
            }
          ]
        }
      ]
    }
  }
}

# 7. Allow cert-manager HTTP01 solver
resource "kubernetes_manifest" "allow_cert_manager_http01" {
  manifest = {
    apiVersion = "networking.k8s.io/v1"
    kind       = "NetworkPolicy"
    metadata = {
      name      = "allow-cert-manager-http01"
      namespace = var.namespace
    }
    spec = {
      podSelector = {
        matchLabels = {
          "acme.cert-manager.io/http01-solver" = "true"
        }
      }
      policyTypes = ["Ingress"]
      ingress = [
        {
          from = [
            {
              namespaceSelector = {
                matchLabels = {
                  "kubernetes.io/metadata.name" = "ingress"
                }
              }
            }
          ]
          ports = [
            {
              port     = 8089
              protocol = "TCP"
            }
          ]
        }
      ]
    }
  }
}
