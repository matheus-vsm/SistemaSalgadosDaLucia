#!/usr/bin/env bash
set -euo pipefail
declare -A seen=()
while IFS='=' read -r name value || [[ -n $name ]]; do
    [[ -z $name || $name == \#* ]] && continue
    case "$name" in
        BACKEND_IMAGE|FRONTEND_IMAGE) ;;
        *) echo 'Variavel inesperada em images.env.' >&2; exit 64 ;;
    esac
    [[ ${seen[$name]:-0} == 0 ]] || { echo 'Imagem duplicada em images.env.' >&2; exit 64; }
    [[ $value =~ ^ghcr.io/[a-z0-9._/-]+@sha256:[0-9a-f]{64}$ ]] || { echo 'Use imagens GHCR por digest, nunca tags mutaveis.' >&2; exit 64; }
    seen[$name]=1
done < "${1:?Informe images.env}"
[[ ${seen[BACKEND_IMAGE]:-0} == 1 && ${seen[FRONTEND_IMAGE]:-0} == 1 ]]
