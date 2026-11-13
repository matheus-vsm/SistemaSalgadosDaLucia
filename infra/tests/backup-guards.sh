#!/usr/bin/env bash
set -euo pipefail
repository=$(cd "$(dirname "$0")/../.." && pwd)
fixture=$(mktemp -d)
trap 'rm -rf -- "$fixture"' EXIT
export DEPLOY_ROOT="$fixture"
mkdir -p "$fixture/releases/test" "$fixture/shared" "$fixture/bin"
head -c 32 /dev/urandom | base64 > "$fixture/key"
printf 'RESTIC_REPOSITORY=%s\nRESTIC_PASSWORD_FILE=%s\n' "$fixture/repository" "$fixture/key" > "$fixture/shared/backup.env"
export CALL_LOG="$fixture/calls"
cat > "$fixture/bin/docker" <<'MOCK'
#!/bin/sh
test "${DUMP_FAIL:-false}" = false || exit 9
printf '%s\n' '-- Dump de teste completo'
MOCK
cat > "$fixture/bin/restic" <<'MOCK'
#!/bin/sh
printf '%s\n' "$1" >> "$CALL_LOG"
if test "$1" = backup; then cat >/dev/null; fi
if test "$1" = check && test "${CHECK_FAIL:-false}" = true; then exit 7; fi
MOCK
chmod +x "$fixture/bin/"*
export PATH="$fixture/bin:$PATH"
export DUMP_FAIL=true
if bash "$repository/infra/production/backup.sh" "$fixture/releases/test"; then exit 1; fi
test ! -e "$CALL_LOG"
export DUMP_FAIL=false CHECK_FAIL=true
if bash "$repository/infra/production/backup.sh" "$fixture/releases/test"; then exit 1; fi
test ! -e "$fixture/shared/last-backup"
export CHECK_FAIL=false
bash "$repository/infra/production/backup.sh" "$fixture/releases/test"
test -s "$fixture/shared/last-backup"
if bash "$repository/infra/production/restore.sh" 12345678 salgados-production; then exit 1; fi
printf 'EVIL=invalid\n' > "$fixture/shared/backup.env"
if bash "$repository/infra/production/backup.sh" "$fixture/releases/test"; then exit 1; fi
echo 'PASS: dump incompleto nao cria snapshot, check falho nao confirma backup, restauracao nao aceita producao e ambiente rejeita variaveis arbitrarias.'
