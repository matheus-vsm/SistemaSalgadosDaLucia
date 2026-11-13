FROM nginx:1.28-alpine@sha256:a8b39bd9cf0f83869a2162827a0caf6137ddf759d50a171451b335cecc87d236
COPY --chmod=755 infra/nginx/docker/validate-env.sh /docker-entrypoint.d/19-validate-env.sh
RUN sed -i 's/\r$//' /docker-entrypoint.d/19-validate-env.sh
COPY infra/nginx/docker/default.conf.template /etc/nginx/templates/default.conf.template
ENV NGINX_ENVSUBST_FILTER="^(BACKEND_URL)$"
COPY frontend /usr/share/nginx/html
EXPOSE 80
