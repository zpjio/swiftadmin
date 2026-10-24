import { ref, watch } from 'vue'
import { defineStore } from 'pinia'
import type { RouteLocationNormalized } from 'vue-router'

export interface TagView extends Partial<RouteLocationNormalized> {
  title?: string
  icon?: string
  closable?: boolean
}

export const useTagsStore = defineStore('tags', () => {
  // 标签页列表
  const visitedViews = ref<TagView[]>([])
  // 缓存的组件名称
  const cachedViews = ref<string[]>([])
  // 当前激活的标签页
  const activeView = ref<TagView | null>(null)

  // 添加标签页
  const addView = (view: TagView) => {
    // 如果已存在，不重复添加
    const exists = visitedViews.value.find((v) => v.path === view.path)
    if (exists) {
      // 更新现有标签页
      updateView(view)
      return
    }

    // 添加新标签页
    visitedViews.value.push({
      ...view,
      title: view.meta?.title as string,
      icon: view.meta?.icon as string,
      closable: view.name !== 'Dashboard', // 首页不可关闭
    })

    // 添加到缓存
    if (view.name && view.meta?.keepAlive) {
      cachedViews.value.push(view.name as string)
    }
  }

  // 更新标签页
  const updateView = (view: TagView) => {
    const index = visitedViews.value.findIndex((v) => v.path === view.path)
    if (index !== -1) {
      visitedViews.value[index] = {
        ...visitedViews.value[index],
        ...view,
        title: view.meta?.title as string,
        icon: view.meta?.icon as string,
      }
    }
  }

  // 关闭标签页
  const closeView = (view: TagView) => {
    const index = visitedViews.value.findIndex((v) => v.path === view.path)
    if (index === -1 || !view.closable) return

    const isCurrentActive = activeView.value?.path === view.path

    // 从访问列表中移除
    visitedViews.value.splice(index, 1)

    // 从缓存中移除
    if (view.name) {
      const cacheIndex = cachedViews.value.indexOf(view.name as string)
      if (cacheIndex > -1) {
        cachedViews.value.splice(cacheIndex, 1)
      }
    }

    // 如果关闭的是当前标签页，激活前一个标签页
    if (isCurrentActive && visitedViews.value.length > 0) {
      const newIndex = Math.min(index, visitedViews.value.length - 1)
      setActiveView(visitedViews.value[newIndex])
    }
  }

  // 关闭其他标签页
  const closeOthers = (view: TagView) => {
    visitedViews.value = visitedViews.value.filter((v) => {
      if (v.path === view.path) {
        return true
      }
      // 从缓存中移除
      if (v.name) {
        const cacheIndex = cachedViews.value.indexOf(v.name as string)
        if (cacheIndex > -1) {
          cachedViews.value.splice(cacheIndex, 1)
        }
      }
      return false
    })
  }

  // 关闭所有标签页
  const closeAll = () => {
    visitedViews.value = visitedViews.value.filter((v) => !v.closable)
    cachedViews.value = []
    
    // 激活第一个标签页（通常是首页）
    if (visitedViews.value.length > 0) {
      setActiveView(visitedViews.value[0])
    }
  }

  // 设置激活的标签页
  const setActiveView = (view: TagView) => {
    activeView.value = view
  }

  // 重置标签页
  const resetViews = () => {
    visitedViews.value = []
    cachedViews.value = []
    activeView.value = null
  }

  return {
    visitedViews,
    cachedViews,
    activeView,
    addView,
    updateView,
    closeView,
    closeOthers,
    closeAll,
    setActiveView,
    resetViews,
  }
})
