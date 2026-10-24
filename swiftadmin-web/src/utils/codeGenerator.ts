import { pinyin } from 'pinyin-pro'

/**
 * 根据中文名称生成角色编码
 * @param name 角色名称
 * @param prefix 前缀，默认 'role_'
 * @returns 角色编码（小写字母、数字、下划线组成）
 */
export function generateRoleCode(name: string, prefix = 'role_'): string {
  if (!name) {
    return `${prefix}${Date.now()}`
  }

  // 转换为拼音，不带声调，用下划线连接
  const py = pinyin(name, {
    pattern: 'pinyin',    // 返回拼音
    toneType: 'none',     // 不带声调
    type: 'array',        // 返回数组
  })

  // 拼接拼音，转小写
  let code = py.join('_').toLowerCase()

  // 移除特殊字符，只保留字母、数字、下划线
  code = code.replace(/[^a-z0-9_]/g, '')

  // 添加前缀
  return `${prefix}${code}`
}

/**
 * 根据名称生成唯一编码（带时间戳防重复）
 * @param name 名称
 * @param prefix 前缀
 * @returns 唯一编码
 */
export function generateUniqueCode(name: string, prefix = 'role_'): string {
  const baseCode = generateRoleCode(name, prefix)
  // 如果名称较短，添加时间戳后缀防重复
  if (baseCode.length < 20) {
    const timestamp = Date.now().toString(36) // 36进制时间戳，更短
    return `${baseCode}_${timestamp}`
  }
  return baseCode
}