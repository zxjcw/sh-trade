import type { Item, ItemCondition, NotesTag } from '@/types'
import { http } from './client'

export interface ItemQuery {
  keyword?: string
  categoryId?: number
  condition?: string
  status?: string
  sort?: 'latest' | 'price_asc' | 'price_desc'
  scope?: 'mine'
  limit?: number
}

export interface CreateItemPayload {
  title: string
  categoryId: number
  price: number
  originalPrice: number
  purchasePrice?: number
  condition: ItemCondition
  description: string
  source: 'platform' | 'offline'
  notesTag: NotesTag
  coverSeed?: number
}

export const itemApi = {
  list(params: ItemQuery = {}) {
    return http.get<Item[]>('/items', params as Record<string, unknown>)
  },
  detail(id: number) {
    return http.get<Item>(`/items/${id}`)
  },
  create(payload: CreateItemPayload) {
    return http.post<Item>('/items', payload)
  },
  relist(id: number, payload: { price: number; condition: ItemCondition; description?: string }) {
    return http.post<Item>(`/items/${id}/relist`, payload)
  },
  toggleFavorite(id: number) {
    return http.post<{ favorited: boolean }>(`/items/${id}/favorite`)
  },
  favorites() {
    return http.get<Item[]>('/favorites')
  }
}
