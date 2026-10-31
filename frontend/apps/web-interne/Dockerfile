# syntax=docker/dockerfile:1.7
# Image de l'application web interne CSE : construction Vite puis service par Nginx non-root en HTTPS.
# Contexte = frontend/ ; contexte additionnel « nginx » = infra/nginx (configuration du frontal) :
#   docker build -f frontend/apps/web-interne/Dockerfile --build-context nginx=infra/nginx \
#     --build-arg VITE_KEYCLOAK_URL=https://auth.cse.sn -t cse/web-interne frontend
# Les variables VITE_* sont figées dans le bundle à la construction (Vite) : une image par environnement.

# ---- Étape 1 : dépendances (lockfile figé, uniquement le paquet @cse/web-interne) ----
FROM node:20-alpine AS deps
ARG PNPM_VERSION=10.28.2
RUN corepack enable && corepack prepare pnpm@${PNPM_VERSION} --activate
WORKDIR /src
# Manifestes seuls d'abord : la couche d'installation reste en cache tant que les dépendances ne changent pas.
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY apps/web-interne/package.json apps/web-interne/
COPY apps/portail-fournisseur/package.json apps/portail-fournisseur/
COPY packages/ packages/
RUN --mount=type=cache,id=pnpm-store,target=/root/.local/share/pnpm/store \
    pnpm install --frozen-lockfile --filter @cse/web-interne...

# ---- Étape 2 : construction du bundle ----
FROM deps AS build
ARG VITE_API_BASE_URL=/api
ARG VITE_KEYCLOAK_URL=http://localhost:8180
ARG VITE_KEYCLOAK_REALM=cse
ARG VITE_KEYCLOAK_CLIENT_ID=cse-web-interne
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL} \
    VITE_KEYCLOAK_URL=${VITE_KEYCLOAK_URL} \
    VITE_KEYCLOAK_REALM=${VITE_KEYCLOAK_REALM} \
    VITE_KEYCLOAK_CLIENT_ID=${VITE_KEYCLOAK_CLIENT_ID}
COPY apps/web-interne/ apps/web-interne/
# Le contrôle de types (tsc) et les tests sont la responsabilité de la CI (make front-verify) ;
# l'image ne fait que produire le bundle.
RUN pnpm --filter @cse/web-interne exec vite build \
 # Les cartes de sources ne sont pas publiées en production.
 && find apps/web-interne/dist -name '*.map' -delete \
 # Précompression pour gzip_static (aucun coût CPU à la requête).
 && find apps/web-interne/dist -type f \( -name '*.js' -o -name '*.css' -o -name '*.html' -o -name '*.svg' -o -name '*.json' \) \
      -size +1k -exec gzip -9 -k {} \;
COPY --from=nginx web-interne.conf csp-hashes.mjs /nginx/
RUN node /nginx/csp-hashes.mjs apps/web-interne/dist /nginx/web-interne.conf

# ---- Étape 3 : service HTTPS (utilisateur nginx UID 101, ports > 1024) ----
FROM nginxinc/nginx-unprivileged:1.27-alpine
ENV NGINX_HTTP_PORT=8080 \
    NGINX_HTTPS_PORT=8443 \
    PUBLIC_HTTPS_PORT=8443 \
    GATEWAY_UPSTREAM=ms-gateway:8080 \
    KEYCLOAK_PUBLIC_URL=http://localhost:8180 \
    NGINX_SSE_TIMEOUT=3600s \
    NGINX_ENVSUBST_FILTER="^(NGINX_HTTP_PORT|NGINX_HTTPS_PORT|PUBLIC_HTTPS_PORT|GATEWAY_UPSTREAM|KEYCLOAK_PUBLIC_URL|NGINX_SSE_TIMEOUT)$"
USER root
RUN rm -f /etc/nginx/conf.d/default.conf
USER 101
COPY --from=build --chown=101:101 /nginx/web-interne.conf /etc/nginx/templates/default.conf.template
COPY --from=build --chown=101:101 /src/apps/web-interne/dist/ /usr/share/nginx/html/
EXPOSE 8080 8443
HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- "http://127.0.0.1:${NGINX_HTTP_PORT}/healthz" >/dev/null || exit 1
