import type { DeliveryMethod, Order, Review } from '@/types'
import { http } from './client'

export interface CreateOrderPayload {
  itemId: number
  deliveryMethod: DeliveryMethod
  pickupPointId?: number
}

export interface ReviewPayload {
  rating: number
  tags: string[]
  content: string
}

export interface AftersalePayload {
  reason: string
  evidence?: string
}

export const tradeApi = {
  orders(params: { role?: 'buyer' | 'seller' | 'all'; status?: string } = {}) {
    return http.get<Order[]>('/orders', params as Record<string, unknown>)
  },
  order(id: string) {
    return http.get<Order>(`/orders/${id}`)
  },
  create(payload: CreateOrderPayload) {
    return http.post<Order>('/orders', payload)
  },
  pay(id: string) {
    return http.post<Order>(`/orders/${id}/pay`)
  },
  cancel(id: string) {
    return http.post<Order>(`/orders/${id}/cancel`)
  },
  confirm(id: string) {
    return http.post<Order>(`/orders/${id}/confirm`)
  },
  review(id: string, payload: ReviewPayload) {
    return http.post<Review>(`/orders/${id}/review`, payload)
  },
  applyAftersale(id: string, payload: AftersalePayload) {
    return http.post<Order>(`/orders/${id}/aftersale`, payload)
  }
}
