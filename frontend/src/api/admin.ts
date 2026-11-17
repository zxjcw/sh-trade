import type { Category, PickupPoint, RosterEntry, User } from '@/types'
import { http } from './client'

export const adminApi = {
  roster(keyword = '') {
    return http.get<RosterEntry[]>('/admin/roster', keyword ? { keyword } : undefined)
  },
  importRoster(rows: RosterEntry[]) {
    return http.post<{ count: number }>('/admin/roster/import', { rows })
  },
  categories() {
    return http.get<Category[]>('/admin/categories')
  },
  createCategory(payload: { name: string; parentId: number | null }) {
    return http.post<Category>('/admin/categories', payload)
  },
  updateCategory(id: number, payload: { name?: string; enabled?: boolean }) {
    return http.put<Category>(`/admin/categories/${id}`, payload)
  },
  deleteCategory(id: number) {
    return http.delete<null>(`/admin/categories/${id}`)
  },
  pickupPoints() {
    return http.get<PickupPoint[]>('/admin/pickup-points')
  },
  createPickupPoint(payload: { name: string; address: string }) {
    return http.post<PickupPoint>('/admin/pickup-points', payload)
  },
  updatePickupPoint(id: number, payload: { name?: string; address?: string }) {
    return http.put<PickupPoint>(`/admin/pickup-points/${id}`, payload)
  },
  deletePickupPoint(id: number) {
    return http.delete<null>(`/admin/pickup-points/${id}`)
  },
  operators() {
    return http.get<User[]>('/admin/operators')
  },
  createOperator(payload: { username: string; nickname: string; password: string }) {
    return http.post<User>('/admin/operators', payload)
  }
}
