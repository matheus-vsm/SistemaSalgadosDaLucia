#!/usr/bin/env bash
set -euo pipefail
project=${1:?Informe salgados-restore-NOME}
[[ $project =~ ^salgados-restore-[a-z0-9][a-z0-9-]*$ ]]
set -- "${2:-${DEPLOY_ROOT:-/opt/salgados}/current}"
# shellcheck source=infra/production/common.sh
source "$(dirname "$0")/common.sh"
isolated_compose "$project" up -d --wait --wait-timeout 240 backend
read -r -p 'Username do banco restaurado: ' RESTORE_USERNAME
read -r -s -p 'Senha: ' RESTORE_PASSWORD
printf '\n'
export RESTORE_USERNAME RESTORE_PASSWORD
trap 'unset RESTORE_USERNAME RESTORE_PASSWORD' EXIT
jq -n '{username:env.RESTORE_USERNAME,senha:env.RESTORE_PASSWORD}' |
    isolated_compose "$project" exec -T backend curl --fail --silent --show-error --output /dev/null \
        --write-out 'Login restaurado: HTTP %{http_code}\n' --request POST --header 'Content-Type: application/json' \
        --data-binary @- http://127.0.0.1:8080/api/salgados-da-lucia-kojima/autenticacao/login
