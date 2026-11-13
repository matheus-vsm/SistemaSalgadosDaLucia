#!/usr/bin/env bash
set -euo pipefail
root=$(realpath "${DEPLOY_ROOT:-/opt/salgados}")
previous=$(readlink -f "$root/previous")
test -d "$previous" || { echo 'Nao ha release anterior registrada.' >&2; exit 1; }
echo 'Reversao da aplicacao: exige schema compativel; nao reverte migracoes ou restaura dados.'
bash "$previous/infra/deploy/deploy.sh" --execute "$previous"
