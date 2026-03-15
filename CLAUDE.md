# SwiftAdmin 项目指南

## 权限配置

授权 Claude 对本项目所有文件拥有完整的编辑、删除、执行权限。

## 技术栈

### 后端
- **Java 17** + **Spring Boot 3.5.x**
- **MyBatis-Flex 1.11.x** (ORM)
- **Spring Security** + **JWT** (认证授权)
- **SpringDoc OpenAPI** (API 文档)
- **Lombok** (简化代码)

### 前端
- **Vue 3** + **TypeScript**
- **Naive UI** (组件库)
- **Pinia** (状态管理)
- **Vue Router** (路由)
- **Vite** (构建工具)

## 项目结构

```
swiftadmin/
├── backend/                           # 后端项目
│   ├── swiftadmin-boot/               # 启动模块 (入口、配置)
│   ├── swiftadmin-auth/               # 认证模块 (登录、JWT、社交登录)
│   │   ├── swiftadmin-auth-api/       # API 层 (DTO、Service 接口)
│   │   └── swiftadmin-auth-biz/       # 业务层 (Controller、Service 实现)
│   ├── swiftadmin-system/             # 系统模块 (用户、角色、权限、字典)
│   │   ├── swiftadmin-system-api/     # API 层
│   │   └── swiftadmin-system-biz/     # 业务层
│   ├── swiftadmin-message/            # 消息模块 (站内信、消息渠道)
│   │   ├── swiftadmin-message-api/
│   │   └── swiftadmin-message-biz/
│   └── swiftadmin-common/             # 公共模块
│       ├── swiftadmin-common-core/    # 核心工具 (Result、Exception、Utils)
│       ├── swiftadmin-common-security/# 安全模块 (注解、切面、认证)
│       └── swiftadmin-common-mybatis/ # MyBatis 配置 (分页、数据权限)
├── frontend/
│   └── apps/admin-web/                # 管理后台
│       └── src/
│           ├── api/                   # API 请求
│           ├── views/                 # 页面组件
│           ├── components/            # 公共组件
│           ├── composables/           # 组合式函数
│           ├── store/                 # Pinia 状态
│           └── router/                # 路由配置
└── deploy/                            # 部署配置
```

## 后端分层架构

```
Controller → Service (接口) → ServiceImpl → Mapper → Entity
     ↓           ↓
   DTO/VO     IService<T>
```

### 层级职责
| 层级 | 包路径 | 职责 |
|------|--------|------|
| Entity | `api/entity` | 数据库实体，对应表结构 |
| DTO | `api/dto` | 数据传输对象，接收请求参数 |
| VO | `api/vo` | 视图对象，返回响应数据 |
| Service | `api/service` | 服务接口，继承 `IService<T>` |
| ServiceImpl | `biz/service/impl` | 服务实现，继承 `ServiceImpl<M, T>` |
| Mapper | `biz/mapper` | MyBatis-Flex Mapper 接口 |
| Controller | `biz/controller` | REST API 控制器 |

## 编码规范

### Java 非空判断

```java
// 字符串非空
import org.springframework.util.StringUtils;
if (StringUtils.hasText(str)) { }

// 集合非空
import org.springframework.util.CollectionUtils;
if (!CollectionUtils.isEmpty(list)) { }

// 对象非空
import org.springframework.util.ObjectUtils;
if (!ObjectUtils.isEmpty(obj)) { }
```

### 命名规范

| 类型 | 命名规则 | 示例 |
|------|----------|------|
| Entity | `Sys` + 表名 | `SysUser`, `SysRole` |
| DTO | 实体名 + `DTO` | `UserDTO`, `RoleDTO` |
| VO | 实体名 + `VO` | `UserVO`, `RoleVO` |
| Service | 实体名 + `Service` | `UserService` |
| ServiceImpl | 实体名 + `ServiceImpl` | `UserServiceImpl` |
| Mapper | 实体名 + `Mapper` | `SysUserMapper` |
| Controller | 实体名 + `Controller` | `UserController` |

### 常用注解

```java
// Controller
@Tag(name = "模块名称")
@RestController
@RequestMapping("/api/module/resource")
@RequiredArgsConstructor

// 权限
@RequiresPermission("system:user:list")  // 权限校验
@OperationLog(title = "用户管理", businessType = BusinessType.INSERT)  // 操作日志

// 实体
@Table("table_name")  // MyBatis-Flex 表映射
@Id(keyType = KeyType.Auto)  // 主键自增
@Column(isLogicDelete = true)  // 逻辑删除
```

## 常用命令

### 后端
```bash
# 开发启动 (从项目根目录执行)
./run-dev.sh              # 启动后端 (默认先终止已运行进程)
./run-dev.sh --no-kill    # 不终止进程直接启动
./run-dev.sh -f           # 同时启动前端和后端
./run-dev.sh -s           # 跳过编译直接启动
./run-dev.sh --help       # 查看帮助

# 或手动操作
cd backend
mvn compile                    # 编译
mvn compile -DskipTests        # 编译并跳过测试
mvn spring-boot:run -pl swiftadmin-boot  # 运行
mvn package -DskipTests        # 打包
```

### 环境配置
```bash
# 复制示例文件创建自己的配置
cp backend/.env.local.example backend/.env.local

# 编辑配置 (数据库、Redis、OSS 等)
vim backend/.env.local
```

### 前端
```bash
cd frontend/apps/admin-web

# 安装依赖
pnpm install

# 开发模式
pnpm dev

# 类型检查
pnpm typecheck

# 构建
pnpm build
```

### 数据库
```bash
# Flyway 迁移 (如果启用)
mvn flyway:migrate

# 数据库结构文件位置 (按模块组织，一个模块一个文件)
backend/swiftadmin-boot/src/main/resources/db/migration/
├── V1__system.sql    # 系统模块 (用户、角色、菜单、部门、字典、配置、日志、文件)
└── V2__message.sql   # 消息模块 (渠道、模板、记录、订阅、站内信)
```

## API 规范

### 响应格式

```typescript
interface R<T> {
  code: number      // 0=成功，其他=失败
  message: string
  data: T
  traceId: string
  timestamp: number
}
```

### RESTful 路由

| 操作 | 方法 | 路径 | 示例 |
|------|------|------|------|
| 分页查询 | GET | `/api/module/resource/page` | `GET /api/system/user/page` |
| 详情 | GET | `/api/module/resource/{id}` | `GET /api/system/user/1` |
| 列表 | GET | `/api/module/resource/list` | `GET /api/system/role/list` |
| 新增 | POST | `/api/module/resource` | `POST /api/system/user` |
| 更新 | PUT | `/api/module/resource` | `PUT /api/system/user` |
| 删除 | DELETE | `/api/module/resource/{ids}` | `DELETE /api/system/user/1,2,3` |

### 权限标识格式

```
模块:资源:操作
system:user:list    # 用户列表
system:user:add     # 用户新增
system:user:edit    # 用户编辑
system:user:delete  # 用户删除
system:user:query   # 用户详情
```

## 前端开发规范

### API 文件结构

```typescript
// src/api/user.ts
import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types'

export interface UserVO { ... }
export interface UserDTO { ... }
export interface UserQuery { ... }

export function getUserPageApi(params: UserQuery) {
  return get<PageResult<UserVO>>('/system/user/page', params)
}

export function createUserApi(data: UserDTO) {
  return post<number>('/system/user', data)
}
```

### 页面组件结构

```vue
<template>
  <NCard title="模块名称">
    <!-- 搜索栏 -->
    <NForm :model="queryForm" inline>...</NForm>

    <!-- 操作按钮 -->
    <NSpace>
      <NButton v-auth="'module:resource:add'" @click="handleAdd">新增</NButton>
    </NSpace>

    <!-- 数据表格 -->
    <NDataTable :columns="columns" :data="tableData" />

    <!-- 表单弹窗 -->
    <NModal v-model:show="showModal">...</NModal>
  </NCard>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import { ...Api } from '@/api/resource'

// 状态
const loading = ref(false)
const tableData = ref([])

// 方法
const loadData = async () => { ... }

// 生命周期
onMounted(() => loadData())
</script>
```

## 注意事项

1. **修改现有功能时**：先阅读相关代码，理解设计意图再动手
2. **新增模块时**：遵循现有目录结构和命名规范
3. **数据库变更**：
   - 新增模块：创建 `V{N}__{module}.sql`，包含表结构和初始化数据
   - 修改表结构：新增迁移文件 `V{N+1}__{description}.sql`
   - 已执行过旧迁移的数据库需清空 `flyway_schema_history` 表后重新迁移
4. **权限配置**：新功能需要添加对应的菜单和权限数据
5. **前后端同步**：API 变更需同时更新前端 TypeScript 类型