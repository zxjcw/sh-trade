import type { Notification } from '@/types'
import { http } from './client'

export const notificationApi = {
  list() {
    return http.get<Notification[]>('/notifications')
  },
  read(id: number) {
    return http.post<Notification>(`/notifications/${id}/read`)
  },
  readAll() {
    return http.post<null>('/notifications/read-all')
  }
}
