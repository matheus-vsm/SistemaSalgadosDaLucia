#!/usr/bin/env bash
set -euo pipefail
umask 077
mode=${1:---check}
[[ $mode == --check || $mode == --execute ]] || { echo 'Use --check ou --execute e o diretorio da release.' >&2; exit 64; }
release=$(realpath "${2:?Informe o diretorio da release}")
root=$(realpath "${DEPLOY_ROOT:-/opt/salgados}")
case "$release" in "$root"/releases/*) ;; *) echo 'Release fora do diretorio permitido.' >&2; exit 64;; esac
test -f "$root/shared/app.env"
test -f "$release/images.env"
bash "$(dirname "$0")/validate-images.sh" "$release/images.env"
test -f "$release/infra/production/compose.yaml" || { echo 'Infraestrutura de producao ainda nao incluida nesta versao.' >&2; exit 1; }
compose() {
    env -u BACKEND_IMAGE -u FRONTEND_IMAGE docker compose --env-file "$root/shared/app.env" --env-file "$release/images.env" \
        -p salgados-production -f "$release/infra/production/compose.yaml" "$@"
}
compose config --quiet
if [[ $mode == --check ]]; then
    echo 'Configuracao validada; nenhum servico alterado.'
    exit 0
fi
test -f "$root/shared/production-ready" || { echo 'Producao nao habilitada.' >&2; exit 1; }
exec 9> "$root/deploy.lock"
flock -n 9 || { echo 'Outro deploy esta em andamento.' >&2; exit 1; }
compose pull db backend frontend
# Backup deve terminar antes de uma nova versao aplicar migracoes.
bash "$release/infra/production/backup.sh" "$release"
printf '%s\n' "$release" > "$root/candidate"
compose up -d --no-build --wait --wait-timeout 240 db backend frontend
domain=$(compose exec -T frontend printenv DOMAIN)
[[ $domain =~ ^[a-zA-Z0-9.-]+$ ]]
curl --fail --silent --show-error --max-time 20 "https://$domain/" >/dev/null
previous=$(readlink -f "$root/current" || true)
if [[ -n $previous && $previous != "$release" ]]; then
    ln -sfn "$previous" "$root/previous"
fi
ln -sfn "$release" "$root/current"
printf '%s\n' "$(date -u +%FT%TZ) $release" >> "$root/deploy-history.log"
echo 'Deploy validado. Em falha, consulte candidate e execute rollback manual; banco nunca e restaurado automaticamente.'
