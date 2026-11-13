"""Generate disposable credentials for the isolated CI database, never for deployment."""
import os
import secrets
import sys

password = secrets.token_hex(32)
values = {
    "DB_NAME_SALGADOS_MYSQL": "salgados_ci",
    "DB_USER_MYSQL": "ci_app",
    "DB_PASSWORD_MYSQL": secrets.token_hex(32),
    "MYSQL_ROOT_PASSWORD": secrets.token_hex(32),
    "JWT_HMAC256_SECRET": secrets.token_hex(32),
    "JWT_ISSUER_SALGADOS_DA_LUCIA": "ci-local",
    "FRONTEND_PORT": "15500",
    "BACKEND_IMAGE": "salgados-backend:ci",
    "FRONTEND_IMAGE": "salgados-frontend:ci",
    "BOOTSTRAP_ADMIN_NAME": "CI",
    "BOOTSTRAP_ADMIN_USERNAME": "ci-admin",
    "BOOTSTRAP_ADMIN_PASSWORD": password,
    "SMOKE_ADMIN_USERNAME": "ci-admin",
    "SMOKE_ADMIN_PASSWORD": password,
    "SMOKE_ALLOW_WRITES": "true",
}
destination = sys.argv[1]
descriptor = os.open(destination, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
with os.fdopen(descriptor, "w", encoding="utf-8") as output:
    output.write("".join(f"{name}={value}\n" for name, value in values.items()))
