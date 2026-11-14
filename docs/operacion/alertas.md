# Alertas de seguridad (V-12, V-13)

La auditoría sale por el logger `AUDIT` como **una línea JSON** por evento (`logging.structured.format.console=logstash`, desactivable con `LOG_FORMAT`). Campos: `action`, `actor`, `ip`, `outcome` (`SUCCESS`/`FAILURE`), `traceId` (el mismo que devuelven los errores) y `details`. Los valores de claves como `password`, `token` o `secret` se enmascaran con `***`.

## Contadores Micrometer
| Contador | Se incrementa con |
|---|---|
| `reservahub.auth.login.failed` | `LOGIN_FAILED`, `LOGIN_LOCKED` |
| `reservahub.ratelimit.rejected` | `RATE_LIMITED` |
| `reservahub.provider.registered` | `PROVIDER_REGISTERED` |

Actuator solo expone `health`; los contadores no se publican por HTTP. Quedan listos para cuando se añada un registro Prometheus.

## Umbrales (ventana de 5 minutos, configurables por variable)
| Acción | Variable | Por defecto |
|---|---|---|
| `LOGIN_FAILED` | `UMBRAL_LOGIN_FAILED` | 20 |
| `LOGIN_LOCKED` | `UMBRAL_LOGIN_LOCKED` | 5 |
| `RATE_LIMITED` | `UMBRAL_RATE_LIMITED` | 30 |
| `PROVIDER_REGISTERED` | `UMBRAL_PROVIDER_REGISTERED` | 5 |

`scripts/alerta-seguridad.sh` lee `docker logs reservahub-app --since 5m` (o `--file ruta.log`), imprime el conteo de cada acción y sale con código 1 si alguna lo supera.

Cron sugerido (cada 5 minutos, avisando por correo o webhook al fallar):
```
*/5 * * * * cd /opt/reservahub && scripts/alerta-seguridad.sh || mail -s "ReservaHub: alerta de seguridad" ops@example.com
```

## Regla equivalente cuando haya Loki / Prometheus
LogQL: `sum(count_over_time({container="reservahub-app"} | json | action="LOGIN_FAILED" [5m])) > 20`

PromQL: `increase(reservahub_auth_login_failed_total[5m]) > 20` (idem `reservahub_ratelimit_rejected_total > 30` y `reservahub_provider_registered_total > 5`).
