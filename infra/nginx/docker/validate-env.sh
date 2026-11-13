#!/bin/sh
set -eu
: "${BACKEND_URL:?Defina BACKEND_URL com protocolo, host e porta do backend}"
if ! printf '%s' "$BACKEND_URL" | grep -Eq '^https?://[a-zA-Z0-9._-]+(:[0-9]{1,5})?$'; then
    echo 'BACKEND_URL invalida: use http(s)://host:porta, sem caminho ou barra final.' >&2
    exit 1
fi
