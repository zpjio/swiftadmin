# SwiftAdmin Web 前端项目

**创建时间：** 2026-03-11  
**技术栈：** Vue 3 + Vite + Naive UI + TypeScript  
**后端接口：** http://localhost:8080

---

## 项目结构

```
swiftadmin-web/
├── src/
│   ├── api/              # API 接口定义
│   │   ├── auth.ts       # 认证接口
│   │   ├── user.ts       # 用户管理
│   │   ├── role.ts       # 角色管理
│   │   ├── menu.ts       # 菜单管理
│   │   ├── dept.ts       # 部门管理
│   │   └── online.ts     # 在线用户
│   ├── layout/           # 布局组件
│   ├── router/           # 路由配置
│   ├── store/            # Pinia 状态管理
│   ├── utils/            # 工具函数
│   ├── types/            # TypeScript 类型
│   └── views/            # 页面视图
│       ├── auth/         # 认证页面
│       ├── dashboard/    # 工作台
│       ├── system/       # 系统管理
│       └── monitor/      # 系统监控
├── docs/                 # 文档
└── README.md
```

---

## ✅ 已完成功能

### 1. 基础框架
- [x] Vue 3 + Vite + TypeScript 项目初始化
- [x] Naive UI 组件库集成（自动导入）
- [x] Pinia 状态管理
- [x] Vue Router 路由配置（带守卫）
- [x] Axios 请求封装（拦截器、错误处理）
- [x] TailwindCSS 样式工具
- [x] 响应式布局支持

### 2. 认证模块
- [x] 登录页面（账号密码登录）
- [x] Token 管理（localStorage 持久化）
- [x] 自动登录（Token 恢复）
- [x] 401 自动跳转登录
- [x] 登出功能

**对接接口：**
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/logout` - 用户登出
- `POST /api/auth/refresh` - 刷新 Token
- `POST /api/auth/userinfo` - 获取用户信息

### 3. 用户管理
- [x] 用户列表（分页、搜索、筛选）
- [x] 新增用户（表单验证）
- [x] 编辑用户
- [x] 删除用户（批量删除）
- [x] 重置密码
- [x] 更新用户状态
- [x] 分配角色

**对接接口：**
- `GET /api/system/user/page` - 分页查询
- `GET /api/system/user/{id}` - 用户详情
- `POST /api/system/user` - 创建用户
- `PUT /api/system/user` - 更新用户
- `DELETE /api/system/user/{ids}` - 批量删除
- `PUT /api/system/user/status` - 更新状态
- `PUT /api/system/user/reset-password` - 重置密码
- `PUT /api/system/user/role` - 分配角色

### 4. 角色管理
- [x] 角色列表（分页、搜索、筛选）
- [x] 新增角色
- [x] 编辑角色
- [x] 删除角色（批量删除）
- [x] 菜单权限分配（树形选择）
- [x] 数据范围设置

**对接接口：**
- `GET /api/system/role/page` - 分页查询
- `GET /api/system/role/{id}` - 角色详情
- `GET /api/system/role/list` - 角色列表（下拉）
- `POST /api/system/role` - 创建角色
- `PUT /api/system/role` - 更新角色
- `DELETE /api/system/role/{ids}` - 批量删除
- `PUT /api/system/role/menu` - 分配菜单权限

### 5. 菜单管理
- [x] 菜单树形展示
- [x] 新增菜单（目录/菜单/按钮）
- [x] 编辑菜单
- [x] 删除菜单
- [x] 菜单类型选择
- [x] 权限标识配置
- [x] 图标配置

**对接接口：**
- `GET /api/system/menu/tree` - 菜单树
- `GET /api/system/menu/{id}` - 菜单详情
- `GET /api/system/menu/select-tree` - 菜单下拉树
- `GET /api/system/menu/user` - 当前用户菜单
- `POST /api/system/menu` - 创建菜单
- `PUT /api/system/menu` - 更新菜单
- `DELETE /api/system/menu/{id}` - 删除菜单

### 6. 部门管理
- [x] 部门树形展示
- [x] 新增部门
- [x] 编辑部门
- [x] 删除部门
- [x] 负责人、联系方式配置

**对接接口：**
- `GET /api/system/dept/tree` - 部门树
- `GET /api/system/dept/{id}` - 部门详情
- `POST /api/system/dept` - 创建部门
- `PUT /api/system/dept` - 更新部门
- `DELETE /api/system/dept/{id}` - 删除部门

### 7. 在线用户
- [x] 在线用户列表
- [x] 搜索（用户名、IP）
- [x] 强制用户下线
- [x] 批量强退

**对接接口：**
- `GET /api/system/online/page` - 分页查询
- `GET /api/system/online/list` - 所有在线用户
- `GET /api/system/online/count` - 在线数量
- `DELETE /api/system/online/{sessionId}` - 强退
- `POST /api/system/online/batch` - 批量强退

### 8. 系统监控
- [ ] 操作日志（占位）
- [ ] 审计日志（待实现）
- [ ] 系统配置（待实现）

---

## 🚀 快速启动

```bash
# 进入项目目录
cd /Users/zhangpengju/MyProjects/swiftadmin-web

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 访问地址
# http://localhost:5173
```

**默认账号：**
- 用户名：`admin`
- 密码：`123456`

---

## 📦 构建部署

```bash
# 构建生产版本
npm run build

# 预览生产构建
npm run preview

# 输出目录：dist/
```

---

## 🔧 环境配置

### 开发环境 (`.env.development`)
```env
VITE_APP_TITLE=SwiftAdmin
VITE_API_BASE_URL=http://localhost:8080
VITE_PORT=5173
```

### 生产环境 (`.env.production`)
```env
VITE_APP_TITLE=SwiftAdmin
VITE_API_BASE_URL=https://api.swiftadmin.com
VITE_PORT=5173
```

---

## 📚 API 文档

详细接口文档见：`docs/API_INTEGRATION.md`

### 统一响应格式
```json
{
  "code": 0,
  "message": "操作成功",
  "data": {},
  "traceId": "xxx",
  "timestamp": 1234567890
}
```

### 认证方式
```
Authorization: Bearer <accessToken>
```

---

## 🎨 技术选型

| 类别 | 技术 | 版本 |
|------|------|------|
| 框架 | Vue 3 | 3.4+ |
| 构建工具 | Vite | 5.x |
| UI 组件库 | Naive UI | 2.x |
| 状态管理 | Pinia | 2.x |
| 路由 | Vue Router | 4.x |
| HTTP | Axios | 1.x |
| 样式 | TailwindCSS | 3.x |
| 图表 | ECharts | 5.x |
| 图标 | Iconify | 最新 |
| 语言 | TypeScript | 5.x |

---

## 📋 待办事项

### 高优先级
- [ ] 动态路由（根据用户权限生成菜单）
- [ ] 按钮级权限控制指令
- [ ] 操作日志页面实现
- [ ] 系统配置页面实现
- [ ] 字典管理页面实现
- [ ] 文件管理页面实现

### 中优先级
- [ ] 个人中心页面
- [ ] 密码修改功能
- [ ] 主题切换（深色模式）
- [ ] 多标签页支持
- [ ] 页面缓存

### 低优先级
- [ ] 国际化支持
- [ ] 打印功能
- [ ] 数据导出（Excel、PDF）
- [ ] 富文本编辑器
- [ ] 图表大屏

---

## 📝 开发规范

### 代码风格
- 使用 TypeScript 严格模式
- 组合式 API (setup 语法糖)
- 组件名使用 PascalCase
- 文件命名：组件用 PascalCase，工具用 camelCase

### Git 提交规范
```
feat: 新功能
fix: 修复 bug
docs: 文档更新
style: 代码格式调整
refactor: 重构代码
test: 测试相关
chore: 构建/工具链相关
```

---

## 📄 许可证

MIT License

---

**负责人：** 小 A  
**最后更新：** 2026-03-11
