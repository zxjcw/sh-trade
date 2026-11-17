import type { Arbitration, Item, ReportData } from '@/types'
import { http } from './client'

export const opsApi = {
  pendingItems() {
    return http.get<Item[]>('/ops/items/pending')
  },
  auditItem(id: number, result: 'approved' | 'rejected', reason?: string) {
    return http.post<Item>(`/ops/items/${id}/audit`, { result, reason })
  },
  arbitrations() {
    return http.get<Arbitration[]>('/ops/arbitrations')
  },
  resolveArbitration(id: number, result: 'buyer' | 'seller', note?: string) {
    return http.post<Arbitration>(`/ops/arbitrations/${id}/resolve`, { result, note })
  },
  report() {
    return http.get<ReportData>('/ops/report')
  }
}
