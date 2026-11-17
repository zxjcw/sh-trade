import type { Order } from '@/types'

const AFTER_SALE_WINDOW_MS = 5 * 60_000

export function canApplyAftersale(order: Order): boolean {
  if (order.afterSaleStatus !== 'none') return false
  if (order.tradeStatus === 'pending_payment' || order.tradeStatus === 'cancelled') return false
  if (order.tradeStatus === 'completed') {
    if (!order.completedAt) return false
    return Date.now() - new Date(order.completedAt).getTime() <= AFTER_SALE_WINDOW_MS
  }
  return true
}
