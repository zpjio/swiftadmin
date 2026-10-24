# 构建阶段
# 构建上下文为 frontend/ 目录（pnpm workspace 根目录）
FROM node:22-alpine AS builder

WORKDIR /app

RUN npm install -g pnpm

# 先复制依赖清单，复用安装缓存
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY apps/admin-web/package.json apps/admin-web/

RUN pnpm install --frozen-lockfile

# 复制源代码并构建
COPY apps/admin-web apps/admin-web
RUN pnpm --filter admin-web build

# 生产阶段（Nginx）
FROM nginx:alpine

# 复制构建产物（自定义 nginx.conf 由 docker-compose 挂载）
COPY --from=builder /app/apps/admin-web/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -qO- http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
