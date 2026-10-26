const messageMap = new Map<number, string>([
  [0, '操作成功'],
  [400, '请求参数错误'],
  [401, '未授权，请重新登录'],
  [403, '拒绝访问'],
  [404, '请求地址不存在'],
  [405, '请求方法错误'],
  [408, '请求超时'],
  [500, '服务器内部错误'],
  [501, '服务未实现'],
  [502, '网关错误'],
  [503, '服务不可用'],
  [504, '网关超时']
])

export function showMessage(code: number, customMessage?: string) {
  // 优先使用自定义消息
  if (customMessage) {
    return customMessage
  }
  const msg = messageMap.get(code) || '接口请求失败'
  return msg
}
