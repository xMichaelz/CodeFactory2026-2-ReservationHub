#!/usr/bin/env bash
# Alerta por umbral sobre la auditoría JSON (V-12/V-13). Sale con 1 si algún contador supera su umbral.
# Uso: scripts/alerta-seguridad.sh [--file ruta.log]   (por defecto lee `docker logs reservahub-app --since $VENTANA`)
# Variables: VENTANA (5m), UMBRAL_LOGIN_FAILED (20), UMBRAL_LOGIN_LOCKED (5), UMBRAL_RATE_LIMITED (30),
#            UMBRAL_PROVIDER_REGISTERED (5), CONTENEDOR (reservahub-app)
set -euo pipefail

VENTANA="${VENTANA:-5m}"
CONTENEDOR="${CONTENEDOR:-reservahub-app}"

if [[ "${1:-}" == "--file" ]]; then
  logs="$(cat "${2:?falta la ruta del log}")"
else
  logs="$(docker logs "$CONTENEDOR" --since "$VENTANA" 2>&1)"
fi

estado=0
for accion in LOGIN_FAILED LOGIN_LOCKED RATE_LIMITED PROVIDER_REGISTERED; do
  umbral_var="UMBRAL_${accion}"
  case "$accion" in
    LOGIN_FAILED) por_defecto=20 ;;
    LOGIN_LOCKED) por_defecto=5 ;;
    RATE_LIMITED) por_defecto=30 ;;
    PROVIDER_REGISTERED) por_defecto=5 ;;
  esac
  umbral="${!umbral_var:-$por_defecto}"
  total="$(printf '%s\n' "$logs" | grep -c "\"action\":\"$accion\"" || true)"
  if (( total > umbral )); then
    echo "ALERTA $accion: $total eventos en $VENTANA (umbral $umbral)"
    estado=1
  else
    echo "ok $accion: $total (umbral $umbral)"
  fi
done
exit "$estado"
