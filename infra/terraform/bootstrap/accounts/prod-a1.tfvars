# Bootstrap values of account PROD-A1 (infra/accounts.yml).
environment = "prod"
slot        = "A1"
region      = "us-east-1"
vpc_cidr    = "10.20.0.0/16"

# Pass these on the command line, never commit them:
#   -var runner_registration_token=<token>   only when the runner is created or replaced
#   -var budget_email=<address>
