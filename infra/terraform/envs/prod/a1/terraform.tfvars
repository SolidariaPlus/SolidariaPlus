# PROD-A1 (KAN-11 section 5.3: high availability, backups and full monitoring).
# Only this file differs between envs/qa/a1 and envs/prod/a1 (principle E-03).
environment   = "prod"
slot          = "A1"
region        = "us-east-1"
platform_slot = true

node_instance_types = ["t3.large"]
node_desired_size   = 2
node_min_size       = 2
node_max_size       = 3
nat_gateway_count   = 1

services = ["sample-service", "identity-access-service"]

database_services = {
  "identity-access-service" = "identity_access"
}

cache_services = {
  "identity-access-service" = "identity-access"
}

aurora_instance_class = "db.t4g.medium"
aurora_reader_count   = 0
valkey_node_type      = "cache.t4g.micro"
valkey_replicas       = 0

api_throttling_rate_limit  = 500
api_throttling_burst_limit = 1000
log_retention_days         = 30
