import { get, post, put, del } from '@/utils/request'

/**
 * 渠道类型
 */
export type ChannelType = 'sms' | 'email' | 'inbox' | 'wecom' | 'dingtalk' | 'feishu' | 'webhook'

/**
 * 消息状态
 */
export type MessageStatus = 'pending' | 'sending' | 'success' | 'failed'

/**
 * 消息渠道
 */
export interface MessageChannel {
  id: number
  channelName: string
  channelType: ChannelType
  channelCode: string
  config: string
  status: number
  priority: number
  remark?: string
  createTime?: string
  updateTime?: string
}

/**
 * 消息模板
 */
export interface MessageTemplate {
  id: number
  templateName: string
  templateCode: string
  channelType: ChannelType
  subject?: string
  content: string
  variables?: string
  status: number
  remark?: string
  createTime?: string
  updateTime?: string
}

/**
 * 发送消息参数
 */
export interface SendMessageParams {
  to: string[]
  channelType?: ChannelType
  templateCode?: string
  subject?: string
  content?: string
  variables?: Record<string, any>
  bizType?: string
  bizId?: string
}

/**
 * 发送结果
 */
export interface SendResult {
  success: boolean
  messageId?: string
  toAddress: string
  errorMsg?: string
  sendTime?: string
}

/**
 * 消息发送记录
 */
export interface MessageRecord {
  id: number
  messageId: string
  channelType: ChannelType
  channelId?: number
  templateId?: number
  templateCode?: string
  toAddress: string
  subject?: string
  content?: string
  variables?: string
  bizType?: string
  bizId?: string
  status: MessageStatus
  errorMsg?: string
  retryCount: number
  sendTime?: string
  arriveTime?: string
  costTime?: number
  createTime: string
}

/**
 * 站内信
 */
export interface InboxMessage {
  id: number
  messageId: string
  title: string
  content: string
  msgType?: string
  priority: number
  isRead: number
  readTime?: string
  isStar: number
  senderId?: number
  senderName?: string
  expireTime?: string
  createTime: string
}

/**
 * 未读消息统计
 */
export interface UnreadCount {
  total: number
  byType: Record<string, number>
}

// ==================== 消息发送 ====================

/**
 * 发送消息
 */
export function sendMessageApi(data: SendMessageParams) {
  return post<SendResult[]>('/message/send', data)
}

/**
 * 批量发送消息
 */
export function batchSendMessageApi(data: SendMessageParams[]) {
  return post<SendResult[]>('/message/batch-send', { messages: data })
}

// ==================== 发送记录 ====================

/**
 * 分页查询发送记录
 */
export function getMessageRecordPageApi(params: {
  page: number
  pageSize: number
  messageId?: string
  channelType?: ChannelType
  toAddress?: string
  status?: MessageStatus
  bizType?: string
  startTime?: string
  endTime?: string
}) {
  return get<{ records: MessageRecord[]; totalRow: number }>('/message/record/page', params)
}

/**
 * 获取发送记录详情
 */
export function getMessageRecordDetailApi(id: number) {
  return get<MessageRecord>(`/message/record/${id}`)
}

/**
 * 重试发送
 */
export function retryMessageApi(id: number) {
  return post<SendResult>(`/message/record/retry/${id}`)
}

// ==================== 消息模板 ====================

/**
 * 分页查询模板
 */
export function getTemplatePageApi(params: {
  page: number
  pageSize: number
  templateName?: string
  templateCode?: string
  channelType?: ChannelType
  status?: number
}) {
  return get<{ records: MessageTemplate[]; totalRow: number }>('/message/template/page', params)
}

/**
 * 获取模板详情
 */
export function getTemplateDetailApi(id: number) {
  return get<MessageTemplate>(`/message/template/${id}`)
}

/**
 * 获取渠道模板列表
 */
export function getTemplateByChannelApi(channelType: ChannelType) {
  return get<MessageTemplate[]>(`/message/template/channel/${channelType}`)
}

/**
 * 创建模板
 */
export function createTemplateApi(data: Partial<MessageTemplate>) {
  return post<number>('/message/template', data)
}

/**
 * 更新模板
 */
export function updateTemplateApi(data: Partial<MessageTemplate>) {
  return put<void>('/message/template', data)
}

/**
 * 删除模板
 */
export function deleteTemplateApi(ids: number[]) {
  return del<void>(`/message/template/${ids.join(',')}`)
}

/**
 * 更新模板状态
 */
export function updateTemplateStatusApi(id: number, status: number) {
  return put<void>(`/message/template/${id}/status/${status}`)
}

// ==================== 消息渠道 ====================

/**
 * 分页查询渠道
 */
export function getChannelPageApi(params: {
  page: number
  pageSize: number
  channelName?: string
  channelType?: ChannelType
  status?: number
}) {
  return get<{ records: MessageChannel[]; totalRow: number }>('/message/channel/page', params)
}

/**
 * 获取渠道详情
 */
export function getChannelDetailApi(id: number) {
  return get<MessageChannel>(`/message/channel/${id}`)
}

/**
 * 创建渠道
 */
export function createChannelApi(data: Partial<MessageChannel>) {
  return post<number>('/message/channel', data)
}

/**
 * 更新渠道
 */
export function updateChannelApi(data: Partial<MessageChannel>) {
  return put<void>('/message/channel', data)
}

/**
 * 删除渠道
 */
export function deleteChannelApi(ids: number[]) {
  return del<void>(`/message/channel/${ids.join(',')}`)
}

/**
 * 更新渠道状态
 */
export function updateChannelStatusApi(id: number, status: number) {
  return put<void>(`/message/channel/${id}/status/${status}`)
}

/**
 * 测试渠道连接
 */
export function testChannelApi(id: number) {
  return post<boolean>(`/message/channel/test/${id}`)
}

// ==================== 站内信 ====================

/**
 * 分页查询站内信
 */
export function getInboxPageApi(params: {
  page: number
  pageSize: number
  isRead?: number
  msgType?: string
  isStar?: number
  keyword?: string
}) {
  return get<{ records: InboxMessage[]; totalRow: number }>('/message/inbox/page', params)
}

/**
 * 获取站内信详情
 */
export function getInboxDetailApi(id: number) {
  return get<InboxMessage>(`/message/inbox/${id}`)
}

/**
 * 获取未读消息数量
 */
export function getUnreadCountApi() {
  return get<UnreadCount>('/message/inbox/unread-count')
}

/**
 * 发送站内信
 */
export function sendInboxApi(data: {
  userIds: string
  title: string
  content: string
  msgType?: string
  priority?: number
  senderName?: string
  expireTime?: string
}) {
  return post<number>('/message/inbox/send', data)
}

/**
 * 标记消息已读
 */
export function markInboxReadApi(ids: number[]) {
  return put<void>('/message/inbox/read', ids)
}

/**
 * 标记全部已读
 */
export function markAllInboxReadApi() {
  return put<void>('/message/inbox/read-all')
}

/**
 * 设置星标
 */
export function setInboxStarApi(id: number, isStar: boolean) {
  return put<void>(`/message/inbox/${id}/star/${isStar}`)
}

/**
 * 删除站内信
 */
export function deleteInboxApi(ids: number[]) {
  return del<void>(`/message/inbox/${ids.join(',')}`)
}

// ==================== 消息订阅 ====================

/**
 * 消息订阅
 */
export interface MessageSubscription {
  id: number
  msgType: string
  channelType: ChannelType
  subscribed: number
  createTime?: string
  updateTime?: string
}

/**
 * 获取订阅列表
 */
export function getSubscriptionListApi() {
  return get<MessageSubscription[]>('/message/subscription/list')
}

/**
 * 更新订阅状态
 */
export function updateSubscriptionApi(data: Partial<MessageSubscription>) {
  return put<void>('/message/subscription', data)
}

/**
 * 批量更新订阅状态
 */
export function batchUpdateSubscriptionApi(data: Partial<MessageSubscription>[]) {
  return put<void>('/message/subscription/batch', data)
}