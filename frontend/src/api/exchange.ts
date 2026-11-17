import type { ExchangeIntent, ExchangePlan } from '@/types'
import { http } from './client'

export interface CreateIntentPayload {
  myItemId: number
  matchType: 'specific' | 'condition'
  targetItemId?: number
  targetCategoryId?: number
  keyword?: string
}

export const exchangeApi = {
  intents() {
    return http.get<ExchangeIntent[]>('/exchange/intents')
  },
  createIntent(payload: CreateIntentPayload) {
    return http.post<ExchangeIntent>('/exchange/intents', payload)
  },
  plans() {
    return http.get<ExchangePlan[]>('/exchange/plans')
  },
  confirmPlan(id: number) {
    return http.post<ExchangePlan>(`/exchange/plans/${id}/confirm`)
  },
  rejectPlan(id: number) {
    return http.post<ExchangePlan>(`/exchange/plans/${id}/reject`)
  }
}
