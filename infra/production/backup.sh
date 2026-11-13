#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=infra/production/common.sh
source "$(dirname "$0")/common.sh"
load_backup_environment
exec 8> "$ROOT/backup.lock"
flock -n 8 || { echo 'Outro backup esta em andamento.' >&2; exit 1; }
temporary=$(mktemp)
trap 'rm -f -- "$temporary"' EXIT
# O dump deve terminar com sucesso antes de criar um snapshot Restic.
# As variaveis abaixo sao expandidas dentro do container, nao no host.
# shellcheck disable=SC2016
compose exec -T db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysqldump --single-transaction --no-tablespaces --set-gtid-purged=OFF -u "$MYSQL_USER" "$MYSQL_DATABASE"' > "$temporary"
test -s "$temporary"
restic backup --stdin --stdin-filename mysql.sql --host salgados-production --tag salgados-mysql < "$temporary"
restic check
restic forget --host salgados-production --tag salgados-mysql --keep-daily 7 --keep-weekly 4 --keep-monthly 3 --prune
date -u +%FT%TZ > "$ROOT/shared/last-backup"
echo 'Backup concluido e repositorio verificado.'
