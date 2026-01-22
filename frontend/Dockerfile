# -------- Builder --------
FROM node:alpine AS builder

RUN npm install -g pnpm

WORKDIR /app

ENV PNPM_CONFIG_UPDATE_CHECK=false

COPY package.json pnpm-lock.yaml ./

RUN pnpm install --frozen-lockfile

COPY . .

RUN pnpm run build

# -------- Runtime --------
FROM nginx:alpine

COPY --from=builder /app/dist /usr/share/nginx/html

COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]
