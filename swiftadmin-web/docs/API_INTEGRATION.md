# SwiftAdmin Web 接口对接文档

**更新时间：** 2026-03-11  
**后端地址：** http://localhost:8080  
**前端地址：** http://localhost:5173

---

## 接口规范

### 统一响应格式

所有后端接口返回统一格式：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {},
  "traceId": "xxx",
  "timestamp": 1234567890
}
```

**响应码说明：**
- `code: 0` - 成功
- `code: 401` - 未授权，需要重新登录
- `code: 403` - 拒绝访问（权限不足）
- 其他 - 失败

### 认证方式

采用 JWT Token 认证：

```
Authorization: Bearer <accessToken>
```

---

## 已对接接口

### 1. 认证模块 (`/api/auth/*`)

#### 1.1 用户登录

**接口：** `POST /api/auth/login`

**请求参数：**
```typescript
{
  type: 'password' | 'sms' | 'wecom' | 'dingtalk' | 'feishu',
  username?: string,      // 账号密码登录时必需
  password?: string,      // 账号密码登录时必需
  phone?: string,         // 短信登录时必需
  code?: string,          // 短信验证码
  captchaId?: string,     // 图形验证码 ID
  captchaCode?: string,   // 图形验证码
}
```

**响应数据：**
```typescript
{
  accessToken: string,    // 访问 Token
  refreshToken: string,   // 刷新 Token
  expiresIn: number,      // 过期时间（秒）
  tokenType: string,      // Token 类型：Bearer
  userInfo?: {
    nickname: string,
    avatar: string,
    source: string
  },
  requirePasswordChange?: boolean,  // 是否需要强制改密
  passwordExpired?: boolean         // 密码是否过期
}
```

**前端调用：**
```typescript
import { loginApi } from '@/api/auth'

const res = await loginApi({
  type: 'password',
  username: 'admin',
  password: '123456',
})

// 保存 Token
userStore.setToken(res.data.accessToken, res.data.refreshToken)
```

---

#### 1.2 用户登出

**接口：** `POST /api/auth/logout`

**请求头：**
```
Authorization: Bearer <accessToken>
```

**响应数据：** 无

**前端调用：**
```typescript
import { logoutApi } from '@/api/auth'

await logoutApi()
userStore.logout()
```

---

#### 1.3 刷新 Token

**接口：** `POST /api/auth/refresh`

**请求头：**
```
Refresh-Token: <refreshToken>
```

**响应数据：** 同登录接口

**前端调用：**
```typescript
import { refreshTokenApi } from '@/api/auth'

const res = await refreshTokenApi(refreshToken)
userStore.setToken(res.data.accessToken, res.data.refreshToken)
```

---

#### 1.4 获取用户信息

**接口：** `POST /api/auth/userinfo`

**请求头：**
```
Authorization: Bearer <accessToken>
```

**响应数据：**
```typescript
{
  id: number,
  username: string,
  nickname: string,
  avatar: string,
  email: string,
  phone: string,
  sex: number,        // 0-未知 1-男 2-女
  status: number,     // 0-禁用 1-正常
  deptId: number,
  deptName: string,
  roles: string[],
  permissions: string[]
}
```

**前端调用：**
```typescript
import { getCurrentUserApi } from '@/api/auth'

const res = await getCurrentUserApi()
userStore.setUserInfo(res.data)
```

---

### 2. 用户管理模块 (`/api/system/user/*`)

#### 2.1 分页查询用户

**接口：** `GET /api/system/user/page`

**请求参数：**
```typescript
{
  page: number,       // 页码
  size: number,       // 每页数量
  username?: string,  // 用户名（模糊）
  phone?: string,     // 手机号
  email?: string,     // 邮箱
  status?: number,    // 状态 0-禁用 1-正常
  deptId?: number,    // 部门 ID
  roleId?: number,    // 角色 ID
}
```

**响应数据：**
```typescript
{
  records: UserVO[],
  total: number,
  size: number,
  current: number,
  pages: number
}
```

**前端调用：**
```typescript
import { getUserPageApi } from '@/api/user'

const res = await getUserPageApi({
  page: 1,
  size: 10,
  username: 'admin',
})
```

---

#### 2.2 获取用户详情

**接口：** `GET /api/system/user/{id}`

**响应数据：** `UserVO`

---

#### 2.3 创建用户

**接口：** `POST /api/system/user`

**请求参数：**
```typescript
{
  username: string,
  password: string,
  nickname: string,
  avatar?: string,
  email?: string,
  phone?: string,
  sex?: number,
  status?: number,
  deptId?: number,
  roleIds?: number[],
  remark?: string
}
```

---

#### 2.4 更新用户

**接口：** `PUT /api/system/user`

**请求参数：** 同创建用户（`id` 必需）

---

#### 2.5 批量删除用户

**接口：** `DELETE /api/system/user/{ids}`

**路径参数：** `ids` - 逗号分隔的用户 ID 列表

---

#### 2.6 更新用户状态

**接口：** `PUT /api/system/user/status`

**请求参数：**
```typescript
{
  userId: number,
  status: number
}
```

---

#### 2.7 重置用户密码

**接口：** `PUT /api/system/user/reset-password`

**请求参数：**
```typescript
{
  userId: number,
  password: string
}
```

---

#### 2.8 分配角色

**接口：** `PUT /api/system/user/role`

**请求参数：**
```typescript
{
  userId: number,
  roleIds: number[]
}
```

---

### 3. 角色管理模块 (`/api/system/role/*`)

#### 3.1 分页查询角色

**接口：** `GET /api/system/role/page`

**请求参数：**
```typescript
{
  page: number,
  size: number,
  roleName?: string,
  status?: number,
}
```

---

#### 3.2 获取角色详情

**接口：** `GET /api/system/role/{id}`

---

#### 3.3 获取角色列表（下拉）

**接口：** `GET /api/system/role/list`

**响应数据：** `RoleVO[]`

---

#### 3.4 创建角色

**接口：** `POST /api/system/role`

**请求参数：**
```typescript
{
  roleName: string,
  roleCode: string,
  sort: number,
  status: number,
  remark: string,
  menuIds?: number[]
}
```

---

#### 3.5 更新角色

**接口：** `PUT /api/system/role`

---

#### 3.6 批量删除角色

**接口：** `DELETE /api/system/role/{ids}`

---

#### 3.7 分配菜单权限

**接口：** `PUT /api/system/role/menu`

**请求参数：**
```typescript
{
  roleId: number,
  menuIds: number[]
}
```

---

### 4. 菜单管理模块 (`/api/system/menu/*`)

#### 4.1 获取菜单树

**接口：** `GET /api/system/menu/tree`

**请求参数：**
```typescript
{
  status?: number  // 可选，0-禁用 1-启用
}
```

**响应数据：** `MenuVO[]`（树形结构）

---

#### 4.2 获取菜单详情

**接口：** `GET /api/system/menu/{id}`

---

#### 4.3 创建菜单

**接口：** `POST /api/system/menu`

**请求参数：**
```typescript
{
  parentId: number,
  name: string,
  path: string,
  component?: string,
  permission?: string,
  type: number,       // 1-目录 2-菜单 3-按钮
  icon?: string,
  sort: number,
  status: number,
  visible: number
}
```

---

#### 4.4 更新菜单

**接口：** `PUT /api/system/menu`

---

#### 4.5 删除菜单

**接口：** `DELETE /api/system/menu/{id}`

---

#### 4.6 获取菜单下拉树

**接口：** `GET /api/system/menu/select-tree`

**响应数据：** `MenuTreeVO[]`（简化的树形结构，用于选择父级）

---

#### 4.7 获取当前用户菜单

**接口：** `GET /api/system/menu/user`

**请求头：** `Authorization: Bearer <accessToken>`

**响应数据：** `MenuVO[]`（当前用户可访问的菜单）

---

## 待对接接口

### 5. 部门管理 (`/api/system/dept/*`)
- [ ] 获取部门树
- [ ] 创建部门
- [ ] 更新部门
- [ ] 删除部门

### 6. 字典管理 (`/api/system/dict/*`)
- [ ] 获取字典列表
- [ ] 获取字典项
- [ ] 创建字典
- [ ] 更新字典

### 7. 文件管理 (`/api/system/file/*`)
- [ ] 文件上传
- [ ] 文件列表
- [ ] 文件删除

### 8. 操作日志 (`/api/system/oper-log/*`)
- [ ] 日志列表
- [ ] 日志详情
- [ ] 日志导出

---

## 错误处理

### HTTP 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权，需要重新登录 |
| 403 | 拒绝访问（权限不足） |
| 404 | 请求地址不存在 |
| 500 | 服务器内部错误 |

### 前端统一错误处理

在 `src/utils/request.ts` 中已配置响应拦截器：

```typescript
// 自动处理 401 未授权
if (res.code === 401) {
  userStore.logout()
  window.location.href = '/login'
}

// 显示错误消息
showMessage(res.code, res.message)
```

---

## 开发建议

### 1. Token 管理

- Token 存储在 localStorage 中
- 登出时清除 Token
- 401 错误自动跳转登录页
- 可添加 Token 刷新机制（使用 refreshToken）

### 2. 权限控制

- 路由守卫检查登录状态
- 按钮级权限使用 `v-permission` 指令（待实现）
- 菜单根据用户权限动态生成（待实现）

### 3. 请求优化

- 相同请求添加防抖/节流
- 列表页添加缓存
- 大文件上传添加进度条

---

## 测试账号

```
用户名：admin
密码：123456
```

---

**负责人：** 小 A  
**最后更新：** 2026-03-11
