# Build stage: compila la SPA con Vite
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
# Vacío = la SPA llama a rutas relativas (/api/...) que nginx reenvía a la API.
ARG VITE_API_URL=""
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

# Runtime stage: nginx sirve los estáticos y hace de proxy hacia Spring Boot
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
# Host:puerto de la API dentro de la red de Docker Compose
ENV API_UPSTREAM=app:8080
EXPOSE 80
HEALTHCHECK --interval=10s --timeout=3s --retries=5 CMD wget -qO- http://127.0.0.1/healthz || exit 1
