# QA-A1 (KAN-11 section 5.3: same architecture as Production, reduced scale).
# Only this file differs between envs/qa/a1 and envs/prod/a1 (principle E-03).
environment   = "qa"
slot          = "A1"
region        = "us-west-2"
platform_slot = true

node_instance_types = ["t3.medium"]
node_desired_size   = 2
node_min_size       = 2
node_max_size       = 3
nat_gateway_count   = 1

# Services of A1 deployed in this increment (KAN-21): the sample service of the delivery path
# (KAN-34) and MS-17, whose image arrives with KAN-18. MS-01 and MS-02 are added when they start.
services = ["sample-service", "identity-access-service"]

# JWKS endpoint of MS-17 used by RabbitMQ to validate JWT Bearer tokens (KAN-18 follow-up).
rabbitmq_jwks_url = "https://qa.solidariaplus.ec/auth/oauth2/jwks"

database_services = {
  "identity-access-service" = "identity_access"
}

cache_services = {
  "identity-access-service" = "ia"
}

aurora_instance_class = "db.t4g.medium"
aurora_reader_count   = 0
valkey_node_type      = "cache.t4g.micro"
valkey_replicas       = 0

api_throttling_rate_limit  = 200
api_throttling_burst_limit = 400
log_retention_days         = 14
