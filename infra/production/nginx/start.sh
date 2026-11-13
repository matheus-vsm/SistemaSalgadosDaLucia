#!/bin/sh
set -eu
: "${DOMAIN:?Defina DOMAIN}"
if ! printf '%s' "$DOMAIN" | grep -Eq '^[a-zA-Z0-9]([a-zA-Z0-9.-]*[a-zA-Z0-9])?\.[a-zA-Z]{2,}$'; then
    echo 'DOMAIN invalido; informe apenas o nome DNS completo.' >&2
    exit 1
fi
case "${TLS_ENABLED:-false}" in
    true) template=https.conf.template ;;
    false) template=bootstrap.conf.template ;;
    *) echo 'TLS_ENABLED deve ser true ou false.' >&2; exit 1 ;;
esac
cp "/opt/production/$template" /etc/nginx/templates/default.conf.template
export NGINX_ENVSUBST_FILTER='^(BACKEND_URL|DOMAIN)$'
exec /docker-entrypoint.sh nginx -g 'daemon off;'
