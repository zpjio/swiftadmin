<template>
  <NForm ref="formRef" :model="modelValue" :rules="rules" label-placement="left" label-width="100">
    <NFormItem label="菜单类型" path="type">
      <NRadioGroup v-model:value="modelValue.type">
        <NRadio :value="1">目录</NRadio>
        <NRadio :value="2">菜单</NRadio>
        <NRadio :value="3">按钮</NRadio>
      </NRadioGroup>
    </NFormItem>
    <NFormItem label="上级菜单" path="parentId">
      <NTreeSelect
        v-model:value="modelValue.parentId"
        :options="menuOptions"
        placeholder="请选择上级菜单"
        clearable
        default-expand-all
      />
    </NFormItem>
    <NFormItem label="菜单名称" path="name">
      <NInput v-model:value="modelValue.name" placeholder="请输入菜单名称" />
    </NFormItem>
    <NFormItem v-if="modelValue.type !== 3" label="路由地址" path="path">
      <NInput v-model:value="modelValue.path" placeholder="请输入路由地址，如：/system/user" />
    </NFormItem>
    <NFormItem v-if="modelValue.type === 2" label="组件路径" path="component">
      <NInput
        v-model:value="modelValue.component"
        placeholder="请输入组件路径，如：system/user/index"
      />
    </NFormItem>
    <NFormItem v-if="modelValue.type === 3" label="权限标识" path="permission">
      <NInput
        v-model:value="modelValue.permission"
        placeholder="请输入权限标识，如：system:user:add"
      />
    </NFormItem>
    <NFormItem v-if="modelValue.type !== 3" label="菜单图标" path="icon">
      <div class="flex items-center gap-2">
        <IconSelect v-model="modelValue.icon" />
        <NInput
          v-model:value="modelValue.icon"
          placeholder="或手动输入，如：mdi-account"
          style="width: 200px"
        />
      </div>
    </NFormItem>
    <NFormItem label="排序" path="sortOrder">
      <NInputNumber v-model:value="modelValue.sortOrder" :min="0" />
    </NFormItem>
    <NFormItem label="状态" path="status">
      <NRadioGroup v-model:value="modelValue.status">
        <NRadio :value="1">正常</NRadio>
        <NRadio :value="0">禁用</NRadio>
      </NRadioGroup>
    </NFormItem>
    <NFormItem v-if="modelValue.type !== 3" label="是否外链" path="isExternal">
      <NSwitch v-model:value="modelValue.isExternal" />
    </NFormItem>
    <NFormItem v-if="modelValue.type === 2" label="是否缓存" path="isCache">
      <NSwitch v-model:value="modelValue.isCache" />
    </NFormItem>
    <NFormItem v-if="modelValue.type !== 3" label="是否显示" path="isVisible">
      <NSwitch v-model:value="modelValue.isVisible" />
    </NFormItem>
  </NForm>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { FormInst, FormRules, TreeSelectOption } from 'naive-ui'
import type { MenuDTO } from '@/api/menu'
import IconSelect from '@/components/IconSelect.vue'

interface MenuFormData extends MenuDTO {
  isExternal?: boolean
  isCache?: boolean
  isVisible?: boolean
}

const modelValue = defineModel<MenuFormData>({ required: true })

defineProps<{
  menuOptions: TreeSelectOption[]
}>()

const formRef = ref<FormInst | null>(null)
const rules: FormRules = {
  name: { required: true, message: '请输入菜单名称', trigger: 'blur' },
  type: [
    {
      required: true,
      validator: (_rule, value) => {
        if (value === undefined || value === null || value === '') {
          return new Error('请选择菜单类型')
        }
        return true
      },
      trigger: 'change'
    }
  ]
}

// 暴露验证方法给父组件
defineExpose({
  validate: () => formRef.value?.validate(),
  restoreValidation: () => formRef.value?.restoreValidation()
})
</script>
