# Bootstrap values of account QA-A1 (infra/accounts.yml).
environment = "qa"
slot        = "A1"
region      = "us-west-2"
vpc_cidr    = "10.10.0.0/16"

# Pass these on the command line, never commit them:
#   -var runner_registration_token=<token>   only when the runner is created or replaced
#   -var budget_email=<address>
