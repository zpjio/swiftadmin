import { ref } from 'vue'
import { getDictDataByTypeApi } from '@/api/dict'

// 字典数据缓存
const dictCache = new Map<string, { label: string; value: string }[]>()

// 正在加载的字典类型
const loadingMap = new Map<string, boolean>()

/**
 * 字典选项类型
 */
export interface DictOption {
  label: string
  value: string
}

/**
 * 获取字典数据的 composable
 * @param dictType 字典类型
 * @param immediate 是否立即加载，默认 true
 * @returns
 */
export function useDict(dictType: string, immediate = true) {
  const options = ref<DictOption[]>([])
  const loading = ref(false)

  // 加载字典数据
  const loadDict = async () => {
    // 检查缓存
    if (dictCache.has(dictType)) {
      options.value = dictCache.get(dictType) || []
      return
    }

    // 检查是否正在加载
    if (loadingMap.get(dictType)) {
      // 等待加载完成
      const checkLoaded = () => {
        return new Promise<void>((resolve) => {
          const timer = setInterval(() => {
            if (!loadingMap.get(dictType)) {
              clearInterval(timer)
              options.value = dictCache.get(dictType) || []
              resolve()
            }
          }, 50)
        })
      }
      await checkLoaded()
      return
    }

    loading.value = true
    loadingMap.set(dictType, true)

    try {
      const data = await getDictDataByTypeApi(dictType)
      const dictOptions = (data || []).map((item: any) => ({
        label: item.label,
        value: item.value,
      }))

      // 存入缓存
      dictCache.set(dictType, dictOptions)
      options.value = dictOptions
    } catch (error) {
      console.error(`加载字典 ${dictType} 失败:`, error)
      options.value = []
    } finally {
      loading.value = false
      loadingMap.set(dictType, false)
    }
  }

  // 根据 value 获取 label
  const getLabel = (value: string | number): string => {
    const option = options.value.find(item => item.value === String(value))
    return option?.label || String(value)
  }

  // 根据 label 获取 value
  const getValue = (label: string): string | undefined => {
    const option = options.value.find(item => item.label === label)
    return option?.value
  }

  // 立即加载
  if (immediate) {
    loadDict()
  }

  return {
    options,
    loading,
    loadDict,
    getLabel,
    getValue,
  }
}

/**
 * 批量加载多个字典
 * @param dictTypes 字典类型数组
 * @returns
 */
export function useDicts(dictTypes: string[]) {
  const optionsMap = ref<Record<string, DictOption[]>>({})
  const loading = ref(false)

  const loadAll = async () => {
    loading.value = true
    try {
      await Promise.all(
        dictTypes.map(async (type) => {
          const { options } = useDict(type)
          optionsMap.value[type] = options.value
        })
      )
    } finally {
      loading.value = false
    }
  }

  const getLabel = (dictType: string, value: string | number): string => {
    const options = optionsMap.value[dictType] || []
    const option = options.find(item => item.value === String(value))
    return option?.label || String(value)
  }

  return {
    optionsMap,
    loading,
    loadAll,
    getLabel,
  }
}

/**
 * 清除字典缓存
 * @param dictType 指定字典类型，不传则清除所有
 */
export function clearDictCache(dictType?: string) {
  if (dictType) {
    dictCache.delete(dictType)
  } else {
    dictCache.clear()
  }
}