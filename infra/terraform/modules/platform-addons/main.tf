# Cluster add-ons and the observability baseline of one account (KAN-29, KAN-33; KAN-2 ADR-058,
# ADR-107, ADR-112, ADR-121, ADR-123). Applied by the root envs/<env>/<slot>-platform, whose
# Kubernetes and Helm providers authenticate with the ci-deployer ServiceAccount token through the
# private endpoint (the runner cannot use LabRole, which is mapped to system:node).
#
#   kube-system   AWS Load Balancer Controller, Cluster Autoscaler (LabRole of the nodes, EKS-08)
#   keda          KEDA (Kafka lag and RabbitMQ queue scalers of the shared chart)
#   observability kube-prometheus-stack (central Prometheus, Alertmanager to SNS, Grafana),
#                 Fluent Bit to CloudWatch Logs, OpenTelemetry collector to X-Ray (optional),
#                 dashboards and alert rules of deploy/observability
#   platform      db-provisioner Job: one database and one user per service in Aurora
#
# The cluster is recreated at every session-start, so session.yml applies this root after the
# cluster root and destroys it before session-stop.

locals {
  environment_label = var.environment == "prod" ? "production" : "qa"
  dashboards        = fileset("${var.observability_dir}/dashboards", "*.json")
  alert_files       = fileset("${var.observability_dir}/alerts", "*.yaml")
}

resource "kubernetes_namespace_v1" "this" {
  for_each = toset(["observability", "keda", "platform"])

  metadata {
    name = each.key
    labels = {
      "app.kubernetes.io/managed-by" = "terraform"
    }
  }
}

# ---------------------------------------------------------------------------------------------
# Load balancing, autoscaling and event-driven scaling
# ---------------------------------------------------------------------------------------------
resource "helm_release" "aws_load_balancer_controller" {
  name       = "aws-load-balancer-controller"
  namespace  = "kube-system"
  repository = "https://aws.github.io/eks-charts"
  chart      = "aws-load-balancer-controller"
  version    = var.chart_versions.aws_load_balancer_controller
  wait       = true
  timeout    = 600

  values = [yamlencode({
    clusterName  = var.name
    region       = var.region
    vpcId        = var.vpc_id
    replicaCount = 1
    serviceAccount = {
      create = true
      name   = "aws-load-balancer-controller"
    }
    # Pods use the LabRole credentials of the node through IMDSv2 (hop limit 2 on the nodes).
    enableServiceMutatorWebhook = false
  })]
}

resource "helm_release" "cluster_autoscaler" {
  name       = "cluster-autoscaler"
  namespace  = "kube-system"
  repository = "https://kubernetes.github.io/autoscaler"
  chart      = "cluster-autoscaler"
  version    = var.chart_versions.cluster_autoscaler
  wait       = true

  values = [yamlencode({
    autoDiscovery = { clusterName = var.name }
    awsRegion     = var.region
    extraArgs = {
      "balance-similar-node-groups" = true
      "skip-nodes-with-system-pods" = false
      "scale-down-unneeded-time"    = "5m"
    }
  })]
}

resource "helm_release" "keda" {
  name       = "keda"
  namespace  = kubernetes_namespace_v1.this["keda"].metadata[0].name
  repository = "https://kedacore.github.io/charts"
  chart      = "keda"
  version    = var.chart_versions.keda
  wait       = true
}

# ---------------------------------------------------------------------------------------------
# Observability (KAN-33)
# ---------------------------------------------------------------------------------------------
resource "random_password" "grafana_admin" {
  count = var.central_monitoring ? 1 : 0

  length  = 24
  special = false
}

resource "kubernetes_secret_v1" "grafana_admin" {
  count = var.central_monitoring ? 1 : 0

  metadata {
    name      = "grafana-admin"
    namespace = kubernetes_namespace_v1.this["observability"].metadata[0].name
  }

  data = {
    admin-user     = "admin"
    admin-password = random_password.grafana_admin[0].result
  }
}

resource "aws_ssm_parameter" "grafana_admin" {
  count = var.central_monitoring ? 1 : 0

  name        = "/solidaria/${var.environment}/observability/grafana-admin-password"
  description = "Password of the Grafana admin user; Grafana is reached with port forwarding from the bastion."
  type        = "SecureString"
  value       = random_password.grafana_admin[0].result
}

resource "kubernetes_config_map_v1" "dashboards" {
  for_each = var.central_monitoring ? local.dashboards : toset([])

  metadata {
    name      = "dashboard-${trimsuffix(each.value, ".json")}"
    namespace = kubernetes_namespace_v1.this["observability"].metadata[0].name
    labels = {
      grafana_dashboard = "1"
    }
    annotations = {
      grafana_folder = "Solidaria+"
    }
  }

  data = {
    (each.value) = file("${var.observability_dir}/dashboards/${each.value}")
  }
}

resource "helm_release" "kube_prometheus_stack" {
  count = var.central_monitoring ? 1 : 0

  name       = "monitoring"
  namespace  = kubernetes_namespace_v1.this["observability"].metadata[0].name
  repository = "https://prometheus-community.github.io/helm-charts"
  chart      = "kube-prometheus-stack"
  version    = var.chart_versions.kube_prometheus_stack
  wait       = true
  timeout    = 900

  values = [
    file("${var.observability_dir}/kube-prometheus-stack/values.yaml"),
    file("${var.observability_dir}/kube-prometheus-stack/values-${var.environment}.yaml"),
    yamlencode({
      prometheus = {
        prometheusSpec = {
          externalLabels = {
            environment = local.environment_label
            cluster     = var.name
          }
        }
      }
      alertmanager = {
        config = {
          receivers = [
            { name = "null" },
            {
              name = "sns"
              sns_configs = [{
                send_resolved = true
                topic_arn     = var.alerts_topic_arn
                sigv4         = { region = var.region }
                subject       = "[${upper(local.environment_label)}] {{ .CommonLabels.alertname }}"
                message       = "{{ range .Alerts }}{{ .Annotations.summary }}\n{{ .Annotations.description }}\n{{ end }}"
              }]
            },
          ]
        }
      }
      grafana = {
        admin = {
          existingSecret = kubernetes_secret_v1.grafana_admin[0].metadata[0].name
          userKey        = "admin-user"
          passwordKey    = "admin-password"
        }
      }
      additionalPrometheusRulesMap = {
        for file_name in local.alert_files :
        trimsuffix(file_name, ".yaml") => yamldecode(file("${var.observability_dir}/alerts/${file_name}"))
      }
    }),
  ]

  depends_on = [kubernetes_config_map_v1.dashboards]
}

resource "helm_release" "fluent_bit" {
  name       = "fluent-bit"
  namespace  = kubernetes_namespace_v1.this["observability"].metadata[0].name
  repository = "https://fluent.github.io/helm-charts"
  chart      = "fluent-bit"
  version    = var.chart_versions.fluent_bit
  wait       = true

  values = [
    file("${var.observability_dir}/fluent-bit/values.yaml"),
    yamlencode({
      env = [
        { name = "AWS_REGION", value = var.region },
        { name = "LOG_GROUP", value = var.apps_log_group },
        { name = "ENVIRONMENT", value = local.environment_label },
      ]
    }),
  ]
}

resource "helm_release" "otel_collector" {
  count = var.enable_xray ? 1 : 0

  name       = "otel-collector"
  namespace  = kubernetes_namespace_v1.this["observability"].metadata[0].name
  repository = "https://open-telemetry.github.io/opentelemetry-helm-charts"
  chart      = "opentelemetry-collector"
  version    = var.chart_versions.opentelemetry_collector
  wait       = true

  values = [
    file("${var.observability_dir}/otel-collector/values.yaml"),
    yamlencode({ extraEnvs = [{ name = "AWS_REGION", value = var.region }] }),
  ]
}

# ---------------------------------------------------------------------------------------------
# db-provisioner: one database and one user per service (KAN-30), with the same script as
# localhost. It runs inside the cluster, because Aurora admits only the nodes and the bastion.
# ---------------------------------------------------------------------------------------------
locals {
  database_enabled = length(var.database_services) > 0
  services_conf    = join("\n", [for service, database in var.database_services : "${service} ${database} -"])
}

resource "kubernetes_secret_v1" "db_provisioner" {
  count = local.database_enabled ? 1 : 0

  metadata {
    name      = "db-provisioner"
    namespace = kubernetes_namespace_v1.this["platform"].metadata[0].name
  }

  data = merge(
    { "master-password" = var.database_passwords["master"] },
    { for service in keys(var.database_services) : service => var.database_passwords[service] },
  )
}

resource "kubernetes_config_map_v1" "db_provisioner" {
  count = local.database_enabled ? 1 : 0

  metadata {
    name      = "db-provisioner"
    namespace = kubernetes_namespace_v1.this["platform"].metadata[0].name
  }

  data = {
    "provision.sh"  = file(var.db_provisioner_script)
    "services.conf" = local.services_conf
  }
}

resource "kubernetes_job_v1" "db_provisioner" {
  count = local.database_enabled ? 1 : 0

  metadata {
    name      = "db-provisioner-${substr(sha1(join("", [local.services_conf, file(var.db_provisioner_script)])), 0, 8)}"
    namespace = kubernetes_namespace_v1.this["platform"].metadata[0].name
  }

  spec {
    backoff_limit              = 4
    ttl_seconds_after_finished = 3600

    template {
      metadata {
        labels = {
          "app.kubernetes.io/name" = "db-provisioner"
        }
      }

      spec {
        restart_policy = "OnFailure"

        security_context {
          run_as_non_root = true
          run_as_user     = 70
        }

        container {
          name    = "provision"
          image   = var.postgres_image
          command = ["sh", "/opt/solidaria/provision.sh"]

          env {
            name  = "PGHOST"
            value = var.database_host
          }
          env {
            name  = "PGUSER"
            value = var.database_master_user
          }
          env {
            name  = "PGSSLMODE"
            value = "require"
          }
          env {
            name = "PGPASSWORD"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.db_provisioner[0].metadata[0].name
                key  = "master-password"
              }
            }
          }
          env {
            name  = "PASSWORD_DIR"
            value = "/opt/solidaria/passwords"
          }
          env {
            name  = "SERVICES_FILE"
            value = "/opt/solidaria/services.conf"
          }

          volume_mount {
            name       = "scripts"
            mount_path = "/opt/solidaria/provision.sh"
            sub_path   = "provision.sh"
          }
          volume_mount {
            name       = "scripts"
            mount_path = "/opt/solidaria/services.conf"
            sub_path   = "services.conf"
          }
          volume_mount {
            name       = "passwords"
            mount_path = "/opt/solidaria/passwords"
            read_only  = true
          }

          resources {
            requests = {
              cpu    = "10m"
              memory = "32Mi"
            }
            limits = {
              memory = "64Mi"
            }
          }
        }

        volume {
          name = "scripts"
          config_map {
            name = kubernetes_config_map_v1.db_provisioner[0].metadata[0].name
          }
        }

        volume {
          name = "passwords"
          secret {
            secret_name = kubernetes_secret_v1.db_provisioner[0].metadata[0].name
          }
        }
      }
    }
  }

  wait_for_completion = true

  timeouts {
    create = "10m"
  }
}
