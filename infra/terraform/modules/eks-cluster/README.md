# modules/eks-cluster

## Overview

Amazon EKS cluster and managed node group of one account (KAN-29; KAN-2 ADR-112, ADR-113, ADR-120, ADR-123), created at session-start and deleted at session-stop.

## Interface

Inputs: `name`, `environment`, `slot`, `session_on`, `kubernetes_version`, `subnet_ids`, `node_security_group_id`, `api_client_security_group_ids` (map: runner, bastion), node sizes. Outputs: `cluster_name`, `cluster_endpoint`, `cluster_security_group_id`, `ci_deployer_parameter_dir`. `scripts/bootstrap-ci-deployer.sh` runs from Terraform on the runner (needs `aws` and `kubectl`).

## Invariants

- ADR-123 order: cluster (LabRole, private endpoint, API authentication, creator admin entry) > `ci-deployer` ServiceAccount, ClusterRoleBinding and token in Parameter Store (`/solidaria/<env>/<slot>/eks/{ci-deployer-token,endpoint,ca}`) > deletion of the LabRole access entry > node group with LabRole.
- The API endpoint is private; only the runner and the bastion security groups reach port 443.
- Nodes use IMDSv2 with hop limit 2 (pods use the LabRole credentials of the node) and the persistent node security group that the stores admit.
- The VPC CNI enforces NetworkPolicies.

## Side Effects

- During a session: control plane (USD 0.10 per hour) and the node group. Writes three Parameter Store entries.

## Verification

- `terraform init -backend=false && terraform validate` and `tflint` in this directory (`ci.yml` and `infra.yml`).
- `trivy config infra` reports no HIGH or CRITICAL finding without a documented ignore.
