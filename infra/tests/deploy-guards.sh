#!/usr/bin/env bash
set -euo pipefail
repository=$(cd "$(dirname "$0")/../.." && pwd)
fixture=$(mktemp -d)
trap 'rm -rf -- "$fixture"' EXIT
export DEPLOY_ROOT="$fixture"
mkdir -p "$fixture/releases/test/infra/production" "$fixture/shared" "$fixture/bin"
touch "$fixture/releases/test/images.env" "$fixture/releases/test/infra/production/compose.yaml" "$fixture/shared/app.env"
export CALL_LOG="$fixture/calls"
cat > "$fixture/bin/docker" <<'MOCK'
#!/bin/sh
printf '%s\n' "$*" >> "$CALL_LOG"
MOCK
chmod +x "$fixture/bin/docker"
export PATH="$fixture/bin:$PATH"
bash "$repository/infra/deploy/deploy.sh" --check "$fixture/releases/test"
if bash "$repository/infra/deploy/deploy.sh" --execute "$fixture/releases/test"; then
    echo 'Deploy nao deveria executar sem production-ready.' >&2
    exit 1
fi
if grep -E ' (pull|up|exec)( |$)' "$CALL_LOG"; then
    echo 'Comando de mutacao executado sem habilitacao.' >&2
    exit 1
fi
if bash "$repository/infra/deploy/deploy.sh" --execute /tmp; then exit 1; fi
if bash "$repository/infra/deploy/receive.sh" invalid invalid invalid; then exit 1; fi
echo 'PASS: validacao sem mutacao, bloqueio sem habilitacao e rejeicao de caminhos/referencias invalidos.'
