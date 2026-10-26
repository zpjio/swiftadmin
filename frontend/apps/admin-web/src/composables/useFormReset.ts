import { reactive, type UnwrapNestedRefs } from 'vue'

/**
 * 表单重置组合式函数
 *
 * @example
 * ```ts
 * const { formData, resetForm } = useFormReset({
 *   name: '',
 *   age: 0,
 *   status: 1,
 * })
 *
 * // 重置表单到初始值
 * resetForm()
 *
 * // 重置并设置新值
 * resetForm({ name: 'test' })
 * ```
 */
export function useFormReset<T extends Record<string, any>>(defaultValues: T) {
  const _defaultValues = { ...defaultValues }
  const formData = reactive<T>({ ...defaultValues } as T)

  /**
   * 重置表单到默认值
   * @param newValues 可选，重置后覆盖的值
   */
  function resetForm(newValues?: Partial<T>) {
    // 先重置所有字段到默认值
    Object.keys(_defaultValues).forEach((key) => {
      ;(formData as any)[key] = _defaultValues[key]
    })
    // 清除不在默认值中的字段（如动态添加的 id）
    Object.keys(formData).forEach((key) => {
      if (!(key in _defaultValues)) {
        delete (formData as any)[key]
      }
    })
    // 应用新值
    if (newValues) {
      Object.assign(formData, newValues)
    }
  }

  /**
   * 设置表单值
   */
  function setForm(values: Partial<T> & Record<string, any>) {
    Object.assign(formData, values)
  }

  /**
   * 更新默认值（影响后续 resetForm 的行为）
   */
  function updateDefaults(newDefaults: Partial<T>) {
    Object.assign(_defaultValues, newDefaults)
  }

  return {
    formData: formData as UnwrapNestedRefs<T>,
    resetForm,
    setForm,
    updateDefaults
  }
}
