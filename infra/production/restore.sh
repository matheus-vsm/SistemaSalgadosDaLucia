#!/usr/bin/env bash
set -euo pipefail
snapshot=${1:?Informe o ID do snapshot Restic}
project=${2:?Informe salgados-restore-NOME para criar banco isolado}
[[ $snapshot =~ ^[0-9a-f]{8,64}$ ]]
[[ $project =~ ^salgados-restore-[a-z0-9][a-z0-9-]*$ ]] || { echo 'Restauracao permitida somente em projeto isolado salgados-restore-NOME.' >&2; exit 64; }
# common.sh recebe o terceiro argumento como release, ou current por padrao.
set -- "${3:-${DEPLOY_ROOT:-/opt/salgados}/current}"
# shellcheck source=infra/production/common.sh
source "$(dirname "$0")/common.sh"
load_backup_environment
if docker volume inspect "${project}_mysql-data" >/dev/null 2>&1; then
    echo 'Volume de destino ja existe; nao sera sobrescrito.' >&2
    exit 1
fi
temporary=$(mktemp)
trap 'rm -f -- "$temporary"' EXIT
restic dump "$snapshot" mysql.sql > "$temporary"
test -s "$temporary"
isolated_compose "$project" up -d --wait --wait-timeout 240 db
# Variaveis do ambiente interno do MySQL.
# shellcheck disable=SC2016
isolated_compose "$project" exec -T db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql -u "$MYSQL_USER" "$MYSQL_DATABASE"' < "$temporary"
# shellcheck disable=SC2016
isolated_compose "$project" exec -T db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql -u "$MYSQL_USER" "$MYSQL_DATABASE" -e "SELECT COUNT(*) AS migracoes FROM flyway_schema_history WHERE success=1; SELECT COUNT(*) AS usuarios FROM usuarios;"'
echo "Restauracao em $project concluida. Valide dados e login antes de remover o ambiente isolado. Producao nao foi alterada."
