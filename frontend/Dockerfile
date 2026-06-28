# Image du POSTE AGA : build Vite (node) → service statique nginx.
# IMPORTANT : le contexte de build doit être la RACINE du dépôt (le poste importe la couche
# partagée shared/ via des alias relatifs ../shared). Voir docker-compose.full.yml (context: .).

# --- build ---
FROM node:20-alpine AS build
WORKDIR /repo
# Couche partagée + front (le .dockerignore exclut node_modules/dist/.git).
COPY shared ./shared
COPY frontend ./frontend
WORKDIR /repo/frontend
# Mode/URL d'API au BUILD (Vite inline les VITE_* à la compilation). Surcharge possible par domaine.
ARG VITE_API_MODE=real
ARG VITE_API_BASE_URL=
ARG VITE_DECSIN_MODE=
ARG VITE_SOUSCRIPTION_MODE=
ARG VITE_RECO_MODE=
ENV VITE_API_MODE=$VITE_API_MODE \
    VITE_API_BASE_URL=$VITE_API_BASE_URL \
    VITE_DECSIN_MODE=$VITE_DECSIN_MODE \
    VITE_SOUSCRIPTION_MODE=$VITE_SOUSCRIPTION_MODE \
    VITE_RECO_MODE=$VITE_RECO_MODE
RUN npm install && npm run build

# --- runtime (nginx) ---
FROM nginx:1.27-alpine AS runtime
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /repo/frontend/dist /usr/share/nginx/html
EXPOSE 80
