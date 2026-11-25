# QA-A1, Kubernetes side. Only this file differs from envs/prod/a1-platform (principle E-03).
environment = "qa"
slot        = "A1"
region      = "us-west-2"
enable_xray = false

database_services = {
  "identity-access-service" = "identity_access"
}
