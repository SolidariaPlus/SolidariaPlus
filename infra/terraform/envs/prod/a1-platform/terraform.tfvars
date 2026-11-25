# PROD-A1, Kubernetes side. Only this file differs from envs/qa/a1-platform (principle E-03).
environment = "prod"
slot        = "A1"
region      = "us-east-1"
enable_xray = false

database_services = {
  "identity-access-service" = "identity_access"
}
