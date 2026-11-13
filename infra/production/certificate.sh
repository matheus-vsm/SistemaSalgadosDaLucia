#!/usr/bin/env bash
set -euo pipefail
mode=${1:?Use issue, renew ou dry-run}
[[ $mode == issue || $mode == renew || $mode == dry-run ]]
set -- "${2:-${DEPLOY_ROOT:-/opt/salgados}/current}"
# shellcheck source=infra/production/common.sh
source "$(dirname "$0")/common.sh"
exec 7> "$ROOT/certificate.lock"
flock -n 7 || { echo 'Outro comando de certificado esta em andamento.' >&2; exit 1; }
if [[ $mode == issue ]]; then
    # Expandir DOMAIN/email/aceite dentro do container Certbot.
    # shellcheck disable=SC2016
    compose run --rm --no-deps --entrypoint /bin/sh certbot -c '
        test "$ACME_TERMS_ACCEPTED" = true || { echo "Confirme os termos ACME no arquivo de ambiente antes de emitir." >&2; exit 1; }
        exec certbot certonly --non-interactive --webroot -w /var/www/acme -d "$DOMAIN" -m "$ACME_EMAIL" --agree-tos
    '
else
    arguments=(renew --non-interactive --webroot -w /var/www/acme)
    [[ $mode == dry-run ]] && arguments+=(--dry-run)
    compose run --rm --no-deps certbot "${arguments[@]}"
    compose exec -T frontend nginx -t
    compose exec -T frontend nginx -s reload
fi
