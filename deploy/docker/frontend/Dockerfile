# 构建阶段
# 构建上下文为 frontend/ 目录（pnpm workspace 根目录）
FROM node:22-alpine AS builder

WORKDIR /app

# 与 frontend/package.json 的 packageManager 钉死版本保持一致，避免升降级导致锁文件解析差异
RUN npm install -g pnpm@10.34.8

# 先复制依赖清单，复用安装缓存
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY apps/admin-web/package.json apps/admin-web/

# pnpm store 走 BuildKit 缓存挂载
RUN --mount=type=cache,target=/root/.local/share/pnpm/store \
    pnpm install --frozen-lockfile

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
