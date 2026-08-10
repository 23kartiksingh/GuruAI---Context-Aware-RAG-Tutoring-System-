# ================================================================
# frontend — Dockerfile
# ================================================================
# Multi-stage build:
#   1. "builder" — npm install + vite build. Vite inlines VITE_* env vars
#      into the JS bundle at BUILD time (not runtime), so they have to
#      arrive as Docker build args here — setting them under `environment:`
#      in docker-compose.yml would have no effect on an already-built
#      static bundle.
#   2. "runtime" — serves the built static files with nginx. No Node.js
#      (or the app's source) ships in the final image.
# ================================================================

# ---- Stage 1: Build ----
FROM node:22-alpine AS builder
WORKDIR /build

# Build args → env vars Vite reads at build time (see vite-env.d.ts).
ARG VITE_API_BASE_URL=http://localhost:8080
ARG VITE_WS_BASE_URL=ws://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_WS_BASE_URL=$VITE_WS_BASE_URL

# Copy manifests first so `npm install` is cached separately from source
# changes — most rebuilds only touch src/, not package.json.
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .
RUN npm run build

# ---- Stage 2: Runtime ----
FROM nginx:1.27-alpine AS runtime
COPY --from=builder /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
