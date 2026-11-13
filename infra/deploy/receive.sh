#!/usr/bin/env bash
set -euo pipefail
umask 077
sha=${1:?Informe o commit}
backend=${2:?Informe a imagem backend por digest}
frontend=${3:?Informe a imagem frontend por digest}
[[ $sha =~ ^[0-9a-f]{40}$ ]]
for image in "$backend" "$frontend"; do
    [[ $image =~ ^ghcr.io/[a-z0-9._/-]+@sha256:[0-9a-f]{64}$ ]] || exit 64
done
root=/opt/salgados
test -f "$root/shared/production-ready" || { echo 'Producao ainda nao habilitada na VPS.' >&2; exit 1; }
archive="$root/incoming/$sha.tar.gz"
release="$root/releases/$sha"
test -f "$archive"
test ! -e "$release" || { echo 'Esta versao ja possui diretorio; use deploy.sh para repetir.' >&2; exit 1; }
if tar -tzf "$archive" | grep -E '(^/|(^|/)\.\.(/|$))' >/dev/null; then
    echo 'Arquivo de release contem caminho invalido.' >&2
    exit 1
fi
mkdir -p "$release"
tar -xzf "$archive" -C "$release" --no-same-owner --no-same-permissions
printf 'BACKEND_IMAGE=%s\nFRONTEND_IMAGE=%s\n' "$backend" "$frontend" > "$release/images.env"
find "$release/infra" -type f -name '*.sh' -exec chmod u+x {} +
bash "$release/infra/deploy/deploy.sh" --execute "$release"
