{{/* Name of the service: nameOverride or the release name. */}}
{{- define "solidaria.name" -}}
{{- default .Release.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/* Common labels (principle E-07: every object names its environment). */}}
{{- define "solidaria.labels" -}}
app.kubernetes.io/name: {{ include "solidaria.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: solidaria-plus
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version }}
solidaria.io/environment: {{ .Values.environment | quote }}
solidaria.io/slot: {{ .Values.slot | quote }}
{{- end -}}

{{- define "solidaria.selectorLabels" -}}
app.kubernetes.io/name: {{ include "solidaria.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{/* Image reference: by digest in QA and Production, by tag only for local tests. */}}
{{- define "solidaria.image" -}}
{{- if .Values.image.digest -}}
{{- printf "%s@%s" .Values.image.repository .Values.image.digest -}}
{{- else -}}
{{- printf "%s:%s" .Values.image.repository .Values.image.tag -}}
{{- end -}}
{{- end -}}

{{- define "solidaria.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
{{- include "solidaria.name" . -}}
{{- else -}}
default
{{- end -}}
{{- end -}}

{{/* Environment variables shared by the Deployment, the CronJobs and the migration Job. */}}
{{- define "solidaria.env" -}}
- name: SOLIDARIA_ENVIRONMENT
  value: {{ .Values.environment | quote }}
- name: OTEL_RESOURCE_ATTRIBUTES
  value: {{ printf "deployment.environment=%s,service.name=%s" .Values.environment (include "solidaria.name" .) | quote }}
- name: AWS_REGION
  value: {{ .Values.awsRegion | default "" | quote }}
{{- range $name, $value := .Values.env }}
- name: {{ $name }}
  value: {{ $value | quote }}
{{- end }}
{{- end -}}

{{/* Writable directories with a read-only root file system. */}}
{{- define "solidaria.tmpVolumeMount" -}}
- name: tmp
  mountPath: /tmp
{{- end -}}
{{- define "solidaria.tmpVolume" -}}
- name: tmp
  emptyDir: {}
{{- end -}}
