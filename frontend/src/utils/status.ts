import type {
  AfterSaleStatus,
  BountyStatus,
  IntentStatus,
  ItemCondition,
  ItemStatus,
  NotificationType,
  NotesTag,
  PlanStatus,
  TradeStatus
} from '@/types'

export const conditionLabels: Record<ItemCondition, string> = {
  new: '全新',
  like_new: '九成新',
  good: '七成新',
  fair: '五成新',
  poor: '三成新'
}

export const conditionOptions = Object.entries(conditionLabels).map(([value, label]) => ({
  value: value as ItemCondition,
  label
}))

export const sourceLabels = {
  platform: '平台内购买',
  offline: '线下获得'
}

export const notesLabels: Record<NotesTag, string> = {
  with_notes: '含笔记',
  with_answers: '含答案',
  no_notes: '无笔记'
}

export const itemStatusLabels: Record<ItemStatus, string> = {
  pending: '待审核',
  on_sale: '在售',
  locked: '已锁定',
  completed: '已完成',
  rejected: '已驳回',
  off_shelf: '已下架',
  frozen: '已冻结'
}

export const itemStatusTypes: Record<ItemStatus, 'success' | 'warning' | 'info' | 'danger' | 'primary'> = {
  pending: 'warning',
  on_sale: 'success',
  locked: 'primary',
  completed: 'info',
  rejected: 'danger',
  off_shelf: 'info',
  frozen: 'danger'
}

export const tradeStatusLabels: Record<TradeStatus, string> = {
  pending_payment: '待支付',
  pending_delivery: '待交付',
  pending_confirm: '待确认',
  completed: '已完成',
  cancelled: '已取消'
}

export const tradeStatusTypes: Record<TradeStatus, 'success' | 'warning' | 'info' | 'danger' | 'primary'> = {
  pending_payment: 'warning',
  pending_delivery: 'primary',
  pending_confirm: 'warning',
  completed: 'success',
  cancelled: 'info'
}

export const afterSaleLabels: Record<AfterSaleStatus, string> = {
  none: '',
  refunding: '退款中',
  arbitrating: '仲裁中',
  refunded: '已退款'
}

export const bountyStatusLabels: Record<BountyStatus, string> = {
  open: '悬赏中',
  matched: '已匹配',
  closed: '已关闭',
  expired: '已过期'
}

export const intentStatusLabels: Record<IntentStatus, string> = {
  active: '有效',
  matched: '已匹配',
  cancelled: '已取消'
}

export const planStatusLabels: Record<PlanStatus, string> = {
  pending: '待确认',
  active: '已生效',
  void: '已作废',
  completed: '已完成'
}

export const notificationTypeLabels: Record<NotificationType, string> = {
  verification: '认证结果',
  audit: '审核结果',
  bounty_match: '悬赏命中',
  exchange_invite: '置换邀请',
  aftersale_result: '售后结果',
  arbitration_accepted: '仲裁受理',
  arbitration_result: '仲裁结果',
  order_completed: '订单完成'
}

export const notificationTypeColors: Record<NotificationType, string> = {
  verification: 'success',
  audit: 'warning',
  bounty_match: 'primary',
  exchange_invite: 'primary',
  aftersale_result: 'info',
  arbitration_accepted: 'danger',
  arbitration_result: 'danger',
  order_completed: 'success'
}

export const deliveryMethodLabels = {
  meetup: '当面交易',
  pickup: '自提点自提'
}

export const coverGradients = [
  'radial-gradient(120% 120% at 22% 12%, #455a49 0%, #26342a 72%)',
  'radial-gradient(120% 120% at 20% 10%, #54493a 0%, #302a22 72%)',
  'radial-gradient(120% 120% at 22% 12%, #3d4a4e 0%, #242c2f 72%)',
  'radial-gradient(120% 120% at 20% 12%, #57453b 0%, #332923 72%)',
  'radial-gradient(120% 120% at 22% 12%, #474a3b 0%, #2a2c24 72%)',
  'radial-gradient(120% 120% at 22% 12%, #3a3f3b 0%, #232624 72%)'
]

export function coverGradient(seed: number): string {
  return coverGradients[Math.abs(seed) % coverGradients.length]
}
