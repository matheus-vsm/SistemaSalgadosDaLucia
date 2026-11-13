#!/usr/bin/env bash
# Biblioteca dos comandos operacionais; apenas importar, nao executa servicos.
set -euo pipefail
umask 077
ROOT=$(realpath "${DEPLOY_ROOT:-/opt/salgados}")
RELEASE=$(realpath "${1:-$ROOT/current}")
case "$RELEASE" in "$ROOT"/releases/*) ;; *) echo 'Release fora do diretorio permitido.' >&2; exit 64;; esac
compose() {
    env -u BACKEND_IMAGE -u FRONTEND_IMAGE docker compose --env-file "$ROOT/shared/app.env" --env-file "$RELEASE/images.env" \
        -p salgados-production -f "$RELEASE/infra/production/compose.yaml" "$@"
}
isolated_compose() {
    local project=${1:?Informe projeto isolado}
    shift
    [[ $project =~ ^salgados-restore-[a-z0-9][a-z0-9-]*$ ]] || return 64
    env -u BACKEND_IMAGE -u FRONTEND_IMAGE docker compose --env-file "$ROOT/shared/app.env" --env-file "$RELEASE/images.env" \
        -p "$project" -f "$RELEASE/infra/production/compose.yaml" "$@"
}
load_backup_environment() {
    local key value
    while IFS='=' read -r key value || [[ -n $key ]]; do
        [[ -z $key || $key == \#* ]] && continue
        case "$key" in
            RESTIC_REPOSITORY|RESTIC_PASSWORD_FILE|AWS_ACCESS_KEY_ID|AWS_SECRET_ACCESS_KEY|AWS_DEFAULT_REGION|AWS_SESSION_TOKEN)
                export "$key=$value" ;;
            *) echo 'Variavel nao permitida no arquivo de backup.' >&2; exit 64 ;;
        esac
    done < "$ROOT/shared/backup.env"
    : "${RESTIC_REPOSITORY:?Defina o repositorio externo}"
    : "${RESTIC_PASSWORD_FILE:?Defina o arquivo da senha Restic}"
    test -s "$RESTIC_PASSWORD_FILE"
}
