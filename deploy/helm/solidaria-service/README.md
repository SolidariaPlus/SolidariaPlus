# deploy/helm/solidaria-service

## Overview

Shared Helm chart of every Solidaria+ microservice on Amazon EKS in QA and Production (KAN-33;
KAN-11 §6.6; KAN-2 ADR-112, ADR-113). One chart, values per environment and per service
(principle E-03).

| Template              | Objects                                                                                                                                                         |
| --------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `deployment.yaml`     | Deployment: startup, readiness and liveness probes on the Actuator groups; non-root, read-only root file system; image by digest; zone spread when replicas > 1 |
| `service.yaml`        | Service (label `service`, copied to the metrics) and ServiceAccount                                                                                             |
| `ingress.yaml`        | Ingress for the AWS Load Balancer Controller: one ALB per account (group), CloudFront origin header condition                                                   |
| `autoscaling.yaml`    | HPA on CPU, or a KEDA ScaledObject (Kafka lag, RabbitMQ depth), and the PodDisruptionBudget                                                                     |
| `cronjobs.yaml`       | CronJobs that run the same digest as the service                                                                                                                |
| `migrations.yaml`     | Flyway migrations as a `pre-install,pre-upgrade` hook Job                                                                                                       |
| `networkpolicy.yaml`  | Default-deny ingress plus the allowed namespaces and CIDRs                                                                                                      |
| `servicemonitor.yaml` | ServiceMonitor for the kube-prometheus-stack                                                                                                                    |

## Interface

Values layering, later wins:

1. `values.yaml` (chart defaults)
2. `values-qa.yaml` or `values-prod.yaml` (environment defaults)
3. `services/<context>/<service>/helm/values.yaml`
4. `services/<context>/<service>/helm/values-<env>.yaml`
5. values computed by `.github/scripts/helm-deploy.sh`: `image.repository`, `image.digest`, `awsRegion`,
   `slot`, `ingress.groupName`, `ingress.securityGroupId`, `ingress.originVerify.value`, and
   `networkPolicy.allowFromCidrs`

A service that is not deployed yet keeps `enabled: false`, and the chart renders nothing for it.

## Invariants

- QA and Production always deploy by digest. `image.tag` exists only for local tests.
- Secrets never go into values: services read them from AWS Secrets Manager and Parameter Store with
  Spring Cloud AWS and the node role.
- Rolling update with `maxUnavailable: 0`. `helm upgrade --atomic` rolls back a release whose pods do
  not become ready.
- Every object carries `solidaria.io/environment` and `solidaria.io/slot` (principle E-07).

## Side Effects

- An enabled Ingress makes the AWS Load Balancer Controller create or update the ALB of the slot.

## Verification

```bash
helm lint --strict deploy/helm/solidaria-service
helm template ci deploy/helm/solidaria-service \
  -f services/d00-platform/sample-service/helm/values.yaml \
  -f services/d00-platform/sample-service/helm/values-qa.yaml | kubeconform -strict -ignore-missing-schemas
```

`ci.yml` (job `helm`) runs both commands with the QA and Production values of every service.
