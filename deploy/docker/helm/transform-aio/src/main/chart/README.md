## Parameters

### Global parameters

| Name                                    | Description                           | Value                  |
| --------------------------------------- | ------------------------------------- | ---------------------- |
| `global.annotations`                    | Define global annotations             | `{}`                   |
| `global.cluster.istio.enabled`          | Enable Istio Service mesh             | `false`                |
| `global.cluster.pdb.enabled`            | Enable PDF                            | `false`                |
| `global.debug`                          | Enable global debugging               | `false`                |
| `global.image.pullPolicy`               | Set global image pullPolicy           | `Always`               |
| `global.image.pullSecrets`              | Set global image pullSecrets          | `[]`                   |
| `global.image.registry`                 | Set global image container registry   | `${docker.registry}`   |
| `global.image.repository`               | Set global image container repository | `${docker.repository}` |
| `global.image.common`                   | Set global image container common     | `${docker.common}`     |
| `global.metrics.scrape.interval`        | Set prometheus scrape interval        | `60s`                  |
| `global.metrics.scrape.timeout`         | Set prometheus scrape timeout         | `60s`                  |
| `global.metrics.servicemonitor.enabled` | Enable metrics service monitor        | `false`                |
| `global.security`                       | Set global security parameters        | `{}`                   |

### Local parameters

| Name                                       | Description                                       | Value                                                                     |
| ------------------------------------------ | ------------------------------------------------- | ------------------------------------------------------------------------- |
| `nameOverride`                             | Override name                                     | `edusharing-repository-transform-aio`                                     |
| `image.name`                               | Set image name                                    | `${docker.edu_sharing.community.common.alfresco.transform.core.aio.name}` |
| `image.tag`                                | Set image tag                                     | `${docker.edu_sharing.community.common.alfresco.transform.core.aio.tag}`  |
| `replicaCount`                             | Define amount of parallel replicas to run         | `1`                                                                       |
| `autoscaling.enabled`                      | Enable autoscaling                                | `false`                                                                   |
| `autoscaling.minReplicas`                  | Define minimum number of replicas to have running | `3`                                                                       |
| `autoscaling.maxReplicas`                  | Define maximum number of replicas to have running | `5`                                                                       |
| `autoscaling.targetCPU`                    | Set CPU limit when to scale                       | `80`                                                                      |
| `service.port.api`                         | Set port for service API                          | `8090`                                                                    |
| `config.jvm.ram.minPercentage`             | Set minimum memory in percentages                 | `90.0`                                                                    |
| `config.jvm.ram.maxPercentage`             | Set maximum memory in percentages                 | `90.0`                                                                    |
| `config.metrics.enabled`                   | Enable metrics                                    | `true`                                                                    |
| `config.metrics.relabelings`               | Relable metrics                                   | `[]`                                                                      |
| `config.override`                          | Set custom overrides                              | `""`                                                                      |
| `debug`                                    | Enable debugging                                  | `false`                                                                   |
| `nodeAffinity`                             | Set node affinity                                 | `{}`                                                                      |
| `tolerations`                              | Set tolerations                                   | `[]`                                                                      |
| `podAnnotations`                           | Set custom pod annotations                        | `{}`                                                                      |
| `podSecurityContext.fsGroup`               | Set fs group for access                           | `1000`                                                                    |
| `podSecurityContext.fsGroupChangePolicy`   | Set change policy for fs group                    | `OnRootMismatch`                                                          |
| `securityContext.allowPrivilegeEscalation` | Allow privilege escalation                        | `false`                                                                   |
| `securityContext.capabilities.drop`        | Set drop capabilities                             | `["ALL"]`                                                                 |
| `securityContext.runAsUser`                | Define user to run under                          | `1000`                                                                    |
| `terminationGracePeriod`                   | Define grace period for termination               | `120`                                                                     |
| `startupProbe.failureThreshold`            | Failure threshold for startupProbe                | `30`                                                                      |
| `startupProbe.initialDelaySeconds`         | Initial delay seconds for startupProbe            | `0`                                                                       |
| `startupProbe.periodSeconds`               | Period seconds for startupProbe                   | `20`                                                                      |
| `startupProbe.successThreshold`            | Success threshold for startupProbe                | `1`                                                                       |
| `startupProbe.timeoutSeconds`              | Timeout seconds for startupProbe                  | `10`                                                                      |
| `livenessProbe.failureThreshold`           | Failure threshold for livenessProbe               | `3`                                                                       |
| `livenessProbe.initialDelaySeconds`        | Initial delay seconds for livenessProbe           | `30`                                                                      |
| `livenessProbe.periodSeconds`              | Period seconds for livenessProbe                  | `30`                                                                      |
| `livenessProbe.timeoutSeconds`             | Timeout seconds for livenessProbe                 | `10`                                                                      |
| `readinessProbe.failureThreshold`          | Failure threshold for readinessProbe              | `1`                                                                       |
| `readinessProbe.initialDelaySeconds`       | Initial delay seconds for readinessProbe          | `10`                                                                      |
| `readinessProbe.periodSeconds`             | Period seconds for readinessProbe                 | `10`                                                                      |
| `readinessProbe.successThreshold`          | Set threshold for success on readiness probe      | `1`                                                                       |
| `readinessProbe.timeoutSeconds`            | Timeout seconds for readinessProbe                | `10`                                                                      |
| `resources.limits.cpu`                     | Set CPU limit on resources                        | `500m`                                                                    |
| `resources.limits.memory`                  | Set memory limit on resources                     | `2Gi`                                                                     |
| `resources.requests.cpu`                   | Set CPU for requests on resources                 | `500m`                                                                    |
| `resources.requests.memory`                | Set memory for requests on resources              | `2Gi`                                                                     |
