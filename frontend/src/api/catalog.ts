import type { Category, PickupPoint, PublicStats } from '@/types'
import { http } from './client'

export const catalogApi = {
  categories() {
    return http.get<Category[]>('/categories')
  },
  pickupPoints() {
    return http.get<PickupPoint[]>('/pickup-points')
  },
  publicStats() {
    return http.get<PublicStats>('/public/stats')
  }
}
