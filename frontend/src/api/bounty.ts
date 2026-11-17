import type { Bounty, ItemCondition } from '@/types'
import { http } from './client'

export interface CreateBountyPayload {
  categoryId: number
  keyword: string
  budget: number
  condition: ItemCondition
  expiresDays: number
}

export const bountyApi = {
  list(scope: 'hall' | 'mine' = 'hall') {
    return http.get<Bounty[]>('/bounties', { scope })
  },
  create(payload: CreateBountyPayload) {
    return http.post<Bounty>('/bounties', payload)
  },
  close(id: number) {
    return http.post<Bounty>(`/bounties/${id}/close`)
  }
}
