# 权限系统使用指南

## 概述

SwiftAdmin 实现了完整的 RBAC（基于角色的访问控制）权限系统，包括：

- **动态路由**：根据用户权限动态生成菜单
- **按钮权限**：控制按钮的显示/隐藏
- **权限指令**：`v-permission` 指令
- **权限函数**：`hasPermission()` 工具函数

---

## 权限标识

权限标识格式：`模块：操作：资源`

示例：
- `system:user:list` - 用户查询
- `system:user:add` - 用户新增
- `system:user:edit` - 用户修改
- `system:user:delete` - 用户删除
- `system:role:list` - 角色查询
- `*:*:*` - 超级管理员（所有权限）

---

## 动态路由

### 工作原理

1. 用户登录成功后，Token 保存到 localStorage
2. 访问需要权限的页面时，路由守卫触发
3. 调用 `/api/system/menu/user` 获取当前用户菜单
4. 将菜单树转换为 Vue Router 路由配置
5. 使用 `router.addRoute()` 动态添加路由
6. 保存权限标识到 Pinia store

### 后端返回格式

```json
[
  {
    "id": 1,
    "parentId": 0,
    "name": "系统管理",
    "path": "/system",
    "component": null,
    "type": 1,
    "icon": "setting",
    "sortOrder": 1,
    "permission": null,
    "children": [
      {
        "id": 2,
        "parentId": 1,
        "name": "用户管理",
        "path": "/system/user",
        "component": "system/user/index",
        "type": 2,
        "icon": "user",
        "sortOrder": 1,
        "permission": "system:user:list",
        "children": [
          {
            "id": 8,
            "parentId": 2,
            "name": "用户新增",
            "path": null,
            "component": null,
            "type": 3,
            "permission": "system:user:add",
            "children": []
          }
        ]
      }
    ]
  }
]
```

### 菜单类型

- `1` - 目录（Directory）：一级菜单，如"系统管理"
- `2` - 菜单（Menu）：二级菜单，可访问页面，如"用户管理"
- `3` - 按钮（Button）：权限标识，不生成路由，如"用户新增"

---

## 按钮权限控制

### 方法 1：使用 `v-permission` 指令（推荐）

```vue
<template>
  <NButton v-permission="['system:user:add']">
    <template #icon><icon-mdi-plus /></template>
    新增用户
  </NButton>
  
  <NButton v-permission="['system:user:edit', 'system:user:delete']">
    批量操作
  </NButton>
</template>
```

### 方法 2：使用 `hasPermission` 函数

```vue
<template>
  <NButton v-if="hasPermission('system:user:add')">
    新增用户
  </NButton>
  
  <NButton v-if="hasPermission('system:user:edit')">
    编辑
  </NButton>
</template>

<script setup lang="ts">
import { hasPermission } from '@/directives/permission'
</script>
```

### 方法 3：使用 `hasPermissions` 函数（多个权限）

```vue
<template>
  <!-- 满足任一权限即可显示 -->
  <NButton v-if="hasPermissions(['system:user:add', 'system:user:edit'])">
    新增或编辑
  </NButton>
  
  <!-- 必须同时满足所有权限 -->
  <NButton v-if="hasPermissions(['system:user:add', 'system:role:list'], 'and')">
    分配角色
  </NButton>
</template>

<script setup lang="ts">
import { hasPermissions } from '@/directives/permission'
</script>
```

---

## 实际示例

### 用户管理页面

```vue
<template>
  <NCard title="用户管理">
    <!-- 工具栏 -->
    <div class="toolbar">
      <!-- 新增按钮：需要 system:user:add 权限 -->
      <NButton 
        v-permission="['system:user:add']" 
        type="primary" 
        @click="handleAdd"
      >
        <template #icon><icon-mdi-plus /></template>
        新增用户
      </NButton>
      
      <!-- 批量删除：需要 system:user:delete 权限 -->
      <NButton 
        v-permission="['system:user:delete']" 
        type="error" 
        @click="handleBatchDelete"
      >
        <template #icon><icon-mdi-delete /></template>
        批量删除
      </NButton>
    </div>
    
    <!-- 表格 -->
    <NDataTable :columns="columns" :data="tableData" />
    
    <!-- 操作列 -->
    <template #action="{ row }">
      <NSpace>
        <!-- 编辑按钮：需要 system:user:edit 权限 -->
        <NButton 
          v-permission="['system:user:edit']" 
          text 
          type="primary"
          @click="handleEdit(row)"
        >
          编辑
        </NButton>
        
        <!-- 删除按钮：需要 system:user:delete 权限 -->
        <NButton 
          v-permission="['system:user:delete']" 
          text 
          type="error"
          @click="handleDelete(row)"
        >
          删除
        </NButton>
        
        <!-- 重置密码：需要 system:user:resetPwd 权限 -->
        <NButton 
          v-permission="['system:user:resetPwd']" 
          text 
          type="warning"
          @click="handleResetPassword(row)"
        >
          重置密码
        </NButton>
      </NSpace>
    </template>
  </NCard>
</template>

<script setup lang="ts">
import { h } from 'vue'
import { useMessage, useDialog, type DataTableColumns } from 'naive-ui'
import { getUserPageApi } from '@/api/user'
import { hasPermission } from '@/directives/permission'

const message = useMessage()
const dialog = useDialog()

// 表格列定义
const columns: DataTableColumns = [
  {
    title: '用户名',
    key: 'username',
  },
  {
    title: '操作',
    key: 'actions',
    render: (row) => {
      return h('div', [
        // 动态渲染操作按钮
        hasPermission('system:user:edit') && h('button', { onClick: () => handleEdit(row) }, '编辑'),
        hasPermission('system:user:delete') && h('button', { onClick: () => handleDelete(row) }, '删除'),
      ])
    },
  },
]

// 其他逻辑...
</script>
```

---

## 超级管理员

如果用户拥有 `*:*:*` 权限标识，则自动拥有所有权限，所有按钮都会显示。

后端在分配角色时，可以给管理员角色分配 `*:*:*` 权限。

---

## 注意事项

1. **权限指令会移除元素**：如果没有权限，元素会从 DOM 中完全移除，而不是隐藏
2. **权限判断在客户端**：前端权限控制仅用于 UI 展示，后端接口仍需进行权限验证
3. **刷新页面会重新加载权限**：页面刷新后，路由守卫会重新获取用户菜单和权限
4. **权限变更需要重新登录**：如果后端修改了用户权限，前端需要重新登录才能生效

---

## 相关文件

- **权限指令**：`src/directives/permission.ts`
- **路由工具**：`src/utils/router-helper.ts`
- **路由配置**：`src/router/index.ts`
- **用户 Store**：`src/store/modules/user.ts`
- **菜单 API**：`src/api/menu.ts`

---

## 测试

1. 使用 `admin` 账号登录（超级管理员，拥有所有权限）
2. 查看所有页面和按钮是否正常显示
3. 在角色管理中创建一个普通角色，只分配部分权限
4. 创建新用户并分配该角色
5. 使用该用户登录，验证菜单和按钮是否按权限显示
