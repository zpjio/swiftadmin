# SwiftAdmin

[![CI](https://github.com/zpjio/swiftadmin/actions/workflows/ci.yml/badge.svg)](https://github.com/zpjio/swiftadmin/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

一个基于 **Spring Boot 4 + Vue 3** 的轻量级后台管理系统脚手架，包含认证授权、RBAC 权限、系统管理、消息通知、定时任务、文件存储等常用能力，开箱即用，适合作为中后台项目的起点。

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

## 相比同类项目的优势

SwiftAdmin 的定位是 **模块化单体 + 少依赖 + 默认安全**，适合中小团队快速搭建、私有化交付和二次开发；如果未来需要拆分微服务，也可以按现有模块边界平滑演进。

| 维度 | SwiftAdmin | 典型微服务型后台框架 |
| --- | --- | --- |
| 运行形态 | 模块化单体，一个 Jar 启动 | 多个服务独立部署 |
| 外部依赖 | MySQL + Redis | 通常还会引入 Nacos、Gateway、Sentinel、XXL-Job 等 |
| 启动链路 | 单进程，无注册中心/配置中心等待 | 基础组件和服务需按顺序启动 |
| 部署成本 | 一个后端镜像 + 两个中间件 | 多镜像、多配置、服务编排 |
| 适用规模 | 中小团队、私有化项目 | 大型组织、强服务隔离场景 |

### 启动速度

- **依赖少**：只依赖 MySQL 和 Redis，不引入注册中心、配置中心、网关和分布式任务调度。
- **单进程**：所有业务模块打包为一个可执行 Jar，启动一次即可提供全部接口。
- **自动初始化**：Flyway 启动时自动建表、建索引并写入种子数据，无需手工导入 SQL。
- **实测参考**：GitHub Actions 标准 Runner 上，完整 Spring 上下文（含 Flyway、MySQL、Redis）启动约 **7.6s**。

### 查询速度

- **分页下推**：`paginateAs` 生成 `LIMIT` 分页 SQL，只返回当前页，不把全表加载进 JVM。
- **权限缓存**：用户权限解析结果缓存到 Redis，后续鉴权不重复查询角色/菜单关联表。
- **SQL 层数据权限**：`@DataScope` 在构造 QueryWrapper 时追加部门/用户条件，过滤由数据库完成。
- **轻量 ORM**：MyBatis-Flex 直接映射 SQL，没有 Hibernate 懒加载代理和隐式 N+1 查询。
- **索引与异步日志**：高频表建立索引，列表默认按时间排序；登录/操作日志异步落库，不阻塞业务请求。
- **实测参考**：本地开发机（JDK 21 / MySQL 8 / Redis 7）1 万用户数据下，用户分页接口 P95 约 **52ms**（含 JWT、会话、权限校验）。

> 性能数字与数据量、硬件、索引和 JVM 参数相关，以上仅作量级参考，建议按自己的数据规模压测。

### 安全默认值

- 默认只放行登录、刷新、验证码、开放文档和健康检查，其余接口一律要求认证。
- 未认证返回 401、无权限返回 403，统一 JSON 响应，前端可直接跳转登录页。
- 内置登录限流、失败锁定、密码过期与历史密码校验、强制改密。
- Actuator 仅匿名开放 health，其余端点需认证；CORS 使用白名单。

### 工程化与可维护性

- **模块化**：认证、系统、消息、任务、公共能力按 Maven 模块拆分，边界清晰。
- **数据库版本化**：Flyway 管理表结构与种子数据，环境之间可重复部署。
- **质量保障**：CI 覆盖后端集成测试（认证、权限、数据权限）和前端类型检查、构建。
- **现代技术栈**：Java 21、Spring Boot 4、Vue 3、Vite、TypeScript、Naive UI。
- **部署友好**：Docker Compose、健康检查、环境变量配置，支持 GraalVM Native 构建。

### 适用场景

- **适合**：中小团队、私有化交付、需要快速二次开发的管理后台。
- **不适合**：必须按微服务拆分、需要多语言服务治理的超大型组织。

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 后端 | Java 21、Spring Boot 4、Spring Security、MyBatis-Flex、Flyway、JJWT |
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
- Node.js 20+、pnpm 10（版本已通过 `frontend/package.json` 的 `packageManager` 钉死；corepack 直接可用：`corepack prepare pnpm@10 --activate`；`swiftadmin-web` 也可直接使用 npm）
- MySQL 8.0+、Redis 7+（或使用 Docker 启动）
- Docker / Docker Compose V2（`docker compose` 子命令）

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

## 跑测试

```bash
cd deploy/docker && docker compose up -d mysql redis   # compose 默认 root/root，与测试默认配置一致
make test        # = mvn test + 前端 type-check
```

没有外部 MySQL/Redis 也不用先起容器：`make test-backend`（或 `cd backend && mvn test`）时，
`IntegrationTestBase` 会检测到未设置 `DB_HOST`，自动用 Testcontainers 拉起临时 `mysql:8.0` +
`redis:7-alpine`，测试结束自动回收。需要 Docker（与 CI 无关，CI 用的是 services 环境变量模式）。

```bash
cd backend && mvn test   # Testcontainers 模式，本机有 Docker 即可零配置运行
```

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
