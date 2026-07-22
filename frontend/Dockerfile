# ─────────────────────────────────────────────
# Stage 1: Build the React app
# ─────────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files first (layer cache)
COPY package.json package-lock.json ./
RUN npm ci --silent

# Copy source and build
COPY . .

# Build with the production API URL pointing to the backend container
ARG VITE_API_BASE_URL=http://localhost:8080/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build

# ─────────────────────────────────────────────
# Stage 2: Serve with Nginx
# ─────────────────────────────────────────────
FROM nginx:alpine

# Copy the built React files
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom Nginx config (handles React Router + API proxy)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
