# SwiftAdmin

[![CI](https://github.com/zpjio/swiftadmin/actions/workflows/ci.yml/badge.svg)](https://github.com/zpjio/swiftadmin/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

基于 Spring Boot 4 与 Vue 3 的轻量后台脚手架：RBAC、JWT、短信、定时任务、文件存储。模块化单体，依赖 MySQL 与 Redis。

## 功能

- 账号密码 / 短信 / 企业微信、钉钉、飞书登录，JWT 访问令牌与刷新令牌
- 用户、角色、菜单、部门、数据权限
- 字典、参数、文件、操作日志、登录日志、在线用户
- 站内信与短信渠道，Cron 定时任务
- 本地磁盘与 S3 兼容对象存储
- Flyway 迁移、SpringDoc（仅开发环境）、Docker Compose

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 后端 | Java 21、Spring Boot 4.1、Spring Security、MyBatis-Flex、Flyway、JJWT |
| 数据 | MySQL 8、Redis 7 |
| 前端 | Vue 3、TypeScript、Vite、Naive UI、Pinia、Vue Router |
| 构建 | Maven、pnpm、Docker Compose |

## 目录

```
backend/            Maven 多模块（common、system、auth、message、schedule、boot）
frontend/           pnpm 工作区，主应用在 apps/admin-web
swiftadmin-web/     可单独用 npm 运行的另一份前端
deploy/docker/      Compose 与镜像
```

## 快速开始

环境：JDK 21、Maven 3.9+、Node.js 20+、pnpm 10（`frontend/package.json` 的 `packageManager`）、MySQL 8、Redis 7。只起依赖时可用 Docker Compose V2。

```bash
cd deploy/docker
docker compose up -d mysql redis
```

后端（默认 `dev` 配置，库账号默认 `root` / `root`，JWT 有仅供开发使用的默认密钥）：

```bash
cp backend/.env.local.example backend/.env.local   # 可选
./run-dev.sh
# 或：cd backend && mvn spring-boot:run -pl swiftadmin-boot
```

- 接口：<http://localhost:8080>
- 文档（dev）：<http://localhost:8080/swagger-ui.html>
- 首次启动由 Flyway 建表并写入种子数据

主前端：

```bash
cd frontend
pnpm install
pnpm dev
```

打开 <http://localhost:5173>。`swiftadmin-web` 使用 `npm install && npm run dev`，默认端口同样是 5173，同时运行时请改其中一个端口。

前端 API 根地址：`frontend/apps/admin-web/.env.*` 与 `swiftadmin-web/.env.*` 中的 `VITE_API_BASE_URL`（默认 `http://localhost:8080/api`）。

| 账号 | 密码 |
| --- | --- |
| `admin` | `admin123` |

该账号只要还是种子密码，登录后只会拿到临时令牌，必须先改密才能进入系统。已经改过密码的环境不会被重新要求修改。密码接口使用 JSON 请求体。

定时任务要调用的 Bean 方法必须标注 `@ScheduledJobTarget`（标在类上则该类的 public 方法都可调度）。预置的日志清理和社交 Token 刷新已经标注。

`/actuator/health` 匿名可访问。`/actuator/metrics` 和 `/actuator/prometheus` 需要权限 `monitor:metrics`、角色 `admin`，或超级管理员。

## 配置

敏感项用环境变量，示例见 `backend/.env.local.example`。生产 profile（`application-prod.yml`）不提供数据库口令和 `JWT_SECRET` 的默认值。

| 变量 | 说明 |
| --- | --- |
| `DB_HOST` `DB_PORT` `DB_NAME` `DB_USERNAME` `DB_PASSWORD` | MySQL。dev 有默认值 |
| `REDIS_HOST` `REDIS_PORT` `REDIS_PASSWORD` | Redis |
| `JWT_SECRET` | 至少 32 字节。生产必填 |
| `SOCIAL_TOKEN_SECRET` | 社交令牌加密密钥，至少 32 字节。生产必填 |
| `CORS_ALLOWED_ORIGINS` | 逗号分隔的前端源。生产必填；`*` 时不携带凭据 |
| `TRUST_FORWARDED_HEADERS` | 默认 `false`，忽略 `X-Forwarded-For`。仅当反向代理会**覆盖**该头时设为 `true`（Compose 里的 Nginx 已覆盖，并默认打开） |
| `SMS_*` / `OSS_*` / `WECOM_*` / `DINGTALK_*` / `FEISHU_*` | 用到对应能力时再填 |

上传默认只允许常见图片、办公文档、文本和压缩包。需要放宽时设置 `file.storage.allowed-extensions`（环境变量 `FILE_STORAGE_ALLOWED_EXTENSIONS`，逗号分隔；留空则只受禁止列表约束）。

## 测试

```bash
cd deploy/docker && docker compose up -d mysql redis
make test
```

未设置 `DB_HOST` 时，`cd backend && mvn test` 会用 Testcontainers 启动临时 MySQL 与 Redis（需要 Docker）。

## 部署

完整栈（后端为 `prod`）必须先准备密钥，否则 Compose 会拒绝启动：

```bash
cp deploy/docker/.env.example deploy/docker/.env
# 编辑 JWT_SECRET、SOCIAL_TOKEN_SECRET 等
cd deploy/docker && docker compose up -d --build
```

表结构仍由 Flyway 迁移。请同时修改数据库口令、限制 CORS，并通过 HTTPS 暴露前端。

## 许可证

[MIT License](LICENSE)
