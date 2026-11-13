FROM nginx:1.30.6-alpine@sha256:901e593c95dc85f5a729f5765516321366ef4ac401821431d408897e95287d0a
COPY --chmod=755 infra/nginx/docker/validate-env.sh /docker-entrypoint.d/19-validate-env.sh
RUN sed -i 's/\r$//' /docker-entrypoint.d/19-validate-env.sh
COPY infra/nginx/docker/default.conf.template /etc/nginx/templates/default.conf.template
ENV NGINX_ENVSUBST_FILTER="^(BACKEND_URL)$"
COPY frontend /usr/share/nginx/html
EXPOSE 80
