import type { CreditRecord, PaymentRecord, Review, User } from '@/types'
import { http } from './client'

export interface VerificationPayload {
  studentNo: string
  name: string
  college: string
  major: string
  grade: string
}

export const userApi = {
  verify(payload: VerificationPayload) {
    return http.post<User>('/verification', payload)
  },
  wallet() {
    return http.get<{ balance: number }>('/wallet')
  },
  recharge(amount: number) {
    return http.post<{ balance: number; record: PaymentRecord }>('/wallet/recharge', { amount })
  },
  payments() {
    return http.get<PaymentRecord[]>('/payments')
  },
  creditRecords() {
    return http.get<CreditRecord[]>('/credit/records')
  },
  reviews() {
    return http.get<Review[]>('/reviews/me')
  }
}
