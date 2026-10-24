# SwiftAdmin

[![CI](https://github.com/zpjio/swiftadmin/actions/workflows/ci.yml/badge.svg)](https://github.com/zpjio/swiftadmin/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

一个基于 **Spring Boot 3 + Vue 3** 的轻量级后台管理系统脚手架，包含认证授权、RBAC 权限、系统管理、消息通知、定时任务、文件存储等常用能力，开箱即用，适合作为中后台项目的起点。

> 前后端分离，后端多模块分层，前端采用 Naive UI，配置全部通过环境变量注入，无硬编码密钥。

## 功能特性

- **认证授权**：账号密码登录、JWT 双 Token（访问 + 刷新）、登录失败锁定、密码策略与历史密码校验、图形/短信验证码
- **RBAC 权限**：用户、角色、菜单、部门、数据权限，前端动态路由与按钮级权限
- **系统管理**：字典管理、参数配置、文件上传、操作日志、登录日志、在线用户
- **消息中心**：站内信 + 阿里云 / 腾讯云短信，可扩展的通知渠道
- **定时任务**：任务管理、Cron 表达式、执行日志、手动触发
- **文件存储**：本地存储与 S3 兼容对象存储（阿里云 OSS / MinIO 等）可切换
- **社交登录**：企业微信、钉钉、飞书 OAuth 对接骨架
- **工程化**：Flyway 数据库版本管理、SpringDoc OpenAPI 文档、Docker Compose 一键部署、GraalVM Native 构建支持

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 后端 | Java 21、Spring Boot 3.5、Spring Security、MyBatis-Flex、Flyway、JJWT |
| 数据库 | MySQL 8、Redis 7 |
| 前端 | Vue 3、TypeScript、Vite、Naive UI、Pinia、Vue Router、ECharts、Tailwind CSS |
| 构建/部署 | Maven、pnpm、Docker Compose、Nginx |

## 目录结构

```
swiftadmin/
├── backend/                        # 后端 Maven 多模块工程
│   ├── swiftadmin-common/          # 公共模块（core / mybatis / security）
│   ├── swiftadmin-system/          # 系统管理（用户、角色、菜单、部门、字典…）
│   ├── swiftadmin-auth/            # 认证授权（登录、JWT、社交登录）
│   ├── swiftadmin-message/         # 消息中心（站内信、短信）
│   ├── swiftadmin-schedule/        # 定时任务
│   └── swiftadmin-boot/            # 启动模块与配置、Flyway 脚本
├── frontend/                       # 前端 pnpm monorepo
│   └── apps/admin-web/             # 主管理后台
├── swiftadmin-web/                 # 独立前端项目（可单独运行，npm 管理）
├── deploy/docker/                  # Docker Compose 与镜像构建
├── run-dev.sh                      # 本地一键启动脚本
└── Makefile                        # 常用命令集合
```

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- Node.js 18+、pnpm 9+（`swiftadmin-web` 也可直接使用 npm）
- MySQL 8.0+、Redis 7+（或使用 Docker 启动）
- Docker / Docker Compose（可选）

### 1. 启动依赖服务

```bash
cd deploy/docker
docker compose up -d mysql redis
```

或使用本地已安装的 MySQL / Redis。

### 2. 启动后端

```bash
# 可选：复制环境变量模板并按需修改
cp backend/.env.local.example backend/.env.local

# 方式一：使用脚本（自动加载 .env.local）
./run-dev.sh

# 方式二：Maven 直接启动
cd backend
mvn spring-boot:run -pl swiftadmin-boot
```

启动后：

- 服务地址：<http://localhost:8080>
- 接口文档：<http://localhost:8080/swagger-ui.html>

数据库表结构与初始数据由 Flyway 在首次启动时自动创建。

### 3. 启动前端

```bash
cd frontend
pnpm install
pnpm dev
```

访问 <http://localhost:5173>。

独立前端 `swiftadmin-web`：

```bash
cd swiftadmin-web
npm install
npm run dev
```

> 两个前端默认端口均为 5173，如需同时运行请修改其中一个的端口。

### 默认账号

| 账号 | 密码 |
| --- | --- |
| `admin` | `admin123` |

> 首次部署后请立即修改默认密码。

## 配置说明

所有敏感配置均通过环境变量注入，参考 `backend/.env.local.example`。

| 变量 | 说明 | 必填 |
| --- | --- | --- |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | MySQL 连接信息 | 开发环境有默认值 |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号密码 | 是 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 连接信息 | 是 |
| `JWT_SECRET` | JWT 签名密钥（≥ 32 字节） | 生产必填 |
| `SMS_ALIYUN_*` / `SMS_TENCENT_*` | 短信服务配置 | 使用短信时必填 |
| `OSS_*` | 对象存储配置 | 使用 OSS 时必填 |
| `CORS_ALLOWED_ORIGINS` | 允许跨域的前端源，逗号分隔；生产环境必须显式配置 | 生产必填 |
| `WECOM_*` / `DINGTALK_*` / `FEISHU_*` | 社交登录配置 | 可选 |
| `SOCIAL_TOKEN_SECRET` | 社交 Token 加密密钥（≥ 32 字节） | 生产必填 |

前端配置位于 `frontend/apps/admin-web/.env.*` 与 `swiftadmin-web/.env.*`：

```dotenv
VITE_APP_TITLE=SwiftAdmin
VITE_API_BASE_URL=http://localhost:8080/api
```

## 常用命令

```bash
make dev-backend     # 启动后端
make dev-frontend    # 启动前端
make build-all       # 构建前后端
make docker-up       # Docker Compose 启动
make clean           # 清理构建产物
```

## 部署

```bash
cd deploy/docker
docker compose up -d --build
```

- 前端：Nginx 静态站点（80 / 443）
- 后端：Spring Boot 容器（8080）
- 数据库表结构由 Flyway 在应用启动时自动迁移

可在 `deploy/docker` 下创建 `.env` 覆盖默认配置（无需修改 compose 文件）：

```dotenv
MYSQL_ROOT_PASSWORD=change-me
DB_USERNAME=swiftadmin
DB_PASSWORD=change-me
JWT_SECRET=<32 字节以上随机字符串>
SOCIAL_TOKEN_SECRET=<32 字节以上随机字符串>
CORS_ALLOWED_ORIGINS=https://admin.example.com
```

生产环境请务必：

1. 设置强随机 `JWT_SECRET`、`SOCIAL_TOKEN_SECRET`；
2. 修改 `docker-compose.yml` 中的数据库与 Redis 密码；
3. 关闭 Swagger 文档（prod profile 已默认关闭）；
4. 配置 HTTPS 与反向代理。

## 参与贡献

欢迎提交 Issue 和 Pull Request。提交前请确保代码可正常编译，并遵循现有代码风格。

## 开源协议

本项目基于 [MIT License](LICENSE) 开源。
