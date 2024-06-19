{{- define "edusharing_repository_service.pvc.share.config" -}}
share-config-{{ include "edusharing_common_lib.names.name" . }}
{{- end -}}

{{- define "edusharing_repository_service.pvc.share.data" -}}
share-data-{{ include "edusharing_common_lib.names.name" . }}
{{- end -}}

{{- define "edusharing_repository_service.pvc.share.safe" -}}
share-safe-{{ include "edusharing_repository_service.name" . }}
{{- end -}}
